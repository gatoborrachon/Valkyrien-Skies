package org.valkyrienskies.mod.common.collision;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.joml.Vector3d;
import org.valkyrienskies.mod.common.ships.block_relocation.SpatialDetector;
import org.valkyrienskies.mod.common.util.VSIterationUtils;
import org.valkyrienskies.mod.common.util.datastructures.IBitOctree;
import org.valkyrienskies.mod.common.util.datastructures.ITerrainOctreeProvider;
import valkyrienwarfare.api.TransformType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

public class ShipCollisionTask implements Callable<Void> {

    public final static int MAX_TASKS_TO_CHECK = 45;
    private final WorldPhysicsCollider toTask;
    private final int taskStartIndex;
    private final int tasksToCheck;
    private final MutableBlockPos mutablePos;
    private final MutableBlockPos inLocalPos;
    private final Vector3d inWorld;
    private final List<CollisionInformationHolder> collisionInformationGenerated;
    private IBlockState inWorldState;
    // public TIntArrayList foundPairs = new TIntArrayList();

    public ShipCollisionTask(WorldPhysicsCollider toTask, int taskStartIndex) {
        this.taskStartIndex = taskStartIndex;
        this.toTask = toTask;
        this.mutablePos = new MutableBlockPos();
        this.inLocalPos = new MutableBlockPos();
        this.inWorld = new Vector3d();
        this.collisionInformationGenerated = new ArrayList<>();
        this.inWorldState = null;

        int size = toTask.getCachedPotentialHitSize();
        if (taskStartIndex + MAX_TASKS_TO_CHECK > size + 1) {
            tasksToCheck = size + 1 - taskStartIndex;
        } else {
            tasksToCheck = MAX_TASKS_TO_CHECK;
        }
    }

    @Override
    public Void call() {
        for (int index = taskStartIndex; index < tasksToCheck + 1; index++) {
            int integer = toTask.getCachedPotentialHit(index);
            processNumber(integer);
        }

        return null;
    }

    public List<CollisionInformationHolder> getCollisionInformationGenerated() {
        return collisionInformationGenerated;
    }

    /**
     * Returns an iterator that loops over the collision information in quasi-random order. This is
     * important to avoid biasing one side over another, because otherwise one side would slowly
     * sink into the ground.
     *
     * @return
     */
    public Iterator<CollisionInformationHolder> getCollisionInformationIterator() {
        // Collections.shuffle(collisionInformationGenerated);
        return collisionInformationGenerated.iterator();
    }

    private void processNumber(int integer) {
        SpatialDetector.setPosWithRespectTo(integer, toTask.getCenterPotentialHit(), mutablePos);
        inWorldState = toTask.getParent().getCachedSurroundingChunks().getBlockState(mutablePos);

        inWorld.x = mutablePos.getX() + .5;
        inWorld.y = mutablePos.getY() + .5;
        inWorld.z = mutablePos.getZ() + .5;

        toTask.getParent().getShipTransformationManager().getCurrentPhysicsTransform()
            .transformPosition(inWorld, TransformType.GLOBAL_TO_SUBSPACE);

        int midX = MathHelper.floor(inWorld.x + .5D);
        int midY = MathHelper.floor(inWorld.y + .5D);
        int midZ = MathHelper.floor(inWorld.z + .5D);

        // Check the 27 possible positions
        VSIterationUtils.expand3d(midX, midY, midZ, (x, y, z) -> checkPosition(x, y, z, integer));
    }

    public void checkPosition(int x, int y, int z, int positionHash) {
        if (!toTask.getParent().getChunkClaim().containsChunk(x >> 4, z >> 4)) {
            return;
        }
        final Chunk chunkIn = toTask.getParent().getChunkAt(x >> 4, z >> 4);
        y = Math.max(0, Math.min(y, 255));

        ExtendedBlockStorage storage = chunkIn.storageArrays[y >> 4];
        if (storage != null) {
            ITerrainOctreeProvider provider = (ITerrainOctreeProvider) storage.data;
            IBitOctree octree = provider.getSolidOctree();

            if (octree.get(x & 15, y & 15, z & 15)) {
                IBlockState inLocalState = chunkIn.getBlockState(x, y, z);
                // Only if you want to stop short
                // foundPairs.add(positionHash);
                // foundPairs.add(x);
                // foundPairs.add(y);
                // foundPairs.add(z);

                inLocalPos.setPos(x, y, z);

                AxisAlignedBB inLocalBB = new AxisAlignedBB(inLocalPos.getX(), inLocalPos.getY(),
                    inLocalPos.getZ(),
                    inLocalPos.getX() + 1, inLocalPos.getY() + 1, inLocalPos.getZ() + 1);
                AxisAlignedBB inGlobalBB = new AxisAlignedBB(mutablePos.getX(), mutablePos.getY(),
                    mutablePos.getZ(),
                    mutablePos.getX() + 1, mutablePos.getY() + 1, mutablePos.getZ() + 1);

                // This changes the box bounding box to the real bounding box, not sure if this
                // is better or worse for this mod
                // List<AxisAlignedBB> colBB = worldObj.getCollisionBoxes(inLocalBB);
                // inLocalBB = colBB.get(0);

                Polygon shipInWorld = new Polygon(inLocalBB,
                    toTask.getParent().getShipTransformationManager().getCurrentPhysicsTransform(),
                    TransformType.SUBSPACE_TO_GLOBAL);
                Polygon worldPoly = new Polygon(inGlobalBB);

                // TODO: Remove the normals crap
                PhysPolygonCollider collider = new PhysPolygonCollider(shipInWorld, worldPoly,
                    toTask.getParent().getShipTransformationManager().normals);

                if (!collider.seperated) {
                    // return handleActualCollision(collider, mutablePos, inLocalPos, inWorldState,
                    // inLocalState);
                    CollisionInformationHolder holder = new CollisionInformationHolder(collider,
                        mutablePos.getX(),
                        mutablePos.getY(), mutablePos.getZ(), inLocalPos.getX(), inLocalPos.getY(),
                        inLocalPos.getZ(), inWorldState, inLocalState);

                    collisionInformationGenerated.add(holder);
                }
            }
        }
    }

    public WorldPhysicsCollider getToTask() {
        return toTask;
    }

    /**
     * Quasi-Random Iterator that uses a linear congruential generator to generate the order of
     * iteration.
     *
     * @param <E>
     * @author thebest108
     */
    private static class QuasiRandomIterator<E> implements Iterator<E> {

        // Any large prime number works here.
        private static final int c = 65537;
        private final List<E> internalList;
        private final int startIndex;
        private int index;
        private boolean isFinished;

        /**
         * Creates a new quasi random iterator for the given list, the list passed must not be empty
         * otherwise an IllegalArgumentException is thrown.
         */
        QuasiRandomIterator(List<E> list) {
            if (list.size() == 0) {
                throw new IllegalArgumentException();
            }
            this.internalList = list;
            this.isFinished = false;
            // Start the index at a random value between 0 <= x < list.size()
            this.startIndex = (int) (Math.random() * list.size());
            this.index = startIndex;
        }

        @Override
        public boolean hasNext() {
            return !isFinished;
        }

        @Override
        public E next() {
            int oldIndex = index;
            advanceIndex();
            return internalList.get(oldIndex);
        }

        /**
         * Sets index to be the next value in the linear congruential generator. Also marks the
         * iterator as finished once a full period has occured.
         */
        private void advanceIndex() {
            index = (index + c) % internalList.size();
            // Stop the iterator after we've been over every element.
            if (index == startIndex) {
                isFinished = true;
            }
        }

    }

}

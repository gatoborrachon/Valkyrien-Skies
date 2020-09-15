package org.valkyrienskies.mod.common.ships;

import lombok.experimental.UtilityClass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import org.valkyrienskies.mod.common.physics.BlockPhysicsDetails;
import org.valkyrienskies.mod.common.ships.physics_data.BasicCenterOfMassProvider;
import org.valkyrienskies.mod.common.ships.physics_data.IPhysicsObjectCenterOfMassProvider;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A utility class that modifies ShipData.
 */
@ParametersAreNonnullByDefault
@UtilityClass
public class ShipDataMethods {

    // Calculates the new center of mass and inertia matrices for ships after a block change.
    private static final IPhysicsObjectCenterOfMassProvider centerOfMassProvider = new BasicCenterOfMassProvider();

    /**
     * Updates the physics data/force positions of shipData.
     */
    public void onSetBlockState(ShipData shipData, BlockPos pos, IBlockState oldState, IBlockState newState) {
        // Make sure that pos is even part of this ship
        if (!shipData.getChunkClaim().containsBlock(pos)) {
            throw new IllegalArgumentException("Get onSetBlockState() called for pos " + pos
                    + ", but this ISN'T a part of the ship " + shipData);
        }

        if (newState.equals(Blocks.AIR.getDefaultState())) {
            shipData.getBlockPositions().remove(pos);
        } else {
            shipData.getBlockPositions().add(pos);
        }

        if (BlockPhysicsDetails.isBlockProvidingForce(newState)) {
            shipData.activeForcePositions.add(pos);
        } else {
            shipData.activeForcePositions.remove(pos);
        }

        centerOfMassProvider.onSetBlockState(shipData.getInertiaData(), pos, oldState, newState);
    }
}

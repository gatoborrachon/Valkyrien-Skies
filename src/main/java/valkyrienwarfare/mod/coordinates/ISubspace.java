package valkyrienwarfare.mod.coordinates;

import javax.annotation.Nullable;

public interface ISubspace {

	/**
	 * True if this subspace has a coordinate for the given ISubspacedEntity.
	 * 
	 * @param subspaced
	 * @return
	 */
	boolean hasRecordForSubspacedEntity(ISubspacedEntity subspaced);

	/**
	 * Returns the coordinates Vector for the given ISubspacedEntity relative to
	 * this ISubSpace.
	 * 
	 * @param subspaced
	 * @return
	 */
	ISubspacedEntityRecord getRecordForSubspacedEntity(ISubspacedEntity subspaced);

	/**
	 * Creates a ISubspacedEntityRecord for the given ISubspacedEntity and stores
	 * the data with the ISubSpace.
	 * 
	 * @param subspaced
	 *            Needs to be in the global coordinate system when we take this
	 *            snapshot, otherwise this will throw an IllegalArgumentException.
	 */
	void snapshotSubspacedEntity(ISubspacedEntity subspaced);

	/**
	 * Returns GLOBAL if this subspace is the world, and SUBSPACE for PhysicsObject
	 * subspaces.
	 * 
	 * @return
	 */
	CoordinateSpaceType getSubspaceCoordinatesType();

	/**
	 * 
	 * @return Null for the world subspace.
	 */
	@Nullable
	ShipTransform getSubspaceTransform();
}

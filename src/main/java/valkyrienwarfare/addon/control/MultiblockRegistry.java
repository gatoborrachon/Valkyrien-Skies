package valkyrienwarfare.addon.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import valkyrienwarfare.addon.control.block.multiblocks.IMulitblockSchematic;

public class MultiblockRegistry {

	private static final Map<String, IMulitblockSchematic> MULTIBLOCK_ID_MAP = new HashMap<String, IMulitblockSchematic>();
	private static final Map<String, List<IMulitblockSchematic>> MULTIBLOCK_PREFIX_TO_VARIENTS = new HashMap<String, List<IMulitblockSchematic>>();
	public static final String EMPTY_SCHEMATIC_ID = "unknown";

	public static void registerSchematic(IMulitblockSchematic schematic) {
		String schematicID = schematic.getSchematicID();
		if (MULTIBLOCK_ID_MAP.containsKey(schematicID)) {
			throw new IllegalArgumentException("Duplicate entry for " + schematicID);
		}
		if (schematicID.equals(EMPTY_SCHEMATIC_ID)) {
			throw new IllegalArgumentException("The ID \"" + EMPTY_SCHEMATIC_ID + "\" is reserved!");
		}
		MULTIBLOCK_ID_MAP.put(schematicID, schematic);
	}
	
	public static IMulitblockSchematic getSchematicByID(String schematicID) {
		if (schematicID.equals(EMPTY_SCHEMATIC_ID)) {
			return null;
		}
		return MULTIBLOCK_ID_MAP.get(schematicID);
	}
	
	public static List<IMulitblockSchematic> getSchematicsWithPrefix(String schematicPrefix) {
		if (MULTIBLOCK_PREFIX_TO_VARIENTS.containsKey(schematicPrefix)) {
			return MULTIBLOCK_PREFIX_TO_VARIENTS.get(schematicPrefix);
		}
		return new ArrayList<IMulitblockSchematic>();
	}

	public static void registerAllPossibleSchematicVariants(Class<? extends IMulitblockSchematic> class1) {
		try {
			// Its a dummy, don't use it for anything that isn't dumb!
			IMulitblockSchematic dummyInstance = class1.newInstance();
			List<IMulitblockSchematic> possibilities = dummyInstance.generateAllVariants();
			for (IMulitblockSchematic schematic : possibilities) {
				registerSchematic(schematic);
			}
			if (MULTIBLOCK_PREFIX_TO_VARIENTS.containsKey(dummyInstance.getSchematicPrefix())) {
				throw new IllegalArgumentException(
						"Duplicate multiblock prefix registered!\n" + dummyInstance.getSchematicPrefix());
			}
			MULTIBLOCK_PREFIX_TO_VARIENTS.put(dummyInstance.getSchematicPrefix(),
					Collections.<IMulitblockSchematic>unmodifiableList(possibilities));
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		
	}
}

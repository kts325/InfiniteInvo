package infiniteinvo.core;

import net.minecraft.nbt.NBTTagCompound;

/**
 * A container for all the configurable settings in the mod
 */
public class II_Settings
{
	public static NBTTagCompound cachedSettings = new NBTTagCompound();
	
	public static int invoSize = 27;
	public static boolean xpUnlock = false;
	public static int unlockedSlots = 0;
	public static int unlockCost = 10;
	public static boolean keepUnlocks = false;
	public static int extraRows = 3;
	public static int extraColumns = 3;
	
	public static void SaveToCache()
	{
		cachedSettings.setInteger("invoSize", invoSize);
		cachedSettings.setBoolean("xpUnlocks", xpUnlock);
		cachedSettings.setInteger("unlockdSlots", unlockedSlots);
		cachedSettings.setInteger("unlockCost", unlockCost);
	}
	
	public static void LoadFromCache()
	{
		LoadFromTags(cachedSettings);
	}
	
	public static void LoadFromTags(NBTTagCompound tags)
	{
		invoSize = tags.getInteger("invoSize");
		xpUnlock = tags.getBoolean("xpUnlocks");
		unlockedSlots = tags.getInteger("unlockdSlots");
		unlockCost = tags.getInteger("unlockCost");
	}
}

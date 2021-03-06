package infiniteinvo.handlers;

import infiniteinvo.client.inventory.GuiBigInventory;
import infiniteinvo.core.II_Settings;
import infiniteinvo.core.InfiniteInvo;
import infiniteinvo.inventory.BigContainerPlayer;
import infiniteinvo.inventory.BigInventoryPlayer;
import infiniteinvo.network.InvoPacket;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import org.apache.logging.log4j.Level;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.WorldEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class EventHandler
{
	public static File worldDir;
	public static HashMap<String, Integer> unlockCache = new HashMap<String, Integer>();
	
	@SubscribeEvent
	public void onPlayerLoad(PlayerEvent.LoadFromFile event)
	{
		EntityPlayer player = event.entityPlayer;
		player.inventory = new BigInventoryPlayer(player);
		player.inventoryContainer = new BigContainerPlayer((BigInventoryPlayer)player.inventory, !player.worldObj.isRemote, player);
		player.openContainer = player.inventoryContainer;
		
		// Reload NBT tags from file for the new inventory replacement
        NBTTagCompound nbttagcompound = null;

        try
        {
            File file1 = new File(event.playerDirectory, player.getUniqueID().toString() + ".dat");

            if (file1.exists() && file1.isFile())
            {
                nbttagcompound = CompressedStreamTools.readCompressed(new FileInputStream(file1));
            }
        }
        catch (Exception exception)
        {
            InfiniteInvo.logger.warn("Failed to load player data for " + player.getCommandSenderName());
        }

        if (nbttagcompound != null)
        {
            player.inventory.readFromNBT(nbttagcompound.getTagList("Inventory", 10));
        }
	}
	
	@SubscribeEvent
	public void onEntityJoinWorld(EntityJoinWorldEvent event)
	{
		if(event.entity instanceof EntityPlayer)
		{
			EntityPlayer player = (EntityPlayer)event.entity;
			
			if(!(player.inventory instanceof BigInventoryPlayer))
			{
				player.inventory = new BigInventoryPlayer(player);
				player.inventoryContainer = new BigContainerPlayer((BigInventoryPlayer)player.inventory, !player.worldObj.isRemote, player);
				player.openContainer = player.inventoryContainer;
			}
			
			if(event.world.isRemote)
			{
				NBTTagCompound requestTags = new NBTTagCompound();
				requestTags.setInteger("ID", 1);
				requestTags.setInteger("World", event.world.provider.dimensionId);
				requestTags.setString("Player", player.getCommandSenderName());
				InfiniteInvo.instance.network.sendToServer(new InvoPacket(requestTags));
			} else
			{
				II_Settings.LoadFromCache();
			}
		}
	}
	
	@SubscribeEvent
	public void onEntityDeath(LivingDeathEvent event)
	{
		if(event.entityLiving instanceof EntityPlayer && !event.entityLiving.worldObj.isRemote)
		{
			if(!II_Settings.keepUnlocks || event.entityLiving.worldObj.getGameRules().getGameRuleBooleanValue("keepInventory"))
			{
				unlockCache.remove(event.entityLiving.getCommandSenderName());
			}
		}
	}
	
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onGuiOpen(GuiOpenEvent event)
	{
		if(event.gui instanceof GuiInventory && !(event.gui instanceof GuiBigInventory))
		{
			event.gui = new GuiBigInventory(Minecraft.getMinecraft().thePlayer);
		}
	}
	
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onGuiPostInit(InitGuiEvent.Post event)
	{
		if(event.gui instanceof GuiBigInventory)
		{
			((GuiBigInventory)event.gui).redoButtons = true;
		}
	}
	
	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load event)
	{
		if(!event.world.isRemote && worldDir == null && MinecraftServer.getServer().isServerRunning())
		{
			MinecraftServer server = MinecraftServer.getServer();
			
			if(InfiniteInvo.proxy.isClient())
			{
				worldDir = server.getFile("saves/" + server.getFolderName());
			} else
			{
				worldDir = server.getFile(server.getFolderName());
			}

			new File(worldDir, "data/").mkdirs();
			LoadCache(new File(worldDir, "data/SlotUnlockCache"));
		}
	}
	
	@SubscribeEvent
	public void onWorldSave(WorldEvent.Save event)
	{
		if(!event.world.isRemote && worldDir != null)
		{
			new File(worldDir, "data/").mkdirs();
			SaveCache(new File(worldDir, "data/SlotUnlockCache"));
			
			worldDir = null;
			unlockCache.clear();
		}
	}
	
	public static void SaveCache(File file)
	{
		try
		{
			if(!file.exists())
			{
				file.createNewFile();
			}
			
			FileOutputStream fos = new FileOutputStream(file);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			
			oos.writeObject(unlockCache);
			
			oos.close();
			fos.close();
		} catch(Exception e)
		{
			InfiniteInvo.logger.log(Level.ERROR, "Failed to save slot unlock cache", e);
		}
	}
	
	public static void LoadCache(File file)
	{
		try
		{
			if(!file.exists())
			{
				file.createNewFile();
			}
			
			FileInputStream fis = new FileInputStream(file);
			
			if(fis.available() <= 0)
			{
				fis.close();
				return;
			}
			
			ObjectInputStream ois = new ObjectInputStream(fis);
			
			unlockCache = (HashMap<String,Integer>)ois.readObject();
			
			ois.close();
			fis.close();
		} catch(Exception e)
		{
			InfiniteInvo.logger.log(Level.ERROR, "Failed to load slot unlock cache", e);
		}
	}
}

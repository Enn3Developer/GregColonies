package com.enn3developer.gregcolonies.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.GregColonies;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyManager;
import com.enn3developer.gregcolonies.entity.EntityCitizen;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class GCNetwork {

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(GregColonies.MODID);

    private static final Queue<Runnable> TASKS = new ConcurrentLinkedQueue<>();

    private GCNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(PacketRequestColony.Handler.class, PacketRequestColony.class, 0, Side.SERVER);
        CHANNEL.registerMessage(PacketColonyData.Handler.class, PacketColonyData.class, 1, Side.CLIENT);
        CHANNEL.registerMessage(PacketCitizenCommand.Handler.class, PacketCitizenCommand.class, 2, Side.SERVER);
        CHANNEL.registerMessage(PacketCitizenGroup.Handler.class, PacketCitizenGroup.class, 3, Side.SERVER);
        CHANNEL.registerMessage(PacketColonyDropOff.Handler.class, PacketColonyDropOff.class, 4, Side.SERVER);
        FMLCommonHandler.instance()
            .bus()
            .register(new GCNetwork());
    }

    static void enqueue(Runnable task) {
        TASKS.add(task);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Runnable task;
        while ((task = TASKS.poll()) != null) {
            task.run();
        }
    }

    static Colony nearestColony(EntityPlayerMP player) {
        World world = player.worldObj;
        return ColonyManager.get(world)
            .getNearestColonyOf(
                player.getUniqueID(),
                world.provider.dimensionId,
                (int) Math.floor(player.posX),
                (int) Math.floor(player.posZ));
    }

    static Colony accessibleColony(EntityPlayerMP player, int colonyId) {
        Colony colony = ColonyManager.get(player.worldObj)
            .getColony(colonyId);
        return colony != null && colony.canAccess(player) ? colony : null;
    }

    static Map<UUID, EntityCitizen> loadedCitizens(World world, int colonyId) {
        Map<UUID, EntityCitizen> loaded = new HashMap<>();
        for (Object object : world.loadedEntityList) {
            if (!(object instanceof EntityCitizen)) {
                continue;
            }
            EntityCitizen citizen = (EntityCitizen) object;
            if (citizen.getColonyId() == colonyId) {
                loaded.put(citizen.getUniqueID(), citizen);
            }
        }
        return loaded;
    }

    static void sendColony(EntityPlayerMP player) {
        Colony colony = nearestColony(player);
        if (colony == null) {
            player.addChatMessage(
                new ChatComponentText(EnumChatFormatting.RED + "You do not own a colony in this dimension"));
            return;
        }
        sendColony(player, colony);
    }

    static void sendColony(EntityPlayerMP player, Colony colony) {
        CHANNEL.sendTo(new PacketColonyData(ColonySnapshot.of(colony, player.worldObj)), player);
    }
}

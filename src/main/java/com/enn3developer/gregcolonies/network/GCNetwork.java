package com.enn3developer.gregcolonies.network;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.GregColonies;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyManager;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class GCNetwork {

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(GregColonies.MODID);

    private static final Queue<EntityPlayerMP> REQUESTS = new ConcurrentLinkedQueue<>();

    private GCNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(PacketRequestColony.Handler.class, PacketRequestColony.class, 0, Side.SERVER);
        CHANNEL.registerMessage(PacketColonyData.Handler.class, PacketColonyData.class, 1, Side.CLIENT);
        FMLCommonHandler.instance()
            .bus()
            .register(new GCNetwork());
    }

    static void queueRequest(EntityPlayerMP player) {
        REQUESTS.add(player);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        EntityPlayerMP player;
        while ((player = REQUESTS.poll()) != null) {
            serve(player);
        }
    }

    private static void serve(EntityPlayerMP player) {
        World world = player.worldObj;
        Colony colony = ColonyManager.get(world)
            .getNearestColonyOf(
                player.getUniqueID(),
                world.provider.dimensionId,
                (int) Math.floor(player.posX),
                (int) Math.floor(player.posZ));
        if (colony == null) {
            player.addChatMessage(
                new ChatComponentText(EnumChatFormatting.RED + "You do not own a colony in this dimension"));
            return;
        }
        CHANNEL.sendTo(new PacketColonyData(ColonySnapshot.of(colony, world)), player);
    }
}

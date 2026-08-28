package com.enn3developer.gregcolonies.network;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.Chat;
import com.enn3developer.gregcolonies.GregColonies;
import com.enn3developer.gregcolonies.colony.BlockPalette;
import com.enn3developer.gregcolonies.colony.Blueprint;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyManager;
import com.enn3developer.gregcolonies.colony.ColonySite;
import com.enn3developer.gregcolonies.colony.ColonySiteKind;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.work.Inventories;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
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
        CHANNEL.registerMessage(PacketColonySite.Handler.class, PacketColonySite.class, 4, Side.SERVER);
        CHANNEL.registerMessage(PacketOpenCitizen.Handler.class, PacketOpenCitizen.class, 6, Side.SERVER);
        CHANNEL.registerMessage(PacketBlueprintSave.Handler.class, PacketBlueprintSave.class, 8, Side.SERVER);
        CHANNEL.registerMessage(PacketColonyBuild.Handler.class, PacketColonyBuild.class, 9, Side.SERVER);
        CHANNEL.registerMessage(PacketBlueprintAction.Handler.class, PacketBlueprintAction.class, 10, Side.SERVER);
        CHANNEL.registerMessage(PacketBlueprintData.Handler.class, PacketBlueprintData.class, 11, Side.CLIENT);
        CHANNEL.registerMessage(PacketCitizenJob.Handler.class, PacketCitizenJob.class, 12, Side.SERVER);
        CHANNEL.registerMessage(PacketColonyPalette.Handler.class, PacketColonyPalette.class, 13, Side.CLIENT);
        FMLCommonHandler.instance()
            .bus()
            .register(new GCNetwork());
    }

    static void enqueue(Runnable task) {
        TASKS.add(task);
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PacketBlueprintSave.forget(event.player);
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
        return ColonyManager.registry(world)
            .getNearestColonyOf(
                player.getUniqueID(),
                world.provider.dimensionId,
                (int) Math.floor(player.posX),
                (int) Math.floor(player.posZ));
    }

    static Colony accessibleColony(EntityPlayerMP player, int colonyId) {
        Colony colony = ColonyManager.registry(player.worldObj)
            .getColony(colonyId);
        return colony != null && colony.canAccess(player) ? colony : null;
    }

    public static Map<UUID, EntityCitizen> loadedCitizens(World world, int colonyId) {
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
            Chat.error(player, "You do not own a colony in this dimension");
            return;
        }
        sendColony(player, colony);
    }

    static void sendColony(EntityPlayerMP player, Colony colony) {
        CHANNEL.sendTo(new PacketColonyData(ColonySnapshot.of(colony, player.worldObj)), player);
    }

    static void sendBlueprint(EntityPlayerMP player, Colony colony, int index) {
        Blueprint blueprint = colony.getBlueprint(index);
        if (blueprint == null) {
            return;
        }
        CHANNEL.sendTo(
            new PacketBlueprintData(colony.getId(), index, blueprint, stock(player.worldObj, colony, blueprint)),
            player);
    }

    static void sendPalette(EntityPlayerMP player, Colony colony) {
        CHANNEL.sendTo(new PacketColonyPalette(colony.getId(), palette(player.worldObj, colony)), player);
    }

    private static List<PacketColonyPalette.Entry> palette(World world, Colony colony) {
        List<PacketColonyPalette.Entry> entries = new ArrayList<>();
        IInventory inventory = materials(world, colony);
        if (inventory == null) {
            return entries;
        }
        Map<String, Integer> held = new LinkedHashMap<>();
        Inventories.forEachExtractable(inventory, stack -> {
            Block block = Block.getBlockFromItem(stack.getItem());
            int meta = stack.getItemDamage();
            if (block == null || meta > 0xFF || !BlockPalette.isBuildable(block, meta)) {
                return;
            }
            String name = Block.blockRegistry.getNameForObject(block);
            if (name == null || name.isEmpty()) {
                return;
            }
            held.merge(name + "@" + meta, stack.stackSize, Integer::sum);
        });
        for (Map.Entry<String, Integer> entry : held.entrySet()) {
            String key = entry.getKey();
            int split = key.lastIndexOf('@');
            entries.add(
                new PacketColonyPalette.Entry(
                    key.substring(0, split),
                    Integer.parseInt(key.substring(split + 1)),
                    entry.getValue()));
        }
        entries.sort(
            Comparator.<PacketColonyPalette.Entry>comparingInt(PacketColonyPalette.Entry::getHeld)
                .reversed());
        return entries;
    }

    private static IInventory materials(World world, Colony colony) {
        ColonySite site = colony.site(ColonySiteKind.MATERIALS);
        if (!site.isPresent()) {
            return null;
        }
        return Inventories.at(world, site.getX(), site.getY(), site.getZ());
    }

    private static Map<Integer, Integer> stock(World world, Colony colony, Blueprint blueprint) {
        Map<Integer, Integer> stock = new LinkedHashMap<>();
        IInventory inventory = materials(world, colony);
        if (inventory == null) {
            return stock;
        }
        for (int cell : blueprint.materials()
            .keySet()) {
            stock.put(
                cell,
                Inventories.count(
                    inventory,
                    stack -> blueprint.getPalette()
                        .matches(cell, stack)));
        }
        return stock;
    }
}

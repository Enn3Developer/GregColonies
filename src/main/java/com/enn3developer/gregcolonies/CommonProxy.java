package com.enn3developer.gregcolonies;

import java.util.function.BooleanSupplier;

import net.minecraft.item.Item;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.enn3developer.gregcolonies.block.GCBlocks;
import com.enn3developer.gregcolonies.command.ColonyCommand;
import com.enn3developer.gregcolonies.entity.GCEntities;
import com.enn3developer.gregcolonies.network.ColonySnapshot;
import com.enn3developer.gregcolonies.network.GCNetwork;
import com.enn3developer.gregcolonies.network.PacketBlueprintData;
import com.enn3developer.gregcolonies.network.PacketColonyPalette;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());

        GregColonies.LOG.info(Config.greeting);
        GregColonies.LOG.info("I am GregColonies at version " + Tags.VERSION);

        GCBlocks.register();
        GCEntities.register();
        GCNetwork.register();
    }

    public void openColonyScreen(ColonySnapshot colony) {}

    public void showBlueprint(PacketBlueprintData data) {}

    public void showPalette(PacketColonyPalette palette) {}

    public String trimText(String text, int width) {
        return text;
    }

    public int textWidth(String text) {
        return 0;
    }

    public IDrawable armorSlotIcon(int armorType, BooleanSupplier visible) {
        return IDrawable.EMPTY;
    }

    public IDrawable itemSlotIcon(Item item, BooleanSupplier visible) {
        return IDrawable.EMPTY;
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {}

    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {}

    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new ColonyCommand());
    }
}

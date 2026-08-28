package com.enn3developer.gregcolonies;

import java.util.function.BooleanSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraftforge.common.MinecraftForge;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.enn3developer.gregcolonies.client.GCClientEvents;
import com.enn3developer.gregcolonies.client.GCKeyBindings;
import com.enn3developer.gregcolonies.client.RenderCitizen;
import com.enn3developer.gregcolonies.client.gui.BlueprintEditorScreen;
import com.enn3developer.gregcolonies.client.gui.BlueprintScreen;
import com.enn3developer.gregcolonies.client.gui.CitizenIcons;
import com.enn3developer.gregcolonies.client.gui.ColonyScreen;
import com.enn3developer.gregcolonies.client.gui.ColonyWorldOverlay;
import com.enn3developer.gregcolonies.client.gui.GuiText;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.network.ColonySnapshot;
import com.enn3developer.gregcolonies.network.PacketBlueprintData;
import com.enn3developer.gregcolonies.network.PacketColonyPalette;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        RenderingRegistry.registerEntityRenderingHandler(EntityCitizen.class, new RenderCitizen());
        GCKeyBindings.register();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        GCClientEvents events = new GCClientEvents();
        FMLCommonHandler.instance()
            .bus()
            .register(events);
        MinecraftForge.EVENT_BUS.register(events);
        MinecraftForge.EVENT_BUS.register(new ColonyWorldOverlay());
    }

    @Override
    public String trimText(String text, int width) {
        return GuiText.trim(text, width);
    }

    @Override
    public int textWidth(String text) {
        return GuiText.width(text);
    }

    @Override
    public IDrawable armorSlotIcon(int armorType, BooleanSupplier visible) {
        return CitizenIcons.armor(armorType, visible);
    }

    @Override
    public IDrawable itemSlotIcon(Item item, BooleanSupplier visible) {
        return CitizenIcons.item(item, visible);
    }

    @Override
    public void openColonyScreen(ColonySnapshot colony) {
        Minecraft.getMinecraft()
            .func_152344_a(() -> ColonyScreen.open(colony));
    }

    @Override
    public void showBlueprint(PacketBlueprintData data) {
        Minecraft.getMinecraft()
            .func_152344_a(() -> BlueprintScreen.accept(data));
    }

    @Override
    public void showPalette(PacketColonyPalette palette) {
        Minecraft.getMinecraft()
            .func_152344_a(() -> BlueprintEditorScreen.accept(palette));
    }
}

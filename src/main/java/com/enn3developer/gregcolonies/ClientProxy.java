package com.enn3developer.gregcolonies;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

import com.enn3developer.gregcolonies.client.GCClientEvents;
import com.enn3developer.gregcolonies.client.GCKeyBindings;
import com.enn3developer.gregcolonies.client.RenderCitizen;
import com.enn3developer.gregcolonies.client.gui.ColonyScreen;
import com.enn3developer.gregcolonies.client.gui.ColonyWorldOverlay;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.network.ColonySnapshot;

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
        FMLCommonHandler.instance()
            .bus()
            .register(new GCClientEvents());
        MinecraftForge.EVENT_BUS.register(new ColonyWorldOverlay());
    }

    @Override
    public void openColonyScreen(ColonySnapshot colony) {
        Minecraft.getMinecraft()
            .func_152344_a(() -> ColonyScreen.open(colony));
    }
}

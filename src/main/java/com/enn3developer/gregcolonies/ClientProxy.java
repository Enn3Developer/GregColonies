package com.enn3developer.gregcolonies;

import net.minecraft.client.renderer.entity.RenderVillager;

import com.enn3developer.gregcolonies.entity.EntityCitizen;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        RenderingRegistry.registerEntityRenderingHandler(EntityCitizen.class, new RenderVillager());
    }
}

package com.enn3developer.gregcolonies.client;

import net.minecraft.client.renderer.entity.RenderVillager;
import net.minecraft.entity.EntityLivingBase;

import org.lwjgl.opengl.GL11;

import com.enn3developer.gregcolonies.entity.EntityCitizen;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderCitizen extends RenderVillager {

    private static final float SLEEP_LIFT = 0.25F;

    private static final float SLEEP_TURN = 180.0F;

    private static final float SLEEP_TILT = 90.0F;

    @Override
    protected void rotateCorpse(EntityLivingBase entity, float partialAge, float bodyYaw, float partialTicks) {
        super.rotateCorpse(entity, partialAge, bodyYaw, partialTicks);
        if (entity instanceof EntityCitizen && ((EntityCitizen) entity).isAsleep()) {
            GL11.glTranslatef(0.0F, SLEEP_LIFT, 0.0F);
            GL11.glRotatef(SLEEP_TURN, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(SLEEP_TILT, 1.0F, 0.0F, 0.0F);
        }
    }
}

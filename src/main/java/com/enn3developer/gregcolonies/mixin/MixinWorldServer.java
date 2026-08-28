package com.enn3developer.gregcolonies.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.world.WorldServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.enn3developer.gregcolonies.entity.EntityCitizen;

@Mixin(WorldServer.class)
public abstract class MixinWorldServer {

    /**
     * WorldServer.updateEntityWithOptionalForce kills every INpc each tick when spawn-npcs is off,
     * and every EntityAnimal when spawn-animals is off. Those switches exist to stop natural
     * spawning, but 1.7.10 implements them by deleting entities that already exist. A colony citizen
     * is placed by a player, not spawned by the world, so it is exempt. Every other entity is left
     * to the vanilla behaviour the server owner asked for.
     */
    @Redirect(
        method = "updateEntityWithOptionalForce",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setDead()V"))
    private void gregcolonies$keepColonyCitizens(Entity entity) {
        if (entity instanceof EntityCitizen) {
            return;
        }
        entity.setDead();
    }
}

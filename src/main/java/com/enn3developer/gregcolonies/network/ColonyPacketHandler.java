package com.enn3developer.gregcolonies.network;

import net.minecraft.entity.player.EntityPlayerMP;

import com.enn3developer.gregcolonies.colony.Colony;

import cpw.mods.fml.common.network.simpleimpl.IMessage;

public abstract class ColonyPacketHandler<T extends IMessage> extends ServerPacketHandler<T> {

    @Override
    protected final void apply(EntityPlayerMP player, T message) {
        Colony colony = GCNetwork.accessibleColony(player, colonyId(message));
        if (colony == null) {
            return;
        }
        apply(player, colony, message);
    }

    protected abstract int colonyId(T message);

    protected abstract void apply(EntityPlayerMP player, Colony colony, T message);
}

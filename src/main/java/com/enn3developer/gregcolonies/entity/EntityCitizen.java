package com.enn3developer.gregcolonies.entity;

import net.minecraft.entity.ai.EntityAIOpenDoor;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.EntityGuiData;
import com.cleanroommc.modularui.factory.GuiFactories;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.item.ItemStackHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.slot.SlotGroup;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyManager;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommand;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommandQueue;
import com.enn3developer.gregcolonies.entity.ai.EntityAICitizenCommands;

public class EntityCitizen extends EntityVillager implements IGuiHolder<EntityGuiData> {

    public static final String NAME = "citizen";

    public static final int INVENTORY_SIZE = 9;

    private static final String SLOT_GROUP = "citizen_inventory";

    private final CitizenCommandQueue commands = new CitizenCommandQueue();
    private final CitizenParameters parameters = new CitizenParameters();
    private final ItemStackHandler inventory = new ItemStackHandler(INVENTORY_SIZE);
    private int colonyId;

    public EntityCitizen(World world) {
        super(world);
        tasks.taskEntries.clear();
        targetTasks.taskEntries.clear();
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(1, new EntityAICitizenCommands(this));
        tasks.addTask(2, new EntityAIOpenDoor(this, true));
        tasks.addTask(3, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
        tasks.addTask(4, new EntityAIWander(this, 0.4D));
    }

    public CitizenCommandQueue getCommands() {
        return commands;
    }

    public CitizenParameters getParameters() {
        return parameters;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public int getColonyId() {
        return colonyId;
    }

    public void setColonyId(int colonyId) {
        this.colonyId = colonyId;
    }

    public Colony getColony() {
        if (colonyId == 0 || worldObj == null || worldObj.isRemote) {
            return null;
        }
        return ColonyManager.get(worldObj)
            .getColony(colonyId);
    }

    public boolean takeColonyOrder() {
        if (colonyId == 0 || worldObj.isRemote) {
            return false;
        }
        CitizenCommand order = ColonyManager.get(worldObj)
            .pollOrder(colonyId);
        if (order == null) {
            return false;
        }
        commands.enqueue(order);
        return true;
    }

    public boolean canAccessInventory(EntityPlayer player) {
        Colony colony = getColony();
        return colony != null && colony.canAccess(player);
    }

    @Override
    protected void updateAITick() {}

    @Override
    public boolean interact(EntityPlayer player) {
        if (worldObj.isRemote) {
            return true;
        }
        if (!canAccessInventory(player)) {
            player.addChatMessage(
                new ChatComponentText(EnumChatFormatting.RED + "You are not allowed to access this citizen"));
            return true;
        }
        GuiFactories.entity()
            .open(player, this);
        return true;
    }

    @Override
    public ModularPanel buildUI(EntityGuiData data, PanelSyncManager syncManager, UISettings settings) {
        syncManager.registerSlotGroup(new SlotGroup(SLOT_GROUP, INVENTORY_SIZE, true));

        ModularPanel panel = ModularPanel.defaultPanel("citizen_inventory");
        panel.child(
            SlotGroupWidget.builder()
                .row("IIIIIIIII")
                .key('I', index -> new ItemSlot().slot(new ModularSlot(inventory, index).slotGroup(SLOT_GROUP)))
                .build()
                .pos(7, 20));
        panel.child(SlotGroupWidget.playerInventory(true));
        return panel;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("colonyId", colonyId);
        tag.setTag("inventory", inventory.serializeNBT());

        NBTTagCompound commandsTag = new NBTTagCompound();
        commands.writeToNBT(commandsTag);
        tag.setTag("commands", commandsTag);

        NBTTagCompound parametersTag = new NBTTagCompound();
        parameters.writeToNBT(parametersTag);
        tag.setTag("parameters", parametersTag);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        colonyId = tag.getInteger("colonyId");
        inventory.deserializeNBT(tag.getCompoundTag("inventory"));
        commands.readFromNBT(tag.getCompoundTag("commands"));
        parameters.readFromNBT(tag.getCompoundTag("parameters"));
    }

    @Override
    protected void dropEquipment(boolean recentlyHit, int looting) {
        super.dropEquipment(recentlyHit, looting);
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack != null) {
                entityDropItem(stack, 0.0F);
                inventory.setStackInSlot(i, null);
            }
        }
    }
}

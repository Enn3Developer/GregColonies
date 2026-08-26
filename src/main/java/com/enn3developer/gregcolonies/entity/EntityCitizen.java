package com.enn3developer.gregcolonies.entity;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.minecraft.block.material.Material;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIOpenDoor;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.factory.EntityGuiData;
import com.cleanroommc.modularui.factory.GuiFactories;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.EntityDisplayWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.slot.SlotGroup;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyCitizen;
import com.enn3developer.gregcolonies.colony.ColonyManager;
import com.enn3developer.gregcolonies.compat.Mods;
import com.enn3developer.gregcolonies.compat.TinkersTools;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommand;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommandQueue;
import com.enn3developer.gregcolonies.entity.ai.EntityAICitizenCommands;
import com.enn3developer.gregcolonies.entity.ai.EntityAICitizenFlee;
import com.enn3developer.gregcolonies.entity.ai.EntityAICitizenIdle;
import com.enn3developer.gregcolonies.entity.diet.CitizenDiet;

public class EntityCitizen extends EntityVillager implements IGuiHolder<EntityGuiData> {

    public static final String NAME = "citizen";

    private static final double PATH_RANGE = 64.0D;

    private static final float WALK_EXHAUSTION = 0.01F;

    private static final float SPRINT_EXHAUSTION = 0.099999994F;

    private static final float SWIM_EXHAUSTION = 0.015F;

    private static final float JUMP_EXHAUSTION = 0.2F;

    private static final float SPRINT_JUMP_EXHAUSTION = 0.8F;

    private static final float ATTACK_EXHAUSTION = 0.3F;

    private static final double BASE_ATTACK_DAMAGE = 1.0D;

    private static final int ROSTER_REFRESH_TICKS = 100;

    private static final double LEAP_LIFT = 0.4D;

    private static final double LEAP_PUSH = 0.4D;

    private static final String ARMOR_GROUP = "citizen_armor";

    private static final String FOOD_GROUP = "citizen_food";

    private static final String TOOL_GROUP = "citizen_tool";

    private static final String MAIN_GROUP = "citizen_main";

    private static final int ARMOR_PRIORITY = 40;

    private static final int FOOD_PRIORITY = 50;

    private static final int TOOL_PRIORITY = 60;

    private static final int MAIN_PRIORITY = 100;

    private static final int PANEL_WIDTH = 176;

    private static final int PANEL_HEIGHT = 192;

    private static final int PREVIEW_WIDTH = 54;

    private static final int PREVIEW_HEIGHT = 72;

    private final CitizenCommandQueue commands = new CitizenCommandQueue();
    private final CitizenParameters parameters = new CitizenParameters();
    private final CitizenInventory inventory = new CitizenInventory(this);
    private final CitizenDiet diet = new CitizenDiet();
    private final EntityAICitizenIdle idle = new EntityAICitizenIdle(this);
    private final Set<EntityPlayer> viewers = new HashSet<>();
    private int colonyId;
    private String group = "";
    private boolean rosterRegistered;
    private int rosterTicks;

    public EntityCitizen(World world) {
        super(world);
        tasks.taskEntries.clear();
        targetTasks.taskEntries.clear();
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(1, new EntityAICitizenFlee(this));
        tasks.addTask(2, new EntityAICitizenCommands(this));
        tasks.addTask(3, new EntityAIOpenDoor(this, true));
        tasks.addTask(4, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
        tasks.addTask(5, idle);
        for (int i = 0; i < equipmentDropChances.length; i++) {
            equipmentDropChances[i] = 0.0F;
        }
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(PATH_RANGE);
        getAttributeMap().registerAttribute(SharedMonsterAttributes.attackDamage);
        getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(BASE_ATTACK_DAMAGE);
    }

    public CitizenCommandQueue getCommands() {
        return commands;
    }

    public CitizenParameters getParameters() {
        return parameters;
    }

    public CitizenInventory getInventory() {
        return inventory;
    }

    public CitizenDiet getDiet() {
        return diet;
    }

    public String getIdleTask() {
        return idle.describeActive();
    }

    public int getColonyId() {
        return colonyId;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group == null ? "" : group;
        if (colonyId != 0 && worldObj != null && !worldObj.isRemote) {
            ColonyManager.get(worldObj)
                .setCitizenGroup(colonyId, getUniqueID(), this.group);
        }
    }

    public void setColonyId(int colonyId) {
        this.colonyId = colonyId;
        this.rosterRegistered = false;
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
            .pollOrder(colonyId, this);
        if (order == null) {
            return false;
        }
        commands.enqueue(order);
        return true;
    }

    public boolean isAfraid() {
        return commands.fearsEnemies();
    }

    public boolean canAccessInventory(EntityPlayer player) {
        Colony colony = getColony();
        return colony != null && colony.canAccess(player);
    }

    public boolean isViewed() {
        return !viewers.isEmpty();
    }

    @Override
    protected boolean isMovementBlocked() {
        return isViewed() || super.isMovementBlocked();
    }

    @Override
    public void onLivingUpdate() {
        if (!worldObj.isRemote && !viewers.isEmpty()) {
            Iterator<EntityPlayer> iterator = viewers.iterator();
            while (iterator.hasNext()) {
                EntityPlayer viewer = iterator.next();
                if (viewer.isDead || viewer.worldObj != worldObj
                    || viewer.openContainer == viewer.inventoryContainer
                    || getDistanceSqToEntity(viewer) > 400.0D) {
                    iterator.remove();
                }
            }
        }
        super.onLivingUpdate();

        if (!worldObj.isRemote) {
            updateRoster();
            syncEquipment();
            diet.update(this);
        }
    }

    private void updateRoster() {
        if (colonyId == 0 || !isEntityAlive()) {
            return;
        }
        ColonyManager manager = ColonyManager.get(worldObj);
        if (!rosterRegistered) {
            ColonyCitizen entry = manager.registerCitizen(colonyId, this);
            if (entry == null) {
                return;
            }
            group = entry.getGroup();
            rosterRegistered = true;
            return;
        }
        if (++rosterTicks >= ROSTER_REFRESH_TICKS) {
            rosterTicks = 0;
            manager.registerCitizen(colonyId, this);
        }
    }

    @Override
    public void onDeath(DamageSource cause) {
        super.onDeath(cause);
        if (!worldObj.isRemote && colonyId != 0) {
            ColonyManager.get(worldObj)
                .removeCitizen(colonyId, getUniqueID());
        }
    }

    private void syncEquipment() {
        setCurrentItemOrArmor(
            0,
            inventory.getTool()
                .getStackInSlot(0));
        for (int i = 0; i < CitizenInventory.ARMOR_SLOTS; i++) {
            setCurrentItemOrArmor(
                4 - i,
                inventory.getArmor()
                    .getStackInSlot(i));
        }
    }

    public void leapTowards(Entity target) {
        double dx = target.posX - posX;
        double dz = target.posZ - posZ;
        float distance = MathHelper.sqrt_double(dx * dx + dz * dz);
        if (distance >= 1.0E-4F) {
            motionX += dx / distance * LEAP_PUSH + motionX * 0.2D;
            motionZ += dz / distance * LEAP_PUSH + motionZ * 0.2D;
        }
        motionY = LEAP_LIFT;
        diet.addExhaustion(isSprinting() ? SPRINT_JUMP_EXHAUSTION : JUMP_EXHAUSTION);
    }

    @Override
    protected void damageArmor(float damage) {
        if (damage < 0.0F) {
            return;
        }
        damage /= 4.0F;
        if (damage < 1.0F) {
            damage = 1.0F;
        }
        for (int i = 0; i < CitizenInventory.ARMOR_SLOTS; i++) {
            ItemStack stack = inventory.getArmor()
                .getStackInSlot(i);
            if (stack == null || !(stack.getItem() instanceof ItemArmor)) {
                continue;
            }
            stack.damageItem((int) damage, this);
            if (stack.stackSize <= 0) {
                inventory.getArmor()
                    .setStackInSlot(i, null);
            }
        }
    }

    public boolean attackTarget(EntityLivingBase target) {
        ItemStack tool = inventory.getHeldTool();
        if (tool != null && Mods.tinkers() && TinkersTools.isTool(tool)) {
            return attackWithTinkersTool(tool, target);
        }
        float damage = (float) getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue()
            + toolDamage(tool)
            + EnchantmentHelper.getEnchantmentModifierLiving(this, target);
        int knockback = EnchantmentHelper.getKnockbackModifier(this, target);

        swingItem();
        diet.addExhaustion(ATTACK_EXHAUSTION);
        if (!target.attackEntityFrom(DamageSource.causeMobDamage(this), damage)) {
            return false;
        }

        if (knockback > 0) {
            target.addVelocity(
                -MathHelper.sin(rotationYaw * (float) Math.PI / 180.0F) * knockback * 0.5F,
                0.1D,
                MathHelper.cos(rotationYaw * (float) Math.PI / 180.0F) * knockback * 0.5F);
            motionX *= 0.6D;
            motionZ *= 0.6D;
        }

        int fireAspect = EnchantmentHelper.getFireAspectModifier(this);
        if (fireAspect > 0) {
            target.setFire(fireAspect * 4);
        }

        if (tool != null) {
            tool.getItem()
                .hitEntity(tool, target, this);
            if (tool.stackSize <= 0) {
                inventory.getTool()
                    .setStackInSlot(0, null);
            }
        }
        return true;
    }

    private boolean attackWithTinkersTool(ItemStack tool, EntityLivingBase target) {
        swingItem();
        diet.addExhaustion(ATTACK_EXHAUSTION);
        boolean hit = TinkersTools.attack(tool, this, target);
        if (tool.stackSize <= 0) {
            inventory.getTool()
                .setStackInSlot(0, null);
        }
        return hit;
    }

    private static float toolDamage(ItemStack stack) {
        if (stack == null) {
            return 0.0F;
        }
        float bonus = 0.0F;
        for (Object entry : stack.getAttributeModifiers()
            .get(SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName())) {
            AttributeModifier modifier = (AttributeModifier) entry;
            if (modifier.getOperation() == 0) {
                bonus += (float) modifier.getAmount();
            }
        }
        return bonus;
    }

    @Override
    public void moveEntityWithHeading(float strafe, float forward) {
        double x = posX;
        double y = posY;
        double z = posZ;
        super.moveEntityWithHeading(strafe, forward);
        if (!worldObj.isRemote) {
            addMovementExhaustion(posX - x, posY - y, posZ - z);
        }
    }

    private void addMovementExhaustion(double dx, double dy, double dz) {
        if (ridingEntity != null) {
            return;
        }

        int distance;
        if (isInsideOfMaterial(Material.water)) {
            distance = Math.round(MathHelper.sqrt_double(dx * dx + dy * dy + dz * dz) * 100.0F);
            if (distance > 0) {
                diet.addExhaustion(SWIM_EXHAUSTION * distance * 0.01F);
            }
        } else if (isInWater()) {
            distance = Math.round(MathHelper.sqrt_double(dx * dx + dz * dz) * 100.0F);
            if (distance > 0) {
                diet.addExhaustion(SWIM_EXHAUSTION * distance * 0.01F);
            }
        } else if (!isOnLadder() && onGround) {
            distance = Math.round(MathHelper.sqrt_double(dx * dx + dz * dz) * 100.0F);
            if (distance > 0) {
                diet.addExhaustion((isSprinting() ? SPRINT_EXHAUSTION : WALK_EXHAUSTION) * distance * 0.01F);
            }
        }
    }

    @Override
    protected void jump() {
        super.jump();
        if (!worldObj.isRemote) {
            diet.addExhaustion(isSprinting() ? SPRINT_JUMP_EXHAUSTION : JUMP_EXHAUSTION);
        }
    }

    @Override
    protected void damageEntity(DamageSource source, float amount) {
        float before = getHealth();
        super.damageEntity(source, amount);
        if (!worldObj.isRemote && getHealth() != before) {
            diet.addExhaustion(source.getHungerDamage());
        }
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
        syncManager.registerSlotGroup(new SlotGroup(ARMOR_GROUP, 1, ARMOR_PRIORITY, true));
        syncManager.registerSlotGroup(new SlotGroup(FOOD_GROUP, 1, FOOD_PRIORITY, true));
        syncManager.registerSlotGroup(new SlotGroup(TOOL_GROUP, 1, TOOL_PRIORITY, true));
        syncManager.registerSlotGroup(new SlotGroup(MAIN_GROUP, CitizenInventory.MAIN_SLOTS, MAIN_PRIORITY, true));
        syncManager.addOpenListener(viewer -> {
            viewers.add(viewer);
            getNavigator().clearPathEntity();
        });
        syncManager.addCloseListener(viewers::remove);

        ModularPanel panel = ModularPanel.defaultPanel("citizen_inventory", PANEL_WIDTH, PANEL_HEIGHT);
        panel.child(
            SlotGroupWidget.builder()
                .row("A")
                .row("A")
                .row("A")
                .row("A")
                .key(
                    'A',
                    index -> new ItemSlot().slot(
                        new ModularSlot(inventory.getArmor(), index).slotGroup(ARMOR_GROUP)
                            .filter(stack -> CitizenInventory.isArmor(stack, index, this))))
                .build()
                .pos(7, 8));
        panel.child(
            SlotGroupWidget.builder()
                .row("F")
                .row("F")
                .row("F")
                .key(
                    'F',
                    index -> new ItemSlot().slot(
                        new ModularSlot(inventory.getFood(), index).slotGroup(FOOD_GROUP)
                            .filter(CitizenInventory::isFood)))
                .build()
                .pos(151, 8));
        panel.child(
            new ItemSlot().slot(
                new ModularSlot(inventory.getTool(), 0).slotGroup(TOOL_GROUP)
                    .filter(CitizenInventory::isTool))
                .pos(151, 66));
        panel.child(
            new Widget<>().size(PREVIEW_WIDTH, PREVIEW_HEIGHT)
                .pos(61, 8)
                .background(GuiTextures.DISPLAY, new EntityDisplayWidget(() -> this).doesLookAtMouse(true)));
        panel.child(
            SlotGroupWidget.builder()
                .row("IIIIIIIII")
                .key(
                    'I',
                    index -> new ItemSlot().slot(new ModularSlot(inventory.getMain(), index).slotGroup(MAIN_GROUP)))
                .build()
                .pos(7, 86));
        panel.child(SlotGroupWidget.playerInventory(true));
        return panel;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("colonyId", colonyId);
        tag.setString("group", group);
        NBTTagCompound inventoryTag = new NBTTagCompound();
        inventory.writeToNBT(inventoryTag);
        tag.setTag("inventory", inventoryTag);

        NBTTagCompound commandsTag = new NBTTagCompound();
        commands.writeToNBT(commandsTag);
        tag.setTag("commands", commandsTag);

        NBTTagCompound parametersTag = new NBTTagCompound();
        parameters.writeToNBT(parametersTag);
        tag.setTag("parameters", parametersTag);

        NBTTagCompound dietTag = new NBTTagCompound();
        diet.writeToNBT(dietTag);
        tag.setTag("diet", dietTag);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        colonyId = tag.getInteger("colonyId");
        group = tag.getString("group");
        inventory.readFromNBT(tag.getCompoundTag("inventory"));
        commands.readFromNBT(tag.getCompoundTag("commands"));
        parameters.readFromNBT(tag.getCompoundTag("parameters"));
        diet.readFromNBT(tag.getCompoundTag("diet"));
    }

    @Override
    protected void dropEquipment(boolean recentlyHit, int looting) {
        super.dropEquipment(recentlyHit, looting);
        for (ItemStack stack : inventory.takeAll()) {
            entityDropItem(stack, 0.0F);
        }
    }
}

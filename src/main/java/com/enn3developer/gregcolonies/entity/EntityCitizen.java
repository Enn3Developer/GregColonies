package com.enn3developer.gregcolonies.entity;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.minecraft.block.material.Material;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIOpenDoor;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.EntityGuiData;
import com.cleanroommc.modularui.factory.GuiFactories;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.enn3developer.gregcolonies.Chat;
import com.enn3developer.gregcolonies.Config;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyCitizen;
import com.enn3developer.gregcolonies.colony.ColonyManager;
import com.enn3developer.gregcolonies.colony.ColonyRegistry;
import com.enn3developer.gregcolonies.compat.Mods;
import com.enn3developer.gregcolonies.compat.TinkersTools;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommand;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommandQueue;
import com.enn3developer.gregcolonies.entity.ai.CitizenNavigate;
import com.enn3developer.gregcolonies.entity.ai.CitizenTravel;
import com.enn3developer.gregcolonies.entity.ai.EntityAICitizenCommands;
import com.enn3developer.gregcolonies.entity.ai.EntityAICitizenDanger;
import com.enn3developer.gregcolonies.entity.ai.EntityAICitizenFlee;
import com.enn3developer.gregcolonies.entity.ai.EntityAICitizenIdle;
import com.enn3developer.gregcolonies.entity.ai.EntityAICitizenLiving;
import com.enn3developer.gregcolonies.entity.ai.EntityAICitizenSwim;
import com.enn3developer.gregcolonies.entity.ai.Hazards;
import com.enn3developer.gregcolonies.entity.diet.CitizenDiet;
import com.enn3developer.gregcolonies.network.PacketOpenCitizen;

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

    private static final double PLAYER_SPRINT_SPEED = 0.13D;

    private static final int ROSTER_REFRESH_TICKS = 100;

    private static boolean previewRender;

    static final double VIEW_RANGE_SQ = PacketOpenCitizen.OPEN_RANGE * PacketOpenCitizen.OPEN_RANGE;

    private static final int NAME_ATTEMPTS = 16;

    private static final float PATH_HAZARD_WEIGHT = 50.0F;

    private static final double CLIMB_SPEED = 0.15D;

    private static final double CLIMB_CENTERING = 0.1D;

    private static final int SLEEPING_WATCHER = 17;

    private static final int GENDER_WATCHER = 18;

    private static final double LEAP_LIFT = 0.4D;

    private static final double LEAP_PUSH = 0.4D;

    private final CitizenCommandQueue commands = new CitizenCommandQueue();
    private final CitizenParameters parameters = new CitizenParameters();
    private final CitizenInventory inventory = new CitizenInventory(this);
    private final CitizenDiet diet = new CitizenDiet();
    private final EntityAICitizenIdle idle = new EntityAICitizenIdle(this);
    private final EntityAICitizenLiving living = new EntityAICitizenLiving(this);
    private final CitizenTravel travel = new CitizenTravel(this);
    private final Set<EntityPlayer> viewers = new HashSet<>();
    private int colonyId;
    private String group = "";
    private CitizenJob job = CitizenJob.NONE;
    private boolean rosterRegistered;
    private int rosterTicks;
    private boolean wantsWater;
    private CitizenNavigate navigator;

    public EntityCitizen(World world) {
        super(world);
        tasks.taskEntries.clear();
        targetTasks.taskEntries.clear();
        tasks.addTask(0, new EntityAICitizenDanger(this));
        tasks.addTask(1, new EntityAICitizenSwim(this));
        tasks.addTask(2, new EntityAICitizenFlee(this));
        tasks.addTask(3, living);
        tasks.addTask(4, new EntityAICitizenCommands(this));
        tasks.addTask(5, new EntityAIOpenDoor(this, true));
        tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
        tasks.addTask(7, idle);
        getNavigator().setBreakDoors(true);
        for (int i = 0; i < equipmentDropChances.length; i++) {
            equipmentDropChances[i] = 0.0F;
        }
        setGender(CitizenGender.random(rand));
    }

    @Override
    public PathNavigate getNavigator() {
        if (navigator == null) {
            navigator = new CitizenNavigate(this, worldObj);
        }
        return navigator;
    }

    @Override
    public float getBlockPathWeight(int x, int y, int z) {
        if (Hazards.isNearDeadly(worldObj, x, y, z)) {
            return -PATH_HAZARD_WEIGHT;
        }
        return super.getBlockPathWeight(x, y, z);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(SLEEPING_WATCHER, Byte.valueOf((byte) 0));
        dataWatcher.addObject(GENDER_WATCHER, Byte.valueOf((byte) 0));
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

    public boolean travelTo(double x, double y, double z, double speed) {
        return travel.moveTo(x, y, z, speed);
    }

    public boolean wantsWater() {
        return wantsWater;
    }

    public void setWantsWater(boolean wantsWater) {
        this.wantsWater = wantsWater;
    }

    public static boolean isPreviewRender() {
        return previewRender;
    }

    static void setPreviewRender(boolean value) {
        previewRender = value;
    }

    void addViewer(EntityPlayer viewer) {
        viewers.add(viewer);
    }

    void removeViewer(EntityPlayer viewer) {
        viewers.remove(viewer);
    }

    public String getIdleTask() {
        return idle.describeActive();
    }

    public String getLivingTask() {
        return living.describeActive();
    }

    public boolean allowsSleep() {
        return commands.allowsSleep();
    }

    public int getColonyId() {
        return colonyId;
    }

    public String getCitizenName() {
        return getCustomNameTag();
    }

    public CitizenGender getGender() {
        return CitizenGender.byId(dataWatcher.getWatchableObjectByte(GENDER_WATCHER));
    }

    public void setGender(CitizenGender gender) {
        parameters.setGender(gender);
        dataWatcher.updateObject(GENDER_WATCHER, Byte.valueOf(CitizenGender.idOf(gender)));
    }

    private CitizenGender readGender() {
        CitizenGender gender = parameters.getGender();
        if (gender == null) {
            gender = CitizenNames.genderOf(getCitizenName());
        }
        return gender == null ? CitizenGender.random(rand) : gender;
    }

    public boolean canWork() {
        return !isChild();
    }

    @Override
    public EntityCitizen createChild(EntityAgeable mate) {
        EntityCitizen child = new EntityCitizen(worldObj);
        child.setColonyId(colonyId);
        child.setGrowingAge(-Config.childGrowthTicks);
        return child;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group == null ? "" : group;
        if (colonyId != 0 && worldObj != null && !worldObj.isRemote) {
            ColonyManager.registry(worldObj)
                .setCitizenGroup(colonyId, getUniqueID(), this.group);
        }
    }

    public CitizenJob getJob() {
        return job;
    }

    public void setJob(CitizenJob job) {
        this.job = job == null ? CitizenJob.NONE : job;
        if (colonyId != 0 && worldObj != null && !worldObj.isRemote) {
            ColonyManager.registry(worldObj)
                .setCitizenJob(colonyId, getUniqueID(), this.job);
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
        return ColonyManager.registry(worldObj)
            .getColony(colonyId);
    }

    public boolean takeColonyOrder() {
        if (colonyId == 0 || worldObj.isRemote || !canWork()) {
            return false;
        }
        CitizenCommand order = ColonyManager.registry(worldObj)
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

    public double panicSpeed() {
        double movement = getEntityAttribute(SharedMonsterAttributes.movementSpeed).getAttributeValue();
        return movement <= 0.0D ? 0.0D : Math.sqrt(PLAYER_SPRINT_SPEED) / movement;
    }

    public boolean canAccessInventory(EntityPlayer player) {
        Colony colony = getColony();
        return colony != null && colony.canAccess(player);
    }

    public boolean isViewed() {
        return !viewers.isEmpty();
    }

    public boolean isAsleep() {
        return dataWatcher.getWatchableObjectByte(SLEEPING_WATCHER) != 0;
    }

    public void setAsleep(boolean asleep) {
        dataWatcher.updateObject(SLEEPING_WATCHER, Byte.valueOf((byte) (asleep ? 1 : 0)));
    }

    @Override
    public boolean canBePushed() {
        return !isAsleep() && super.canBePushed();
    }

    @Override
    public void applyEntityCollision(Entity entity) {
        if (!isAsleep()) {
            super.applyEntityCollision(entity);
        }
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
                    || getDistanceSqToEntity(viewer) > VIEW_RANGE_SQ) {
                    iterator.remove();
                }
            }
        }
        super.onLivingUpdate();

        if (!worldObj.isRemote) {
            ensureName();
            updateRoster();
            syncEquipment();
            if (!isMovementBlocked()) {
                travel.update();
            }
            diet.update(this);
        }
    }

    public void ensureName() {
        if (hasCustomNameTag()) {
            return;
        }
        Colony colony = getColony();
        CitizenGender gender = getGender();
        String name = CitizenNames.generate(rand, gender);
        for (int i = 0; colony != null && i < NAME_ATTEMPTS && colony.hasCitizenNamed(name); i++) {
            name = CitizenNames.generate(rand, gender);
        }
        setCustomNameTag(name);
    }

    private void updateRoster() {
        if (colonyId == 0 || !isEntityAlive()) {
            return;
        }
        ColonyRegistry manager = ColonyManager.registry(worldObj);
        if (!rosterRegistered) {
            ColonyCitizen entry = manager.registerCitizen(colonyId, this);
            if (entry == null) {
                return;
            }
            group = entry.getGroup();
            job = entry.getJob();
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
        if (worldObj.isRemote) {
            return;
        }
        if (colonyId != 0) {
            ColonyManager.registry(worldObj)
                .removeCitizen(colonyId, getUniqueID());
        }
        for (ItemStack stack : inventory.takeAll()) {
            entityDropItem(stack, 0.0F);
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
    public void moveEntity(double dx, double dy, double dz) {
        if (!worldObj.isRemote && !Hazards.isInDanger(this) && burnsAt(posX + dx, posZ + dz)) {
            if (!burnsAt(posX + dx, posZ)) {
                dz = 0.0D;
            } else if (!burnsAt(posX, posZ + dz)) {
                dx = 0.0D;
            } else {
                dx = 0.0D;
                dz = 0.0D;
            }
        }
        super.moveEntity(dx, dy, dz);
    }

    private boolean burnsAt(double x, double z) {
        double reach = width / 2.0D;
        int y = MathHelper.floor_double(boundingBox.minY);
        int maxX = MathHelper.floor_double(x + reach);
        int maxZ = MathHelper.floor_double(z + reach);
        for (int bx = MathHelper.floor_double(x - reach); bx <= maxX; bx++) {
            for (int bz = MathHelper.floor_double(z - reach); bz <= maxZ; bz++) {
                if (Hazards.isDeadlyStep(worldObj, bx, y, bz) || Hazards.isDeadlyDrop(worldObj, bx, y, bz)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void moveEntityWithHeading(float strafe, float forward) {
        double x = posX;
        double y = posY;
        double z = posZ;
        int climb = climbDirection();
        if (climb == 0) {
            super.moveEntityWithHeading(strafe, forward);
        } else {
            motionY = climb > 0 ? CLIMB_SPEED : -CLIMB_SPEED;
            motionX = centering(posX);
            motionZ = centering(posZ);
            fallDistance = 0.0F;
            super.moveEntityWithHeading(0.0F, 0.0F);
        }
        if (!worldObj.isRemote) {
            addMovementExhaustion(posX - x, posY - y, posZ - z);
        }
    }

    private int climbDirection() {
        return worldObj.isRemote ? 0 : ((CitizenNavigate) getNavigator()).getClimbDirection();
    }

    private static double centering(double position) {
        return MathHelper
            .clamp_double(MathHelper.floor_double(position) + 0.5D - position, -CLIMB_CENTERING, CLIMB_CENTERING);
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
    protected void updateAITick() {
        getNavigator().onUpdateNavigation();
    }

    @Override
    public boolean interact(EntityPlayer player) {
        if (worldObj.isRemote) {
            return true;
        }
        if (!canAccessInventory(player)) {
            Chat.error(player, "You are not allowed to access this citizen");
            return true;
        }
        GuiFactories.entity()
            .open(player, this);
        return true;
    }

    @Override
    public ModularPanel buildUI(EntityGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return CitizenPanel.build(this, syncManager, settings);
    }

    public String describeActivity() {
        String living = getLivingTask();
        if (!living.isEmpty()) {
            return living;
        }
        CitizenCommand current = commands.getCurrent();
        if (current != null) {
            return current.describe();
        }
        String idle = getIdleTask();
        return idle.isEmpty() ? "idle" : "idle " + idle;
    }

    public String describeGender() {
        return CitizenGender.describe(getGender(), isChild());
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("colonyId", colonyId);
        tag.setString("group", group);
        tag.setString("job", job.name());
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
        job = CitizenJob.byName(tag.getString("job"));
        inventory.readFromNBT(tag.getCompoundTag("inventory"));
        commands.readFromNBT(tag.getCompoundTag("commands"));
        parameters.readFromNBT(tag.getCompoundTag("parameters"));
        setGender(readGender());
        diet.readFromNBT(tag.getCompoundTag("diet"));
    }

    @Override
    protected void dropEquipment(boolean recentlyHit, int looting) {}
}

package com.enn3developer.gregcolonies.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.network.ColonySnapshot;

public class ColonyCamera extends EntityLivingBase {

    public static final float DEFAULT_PITCH = 55.0F;

    public static final float DEFAULT_YAW = 45.0F;

    public static final float DEFAULT_DISTANCE = 42.0F;

    private static final ItemStack[] NO_ITEMS = new ItemStack[5];

    private static final float MIN_DISTANCE = 6.0F;

    private static final float MAX_DISTANCE = 180.0F;

    private static final float MIN_PITCH = 15.0F;

    private static final float MAX_PITCH = 89.0F;

    private static final double EYE_OFFSET = 1.62D;

    private static ColonyCamera current;

    private static EntityLivingBase previousView;

    private static int previousPerspective;

    private static boolean previousClouds;

    private double targetX;

    private double targetY;

    private double targetZ;

    private float distance = DEFAULT_DISTANCE;

    public ColonyCamera(World world) {
        super(world);
        setSize(0.0F, 0.0F);
        noClip = true;
        yOffset = (float) EYE_OFFSET;
        ignoreFrustumCheck = true;
        rotationYaw = DEFAULT_YAW;
        rotationPitch = DEFAULT_PITCH;
    }

    public static ColonyCamera get() {
        return current;
    }

    public static void install(ColonySnapshot colony) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.theWorld == null || current != null) {
            return;
        }
        ColonyCamera camera = new ColonyCamera(minecraft.theWorld);
        camera.reset(colony);
        previousView = minecraft.renderViewEntity;
        previousPerspective = minecraft.gameSettings.thirdPersonView;
        previousClouds = minecraft.gameSettings.clouds;
        minecraft.gameSettings.thirdPersonView = 0;
        minecraft.gameSettings.clouds = false;
        minecraft.renderViewEntity = camera;
        current = camera;
    }

    public static void remove() {
        if (current == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.renderViewEntity = previousView != null ? previousView : minecraft.thePlayer;
        minecraft.gameSettings.thirdPersonView = previousPerspective;
        minecraft.gameSettings.clouds = previousClouds;
        previousView = null;
        current = null;
    }

    public void reset(ColonySnapshot colony) {
        targetX = colony.getX() + 0.5D;
        targetY = colony.getY();
        targetZ = colony.getZ() + 0.5D;
        distance = DEFAULT_DISTANCE;
        rotationYaw = DEFAULT_YAW;
        rotationPitch = DEFAULT_PITCH;
        place();
    }

    public void focus(double x, double y, double z, float distance) {
        targetX = x;
        targetY = y;
        targetZ = z;
        this.distance = clamp(distance, MIN_DISTANCE, MAX_DISTANCE);
        place();
    }

    public float getDistance() {
        return distance;
    }

    public void zoom(float factor) {
        distance = clamp(distance * factor, MIN_DISTANCE, MAX_DISTANCE);
        place();
    }

    public void rotate(float yaw, float pitch) {
        rotationYaw += yaw;
        rotationPitch = clamp(rotationPitch + pitch, MIN_PITCH, MAX_PITCH);
        place();
    }

    public void pan(double screenX, double screenY, double scale) {
        double yaw = Math.toRadians(rotationYaw);
        double pitch = Math.toRadians(rotationPitch);
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double along = -screenX * scale;
        double away = screenY * scale / Math.max(0.2D, Math.sin(pitch));
        targetX += -cos * along + -sin * away;
        targetZ += -sin * along + cos * away;
        place();
    }

    private void place() {
        double yaw = Math.toRadians(rotationYaw);
        double pitch = Math.toRadians(rotationPitch);
        double horizontal = Math.cos(pitch) * distance;
        setPosition(
            targetX + Math.sin(yaw) * horizontal,
            targetY + Math.sin(pitch) * distance,
            targetZ - Math.cos(yaw) * horizontal);
        prevPosX = lastTickPosX = posX;
        prevPosY = lastTickPosY = posY;
        prevPosZ = lastTickPosZ = posZ;
        prevRotationYaw = rotationYaw;
        prevRotationPitch = rotationPitch;
        rotationYawHead = prevRotationYawHead = rotationYaw;
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
    }

    @Override
    public ItemStack getHeldItem() {
        return null;
    }

    @Override
    public ItemStack getEquipmentInSlot(int slot) {
        return null;
    }

    @Override
    public void setCurrentItemOrArmor(int slot, ItemStack stack) {}

    @Override
    public ItemStack[] getLastActiveItems() {
        return NO_ITEMS;
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {}

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {}
}

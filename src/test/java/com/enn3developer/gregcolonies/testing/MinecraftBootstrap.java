package com.enn3developer.gregcolonies.testing;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.stats.StatList;
import net.minecraft.util.RegistryNamespaced;

import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import sun.misc.Unsafe;

public final class MinecraftBootstrap {

    private static boolean done;

    private MinecraftBootstrap() {}

    public static synchronized void ensure() {
        if (done) return;
        installFakeLoader();
        registerVanilla();
        done = true;
    }

    private static void installFakeLoader() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe unsafe = (Unsafe) theUnsafe.get(null);

            Loader loader = (Loader) unsafe.allocateInstance(Loader.class);
            set(loader, "mods", new ArrayList<>());
            set(loader, "namedMods", new HashMap<>());
            set(loader, "modController", new LoadController(loader));

            Field instance = Loader.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, loader);

            Field side = Class.forName("cpw.mods.fml.relauncher.FMLRelaunchLog")
                .getDeclaredField("side");
            side.setAccessible(true);
            if (side.get(null) == null) side.set(null, Side.SERVER);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("failed to install a fake FML Loader", error);
        }
    }

    private static void registerVanilla() {
        try {
            if (Block.blockRegistry.getObject("fire") == null) Block.registerBlocks();
            repair(Blocks.class, Block.blockRegistry);
            BlockFire.func_149843_e();

            if (Item.itemRegistry.getObject("diamond") == null) Item.registerItems();
            repair(Items.class, Item.itemRegistry);
            StatList.func_151178_a();
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("failed to register vanilla blocks and items", error);
        }
    }

    private static void repair(Class<?> holder, RegistryNamespaced registry) throws ReflectiveOperationException {
        for (Field field : holder.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            field.setAccessible(true);
            if (field.get(null) != null) continue;
            Object value = registry.getObject(field.getName());
            if (value == null) value = registry.getObject(deCamel(field.getName()));
            if (value != null) field.set(null, value);
        }
    }

    private static String deCamel(String name) {
        StringBuilder out = new StringBuilder(name.length() + 4);
        for (int index = 0; index < name.length(); index++) {
            char letter = name.charAt(index);
            if (Character.isUpperCase(letter)) {
                out.append('_')
                    .append(Character.toLowerCase(letter));
            } else {
                out.append(letter);
            }
        }
        return out.toString();
    }

    private static void set(Object target, String name, Object value) throws ReflectiveOperationException {
        Field field = target.getClass()
            .getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}

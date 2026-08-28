package com.enn3developer.gregcolonies.testing.gui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.FutureTask;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.settings.GameSettings;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.enn3developer.gregcolonies.client.GCKeyBindings;
import com.enn3developer.gregcolonies.testing.MinecraftBootstrap;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.IFMLSidedHandler;
import cpw.mods.fml.relauncher.Side;
import sun.misc.Unsafe;

public final class ClientBootstrap {

    public static final int WIDTH = 427;

    public static final int HEIGHT = 240;

    private static final String SCHEDULED = "field_152351_aB";

    private static final String ASCII = "/assets/minecraft/textures/font/ascii.png";

    private static final String GLYPHS = "/assets/minecraft/font/glyph_sizes.bin";

    private static final int GLYPH_PAGES = 65536;

    private static final int ASCII_CHARS = 256;

    private static final int ASCII_GRID = 16;

    private static final int FONT_HEIGHT = 9;

    private static final int COLOR_CODES = 32;

    private static final long START = System.nanoTime();

    private static final long MILLIS = 1_000_000L;

    private static MockedStatic<Minecraft> clock;

    private static Thread owner;

    private static boolean done;

    private ClientBootstrap() {}

    public static synchronized void ensure() {
        if (done) {
            checkThread();
            return;
        }
        MinecraftBootstrap.ensure();
        try {
            Unsafe unsafe = unsafe();
            installClientSide();
            Minecraft minecraft = (Minecraft) unsafe.allocateInstance(Minecraft.class);
            GameSettings settings = new GameSettings();
            settings.guiScale = 1;
            set(minecraft, "gameSettings", settings);
            set(minecraft, "mcLanguageManager", unsafe.allocateInstance(DefaultLanguage.class));
            set(minecraft, "fontRenderer", font(unsafe));
            set(minecraft, "mcSoundHandler", unsafe.allocateInstance(SilentSounds.class));
            set(minecraft, SCHEDULED, new ArrayDeque<FutureTask<?>>());
            set(minecraft, "displayWidth", WIDTH);
            set(minecraft, "displayHeight", HEIGHT);

            Field instance = Minecraft.class.getDeclaredField("theMinecraft");
            instance.setAccessible(true);
            instance.set(null, minecraft);

            GCKeyBindings.register();
            owner = Thread.currentThread();
            installClock();
        } catch (ReflectiveOperationException | IOException error) {
            throw new AssertionError("failed to install a headless Minecraft", error);
        }
        done = true;
    }

    @SuppressWarnings("unchecked")
    public static Queue<FutureTask<?>> scheduled() {
        try {
            Field field = Minecraft.class.getDeclaredField(SCHEDULED);
            field.setAccessible(true);
            return (Queue<FutureTask<?>>) field.get(minecraft());
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("failed to read the scheduled task queue", error);
        }
    }

    public static void display(int width, int height) {
        Minecraft minecraft = minecraft();
        minecraft.displayWidth = width;
        minecraft.displayHeight = height;
    }

    public static Minecraft minecraft() {
        ensure();
        return Minecraft.getMinecraft();
    }

    private static void checkThread() {
        if (Thread.currentThread() != owner) {
            throw new AssertionError(
                "the headless client was booted on thread " + owner.getName()
                    + " and only works there, but this ran on "
                    + Thread.currentThread()
                        .getName());
        }
    }

    private static void installClock() {
        clock = Mockito.mockStatic(Minecraft.class, Mockito.CALLS_REAL_METHODS);
        clock.when(Minecraft::getSystemTime)
            .thenAnswer(call -> (System.nanoTime() - START) / MILLIS);
    }

    private static void installClientSide() throws ReflectiveOperationException {
        FMLCommonHandler handler = FMLCommonHandler.instance();
        Field delegate = FMLCommonHandler.class.getDeclaredField("sidedDelegate");
        delegate.setAccessible(true);
        if (delegate.get(handler) != null) {
            return;
        }
        delegate.set(
            handler,
            Proxy.newProxyInstance(
                ClientBootstrap.class.getClassLoader(),
                new Class<?>[] { IFMLSidedHandler.class },
                (proxy, method, arguments) -> "getSide".equals(method.getName()) ? Side.CLIENT
                    : blank(method.getReturnType())));
    }

    private static Object blank(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) {
            return null;
        }
        if (type == boolean.class) {
            return Boolean.FALSE;
        }
        if (type == char.class) {
            return (char) 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0.0F;
        }
        if (type == double.class) {
            return 0.0D;
        }
        return 0;
    }

    private static FontRenderer font(Unsafe unsafe) throws ReflectiveOperationException, IOException {
        FontRenderer font = (FontRenderer) unsafe.allocateInstance(FontRenderer.class);
        set(font, "charWidth", asciiWidths());
        set(font, "glyphWidth", glyphWidths());
        set(font, "colorCode", new int[COLOR_CODES]);
        set(font, "fontRandom", new Random());
        set(font, "unicodeFlag", false);
        set(font, "bidiFlag", false);
        set(font, "FONT_HEIGHT", FONT_HEIGHT);
        return font;
    }

    private static int[] asciiWidths() throws IOException {
        BufferedImage image;
        try (InputStream in = open(ASCII)) {
            image = ImageIO.read(in);
        }
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        int[] pixels = new int[imageWidth * imageHeight];
        image.getRGB(0, 0, imageWidth, imageHeight, pixels, 0, imageWidth);

        int cellHeight = imageHeight / ASCII_GRID;
        int cellWidth = imageWidth / ASCII_GRID;
        float scale = (float) imageWidth / 128.0F;
        int[] widths = new int[ASCII_CHARS];
        for (int index = 0; index < ASCII_CHARS; index++) {
            int column = index % ASCII_GRID;
            int row = index / ASCII_GRID;
            int last = cellWidth - 1;
            while (last >= 0) {
                int x = column * cellWidth + last;
                boolean blank = true;
                for (int y = 0; y < cellHeight && blank; y++) {
                    if ((pixels[x + (row * cellWidth + y) * imageWidth] >> 24 & 255) != 0) {
                        blank = false;
                    }
                }
                if (!blank) {
                    break;
                }
                last--;
            }
            widths[index] = (int) (0.5D + (last + 1) * scale) + 1;
        }
        return widths;
    }

    private static byte[] glyphWidths() throws IOException {
        byte[] widths = new byte[GLYPH_PAGES];
        try (InputStream in = open(GLYPHS)) {
            int read = 0;
            while (read < widths.length) {
                int got = in.read(widths, read, widths.length - read);
                if (got < 0) {
                    break;
                }
                read += got;
            }
        }
        return widths;
    }

    private static InputStream open(String path) throws IOException {
        InputStream in = ClientBootstrap.class.getResourceAsStream(path);
        if (in == null) {
            throw new IOException("missing client resource " + path);
        }
        return in;
    }

    private static Unsafe unsafe() throws ReflectiveOperationException {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }

    private static void set(Object target, String name, Object value) throws ReflectiveOperationException {
        Field field = target.getClass()
            .getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}

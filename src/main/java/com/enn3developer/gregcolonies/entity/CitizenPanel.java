package com.enn3developer.gregcolonies.entity;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.init.Items;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.EntityDisplayWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.slot.SlotGroup;
import com.enn3developer.gregcolonies.GregColonies;
import com.enn3developer.gregcolonies.entity.diet.HungerModel;

final class CitizenPanel {

    private static final String ARMOR_GROUP = "citizen_armor";

    private static final String FOOD_GROUP = "citizen_food";

    private static final String TOOL_GROUP = "citizen_tool";

    private static final String MAIN_GROUP = "citizen_main";

    private static final String[] ARMOR_HINTS = { "Helmet", "Chestplate", "Leggings", "Boots" };

    private static final int ARMOR_PRIORITY = 40;

    private static final int FOOD_PRIORITY = 50;

    private static final int TOOL_PRIORITY = 60;

    private static final int MAIN_PRIORITY = 100;

    private static final int PANEL_WIDTH = 176;

    private static final int PANEL_HEIGHT = 225;

    private static final int PANEL_TITLE_COLOR = 0xFF404040;

    private static final int PANEL_TEXT_COLOR = 0xFF585F68;

    private static final int PANEL_TASK_COLOR = 0xFF1E5F72;

    private static final int PANEL_LINE_COLOR = 0x30000000;

    private static final int TITLE_ROW = 6;

    private static final int STATUS_ROW = 17;

    private static final int SEPARATOR_ROW = 28;

    private static final int TOP_ROW = 31;

    private static final int TASK_ROW = TOP_ROW + 76;

    private static final int MAIN_ROW = TOP_ROW + 88;

    private static final int TEXT_HEIGHT = 10;

    private static final int TEXT_MARGIN = 8;

    private static final int HUD_ICON = 9;

    private static final UITexture HEART_ICON = UITexture.builder()
        .location("minecraft", "gui/icons")
        .imageSize(256, 256)
        .subAreaXYWH(52, 0, HUD_ICON, HUD_ICON)
        .name("gregcolonies_heart")
        .build();

    private static final UITexture FOOD_ICON = UITexture.builder()
        .location("minecraft", "gui/icons")
        .imageSize(256, 256)
        .subAreaXYWH(52, 27, HUD_ICON, HUD_ICON)
        .name("gregcolonies_food")
        .build();

    private static final int PREVIEW_WIDTH = 54;

    private static final int PREVIEW_HEIGHT = 72;

    private static final int PREVIEW_INSET_X = 8;

    private static final int PREVIEW_INSET_TOP = 15;

    private static final int PREVIEW_INSET_BOTTOM = 5;

    private static final int PREVIEW_X = (PANEL_WIDTH - PREVIEW_WIDTH) / 2;

    private static final int SLOT_SIZE = 18;

    private static final int EDGE_MARGIN = 7;

    private static final int LEFT_COLUMN = EDGE_MARGIN;

    private static final int RIGHT_COLUMN = PANEL_WIDTH - EDGE_MARGIN - SLOT_SIZE;

    private static final int TOOL_ROW = TOP_ROW + SLOT_SIZE * 3 + 4;

    private static final int TEXT_WIDTH = PANEL_WIDTH - TEXT_MARGIN * 2;

    private static final int ICON_GAP = 3;

    private static final int ROW_GAP = 4;

    static ModularPanel build(EntityCitizen citizen, PanelSyncManager syncManager, UISettings settings) {
        CitizenInventory inventory = citizen.getInventory();
        syncManager.registerSlotGroup(new SlotGroup(ARMOR_GROUP, 1, ARMOR_PRIORITY, true));
        syncManager.registerSlotGroup(new SlotGroup(FOOD_GROUP, 1, FOOD_PRIORITY, true));
        syncManager.registerSlotGroup(new SlotGroup(TOOL_GROUP, 1, TOOL_PRIORITY, true));
        syncManager.registerSlotGroup(new SlotGroup(MAIN_GROUP, CitizenInventory.MAIN_SLOTS, MAIN_PRIORITY, true));
        syncManager.addOpenListener(viewer -> {
            citizen.addViewer(viewer);
            citizen.getNavigator()
                .clearPathEntity();
        });
        syncManager.addCloseListener(citizen::removeViewer);
        settings.canInteractWith(
            viewer -> viewer.worldObj == citizen.worldObj && citizen.isEntityAlive()
                && citizen.getDistanceSqToEntity(viewer) <= EntityCitizen.VIEW_RANGE_SQ);

        StringSyncValue task = new StringSyncValue(citizen::describeActivity);
        StringSyncValue group = new StringSyncValue(() -> groupLabel(citizen));
        IntSyncValue food = new IntSyncValue(citizen.getDiet()::getFoodLevel);
        syncManager.syncValue("task", task);
        syncManager.syncValue("group", group);
        syncManager.syncValue("food", food);

        ModularPanel panel = ModularPanel.defaultPanel("citizen_inventory", PANEL_WIDTH, PANEL_HEIGHT);
        panel.child(
            textRow(
                TITLE_ROW,
                IKey.dynamic(() -> rowLabel(citizen.getCitizenName(), healthLabel(citizen))),
                PANEL_TITLE_COLOR,
                iconValue(HEART_ICON, IKey.dynamic(() -> healthLabel(citizen)), PANEL_TITLE_COLOR)));
        panel.child(
            textRow(
                STATUS_ROW,
                IKey.dynamic(() -> rowLabel(group.getValue(), foodLabel(food.getIntValue()))),
                PANEL_TEXT_COLOR,
                iconValue(FOOD_ICON, IKey.dynamic(() -> foodLabel(food.getIntValue())), PANEL_TEXT_COLOR)));
        panel.child(
            new Widget<>().size(TEXT_WIDTH, 1)
                .pos(TEXT_MARGIN, SEPARATOR_ROW)
                .background(new Rectangle().color(PANEL_LINE_COLOR)));
        panel.child(
            IKey.dynamic(() -> GregColonies.proxy.trimText(task.getValue(), TEXT_WIDTH))
                .asWidget()
                .color(PANEL_TASK_COLOR)
                .size(TEXT_WIDTH, TEXT_HEIGHT)
                .pos(TEXT_MARGIN, TASK_ROW));
        panel.child(
            SlotGroupWidget.builder()
                .row("A")
                .row("A")
                .row("A")
                .row("A")
                .key(
                    'A',
                    index -> hint(
                        new ItemSlot().slot(
                            new ModularSlot(inventory.getArmor(), index).slotGroup(ARMOR_GROUP)
                                .filter(stack -> CitizenInventory.isArmor(stack, index, citizen)))
                            .backgroundOverlay(
                                GregColonies.proxy.armorSlotIcon(
                                    index,
                                    () -> inventory.getArmor()
                                        .getStackInSlot(index) == null)),
                        ARMOR_HINTS[index]))
                .build()
                .pos(LEFT_COLUMN, TOP_ROW));
        panel.child(
            SlotGroupWidget.builder()
                .row("F")
                .row("F")
                .row("F")
                .key(
                    'F',
                    index -> hint(
                        new ItemSlot().slot(
                            new ModularSlot(inventory.getFood(), index).slotGroup(FOOD_GROUP)
                                .filter(CitizenInventory::isFood))
                            .backgroundOverlay(
                                GregColonies.proxy.itemSlotIcon(
                                    Items.bread,
                                    () -> inventory.getFood()
                                        .getStackInSlot(index) == null)),
                        "Food"))
                .build()
                .pos(RIGHT_COLUMN, TOP_ROW));
        panel.child(
            hint(
                new ItemSlot().slot(
                    new ModularSlot(inventory.getTool(), 0).slotGroup(TOOL_GROUP)
                        .filter(CitizenInventory::isTool))
                    .backgroundOverlay(
                        GregColonies.proxy.itemSlotIcon(
                            Items.iron_pickaxe,
                            () -> inventory.getTool()
                                .getStackInSlot(0) == null)),
                "Tool").pos(RIGHT_COLUMN, TOOL_ROW));
        IDrawable display = new EntityDisplayWidget(() -> citizen).doesLookAtMouse(true);
        panel.child(
            new Widget<>().size(PREVIEW_WIDTH, PREVIEW_HEIGHT)
                .pos(PREVIEW_X, TOP_ROW)
                .background(GuiTextures.DISPLAY, (context, x, y, width, height, theme) -> {
                    EntityCitizen.setPreviewRender(true);
                    try {
                        display.draw(
                            context,
                            x + PREVIEW_INSET_X,
                            y + PREVIEW_INSET_TOP,
                            width - PREVIEW_INSET_X * 2,
                            height - PREVIEW_INSET_TOP - PREVIEW_INSET_BOTTOM,
                            theme);
                    } finally {
                        EntityCitizen.setPreviewRender(false);
                    }
                }));
        panel.child(
            SlotGroupWidget.builder()
                .row("IIIIIIIII")
                .key(
                    'I',
                    index -> new ItemSlot().slot(new ModularSlot(inventory.getMain(), index).slotGroup(MAIN_GROUP)))
                .build()
                .pos(LEFT_COLUMN, MAIN_ROW));
        panel.child(SlotGroupWidget.playerInventory(true));
        return panel;
    }

    private static Flow textRow(int y, IKey left, int leftColor, IWidget right) {
        return Flow.row()
            .widthRel(1.0F)
            .height(TEXT_HEIGHT)
            .pos(0, y)
            .padding(TEXT_MARGIN, 0)
            .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(
                left.asWidget()
                    .color(leftColor))
            .child(right);
    }

    private static Flow iconValue(UITexture icon, IKey value, int color) {
        return Flow.row()
            .coverChildren()
            .childPadding(3)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(
                icon.asWidget()
                    .size(HUD_ICON, HUD_ICON))
            .child(
                value.asWidget()
                    .color(color));
    }

    private static ItemSlot hint(ItemSlot slot, String text) {
        slot.tooltip()
            .add(IKey.str(text));
        return slot;
    }

    private static String groupLabel(EntityCitizen citizen) {
        CitizenJob job = citizen.getJob();
        String label = job == CitizenJob.NONE ? citizen.getGroup() : job.getLabel();
        if (label.isEmpty()) {
            label = "no group";
        }
        String gender = citizen.describeGender();
        return gender.isEmpty() ? label : gender + ", " + label;
    }

    private static String foodLabel(int level) {
        return level + " / " + HungerModel.MAX_FOOD_LEVEL;
    }

    private static String rowLabel(String left, String right) {
        int used = HUD_ICON + ICON_GAP + GregColonies.proxy.textWidth(right) + ROW_GAP;
        return GregColonies.proxy.trimText(left, TEXT_WIDTH - used);
    }

    private static String healthLabel(EntityCitizen citizen) {
        return String.format(
            "%.0f / %.0f",
            citizen.getHealth(),
            (float) citizen.getEntityAttribute(SharedMonsterAttributes.maxHealth)
                .getAttributeValue());
    }
}

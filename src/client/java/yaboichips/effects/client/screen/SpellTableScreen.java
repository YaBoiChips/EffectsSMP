package yaboichips.effects.client.screen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import yaboichips.effects.common.menu.SpellTableMenu;
import yaboichips.effects.common.spell.Spell;
import yaboichips.effects.core.ModAttachments;
import yaboichips.effects.core.ModComponents;
import yaboichips.effects.core.ModSpells;
import yaboichips.effects.data.SpellBookData;
import yaboichips.effects.network.EquipSpellPayload;
import yaboichips.effects.network.UnlockSpellPayload;
import yaboichips.effects.network.UpgradeSpellPayload;

public class SpellTableScreen extends AbstractContainerScreen<SpellTableMenu> {

    private static final int LIST_ICON_SIZE = 40;
    private static final int LIST_ICON_NATIVE_SIZE = 64;
    private static final int LIST_ITEM_GAP = 10;
    private static final int LIST_VISIBLE_ITEMS = 4;
    private static final int LIST_CONTAINER_HEIGHT = LIST_VISIBLE_ITEMS * LIST_ICON_SIZE + (LIST_VISIBLE_ITEMS - 1) * LIST_ITEM_GAP;
    private static final int SCROLL_ARROW_GAP = 4;
    private static final int SCROLL_ARROW_HEIGHT = 9;
    private static final int BOX_ICON_SIZE = 64;
    private static final int LEVEL_ICON_SIZE = 32;
    private static final int COLUMN_GAP = 20;
    private static final int BOX_WIDTH = 110;
    private static final int SLOT_SIZE = 18;

    private static final int PASSIVE_COLOR = 0xFFADD8E6;
    private static final int AGGRESSIVE_COLOR = 0xFF8B0000;
    private static final int EQUIPPED_HIGHLIGHT_COLOR = 0xFFFFD700;
    private static final int SLOT_FRAME_COLOR = 0xFF555555;

    private final List<ClickZone> clickZones = new ArrayList<>();
    private final List<HoverZone> hoverZones = new ArrayList<>();

    private int passiveScroll;
    private int aggressiveScroll;
    private int passiveListX;
    private int passiveListY;
    private int aggressiveListX;
    private int aggressiveListY;
    private int spellPoints;

    public SpellTableScreen(SpellTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = SpellTableMenu.PANEL_WIDTH;
        this.imageHeight = SpellTableMenu.PANEL_HEIGHT;
        this.inventoryLabelX = SpellTableMenu.MAIN_INV_X;
        this.inventoryLabelY = SpellTableMenu.MAIN_INV_Y - 12;
    }

    /**
     * The base class's title/inventory label positions default to (0,0)
     * unless a subclass sets them, which would draw both labels stacked on
     * top of each other in the panel's top-left corner - and the title would
     * duplicate the one already drawn in renderBg. Override outright instead
     * of just repositioning: draw only the inventory label, in the gap left
     * for it above the player inventory grid.
     */
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private SpellBookData bookData() {
        ItemStack book = this.menu.getBookSlot().getItem();
        return book.getOrDefault(ModComponents.SPELL_BOOK_DATA, SpellBookData.DEFAULT);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fill(leftPos - 2, topPos - 2, leftPos + imageWidth + 2, topPos + imageHeight + 2, 0xFF000000);
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF2B2B2B);

        guiGraphics.drawCenteredString(this.font, this.title, leftPos + imageWidth / 2, topPos + 10, 0xFFFFFFFF);

        if (this.minecraft != null && this.minecraft.player != null) {
            this.spellPoints = this.minecraft.player.getAttachedOrCreate(ModAttachments.SPELL_DATA).spellPoints();
        }
        String pointsText = Component.translatable("gui.effects.spell_points").getString() + spellPoints;
        int pointsTextWidth = this.font.width(pointsText);
        int pointsX = leftPos + imageWidth - 12 - pointsTextWidth;
        int pointsY = topPos + 10;
        guiGraphics.fill(pointsX - 4, pointsY - 3, pointsX + pointsTextWidth + 4, pointsY + 10, 0xAA000000);
        guiGraphics.drawString(this.font, pointsText, pointsX, pointsY, 0xFFFFD700);

        for (Slot slot : this.menu.slots) {
            drawBorder(guiGraphics, leftPos + slot.x - 1, topPos + slot.y - 1, SLOT_SIZE, SLOT_SIZE, SLOT_FRAME_COLOR);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        clickZones.clear();
        hoverZones.clear();

        SpellBookData data = bookData();

        int x = leftPos + COLUMN_GAP;
        int listTop = topPos + 48;
        int boxTop = topPos + 32;

        passiveListX = x;
        passiveListY = listTop;
        x += LIST_ICON_SIZE + COLUMN_GAP;
        int passiveBoxX = x;
        x += BOX_WIDTH + COLUMN_GAP;
        int aggressiveBoxX = x;
        x += BOX_WIDTH + COLUMN_GAP;
        aggressiveListX = x;
        aggressiveListY = listTop;

        renderSpellList(guiGraphics, passiveListX, listTop, ModSpells.passiveSpells(), data, data.equippedPassive(), PASSIVE_COLOR,
                () -> passiveScroll, v -> passiveScroll = v);
        renderSpellList(guiGraphics, aggressiveListX, listTop, ModSpells.aggressiveSpells(), data, data.equippedAggressive(), AGGRESSIVE_COLOR,
                () -> aggressiveScroll, v -> aggressiveScroll = v);

        renderEquippedBox(guiGraphics, passiveBoxX, boxTop, data, data.equippedPassive(), 1, PASSIVE_COLOR);
        renderEquippedBox(guiGraphics, aggressiveBoxX, boxTop, data, data.equippedAggressive(), 2, AGGRESSIVE_COLOR);

        for (HoverZone zone : hoverZones) {
            if (zone.contains(mouseX, mouseY)) {
                guiGraphics.setTooltipForNextFrame(this.font, zone.tooltip(), mouseX, mouseY);
                break;
            }
        }
    }

    private void renderSpellList(GuiGraphics guiGraphics, int listX, int listY,
                                  Collection<? extends Spell> spellCollection, SpellBookData data,
                                  Optional<Identifier> equippedId, int outlineColor,
                                  IntSupplier scrollGetter, IntConsumer scrollSetter) {
        List<? extends Spell> spells = new ArrayList<>(spellCollection);
        int total = spells.size();
        int maxScroll = Math.max(0, total - LIST_VISIBLE_ITEMS);
        int scroll = Math.max(0, Math.min(scrollGetter.getAsInt(), maxScroll));
        scrollSetter.accept(scroll);

        drawBorder(guiGraphics, listX - 4, listY - 4, LIST_ICON_SIZE + 8, LIST_CONTAINER_HEIGHT + 8, outlineColor);

        guiGraphics.enableScissor(listX - 4, listY - 4, listX + LIST_ICON_SIZE + 4, listY + LIST_CONTAINER_HEIGHT + 4);

        int y = listY;
        int end = Math.min(total, scroll + LIST_VISIBLE_ITEMS);
        for (int i = scroll; i < end; i++) {
            Spell spell = spells.get(i);
            boolean unlocked = data.isUnlocked(spell.id());
            boolean equipped = equippedId.map(spell.id()::equals).orElse(false);

            drawIcon(guiGraphics, spell.icon(), listX, y, LIST_ICON_SIZE, LIST_ICON_NATIVE_SIZE);
            if (!unlocked) {
                guiGraphics.fill(listX, y, listX + LIST_ICON_SIZE, y + LIST_ICON_SIZE, 0xA0000000);
            } else if (equipped) {
                drawBorder(guiGraphics, listX, y, LIST_ICON_SIZE, LIST_ICON_SIZE, EQUIPPED_HIGHLIGHT_COLOR);
            }

            int zoneX = listX;
            int zoneY = y;
            clickZones.add(new ClickZone(zoneX, zoneY, zoneX + LIST_ICON_SIZE, zoneY + LIST_ICON_SIZE, () -> {
                if (!unlocked) {
                    if (this.spellPoints >= 1) {
                        ClientPlayNetworking.send(new UnlockSpellPayload(spell.id()));
                        playClickSound();
                    }
                } else if (!equipped) {
                    ClientPlayNetworking.send(new EquipSpellPayload(spell.id()));
                    playClickSound();
                }
            }));
            hoverZones.add(new HoverZone(zoneX, zoneY, zoneX + LIST_ICON_SIZE, zoneY + LIST_ICON_SIZE, spell.name()));

            y += LIST_ICON_SIZE + LIST_ITEM_GAP;
        }

        guiGraphics.disableScissor();

        if (total > LIST_VISIBLE_ITEMS) {
            int arrowCenterX = listX + LIST_ICON_SIZE / 2;
            if (scroll > 0) {
                int upArrowY = listY - 4 - SCROLL_ARROW_GAP - SCROLL_ARROW_HEIGHT;
                guiGraphics.drawCenteredString(this.font, "▲", arrowCenterX, upArrowY, 0xFFFFFFFF);
                clickZones.add(new ClickZone(listX, upArrowY - 2, listX + LIST_ICON_SIZE, upArrowY + SCROLL_ARROW_HEIGHT + 2,
                        () -> scrollSetter.accept(Math.max(0, scrollGetter.getAsInt() - 1))));
            }
            if (scroll < maxScroll) {
                int downArrowY = listY + LIST_CONTAINER_HEIGHT + 4 + SCROLL_ARROW_GAP;
                guiGraphics.drawCenteredString(this.font, "▼", arrowCenterX, downArrowY, 0xFFFFFFFF);
                clickZones.add(new ClickZone(listX, downArrowY - 2, listX + LIST_ICON_SIZE, downArrowY + SCROLL_ARROW_HEIGHT + 2,
                        () -> scrollSetter.accept(Math.min(maxScroll, scrollGetter.getAsInt() + 1))));
            }
        }
    }

    private void renderEquippedBox(GuiGraphics guiGraphics, int boxX, int boxY,
                                    SpellBookData data, Optional<Identifier> equippedId, int upgradeLevels, int outlineColor) {
        guiGraphics.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + 220, 0xFF1A1A1A);
        drawBorder(guiGraphics, boxX, boxY, BOX_WIDTH, 220, outlineColor);

        if (equippedId.isEmpty()) {
            return;
        }
        Identifier spellId = equippedId.get();
        Spell spell = ModSpells.get(spellId);
        if (spell == null) {
            return;
        }
        int currentLevel = data.levelOf(spellId);

        int iconX = boxX + (BOX_WIDTH - BOX_ICON_SIZE) / 2;
        int iconY = boxY + 220 - BOX_ICON_SIZE - 14;
        drawIcon(guiGraphics, spell.icon(), iconX, iconY, BOX_ICON_SIZE, BOX_ICON_SIZE);
        drawBorder(guiGraphics, iconX, iconY, BOX_ICON_SIZE, BOX_ICON_SIZE, EQUIPPED_HIGHLIGHT_COLOR);
        hoverZones.add(new HoverZone(iconX, iconY, iconX + BOX_ICON_SIZE, iconY + BOX_ICON_SIZE, spell.description()));

        int rungY = iconY;
        for (int i = 1; i <= upgradeLevels; i++) {
            int targetLevel = 1 + i;
            if (targetLevel > spell.maxLevel()) {
                break;
            }
            boolean reached = currentLevel >= targetLevel;
            boolean available = currentLevel == targetLevel - 1;
            boolean affordable = this.spellPoints >= 1;

            int arrowY = rungY - 18;
            guiGraphics.drawCenteredString(this.font, "▲", boxX + BOX_WIDTH / 2, arrowY, 0xFFFFFFFF);

            int levelIconY = arrowY - LEVEL_ICON_SIZE - 6;
            int levelIconX = boxX + (BOX_WIDTH - LEVEL_ICON_SIZE) / 2;
            Identifier levelIcon = Identifier.fromNamespaceAndPath(spellId.getNamespace(), "textures/gui/spell/level_" + targetLevel + ".png");
            drawIcon(guiGraphics, levelIcon, levelIconX, levelIconY, LEVEL_ICON_SIZE, LEVEL_ICON_SIZE);
            hoverZones.add(new HoverZone(levelIconX, levelIconY, levelIconX + LEVEL_ICON_SIZE, levelIconY + LEVEL_ICON_SIZE,
                    spell.upgradeDescription(targetLevel)));

            if (!reached && (!available || !affordable)) {
                guiGraphics.fill(levelIconX, levelIconY, levelIconX + LEVEL_ICON_SIZE, levelIconY + LEVEL_ICON_SIZE, 0x90000000);
            } else if (reached) {
                drawBorder(guiGraphics, levelIconX, levelIconY, LEVEL_ICON_SIZE, LEVEL_ICON_SIZE, EQUIPPED_HIGHLIGHT_COLOR);
            }

            if (available && affordable) {
                clickZones.add(new ClickZone(levelIconX, levelIconY, levelIconX + LEVEL_ICON_SIZE, levelIconY + LEVEL_ICON_SIZE,
                        () -> {
                            ClientPlayNetworking.send(new UpgradeSpellPayload(spellId));
                            playClickSound();
                        }));
            }

            rungY = levelIconY;
        }
    }

    private void drawIcon(GuiGraphics guiGraphics, Identifier texture, int x, int y, int drawSize, int nativeSize) {
        // dest size (drawSize) and source-sample size (nativeSize) must be passed
        // separately here - the shorter 10-arg blit overload forces them to be
        // the same value, which just crops to the top-left corner when they differ.
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f,
                drawSize, drawSize, nativeSize, nativeSize, nativeSize, nativeSize);
    }

    private void drawBorder(GuiGraphics guiGraphics, int x, int y, int w, int h, int color) {
        guiGraphics.fill(x - 1, y - 1, x + w + 1, y, color);
        guiGraphics.fill(x - 1, y + h, x + w + 1, y + h + 1, color);
        guiGraphics.fill(x - 1, y, x, y + h, color);
        guiGraphics.fill(x + w, y, x + w + 1, y + h, color);
    }

    private void playClickSound() {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        for (ClickZone zone : clickZones) {
            if (zone.contains(event.x(), event.y())) {
                zone.action().run();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isOverList(mouseX, mouseY, passiveListX, passiveListY)) {
            int maxScroll = Math.max(0, ModSpells.passiveSpells().size() - LIST_VISIBLE_ITEMS);
            passiveScroll = Math.max(0, Math.min(passiveScroll - (int) Math.signum(scrollY), maxScroll));
            return true;
        }
        if (isOverList(mouseX, mouseY, aggressiveListX, aggressiveListY)) {
            int maxScroll = Math.max(0, ModSpells.aggressiveSpells().size() - LIST_VISIBLE_ITEMS);
            aggressiveScroll = Math.max(0, Math.min(aggressiveScroll - (int) Math.signum(scrollY), maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean isOverList(double mouseX, double mouseY, int listX, int listY) {
        return mouseX >= listX - 4 && mouseX < listX + LIST_ICON_SIZE + 4
                && mouseY >= listY - 4 && mouseY < listY + LIST_CONTAINER_HEIGHT + 4;
    }

    private record ClickZone(int x1, int y1, int x2, int y2, Runnable action) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x1 && mouseX < x2 && mouseY >= y1 && mouseY < y2;
        }
    }

    private record HoverZone(int x1, int y1, int x2, int y2, Component tooltip) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x1 && mouseX < x2 && mouseY >= y1 && mouseY < y2;
        }
    }
}

package yaboichips.effects.client.screen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import yaboichips.effects.Effects;
import yaboichips.effects.common.menu.SpellTableMenu;
import yaboichips.effects.common.spell.Spell;
import yaboichips.effects.core.ModAttachments;
import yaboichips.effects.core.ModComponents;
import yaboichips.effects.core.ModSpells;
import yaboichips.effects.data.SpellBookData;
import yaboichips.effects.network.EquipSpellPayload;
import yaboichips.effects.network.UnlockSpellPayload;
import yaboichips.effects.network.UpgradeSpellPayload;

/**
 * The spell table is an open book. Left to right (x in panel space, see
 * SpellTableMenu for the panel's size and why it is that size): a column of
 * spell tabs in the left page's margin, the passive box, the book slot on the
 * spine, the aggressive box, and a column of tabs in the right page's margin.
 * The player inventory sits on a plate hanging off the book's lower edge.
 *
 * <pre>
 *   [ blue ] [ passive    ] S [ aggressive ] [  red  ]     S = book slot
 *   [ tabs ] [ box 80x160 ]   [ box 80x160 ] [ tabs  ]
 *            [      player inventory       ]
 * </pre>
 *
 * The book, boxes and corner pieces are drawn 1:1, so their sizes below are
 * the PNGs' own pixel sizes. The tabs are drawn at half size.
 */
public class SpellTableScreen extends AbstractContainerScreen<SpellTableMenu> {

    // ---- Art (assets/effects/textures/gui/spell/) ----

    private static final Identifier BOOK_BACKGROUND = guiTexture("book_background.png");
    private static final Identifier BLUE_TAB = guiTexture("blue_tab.png");
    private static final Identifier RED_TAB = guiTexture("red_tab.png");
    private static final Identifier DEFAULT_SPELL_BACKGROUND = guiTexture("background/default.png");
    private static final Identifier TOP_PING = guiTexture("background/top_ping.png");
    private static final Identifier BOTTOM_PING = guiTexture("background/bottom_ping.png");

    /** Spell ids end in this; the matching background file name doesn't (archer_spell -> archer.png). */
    private static final String SPELL_ID_SUFFIX = "_spell";

    /** Source art is a 384x240 canvas whose opaque part is inset 43px left/right and 22px from the top. */
    private static final int BOOK_TEXTURE_WIDTH = 384;
    private static final int BOOK_TEXTURE_HEIGHT = 240;
    /** Where that canvas's top-left lands in panel space (the panel starts 3px left of the opaque book, at its top edge). */
    private static final int BOOK_TEXTURE_X = -40;
    private static final int BOOK_TEXTURE_Y = -22;

    // ---- Spell tabs ----
    // The tab art (62x48) has a checkerboard placeholder painted into it (not
    // real transparency) where the spell icon belongs: a 28x28 square. The red
    // tab is the blue tab mirrored, so its square sits 14px further right.
    //
    // At full size a column of them is wider than a page margin and taller than
    // the book, so they're drawn at 1/TAB_SHRINK of the art's size. 2 is the
    // value that suits this art: 62, 48 and the icon square's size and offsets
    // (28, 10, 24) are all even, so the square lands on whole pixels.

    private static final int TAB_TEXTURE_WIDTH = 62;
    private static final int TAB_TEXTURE_HEIGHT = 48;
    private static final int TAB_SHRINK = 2;
    private static final int TAB_WIDTH = TAB_TEXTURE_WIDTH / TAB_SHRINK;
    private static final int TAB_HEIGHT = TAB_TEXTURE_HEIGHT / TAB_SHRINK;
    private static final int TAB_ICON_SIZE = 28 / TAB_SHRINK;
    private static final int TAB_ICON_Y = 10 / TAB_SHRINK;
    private static final int BLUE_TAB_ICON_X = 10 / TAB_SHRINK;
    private static final int RED_TAB_ICON_X = 24 / TAB_SHRINK;
    /**
     * Added to the source UV when drawing a tab. With none, the spot a
     * destination pixel samples can land exactly on the seam between two source
     * pixels (at every pixel at GUI scale 1, every third at scale 3, and so on
     * for odd scales), and nearest-neighbour then picks one side or the other
     * depending on GPU rounding. A tenth of a source pixel moves every sample
     * off the seams so the same pixel is picked on every machine, and is too
     * small to shift anything visibly.
     */
    private static final float TAB_SAMPLE_NUDGE = 0.1f;
    private static final int SPELL_ICON_NATIVE_SIZE = 64;
    /** All current spells fit at once (7 passive, 6 aggressive); a longer list scrolls. */
    private static final int LIST_VISIBLE_ITEMS = 7;
    private static final int LIST_HEIGHT = LIST_VISIBLE_ITEMS * TAB_HEIGHT;

    // ---- Equipped-spell boxes ----

    private static final int BOX_WIDTH = 80;
    private static final int BOX_HEIGHT = 160;
    private static final int PING_SIZE = 22;
    private static final int BOX_ICON_SIZE = 32;
    private static final int BOX_ICON_BOTTOM_MARGIN = 16;
    private static final int LEVEL_ICON_SIZE = 32;
    private static final int LEVEL_ICON_NATIVE_SIZE = 32;
    private static final int LEVEL_ICON_GAP = 2;
    /** Vertical room between two rungs of the upgrade ladder that the arrow button takes. */
    private static final int LEVEL_ARROW_ROW = 12;

    // ---- Panel layout (panel space) ----

    /** The gap between the two boxes, on the spine, that holds the book slot. */
    private static final int SPINE_GAP = 20;
    private static final int LEFT_BOX_X = (SpellTableMenu.PANEL_WIDTH - SPINE_GAP) / 2 - BOX_WIDTH;
    private static final int RIGHT_BOX_X = (SpellTableMenu.PANEL_WIDTH + SPINE_GAP) / 2;
    private static final int BOX_Y = 4;

    /**
     * The tab columns sit on the pages, in the margin between the book's cover
     * and the boxes. They start 2px below the boxes' top edge because the
     * book's torn top edge has small dents that a tab flush with the boxes
     * would poke into.
     */
    private static final int LEFT_TAB_X = 27;
    private static final int RIGHT_TAB_X = SpellTableMenu.PANEL_WIDTH - LEFT_TAB_X - TAB_WIDTH;
    private static final int LIST_Y = BOX_Y + 2;

    /** Only shown when a list has more spells than fit: in the page margin, level with its first and last tab. */
    private static final int SCROLL_BUTTON_WIDTH = 11;
    private static final int SCROLL_BUTTON_HEIGHT = 9;
    private static final int SCROLL_BUTTON_GAP = 2;

    // The inventory sits on a plate that hangs off the book's lower edge: a
    // centre block behind the slots, and a footer the width of the panel
    // beside the hotbar that carries the points counter and the title. The plate
    // starts 2px below the boxes; its slots are SpellTableMenu's MAIN_INV_Y and
    // HOTBAR_Y, so all three move together.
    private static final int SLOT_SIZE = 18;
    private static final int PLATE_X = 67;
    private static final int PLATE_Y = 167;
    private static final int PLATE_WIDTH = 170;
    private static final int PLATE_HEIGHT = 81;
    private static final int WING_Y = 224;
    private static final int WING_HEIGHT = 24;
    private static final int WING_WIDTH = 68;
    private static final int WING_TEXT_PADDING = 3;
    private static final int WING_MAX_LINES = 2;

    // ---- Colours ----

    private static final int PLATE_BORDER_COLOR = 0xFF000000;
    private static final int PLATE_COLOR = 0xFF2B2B2B;
    private static final int SLOT_BACKING_COLOR = 0xFF1A1A1A;
    private static final int SLOT_FRAME_COLOR = 0xFF555555;
    private static final int ICON_BACKING_COLOR = 0xFF1A1A1A;
    private static final int EQUIPPED_HIGHLIGHT_COLOR = 0xFFFFD700;
    private static final int POINTS_COLOR = 0xFFFFD700;
    private static final int TITLE_COLOR = 0xFFFFFFFF;

    private final List<ClickZone> clickZones = new ArrayList<>();
    private final List<HoverZone> hoverZones = new ArrayList<>();
    private final Map<Identifier, Identifier> spellBackgrounds = new HashMap<>();

    private int passiveScroll;
    private int aggressiveScroll;
    private int spellPoints;

    public SpellTableScreen(SpellTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = SpellTableMenu.PANEL_WIDTH;
        this.imageHeight = SpellTableMenu.PANEL_HEIGHT;
    }

    /**
     * The base class would draw the title and "Inventory" at fixed offsets
     * inside the panel, which in this layout is the book page. There's no room
     * for either label there, so draw none: the title is drawn by
     * {@link #drawFooterText} instead and the inventory is self-explanatory.
     */
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private SpellBookData bookData() {
        ItemStack book = this.menu.getBookSlot().getItem();
        return book.getOrDefault(ModComponents.SPELL_BOOK_DATA, SpellBookData.DEFAULT);
    }

    /**
     * Everything visual is drawn here, not in render(): the base class calls
     * renderBg before it draws the slots, their items and the item being
     * dragged, so all of those stay on top of the (now very large) art.
     * The click/hover zones are rebuilt here every frame for the same reason;
     * render() only has to look them up.
     */
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        clickZones.clear();
        hoverZones.clear();

        if (this.minecraft != null && this.minecraft.player != null) {
            this.spellPoints = this.minecraft.player.getAttachedOrCreate(ModAttachments.SPELL_DATA).spellPoints();
        }

        drawTexture(guiGraphics, BOOK_BACKGROUND, leftPos + BOOK_TEXTURE_X, topPos + BOOK_TEXTURE_Y,
                BOOK_TEXTURE_WIDTH, BOOK_TEXTURE_HEIGHT);

        drawPlate(guiGraphics);
        for (Slot slot : this.menu.slots) {
            drawSlot(guiGraphics, slot);
        }
        drawFooterText(guiGraphics);

        SpellBookData data = bookData();

        renderSpellList(guiGraphics, LEFT_TAB_X, BLUE_TAB, BLUE_TAB_ICON_X, ModSpells.passiveSpells(), data,
                data.equippedPassive(), () -> passiveScroll, v -> passiveScroll = v);
        renderSpellList(guiGraphics, RIGHT_TAB_X, RED_TAB, RED_TAB_ICON_X, ModSpells.aggressiveSpells(), data,
                data.equippedAggressive(), () -> aggressiveScroll, v -> aggressiveScroll = v);

        renderEquippedBox(guiGraphics, leftPos + LEFT_BOX_X, topPos + BOX_Y, data, data.equippedPassive(), 1);
        renderEquippedBox(guiGraphics, leftPos + RIGHT_BOX_X, topPos + BOX_Y, data, data.equippedAggressive(), 2);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // AbstractContainerScreen draws the slots and the dragged item but not
        // the tooltip of the item under the cursor: each screen has to ask for
        // it after rendering, as vanilla's own do. Only the first tooltip set in
        // a frame is shown; the hovered slot and the zones below never overlap.
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        for (HoverZone zone : hoverZones) {
            if (zone.contains(mouseX, mouseY)) {
                guiGraphics.setTooltipForNextFrame(this.font, zone.tooltip(), mouseX, mouseY);
                break;
            }
        }
    }

    /**
     * Black frame plus dark fill, the same look the whole panel used to have.
     * The centre block's fill also covers the footer's top frame where the two
     * meet, so they read as one shape.
     */
    private void drawPlate(GuiGraphics guiGraphics) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.fill(x + PLATE_X - 1, y + PLATE_Y - 1, x + PLATE_X + PLATE_WIDTH + 1, y + PLATE_Y + PLATE_HEIGHT + 1, PLATE_BORDER_COLOR);
        guiGraphics.fill(x, y + WING_Y - 1, x + SpellTableMenu.PANEL_WIDTH, y + WING_Y + WING_HEIGHT + 1, PLATE_BORDER_COLOR);
        guiGraphics.fill(x + PLATE_X, y + PLATE_Y, x + PLATE_X + PLATE_WIDTH, y + PLATE_Y + PLATE_HEIGHT, PLATE_COLOR);
        guiGraphics.fill(x + 1, y + WING_Y, x + SpellTableMenu.PANEL_WIDTH - 1, y + WING_Y + WING_HEIGHT, PLATE_COLOR);
        guiGraphics.fill(x + PLATE_X, y + WING_Y - 1, x + PLATE_X + PLATE_WIDTH, y + WING_Y, PLATE_COLOR);
    }

    /** Opaque backing so the slot reads on the book page as well as on the plate. */
    private void drawSlot(GuiGraphics guiGraphics, Slot slot) {
        int x = leftPos + slot.x - 1;
        int y = topPos + slot.y - 1;
        guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_BACKING_COLOR);
        drawBorder(guiGraphics, x, y, SLOT_SIZE, SLOT_SIZE, SLOT_FRAME_COLOR);
    }

    /**
     * The footer has a wing either side of the hotbar. There isn't room on the
     * book for a title row, so the points counter goes in the left wing and the
     * title in the right. Wrapped to the wing's width, so a long translation
     * degrades to two lines instead of running into the hotbar.
     */
    private void drawFooterText(GuiGraphics guiGraphics) {
        Component points = Component.literal(Component.translatable("gui.effects.spell_points").getString() + spellPoints);
        drawWingText(guiGraphics, points, 1, POINTS_COLOR);
        drawWingText(guiGraphics, this.title, SpellTableMenu.PANEL_WIDTH - 1 - WING_WIDTH, TITLE_COLOR);
    }

    private void drawWingText(GuiGraphics guiGraphics, Component text, int wingX, int color) {
        List<FormattedCharSequence> lines = this.font.split(text, WING_WIDTH - 2 * WING_TEXT_PADDING);
        int count = Math.min(lines.size(), WING_MAX_LINES);
        int y = topPos + WING_Y + (WING_HEIGHT - count * this.font.lineHeight) / 2;
        for (int i = 0; i < count; i++) {
            FormattedCharSequence line = lines.get(i);
            int x = leftPos + wingX + (WING_WIDTH - this.font.width(line)) / 2;
            guiGraphics.drawString(this.font, line, x, y + i * this.font.lineHeight, color, true);
        }
    }

    private void renderSpellList(GuiGraphics guiGraphics, int tabX, Identifier tabTexture, int iconOffsetX,
                                  Collection<? extends Spell> spellCollection, SpellBookData data,
                                  Optional<Identifier> equippedId,
                                  IntSupplier scrollGetter, IntConsumer scrollSetter) {
        List<? extends Spell> spells = new ArrayList<>(spellCollection);
        int total = spells.size();
        int maxScroll = Math.max(0, total - LIST_VISIBLE_ITEMS);
        int scroll = Math.max(0, Math.min(scrollGetter.getAsInt(), maxScroll));
        scrollSetter.accept(scroll);

        int x = leftPos + tabX;
        int y = topPos + LIST_Y;
        int end = Math.min(total, scroll + LIST_VISIBLE_ITEMS);
        for (int i = scroll; i < end; i++) {
            Spell spell = spells.get(i);
            boolean unlocked = data.isUnlocked(spell.id());
            boolean equipped = equippedId.map(spell.id()::equals).orElse(false);

            drawTab(guiGraphics, tabTexture, x, y);

            // Solid backing first: the tab's painted checkerboard would
            // otherwise show through any transparent pixels in the icon.
            int iconX = x + iconOffsetX;
            int iconY = y + TAB_ICON_Y;
            guiGraphics.fill(iconX, iconY, iconX + TAB_ICON_SIZE, iconY + TAB_ICON_SIZE, ICON_BACKING_COLOR);
            drawIcon(guiGraphics, spell.icon(), iconX, iconY, TAB_ICON_SIZE, SPELL_ICON_NATIVE_SIZE);
            if (!unlocked) {
                guiGraphics.fill(iconX, iconY, iconX + TAB_ICON_SIZE, iconY + TAB_ICON_SIZE, 0xA0000000);
            } else if (equipped) {
                drawBorder(guiGraphics, iconX, iconY, TAB_ICON_SIZE, TAB_ICON_SIZE, EQUIPPED_HIGHLIGHT_COLOR);
            }

            // The whole tab is the target, not just the icon.
            clickZones.add(new ClickZone(x, y, x + TAB_WIDTH, y + TAB_HEIGHT, () -> {
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
            hoverZones.add(new HoverZone(x, y, x + TAB_WIDTH, y + TAB_HEIGHT, spell.name()));

            y += TAB_HEIGHT;
        }

        if (total > LIST_VISIBLE_ITEMS) {
            // In the page margin, on the side away from the box.
            int buttonX = tabX < SpellTableMenu.PANEL_WIDTH / 2
                    ? x - SCROLL_BUTTON_GAP - SCROLL_BUTTON_WIDTH
                    : x + TAB_WIDTH + SCROLL_BUTTON_GAP;
            int levelWithTab = (TAB_HEIGHT - SCROLL_BUTTON_HEIGHT) / 2;
            if (scroll > 0) {
                int buttonY = topPos + LIST_Y + levelWithTab;
                drawArrowButton(guiGraphics, buttonX, buttonY, "▲");
                clickZones.add(arrowZone(buttonX, buttonY,
                        () -> scrollSetter.accept(Math.max(0, scrollGetter.getAsInt() - 1))));
            }
            if (scroll < maxScroll) {
                int buttonY = topPos + LIST_Y + (LIST_VISIBLE_ITEMS - 1) * TAB_HEIGHT + levelWithTab;
                drawArrowButton(guiGraphics, buttonX, buttonY, "▼");
                clickZones.add(arrowZone(buttonX, buttonY,
                        () -> scrollSetter.accept(Math.min(maxScroll, scrollGetter.getAsInt() + 1))));
            }
        }
    }

    /**
     * An equipped-spell box: the spell's own background art (default.png while
     * nothing is equipped), the spell's icon near the bottom and its upgrades
     * climbing the box above it, and the two ornamental corner pieces.
     */
    private void renderEquippedBox(GuiGraphics guiGraphics, int boxX, int boxY,
                                    SpellBookData data, Optional<Identifier> equippedId, int upgradeLevels) {
        Spell spell = equippedId.map(ModSpells::get).orElse(null);
        drawTexture(guiGraphics, spell != null ? backgroundFor(spell) : DEFAULT_SPELL_BACKGROUND,
                boxX, boxY, BOX_WIDTH, BOX_HEIGHT);

        if (spell != null) {
            renderSpellStack(guiGraphics, boxX, boxY, data, spell, upgradeLevels);
        }

        // Last, so the corners stay crisp over the art and anything stacked on it.
        drawTexture(guiGraphics, TOP_PING, boxX, boxY, PING_SIZE, PING_SIZE);
        drawTexture(guiGraphics, BOTTOM_PING, boxX + BOX_WIDTH - PING_SIZE, boxY + BOX_HEIGHT - PING_SIZE, PING_SIZE, PING_SIZE);
    }

    /**
     * The icon sits at the bottom and each upgrade level stacks above it with
     * an arrow between rungs. Sized for the box to hold two upgrades (the most
     * any spell has) with the stack clear of both corner pieces; a third would
     * need a smaller LEVEL_ICON_SIZE.
     */
    private void renderSpellStack(GuiGraphics guiGraphics, int boxX, int boxY,
                                   SpellBookData data, Spell spell, int upgradeLevels) {
        Identifier spellId = spell.id();
        int currentLevel = data.levelOf(spellId);

        int iconX = boxX + (BOX_WIDTH - BOX_ICON_SIZE) / 2;
        int iconY = boxY + BOX_HEIGHT - BOX_ICON_SIZE - BOX_ICON_BOTTOM_MARGIN;
        drawIcon(guiGraphics, spell.icon(), iconX, iconY, BOX_ICON_SIZE, SPELL_ICON_NATIVE_SIZE);
        drawBorder(guiGraphics, iconX, iconY, BOX_ICON_SIZE, BOX_ICON_SIZE, EQUIPPED_HIGHLIGHT_COLOR);
        hoverZones.add(new HoverZone(iconX, iconY, iconX + BOX_ICON_SIZE, iconY + BOX_ICON_SIZE, spell.description()));

        int availableUpgrades = Math.min(upgradeLevels, spell.maxLevel() - 1);
        boolean affordable = this.spellPoints >= 1;
        int rungY = iconY;

        for (int i = 0; i < availableUpgrades; i++) {
            int targetLevel = 2 + i;
            boolean reached = currentLevel >= targetLevel;
            boolean available = currentLevel == targetLevel - 1;

            int arrowY = rungY - LEVEL_ARROW_ROW;
            drawArrowButton(guiGraphics, boxX + (BOX_WIDTH - SCROLL_BUTTON_WIDTH) / 2, arrowY + 1, "▲");

            int levelIconX = boxX + (BOX_WIDTH - LEVEL_ICON_SIZE) / 2;
            int levelIconY = arrowY - LEVEL_ICON_GAP - LEVEL_ICON_SIZE;
            Identifier levelIcon = Identifier.fromNamespaceAndPath(spellId.getNamespace(), "textures/gui/spell/level_" + targetLevel + ".png");
            drawIcon(guiGraphics, levelIcon, levelIconX, levelIconY, LEVEL_ICON_SIZE, LEVEL_ICON_NATIVE_SIZE);
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

    /**
     * A spell's box background is background/&lt;spell id without "_spell"&gt;.png
     * when that file exists (archer_spell -> background/archer.png) and
     * background/default.png when it doesn't, so a new background only needs
     * its PNG dropped in the folder. Cached for the life of the screen, so a
     * resource reload is picked up the next time the table is opened.
     */
    private Identifier backgroundFor(Spell spell) {
        return spellBackgrounds.computeIfAbsent(spell.id(), id -> {
            String name = id.getPath();
            if (name.endsWith(SPELL_ID_SUFFIX)) {
                name = name.substring(0, name.length() - SPELL_ID_SUFFIX.length());
            }
            Identifier candidate = Identifier.fromNamespaceAndPath(id.getNamespace(), "textures/gui/spell/background/" + name + ".png");
            return this.minecraft.getResourceManager().getResource(candidate).isPresent() ? candidate : DEFAULT_SPELL_BACKGROUND;
        });
    }

    /** A small dark button with a glyph on it: legible whatever art it's laid over. */
    private void drawArrowButton(GuiGraphics guiGraphics, int x, int y, String glyph) {
        guiGraphics.fill(x - 1, y - 1, x + SCROLL_BUTTON_WIDTH + 1, y + SCROLL_BUTTON_HEIGHT + 1, PLATE_BORDER_COLOR);
        guiGraphics.fill(x, y, x + SCROLL_BUTTON_WIDTH, y + SCROLL_BUTTON_HEIGHT, PLATE_COLOR);
        guiGraphics.drawCenteredString(this.font, glyph, x + SCROLL_BUTTON_WIDTH / 2, y, 0xFFFFFFFF);
    }

    private ClickZone arrowZone(int x, int y, Runnable action) {
        return new ClickZone(x - 1, y - 1, x + SCROLL_BUTTON_WIDTH + 1, y + SCROLL_BUTTON_HEIGHT + 1, action);
    }

    /** Draws a texture at its own pixel size, no scaling. */
    private void drawTexture(GuiGraphics guiGraphics, Identifier texture, int x, int y, int width, int height) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f, width, height, width, height);
    }

    /** A tab shrunk to 1/TAB_SHRINK of the art's size; see TAB_SAMPLE_NUDGE. */
    private void drawTab(GuiGraphics guiGraphics, Identifier texture, int x, int y) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, TAB_SAMPLE_NUDGE, TAB_SAMPLE_NUDGE,
                TAB_WIDTH, TAB_HEIGHT, TAB_TEXTURE_WIDTH, TAB_TEXTURE_HEIGHT, TAB_TEXTURE_WIDTH, TAB_TEXTURE_HEIGHT);
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

    private static Identifier guiTexture(String path) {
        return Identifier.fromNamespaceAndPath(Effects.MOD_ID, "textures/gui/spell/" + path);
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
        if (isOverList(mouseX, mouseY, LEFT_TAB_X)) {
            int maxScroll = Math.max(0, ModSpells.passiveSpells().size() - LIST_VISIBLE_ITEMS);
            passiveScroll = Math.max(0, Math.min(passiveScroll - (int) Math.signum(scrollY), maxScroll));
            return true;
        }
        if (isOverList(mouseX, mouseY, RIGHT_TAB_X)) {
            int maxScroll = Math.max(0, ModSpells.aggressiveSpells().size() - LIST_VISIBLE_ITEMS);
            aggressiveScroll = Math.max(0, Math.min(aggressiveScroll - (int) Math.signum(scrollY), maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean isOverList(double mouseX, double mouseY, int tabX) {
        int x = leftPos + tabX;
        int top = topPos + LIST_Y;
        return mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= top && mouseY < top + LIST_HEIGHT;
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

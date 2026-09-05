package yaboichips.effects.client.screen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

import yaboichips.effects.core.ModAttachments;
import yaboichips.effects.core.ModSpells;
import yaboichips.effects.common.spell.Spell;
import yaboichips.effects.data.PlayerSpellData;
import yaboichips.effects.network.EquipSpellPayload;
import yaboichips.effects.network.UnlockSpellPayload;
import yaboichips.effects.network.UpgradeSpellPayload;

public class SpellTableScreen extends Screen {

    private static final int LIST_ICON_SIZE = 40;
    private static final int LIST_ICON_NATIVE_SIZE = 64;
    private static final int BOX_ICON_SIZE = 64;
    private static final int LEVEL_ICON_SIZE = 32;
    private static final int COLUMN_GAP = 20;
    private static final int BOX_WIDTH = 110;
    private static final int BOX_HEIGHT = 220;
    private static final int PANEL_WIDTH = COLUMN_GAP + LIST_ICON_SIZE + COLUMN_GAP + BOX_WIDTH + COLUMN_GAP + BOX_WIDTH + COLUMN_GAP + LIST_ICON_SIZE + COLUMN_GAP;
    private static final int PANEL_HEIGHT = 280;

    private static final int PASSIVE_COLOR = 0xFFADD8E6;
    private static final int AGGRESSIVE_COLOR = 0xFF8B0000;
    private static final int EQUIPPED_HIGHLIGHT_COLOR = 0xFFFFD700;

    private final List<ClickZone> clickZones = new ArrayList<>();
    private final List<HoverZone> hoverZones = new ArrayList<>();

    public SpellTableScreen() {
        super(Component.translatable("gui.effects.spell_table_title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x66000000);
        clickZones.clear();
        hoverZones.clear();

        LocalPlayer player = this.minecraft.player;
        if (player == null) {
            return;
        }
        PlayerSpellData data = player.getAttachedOrCreate(ModAttachments.SPELL_DATA);

        int panelLeft = (this.width - PANEL_WIDTH) / 2;
        int panelTop = (this.height - PANEL_HEIGHT) / 2;

        guiGraphics.fill(panelLeft - 2, panelTop - 2, panelLeft + PANEL_WIDTH + 2, panelTop + PANEL_HEIGHT + 2, 0xFF000000);
        guiGraphics.fill(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelTop + PANEL_HEIGHT, 0xFF2B2B2B);

        guiGraphics.drawCenteredString(this.font, this.title, panelLeft + PANEL_WIDTH / 2, panelTop + 10, 0xFFFFFFFF);

        String pointsText = Component.translatable("gui.effects.spell_points").getString() + data.spellPoints();
        int pointsTextWidth = this.font.width(pointsText);
        int pointsX = panelLeft + PANEL_WIDTH - 12 - pointsTextWidth;
        int pointsY = panelTop + 10;
        guiGraphics.fill(pointsX - 4, pointsY - 3, pointsX + pointsTextWidth + 4, pointsY + 10, 0xAA000000);
        guiGraphics.drawString(this.font, pointsText, pointsX, pointsY, 0xFFFFD700);

        int x = panelLeft + COLUMN_GAP;
        int listTop = panelTop + 40;
        int boxTop = panelTop + 32;

        int passiveListX = x;
        x += LIST_ICON_SIZE + COLUMN_GAP;
        int passiveBoxX = x;
        x += BOX_WIDTH + COLUMN_GAP;
        int aggressiveBoxX = x;
        x += BOX_WIDTH + COLUMN_GAP;
        int aggressiveListX = x;

        renderSpellList(guiGraphics, passiveListX, listTop, ModSpells.passiveSpells(), data, data.equippedPassive(), PASSIVE_COLOR);
        renderSpellList(guiGraphics, aggressiveListX, listTop, ModSpells.aggressiveSpells(), data, data.equippedAggressive(), AGGRESSIVE_COLOR);

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
                                  Collection<? extends Spell> spells, PlayerSpellData data,
                                  Optional<Identifier> equippedId, int outlineColor) {
        int count = Math.max(spells.size(), 1);
        int totalHeight = count * LIST_ICON_SIZE + (count - 1) * 10;
        drawBorder(guiGraphics, listX - 4, listY - 4, LIST_ICON_SIZE + 8, totalHeight + 8, outlineColor);

        int y = listY;
        for (Spell spell : spells) {
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
                    ClientPlayNetworking.send(new UnlockSpellPayload(spell.id()));
                    playClickSound();
                } else if (!equipped) {
                    ClientPlayNetworking.send(new EquipSpellPayload(spell.id()));
                    playClickSound();
                }
            }));
            hoverZones.add(new HoverZone(zoneX, zoneY, zoneX + LIST_ICON_SIZE, zoneY + LIST_ICON_SIZE, spell.name()));

            y += LIST_ICON_SIZE + 10;
        }
    }

    private void renderEquippedBox(GuiGraphics guiGraphics, int boxX, int boxY,
                                    PlayerSpellData data, Optional<Identifier> equippedId, int upgradeLevels, int outlineColor) {
        guiGraphics.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + BOX_HEIGHT, 0xFF1A1A1A);
        drawBorder(guiGraphics, boxX, boxY, BOX_WIDTH, BOX_HEIGHT, outlineColor);

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
        int iconY = boxY + BOX_HEIGHT - BOX_ICON_SIZE - 14;
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
            boolean affordable = data.spellPoints() >= 1;

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
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f, drawSize, drawSize, nativeSize, nativeSize);
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

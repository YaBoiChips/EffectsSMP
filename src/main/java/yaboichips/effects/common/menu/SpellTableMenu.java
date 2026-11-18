package yaboichips.effects.common.menu;

import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import yaboichips.effects.common.item.SpellBookItem;
import yaboichips.effects.common.spell.Spell;
import yaboichips.effects.core.ModAttachments;
import yaboichips.effects.core.ModComponents;
import yaboichips.effects.core.ModMenus;
import yaboichips.effects.data.SpellBookData;

public class SpellTableMenu extends AbstractContainerMenu {

    public static final int BOOK_SLOT = 0;

    /**
     * Panel geometry shared with SpellTableScreen's hand-drawn layout. The
     * menu (common, server-visible) can't depend on the client-only screen
     * class, so both sides hardcode these same numbers - keep them in sync.
     *
     * The panel is the open-book art (book_background.png: a 384x240 canvas of
     * which 298x213 is opaque) plus 3px each side that the plate under the book
     * spans, with the player inventory laid over the book's lower edge on that
     * plate. The spell tabs and the equipped-spell boxes all sit on the book's
     * pages; the book slot sits on the spine, in the 20px gap between the two
     * boxes.
     *
     * Everything that matters has to fit in 320x240: that's the scaled
     * resolution Minecraft always guarantees regardless of the player's GUI
     * Scale setting (Window.calculateScale never lets guiScaledWidth/Height
     * drop below it), so anything past that can clip or render off-screen at
     * high GUI Scale no matter how large the player's actual display is. The
     * panel is 304 wide and 248 tall, so on a 240-high screen its top and
     * bottom 4px are clipped. At the top that is only the book's ragged edge.
     * At the bottom it is the plate's lower border and the last 2px of the
     * hotbar's slot frames; the items themselves stay fully visible.
     */
    public static final int PANEL_WIDTH = 304;
    public static final int BOOK_SLOT_X = 144;
    public static final int BOOK_SLOT_Y = 76;
    public static final int MAIN_INV_X = 72;
    public static final int MAIN_INV_Y = 170;
    public static final int HOTBAR_Y = 228;
    public static final int PANEL_HEIGHT = 248;

    private final Container bookContainer = new SimpleContainer(1);
    private final Slot bookSlot;
    private final UUID ownerId;

    public SpellTableMenu(int containerId, Inventory playerInventory) {
        super(ModMenus.SPELL_TABLE, containerId);
        this.ownerId = playerInventory.player.getUUID();

        this.bookSlot = new BookSlot(bookContainer, 0, BOOK_SLOT_X, BOOK_SLOT_Y, ownerId);
        addSlot(bookSlot);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, 9 + row * 9 + col, MAIN_INV_X + col * 18, MAIN_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, MAIN_INV_X + col * 18, HOTBAR_Y));
        }
    }

    public Slot getBookSlot() {
        return bookSlot;
    }

    public void tryUnlockSpell(ServerPlayer player, Spell spell) {
        if (spell == null) {
            return;
        }
        ItemStack book = bookSlot.getItem();
        if (!(book.getItem() instanceof SpellBookItem)) {
            return;
        }
        SpellBookData data = book.getOrDefault(ModComponents.SPELL_BOOK_DATA, SpellBookData.DEFAULT);
        if (data.isBoundToOther(ownerId) || data.isUnlocked(spell.id())) {
            return;
        }
        if (player.getAttachedOrCreate(ModAttachments.SPELL_DATA).spellPoints() < 1) {
            return;
        }
        SpellBookData bound = data.owner().isPresent() ? data : data.withOwner(ownerId);
        book.set(ModComponents.SPELL_BOOK_DATA, bound.withUnlocked(spell));
        player.modifyAttached(ModAttachments.SPELL_DATA, d -> d.withSpellPoints(d.spellPoints() - 1));
    }

    public void tryUpgradeSpell(ServerPlayer player, Spell spell) {
        if (spell == null) {
            return;
        }
        ItemStack book = bookSlot.getItem();
        if (!(book.getItem() instanceof SpellBookItem)) {
            return;
        }
        SpellBookData data = book.getOrDefault(ModComponents.SPELL_BOOK_DATA, SpellBookData.DEFAULT);
        int level = data.levelOf(spell.id());
        if (data.isBoundToOther(ownerId) || level <= 0 || level >= spell.maxLevel()) {
            return;
        }
        if (player.getAttachedOrCreate(ModAttachments.SPELL_DATA).spellPoints() < 1) {
            return;
        }
        book.set(ModComponents.SPELL_BOOK_DATA, data.withUpgraded(spell));
        player.modifyAttached(ModAttachments.SPELL_DATA, d -> d.withSpellPoints(d.spellPoints() - 1));
    }

    public void tryEquipSpell(ServerPlayer player, Spell spell) {
        if (spell == null) {
            return;
        }
        ItemStack book = bookSlot.getItem();
        if (!(book.getItem() instanceof SpellBookItem)) {
            return;
        }
        SpellBookData data = book.getOrDefault(ModComponents.SPELL_BOOK_DATA, SpellBookData.DEFAULT);
        if (data.isBoundToOther(ownerId) || !data.isUnlocked(spell.id())) {
            return;
        }
        SpellBookData updated = data.withEquipped(spell);
        book.set(ModComponents.SPELL_BOOK_DATA, updated);
        SpellBookItem.syncAppearance(book, updated);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index == BOOK_SLOT) {
            if (!this.moveItemStackTo(original, 1, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(original, BOOK_SLOT, BOOK_SLOT + 1, false)) {
            return ItemStack.EMPTY;
        }
        if (original.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY, copy);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.clearContainer(player, bookContainer);
    }

    private static class BookSlot extends Slot {
        private final UUID menuOwnerId;

        BookSlot(Container container, int index, int x, int y, UUID menuOwnerId) {
            super(container, index, x, y);
            this.menuOwnerId = menuOwnerId;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!(stack.getItem() instanceof SpellBookItem)) {
                return false;
            }
            SpellBookData data = stack.getOrDefault(ModComponents.SPELL_BOOK_DATA, SpellBookData.DEFAULT);
            return !data.isBoundToOther(menuOwnerId);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}

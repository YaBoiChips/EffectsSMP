package yaboichips.effects.common.item;

import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import yaboichips.effects.core.ModAttachments;
import yaboichips.effects.core.ModComponents;
import yaboichips.effects.data.SpellBookData;

public class SpellBookItem extends Item {

    public SpellBookItem(Properties properties) {
        super(properties);
    }

    /**
     * Sneak-using a book bound to someone else destroys it and refunds half
     * of what was invested in it to whoever destroyed it - the theft/griefing
     * deterrent that makes book binding matter.
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        SpellBookData data = stack.getOrDefault(ModComponents.SPELL_BOOK_DATA, SpellBookData.DEFAULT);
        if (!data.isBoundToOther(player.getUUID())) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            int refund = data.investedPoints() / 2;
            if (refund > 0) {
                player.modifyAttached(ModAttachments.SPELL_DATA, d -> d.withSpellPoints(d.spellPoints() + refund));
            }
            stack.shrink(stack.getCount());
        }
        return InteractionResult.CONSUME;
    }

    public static int findBoundBookSlot(Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof SpellBookItem) {
                SpellBookData data = stack.getOrDefault(ModComponents.SPELL_BOOK_DATA, SpellBookData.DEFAULT);
                if (data.isOwner(player.getUUID())) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static SpellBookData getBoundBookData(Player player) {
        int slot = findBoundBookSlot(player);
        if (slot < 0) {
            return SpellBookData.DEFAULT;
        }
        return player.getInventory().getItem(slot).getOrDefault(ModComponents.SPELL_BOOK_DATA, SpellBookData.DEFAULT);
    }

    /**
     * Mirrors the equipped spells into vanilla's CustomModelData so the item
     * model (assets/effects/items/spell_book.json) can pick a texture per
     * layer: strings()[0] drives the passive-spell layer, strings()[1] the
     * aggressive-spell layer, each independently, via minecraft:select on
     * minecraft:custom_model_data. An unequipped slot gets "" - a value that
     * never matches a real spell id, so the model's fallback (default
     * texture) is used for that layer.
     */
    public static void syncAppearance(ItemStack book, SpellBookData data) {
        String passive = data.equippedPassive().map(Identifier::toString).orElse("");
        String aggressive = data.equippedAggressive().map(Identifier::toString).orElse("");
        book.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(passive, aggressive), List.of()));
    }
}

package yaboichips.effects.common.spell;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Inventory item-cost checking/consumption for spells that require materials
 * to cast (e.g. Metallurgy's iron ingot costs). No equivalent existed
 * elsewhere in the mod prior to this.
 */
public final class SpellCosts {

    private SpellCosts() {
    }

    public static boolean hasItems(ServerPlayer player, Item item, int count) {
        int found = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                found += stack.getCount();
                if (found >= count) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void consumeItems(ServerPlayer player, Item item, int count) {
        int remaining = count;
        NonNullList<ItemStack> items = player.getInventory().getNonEquipmentItems();
        for (int i = 0; i < items.size() && remaining > 0; i++) {
            ItemStack stack = items.get(i);
            if (!stack.is(item)) {
                continue;
            }
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
    }
}

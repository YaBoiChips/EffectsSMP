package yaboichips.effects.common.effect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Shuffles the player's hotbar slots once when the effect is applied. The
 * effect only lasts a second, so a single shuffle on start is the whole
 * mechanic - there is no per-tick behavior.
 */
public class RandomizeMobEffect extends MobEffect {

    private static final int HOTBAR_SIZE = 9;

    public RandomizeMobEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF8C00);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        super.onEffectStarted(entity, amplifier);
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        Inventory inventory = player.getInventory();
        List<ItemStack> hotbarSlots = new ArrayList<>(HOTBAR_SIZE);
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            hotbarSlots.add(inventory.getItem(i));
        }
        Collections.shuffle(hotbarSlots);
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            inventory.setItem(i, hotbarSlots.get(i));
        }
    }
}

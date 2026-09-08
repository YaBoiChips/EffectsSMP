package yaboichips.effects.mixin;

import java.util.HashSet;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import yaboichips.effects.core.ModSpells;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {

    @Shadow
    @Final
    private Container enchantSlots;

    @Inject(method = "clickMenuButton", at = @At("RETURN"))
    private void effects$maxEnchantLevels(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || !ModSpells.ENCHANTER_SPELL.isEquippedAtLevel(player, 2)) {
            return;
        }
        ItemStack stack = this.enchantSlots.getItem(0);
        if (stack.isEmpty()) {
            return;
        }
        EnchantmentHelper.updateEnchantments(stack, mutable -> {
            for (var enchantment : new HashSet<>(mutable.keySet())) {
                mutable.upgrade(enchantment, enchantment.value().getMaxLevel());
            }
        });
    }
}

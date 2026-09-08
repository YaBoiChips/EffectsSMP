package yaboichips.effects.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;

import yaboichips.effects.core.ModSpells;

/**
 * Fortune Level 1's golden apple bonus - 1.21's food effects are applied by
 * {@link Consumable#onConsume}, and there's no Fabric event for "just ate
 * this specific item", so this hooks the same way EnchanterSpell/HermitSpell
 * fill other Fabric API gaps: a targeted mixin, gated on the eater's equipped
 * spell level.
 */
@Mixin(Consumable.class)
public abstract class GoldenAppleConsumeMixin {

    private static final float BONUS_ABSORPTION = 4.0f;
    private static final int BONUS_REGEN_TICKS = 100;

    @Inject(method = "onConsume", at = @At("RETURN"))
    private void effects$boostGoldenApple(Level level, LivingEntity entity, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (level.isClientSide()
                || !(stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE))
                || !(entity instanceof ServerPlayer player)
                || !ModSpells.FORTUNE_SPELL.isEquippedAtLevel(player, 1)) {
            return;
        }

        player.setAbsorptionAmount(player.getAbsorptionAmount() + BONUS_ABSORPTION);

        MobEffectInstance regen = player.getEffect(MobEffects.REGENERATION);
        if (regen != null) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                    regen.getDuration() + BONUS_REGEN_TICKS, regen.getAmplifier(), regen.isAmbient(), regen.isVisible()));
        }
    }
}

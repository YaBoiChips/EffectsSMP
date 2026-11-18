package yaboichips.effects.mixin;

import java.util.Collection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import yaboichips.effects.common.effect.NumbnessMobEffect;
import yaboichips.effects.core.ModMobEffects;

@Mixin(LivingEntity.class)
public abstract class NumbnessEffectMixin {

    @Inject(method = "onEffectsRemoved", at = @At("HEAD"))
    private void effects$payoutNumbness(Collection<MobEffectInstance> removedEffects, CallbackInfo ci) {
        if (!(((LivingEntity) (Object) this) instanceof ServerPlayer player)) {
            return;
        }

        for (MobEffectInstance instance : removedEffects) {
            if (instance.getEffect() == ModMobEffects.NUMBNESS) {
                float payout = NumbnessMobEffect.takePayout(player.getUUID());
                if (payout > 0.0f) {
                    ServerLevel serverLevel = player.level();
                    player.hurtServer(serverLevel, serverLevel.damageSources().generic(), payout);
                }
                break;
            }
        }
    }
}

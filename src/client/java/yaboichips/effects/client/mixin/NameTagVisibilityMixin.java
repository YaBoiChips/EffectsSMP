package yaboichips.effects.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import yaboichips.effects.core.ModAttachments;
import yaboichips.effects.core.ModSpells;

@Mixin(LivingEntityRenderer.class)
public abstract class NameTagVisibilityMixin {

    private static final double WATCHER_REVEAL_RANGE = 8.0;

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At("HEAD"), cancellable = true)
    private void effects$nameTagVisibility(LivingEntity entity, double distanceToSqr, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Player targetPlayer
                && targetPlayer.getAttachedOrCreate(ModAttachments.STEALTH_ACTIVE)) {
            cir.setReturnValue(false);
            return;
        }

        if (entity.isInvisible() && revealsInvisible(entity)) {
            cir.setReturnValue(true);
        }
    }

    private boolean revealsInvisible(Entity entity) {
        LocalPlayer local = Minecraft.getInstance().player;
        if (local == null || !local.isCrouching() || local.distanceToSqr(entity) > WATCHER_REVEAL_RANGE * WATCHER_REVEAL_RANGE) {
            return false;
        }
        var data = local.getAttachedOrCreate(ModAttachments.SPELL_DATA);
        return data.equippedPassive().filter(ModSpells.WATCHER_SPELL.id()::equals)
                .map(data::levelOf).orElse(0) >= 1;
    }
}

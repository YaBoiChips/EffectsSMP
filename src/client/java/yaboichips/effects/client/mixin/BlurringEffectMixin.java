package yaboichips.effects.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import yaboichips.effects.core.ModMobEffects;

/**
 * Reuses vanilla's own GUI-background blur post-chain ({@code minecraft:blur})
 * for the Blurring status, the same way {@code checkEntityPostEffect} already
 * swaps in a shader when the camera entity is a Creeper/Spider/Enderman.
 */
@Mixin(GameRenderer.class)
public abstract class BlurringEffectMixin {

    @Shadow
    private void setPostEffect(Identifier identifier) {
    }

    @Inject(method = "checkEntityPostEffect", at = @At("HEAD"), cancellable = true)
    private void effects$applyBlurring(Entity entity, CallbackInfo ci) {
        if (entity instanceof LivingEntity living && living.hasEffect(ModMobEffects.BLURRING)) {
            this.setPostEffect(Identifier.withDefaultNamespace("blur"));
            ci.cancel();
        }
    }
}

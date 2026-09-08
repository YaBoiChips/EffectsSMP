package yaboichips.effects.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;

import yaboichips.effects.client.core.DeafenState;

@Mixin(SoundManager.class)
public abstract class SoundSuppressionMixin {

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void effects$suppressWhileDeafened(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (!DeafenState.isDeafened()) {
            return;
        }
        SoundSource source = instance.getSource();
        if (source != SoundSource.UI && source != SoundSource.MASTER) {
            cir.setReturnValue(null);
        }
    }
}

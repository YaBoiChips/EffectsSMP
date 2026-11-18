package yaboichips.effects.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Purely a visual debuff - {@code client.mixin.BlurringEffectMixin} drives the
 * actual blur post-process shader on the client while this effect is active.
 */
public class BlurringMobEffect extends MobEffect {

    public BlurringMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x8FA6B2);
    }
}

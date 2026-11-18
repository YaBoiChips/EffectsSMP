package yaboichips.effects.core;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;

import yaboichips.effects.Effects;
import yaboichips.effects.common.effect.BlurringMobEffect;
import yaboichips.effects.common.effect.NumbnessMobEffect;
import yaboichips.effects.common.effect.RandomizeMobEffect;
import yaboichips.effects.common.effect.SpringMobEffect;

public class ModMobEffects {

    public static final Holder<MobEffect> BLURRING = registerEffect("blurring", new BlurringMobEffect());
    public static final Holder<MobEffect> RANDOMIZE = registerEffect("randomize", new RandomizeMobEffect());
    public static final Holder<MobEffect> SPRING = registerEffect("spring", new SpringMobEffect());
    public static final Holder<MobEffect> NUMBNESS = registerEffect("numbness", new NumbnessMobEffect());

    private static Holder<MobEffect> registerEffect(String name, MobEffect effect) {
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, keyOf(name), effect);
    }

    private static ResourceKey<MobEffect> keyOf(String name) {
        return ResourceKey.create(Registries.MOB_EFFECT, Identifier.fromNamespaceAndPath(Effects.MOD_ID, name));
    }

    public static void registerModMobEffects() {
        Effects.LOGGER.info("Registering mob effects for {}", Effects.MOD_ID);
    }
}

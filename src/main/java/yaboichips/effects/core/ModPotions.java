package yaboichips.effects.core;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;

import yaboichips.effects.Effects;

public class ModPotions {

    private static final int SECONDS_30 = 30 * 20;
    private static final int SECONDS_60 = 60 * 20;
    private static final int MINUTES_4 = 4 * 60 * 20;

    public static final Holder<Potion> BLURRING = registerPotion("blurring",
            new MobEffectInstance(ModMobEffects.BLURRING, SECONDS_30));
    public static final Holder<Potion> RANDOMIZE = registerPotion("randomize",
            new MobEffectInstance(ModMobEffects.RANDOMIZE, 20));
    public static final Holder<Potion> SPRING = registerPotion("spring",
            new MobEffectInstance(ModMobEffects.SPRING, SECONDS_60));
    public static final Holder<Potion> NUMBNESS = registerPotion("numbness",
            new MobEffectInstance(ModMobEffects.NUMBNESS, MINUTES_4));

    private static Holder<Potion> registerPotion(String name, MobEffectInstance... effects) {
        return Registry.registerForHolder(BuiltInRegistries.POTION, keyOf(name), new Potion(name, effects));
    }

    private static ResourceKey<Potion> keyOf(String name) {
        return ResourceKey.create(Registries.POTION, Identifier.fromNamespaceAndPath(Effects.MOD_ID, name));
    }

    public static void registerModPotions() {
        Effects.LOGGER.info("Registering potions for {}", Effects.MOD_ID);
    }
}

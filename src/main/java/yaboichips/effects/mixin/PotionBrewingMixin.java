package yaboichips.effects.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;

import yaboichips.effects.core.ModPotions;

/**
 * Fabric API ships no brewing-recipe registry for this version, so the mod's
 * mixes are added the same way vanilla's own are built: injected at the tail
 * of {@code addVanillaMixes}, onto the same builder instance {@code bootstrap}
 * passes it.
 */
@Mixin(PotionBrewing.class)
public class PotionBrewingMixin {

    @Inject(method = "addVanillaMixes", at = @At("TAIL"))
    private static void effects$addModMixes(PotionBrewing.Builder builder, CallbackInfo ci) {
        builder.addMix(Potions.AWKWARD, Items.DIRT, ModPotions.BLURRING);
        builder.addMix(ModPotions.BLURRING, Items.GLOWSTONE_DUST, ModPotions.RANDOMIZE);
        builder.addMix(Potions.LEAPING, Items.SLIME_BALL, ModPotions.SPRING);
        builder.addMix(Potions.REGENERATION, Items.TOTEM_OF_UNDYING, ModPotions.NUMBNESS);
    }
}

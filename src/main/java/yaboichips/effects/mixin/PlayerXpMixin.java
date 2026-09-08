package yaboichips.effects.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.world.entity.player.Player;

import yaboichips.effects.common.spell.passive.EnchanterSpell;
import yaboichips.effects.core.ModSpells;

@Mixin(Player.class)
public abstract class PlayerXpMixin {

    @ModifyVariable(method = "giveExperiencePoints", at = @At("HEAD"), argsOnly = true)
    private int effects$scaleExperiencePoints(int amount) {
        Player self = (Player) (Object) this;
        if (ModSpells.ENCHANTER_SPELL.isEquippedAtLevel(self, 1)) {
            return Math.round(amount * EnchanterSpell.XP_MULTIPLIER);
        }
        return amount;
    }
}

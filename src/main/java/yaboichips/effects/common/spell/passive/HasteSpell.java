package yaboichips.effects.common.spell.passive;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import yaboichips.effects.common.spell.PassiveSpell;

public class HasteSpell extends PassiveSpell {

    public HasteSpell(Identifier id, Identifier icon) {
        super(id, icon, 2);
    }

    @Override
    public void tickEffect(ServerPlayer player, int level) {
        int amplifier = level - 1;
        MobEffectInstance current = player.getEffect(MobEffects.HASTE);
        if (current == null || current.getAmplifier() != amplifier || current.getDuration() < 100) {
            player.addEffect(new MobEffectInstance(MobEffects.HASTE, 200, amplifier, true, false));
        }
    }
}

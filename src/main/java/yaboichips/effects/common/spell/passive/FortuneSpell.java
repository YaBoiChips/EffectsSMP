package yaboichips.effects.common.spell.passive;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import yaboichips.effects.common.item.SpellBookItem;
import yaboichips.effects.common.spell.PassiveSpell;

/**
 * Level 1's golden apple bonus is implemented via a mixin
 * ({@code GoldenAppleConsumeMixin}) since there's no Fabric event for "just
 * ate this specific item" - it checks this spell's equipped level via
 * {@link #isEquippedAtLevel}, the same pattern EnchanterSpell uses.
 */
public class FortuneSpell extends PassiveSpell {

    private static final float ROULETTE_CHANCE = 0.01f;

    public FortuneSpell(Identifier id, Identifier icon) {
        super(id, icon, 2);
    }

    @Override
    public void tickEffect(ServerPlayer player, int level) {
    }

    @Override
    public void onDealtDamage(ServerPlayer attacker, int level, LivingEntity target, DamageSource source, float damageDealt) {
        if (level < 2 || attacker.getRandom().nextFloat() >= ROULETTE_CHANCE) {
            return;
        }
        OreRoulette result = OreRoulette.roll(attacker.getRandom());
        attacker.displayClientMessage(Component.translatable("spell.effects.fortune_spell.roulette_result",
                Component.translatable("spell.effects.fortune_spell.ore." + result.key())), true);
        target.hurtServer(attacker.level(), attacker.level().damageSources().magic(), result.damage());
    }

    public boolean isEquippedAtLevel(Player player, int requiredLevel) {
        var data = SpellBookItem.getBoundBookData(player);
        return data.equippedPassive()
                .filter(id()::equals)
                .map(data::levelOf)
                .orElse(0) >= requiredLevel;
    }
}

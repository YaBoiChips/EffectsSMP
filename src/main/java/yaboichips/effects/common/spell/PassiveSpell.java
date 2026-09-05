package yaboichips.effects.common.spell;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

public abstract class PassiveSpell extends Spell {

    protected PassiveSpell(Identifier id, Identifier icon, int maxLevel) {
        super(id, icon, maxLevel);
        if (maxLevel > 2) {
            throw new IllegalArgumentException("Passive spells support at most 2 levels");
        }
    }

    @Override
    public SpellType type() {
        return SpellType.PASSIVE;
    }

    /**
     * Called every server tick for a player that has this spell equipped.
     */
    public abstract void tickEffect(ServerPlayer player, int level);

    /**
     * Called after a player with this spell equipped takes damage. No-op by
     * default; spells that react to damage (e.g. Naturalist) override it.
     */
    public void onDamaged(ServerPlayer player, int level, DamageSource source, float damageTaken) {
    }
}

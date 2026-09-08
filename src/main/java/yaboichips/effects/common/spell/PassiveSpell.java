package yaboichips.effects.common.spell;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

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
     * Unlike the base {@link Spell#tickEffect}, this is required for every
     * passive spell since being "always on" is the whole point of the type.
     */
    @Override
    public abstract void tickEffect(ServerPlayer player, int level);
}

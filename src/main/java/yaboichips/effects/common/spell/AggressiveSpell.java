package yaboichips.effects.common.spell;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public abstract class AggressiveSpell extends Spell {

    protected AggressiveSpell(Identifier id, Identifier icon, int maxLevel) {
        super(id, icon, maxLevel);
        if (maxLevel > 3) {
            throw new IllegalArgumentException("Aggressive spells support at most 3 levels");
        }
    }

    @Override
    public SpellType type() {
        return SpellType.AGGRESSIVE;
    }


    public abstract void activate(ServerPlayer player, int level);
}

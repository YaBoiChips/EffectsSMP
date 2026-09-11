package yaboichips.effects.common.spell.passive;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import yaboichips.effects.common.item.SpellBookItem;
import yaboichips.effects.common.spell.PassiveSpell;

/**
 * Both levels of this spell are implemented via mixins ({@code PlayerXpMixin}
 * and {@code EnchantmentMenuMixin}) rather than the usual tick/damage hooks,
 * since there's no Fabric API event for "player gained XP" or "player just
 * enchanted an item" - the mixins check this spell's equipped level via
 * {@link #isEquippedAtLevel}.
 */
public class EnchanterSpell extends PassiveSpell {

    public static final float XP_MULTIPLIER = 1.25f;

    public EnchanterSpell(Identifier id, Identifier icon) {
        super(id, icon, 2);
    }

    @Override
    public void tickEffect(ServerPlayer player, int level) {
    }

    public boolean isEquippedAtLevel(Player player, int requiredLevel) {
        var data = SpellBookItem.getBoundBookData(player);
        return data.equippedPassive()
                .filter(id()::equals)
                .map(data::levelOf)
                .orElse(0) >= requiredLevel;
    }
}

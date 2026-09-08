package yaboichips.effects.common.spell;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public abstract class AggressiveSpell extends Spell {

    private final Map<CooldownKey, Long> cooldownEndTick = new HashMap<>();

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

    /**
     * Per-player, in-memory cooldown tracking (not persisted or synced, same
     * risk profile as the ad hoc UUID-map cooldowns already used elsewhere in
     * this mod). {@code group} distinguishes independent cooldowns within the
     * same spell - most spells just pass the activated level, but a spell
     * whose levels should share one cooldown (e.g. Beast Tamer's Level 2 and
     * 3) can pass the same group for both. A spell opts in by calling
     * {@link #isOnCooldown} at the top of {@link #activate} and
     * {@link #startCooldown} once the effect fires.
     */
    protected boolean isOnCooldown(ServerPlayer player, int group) {
        Long end = cooldownEndTick.get(new CooldownKey(player.getUUID(), group));
        return end != null && player.level().getGameTime() < end;
    }

    protected long remainingCooldownSeconds(ServerPlayer player, int group) {
        Long end = cooldownEndTick.get(new CooldownKey(player.getUUID(), group));
        if (end == null) {
            return 0;
        }
        long remainingTicks = end - player.level().getGameTime();
        return Math.max(0, remainingTicks) / 20;
    }

    protected void startCooldown(ServerPlayer player, int group, int seconds) {
        cooldownEndTick.put(new CooldownKey(player.getUUID(), group), player.level().getGameTime() + (long) seconds * 20L);
    }

    /**
     * Actionbar-only feedback for a cast that failed because it's on cooldown
     * - no sound, matching the spell table GUI's "sound only on a real state
     * change" behavior.
     */
    protected void sendCooldownMessage(ServerPlayer player, int group) {
        player.displayClientMessage(
                Component.translatable("spell.effects.on_cooldown", name(), remainingCooldownSeconds(player, group)), true);
    }

    private record CooldownKey(UUID playerId, int group) {
    }
}

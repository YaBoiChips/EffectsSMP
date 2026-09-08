package yaboichips.effects.common.spell;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public abstract class Spell {

    private final Identifier id;
    private final Identifier icon;
    private final int maxLevel;

    protected Spell(Identifier id, Identifier icon, int maxLevel) {
        this.id = id;
        this.icon = icon;
        this.maxLevel = maxLevel;
    }

    public Identifier id() {
        return id;
    }

    public Identifier icon() {
        return icon;
    }

    public int maxLevel() {
        return maxLevel;
    }

    public Component name() {
        return Component.translatable("spell." + id.getNamespace() + "." + id.getPath());
    }

    public Component description() {
        return Component.translatable("spell." + id.getNamespace() + "." + id.getPath() + ".description");
    }

    public Component upgradeDescription(int level) {
        return Component.translatable("spell." + id.getNamespace() + "." + id.getPath() + ".upgrade." + level);
    }

    public abstract SpellType type();

    /**
     * Called every server tick for a player that has this spell equipped.
     * No-op by default; {@link PassiveSpell} re-declares this abstract since
     * every passive spell needs one, while aggressive spells opt in only if
     * they need background ticking (e.g. resource accrual, timed buffs).
     */
    public void tickEffect(ServerPlayer player, int level) {
    }

    /**
     * Called after a player with this spell equipped takes damage. No-op by
     * default; spells that react to damage override it.
     */
    public void onDamaged(ServerPlayer player, int level, DamageSource source, float damageTaken) {
    }

    /**
     * Called after a player with this spell equipped deals damage to something
     * (attacker's perspective, mirroring {@link #onDamaged}). No-op by default.
     */
    public void onDealtDamage(ServerPlayer attacker, int level, LivingEntity target, DamageSource source, float damageDealt) {
    }

    /**
     * Called before incoming damage is applied to a player with this spell
     * equipped. Return {@code true} to mean "I handled this myself, cancel the
     * original damage" (e.g. to apply a reduced amount directly). Returns
     * {@code false} by default, letting the damage proceed normally.
     */
    public boolean modifyIncomingDamage(ServerPlayer player, int level, DamageSource source, float amount) {
        return false;
    }

    /**
     * Called before damage a player with this spell equipped is about to deal
     * is applied (attacker's perspective). Return a different amount to mean
     * "apply this amount instead" - the caller is expected to cancel the
     * original damage and self-apply the returned amount, mirroring
     * {@link #modifyIncomingDamage}. Returns {@code amount} unchanged by
     * default.
     */
    public float modifyOutgoingDamage(ServerPlayer attacker, int level, LivingEntity target, DamageSource source, float amount) {
        return amount;
    }
}

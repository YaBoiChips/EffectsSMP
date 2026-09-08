package yaboichips.effects.common.spell.aggressive;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import yaboichips.effects.common.spell.AggressiveSpell;
import yaboichips.effects.common.spell.SpellTargeting;

public class ArcherSpell extends AggressiveSpell {

    private static final int CRACK_TRIGGER_COOLDOWN_SECONDS = 120;
    private static final int CRACK_WINDOW_TICKS = 300;
    private static final float CRACK_DAMAGE_MULTIPLIER = 1.2f;

    private static final int GRAPPLE_COOLDOWN_SECONDS = 45;
    private static final double GRAPPLE_RANGE = 32.0;
    private static final double GRAPPLE_PULL_STRENGTH = 1.8;
    private static final double GRAPPLE_SWING_STRENGTH = 1.4;

    private static final int AIMBOT_COOLDOWN_SECONDS = 150;
    private static final int AIMBOT_DURATION_TICKS = 200;

    private final Map<UUID, CrackWindow> crackWindows = new HashMap<>();
    private final Map<UUID, Long> aimbotEndTick = new HashMap<>();

    public ArcherSpell(Identifier id, Identifier icon) {
        super(id, icon, 3);
    }

    @Override
    public void activate(ServerPlayer player, int level) {
        switch (level) {
            case 2 -> activateGrapple(player);
            case 3 -> activateAimbot(player);
            default -> {
            }
        }
    }

    @Override
    public void onDealtDamage(ServerPlayer attacker, int level, LivingEntity target, DamageSource source, float damageDealt) {
        if (!source.is(DamageTypes.ARROW) && !source.is(DamageTypes.TRIDENT)) {
            return;
        }
        if (isOnCooldown(attacker, 1)) {
            return;
        }
        startCooldown(attacker, 1, CRACK_TRIGGER_COOLDOWN_SECONDS);
        long expiry = attacker.level().getGameTime() + CRACK_WINDOW_TICKS;
        crackWindows.put(attacker.getUUID(), new CrackWindow(target.getUUID(), expiry));
    }

    @Override
    public float modifyOutgoingDamage(ServerPlayer attacker, int level, LivingEntity target, DamageSource source, float amount) {
        CrackWindow window = crackWindows.get(attacker.getUUID());
        if (window == null || !window.targetId().equals(target.getUUID())
                || attacker.level().getGameTime() >= window.expiryTick()) {
            return amount;
        }
        return amount * CRACK_DAMAGE_MULTIPLIER;
    }

    private void activateGrapple(ServerPlayer player) {
        if (isOnCooldown(player, 2)) {
            sendCooldownMessage(player, 2);
            return;
        }
        Optional<HitResult> hit = SpellTargeting.findLookTarget(player, GRAPPLE_RANGE);
        if (hit.isEmpty()) {
            return;
        }
        startCooldown(player, 2, GRAPPLE_COOLDOWN_SECONDS);
        boolean sneaking = player.isShiftKeyDown();

        if (hit.get() instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target) {
            if (sneaking) {
                pull(target, player.position(), GRAPPLE_PULL_STRENGTH);
            } else {
                pull(player, target.position(), GRAPPLE_PULL_STRENGTH);
            }
            return;
        }

        if (hit.get() instanceof BlockHitResult blockHit) {
            Vec3 anchor = blockHit.getLocation();
            if (sneaking) {
                swingToward(player, anchor);
            } else {
                pull(player, anchor, GRAPPLE_PULL_STRENGTH);
            }
        }
    }

    private void pull(Entity entity, Vec3 towards, double strength) {
        Vec3 direction = towards.subtract(entity.position());
        if (direction.lengthSqr() < 1.0e-4) {
            return;
        }
        entity.setDeltaMovement(direction.normalize().scale(strength));
        entity.hurtMarked = true;
    }

    private void swingToward(ServerPlayer player, Vec3 anchor) {
        Vec3 direction = anchor.subtract(player.position());
        if (direction.lengthSqr() < 1.0e-4) {
            return;
        }
        Vec3 swing = direction.normalize().scale(GRAPPLE_SWING_STRENGTH).add(0, GRAPPLE_SWING_STRENGTH * 0.4, 0);
        player.setDeltaMovement(swing);
        player.hurtMarked = true;
    }

    private void activateAimbot(ServerPlayer player) {
        if (isOnCooldown(player, 3)) {
            sendCooldownMessage(player, 3);
            return;
        }
        startCooldown(player, 3, AIMBOT_COOLDOWN_SECONDS);
        aimbotEndTick.put(player.getUUID(), player.level().getGameTime() + AIMBOT_DURATION_TICKS);
    }

    public boolean hasAimbotActive(ServerPlayer player) {
        Long end = aimbotEndTick.get(player.getUUID());
        return end != null && player.level().getGameTime() < end;
    }

    private record CrackWindow(UUID targetId, long expiryTick) {
    }
}

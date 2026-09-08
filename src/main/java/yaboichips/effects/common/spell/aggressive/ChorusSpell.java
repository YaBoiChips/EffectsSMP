package yaboichips.effects.common.spell.aggressive;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import yaboichips.effects.common.spell.AggressiveSpell;
import yaboichips.effects.common.spell.SpellTargeting;

public class ChorusSpell extends AggressiveSpell {

    private static final int TELEPORT_COOLDOWN_SECONDS = 60;
    private static final double TELEPORT_RADIUS = 8.0;

    private static final int SHULKER_COOLDOWN_SECONDS = 60;
    private static final double SHULKER_RANGE = 32.0;

    private static final int CHASE_COOLDOWN_SECONDS = 120;
    private static final int CHASE_MAX_HITS = 5;
    private static final int CHASE_FALLBACK_DURATION_TICKS = 600;
    private static final double CHASE_TELEPORT_RADIUS = 2.5;

    private final Map<UUID, ChaseState> chaseStates = new HashMap<>();

    public ChorusSpell(Identifier id, Identifier icon) {
        super(id, icon, 3);
    }

    @Override
    public void activate(ServerPlayer player, int level) {
        switch (level) {
            case 1 -> teleportSelf(player);
            case 2 -> fireLevitationBullet(player);
            case 3 -> activateChase(player);
            default -> {
            }
        }
    }

    private void teleportSelf(ServerPlayer player) {
        if (isOnCooldown(player, 1)) {
            sendCooldownMessage(player, 1);
            return;
        }
        startCooldown(player, 1, TELEPORT_COOLDOWN_SECONDS);
        ServerLevel level = player.level();
        Vec3 destination = SpellTargeting.findSafeTeleportSpot(level, player.position(), TELEPORT_RADIUS);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHORUS_FRUIT_TELEPORT,
                SoundSource.PLAYERS, 1.0f, 1.0f);
        player.teleportTo(destination.x, destination.y, destination.z);
        level.playSound(null, destination.x, destination.y, destination.z, SoundEvents.CHORUS_FRUIT_TELEPORT,
                SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private void fireLevitationBullet(ServerPlayer player) {
        if (isOnCooldown(player, 2)) {
            sendCooldownMessage(player, 2);
            return;
        }
        SpellTargeting.findLookTarget(player, SHULKER_RANGE).ifPresent(hit -> {
            if (!(hit instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof LivingEntity target)) {
                return;
            }
            startCooldown(player, 2, SHULKER_COOLDOWN_SECONDS);
            ServerLevel level = player.level();
            ShulkerBullet bullet = new ShulkerBullet(level, player, target, null);
            level.addFreshEntity(bullet);
        });
    }

    private void activateChase(ServerPlayer player) {
        if (isOnCooldown(player, 3)) {
            sendCooldownMessage(player, 3);
            return;
        }
        startCooldown(player, 3, CHASE_COOLDOWN_SECONDS);
        long expiry = player.level().getGameTime() + CHASE_FALLBACK_DURATION_TICKS;
        chaseStates.put(player.getUUID(), new ChaseState(CHASE_MAX_HITS, expiry));
    }

    @Override
    public void tickEffect(ServerPlayer player, int level) {
        ChaseState state = chaseStates.get(player.getUUID());
        if (state != null && player.level().getGameTime() >= state.expiryTick()) {
            chaseStates.remove(player.getUUID());
        }
    }

    @Override
    public void onDealtDamage(ServerPlayer attacker, int level, LivingEntity target, DamageSource source, float damageDealt) {
        ChaseState state = chaseStates.get(attacker.getUUID());
        if (state == null || state.hitsRemaining() <= 0) {
            return;
        }
        ServerLevel serverLevel = attacker.level();
        Vec3 destination = SpellTargeting.findSafeTeleportSpot(serverLevel, target.position(), CHASE_TELEPORT_RADIUS);
        attacker.teleportTo(destination.x, destination.y, destination.z);
        serverLevel.sendParticles(ParticleTypes.PORTAL, destination.x, destination.y + 1.0, destination.z, 20, 0.3, 0.5, 0.3, 0.05);

        int remaining = state.hitsRemaining() - 1;
        if (remaining <= 0) {
            chaseStates.remove(attacker.getUUID());
        } else {
            chaseStates.put(attacker.getUUID(), new ChaseState(remaining, state.expiryTick()));
        }
    }

    private record ChaseState(int hitsRemaining, long expiryTick) {
    }
}

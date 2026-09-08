package yaboichips.effects.common.spell.aggressive;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import yaboichips.effects.common.spell.AggressiveSpell;
import yaboichips.effects.core.ModAttachments;
import yaboichips.effects.network.SetDeafenedPayload;

public class StealthSpell extends AggressiveSpell {

    private static final int HIDE_DURATION_TICKS = 600;
    private static final int HIDE_COOLDOWN_SECONDS = 180;

    private static final int SMOKESCREEN_COOLDOWN_SECONDS = 300;
    private static final double SMOKESCREEN_RADIUS = 20.0;
    private static final int SMOKESCREEN_DURATION_TICKS = 300;
    private static final int SMOKESCREEN_EFFECT_REFRESH_TICKS = 40;

    private static final int CLOAK_COOLDOWN_SECONDS = 240;
    private static final int CLOAK_DURATION_TICKS = 400;

    private final Map<UUID, Long> hideEndTick = new HashMap<>();
    private final Map<UUID, SmokeZone> smokeZones = new HashMap<>();
    private final Map<UUID, Boolean> cloakActive = new HashMap<>();

    public StealthSpell(Identifier id, Identifier icon) {
        super(id, icon, 3);
    }

    @Override
    public void activate(ServerPlayer player, int level) {
        switch (level) {
            case 1 -> activateHide(player);
            case 2 -> activateSmokescreen(player);
            case 3 -> activateCloak(player);
            default -> {
            }
        }
    }

    private void activateHide(ServerPlayer player) {
        if (isOnCooldown(player, 1)) {
            sendCooldownMessage(player, 1);
            return;
        }
        startCooldown(player, 1, HIDE_COOLDOWN_SECONDS);
        hideEndTick.put(player.getUUID(), player.level().getGameTime() + HIDE_DURATION_TICKS);
        player.setAttached(ModAttachments.STEALTH_ACTIVE, Boolean.TRUE);
    }

    private void activateSmokescreen(ServerPlayer player) {
        if (isOnCooldown(player, 2)) {
            sendCooldownMessage(player, 2);
            return;
        }
        startCooldown(player, 2, SMOKESCREEN_COOLDOWN_SECONDS);
        long now = player.level().getGameTime();
        smokeZones.put(player.getUUID(), new SmokeZone(player.position(), now + SMOKESCREEN_DURATION_TICKS));

        ServerLevel level = player.level();
        Vec3 pos = player.position();
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y + 1.0, pos.z, 80,
                SMOKESCREEN_RADIUS / 2, 1.0, SMOKESCREEN_RADIUS / 2, 0.05);
    }

    private void activateCloak(ServerPlayer player) {
        if (isOnCooldown(player, 3)) {
            sendCooldownMessage(player, 3);
            return;
        }
        startCooldown(player, 3, CLOAK_COOLDOWN_SECONDS);
        cloakActive.put(player.getUUID(), Boolean.TRUE);
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, CLOAK_DURATION_TICKS, 0, false, false));
    }

    @Override
    public void tickEffect(ServerPlayer player, int level) {
        UUID uuid = player.getUUID();
        long now = player.level().getGameTime();

        Long hideEnd = hideEndTick.get(uuid);
        if (hideEnd != null) {
            if (now >= hideEnd) {
                hideEndTick.remove(uuid);
                player.setAttached(ModAttachments.STEALTH_ACTIVE, Boolean.FALSE);
            }
        }

        SmokeZone zone = smokeZones.get(uuid);
        if (zone != null) {
            if (now >= zone.expiryTick()) {
                smokeZones.remove(uuid);
            } else {
                applySmokescreen(player, zone);
            }
        }
    }

    private void applySmokescreen(ServerPlayer caster, SmokeZone zone) {
        ServerLevel level = caster.level();
        AABB area = AABB.ofSize(zone.center(), SMOKESCREEN_RADIUS * 2, SMOKESCREEN_RADIUS * 2, SMOKESCREEN_RADIUS * 2);
        for (ServerPlayer nearby : level.getEntitiesOfClass(ServerPlayer.class, area,
                candidate -> candidate != caster && candidate.position().distanceTo(zone.center()) <= SMOKESCREEN_RADIUS)) {
            nearby.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, SMOKESCREEN_EFFECT_REFRESH_TICKS, 0, false, false));
            ServerPlayNetworking.send(nearby, new SetDeafenedPayload(SMOKESCREEN_EFFECT_REFRESH_TICKS));
        }
    }

    @Override
    public void onDamaged(ServerPlayer player, int level, DamageSource source, float damageTaken) {
        cancelCloakIfActive(player);
    }

    @Override
    public void onDealtDamage(ServerPlayer attacker, int level, LivingEntity target, DamageSource source, float damageDealt) {
        cancelCloakIfActive(attacker);
    }

    private void cancelCloakIfActive(ServerPlayer player) {
        if (Boolean.TRUE.equals(cloakActive.remove(player.getUUID()))) {
            player.removeEffect(MobEffects.INVISIBILITY);
        }
    }

    private record SmokeZone(Vec3 center, long expiryTick) {
    }
}

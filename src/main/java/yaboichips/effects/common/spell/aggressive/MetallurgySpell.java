package yaboichips.effects.common.spell.aggressive;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import yaboichips.effects.common.spell.AggressiveSpell;
import yaboichips.effects.common.spell.SpellCosts;
import yaboichips.effects.common.spell.SpellTargeting;
import yaboichips.effects.common.trust.TrustHelper;

public class MetallurgySpell extends AggressiveSpell {

    private static final int SHIELD_DURATION_TICKS = 200;
    private static final int SHIELD_COOLDOWN_SECONDS = 120;
    private static final int SHIELD_COST = 5;
    private static final float SHIELD_REDUCTION = 0.25f;

    private static final int DAGGER_ACCRUAL_INTERVAL_TICKS = 6000;
    private static final int DAGGER_COST = 2;
    private static final int MAX_DAGGERS = 4;
    private static final float DAGGER_DAMAGE = 4.0f;
    private static final double DAGGER_RANGE = 32.0;

    private static final int SLAM_COOLDOWN_SECONDS = 240;
    private static final int SLAM_COST = 10;
    private static final double SLAM_RADIUS = 5.0;
    private static final float SLAM_DAMAGE = 10.0f;
    private static final int SLAM_STUN_TICKS = 40;

    private final Map<UUID, Long> shieldEndTick = new HashMap<>();
    private final Map<UUID, Integer> daggerCount = new HashMap<>();
    private final Map<UUID, Long> lastDaggerAccrualTick = new HashMap<>();

    public MetallurgySpell(Identifier id, Identifier icon) {
        super(id, icon, 3);
    }

    @Override
    public void activate(ServerPlayer player, int level) {
        switch (level) {
            case 1 -> activateOrbitShield(player);
            case 2 -> throwDagger(player);
            case 3 -> heavySlam(player);
            default -> {
            }
        }
    }

    private void activateOrbitShield(ServerPlayer player) {
        if (isOnCooldown(player, 1)) {
            sendCooldownMessage(player, 1);
            return;
        }
        if (!SpellCosts.hasItems(player, Items.IRON_INGOT, SHIELD_COST)) {
            sendNotEnoughIron(player);
            return;
        }
        SpellCosts.consumeItems(player, Items.IRON_INGOT, SHIELD_COST);
        shieldEndTick.put(player.getUUID(), player.level().getGameTime() + SHIELD_DURATION_TICKS);
        startCooldown(player, 1, SHIELD_COOLDOWN_SECONDS);
    }

    private void throwDagger(ServerPlayer player) {
        int stock = daggerCount.getOrDefault(player.getUUID(), 0);
        if (stock <= 0) {
            player.displayClientMessage(Component.translatable("spell.effects.metallurgy_spell.no_daggers"), true);
            return;
        }
        daggerCount.put(player.getUUID(), stock - 1);
        SpellTargeting.findLookTarget(player, DAGGER_RANGE).ifPresent(hit -> {
            if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target) {
                target.hurtServer(player.level(), player.level().damageSources().playerAttack(player), DAGGER_DAMAGE);
            }
        });
    }

    private void heavySlam(ServerPlayer player) {
        if (isOnCooldown(player, 3)) {
            sendCooldownMessage(player, 3);
            return;
        }
        if (!SpellCosts.hasItems(player, Items.IRON_INGOT, SLAM_COST)) {
            sendNotEnoughIron(player);
            return;
        }
        SpellCosts.consumeItems(player, Items.IRON_INGOT, SLAM_COST);
        startCooldown(player, 3, SLAM_COOLDOWN_SECONDS);

        ServerLevel level = player.level();
        Vec3 pos = player.position();
        level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.5, pos.z, 1, 0.0, 0.0, 0.0, 0.0);

        AABB area = AABB.ofSize(pos, SLAM_RADIUS * 2, SLAM_RADIUS * 2, SLAM_RADIUS * 2);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                candidate -> candidate != player && candidate.position().distanceTo(pos) <= SLAM_RADIUS)) {
            if (target instanceof ServerPlayer targetPlayer && TrustHelper.isAlly(player, targetPlayer)) {
                continue;
            }
            target.hurtServer(level, level.damageSources().playerAttack(player), SLAM_DAMAGE);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLAM_STUN_TICKS, 9, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, SLAM_STUN_TICKS, 3, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, SLAM_STUN_TICKS, 3, false, true));
        }
    }

    @Override
    public void tickEffect(ServerPlayer player, int level) {
        if (level < 2) {
            return;
        }
        UUID uuid = player.getUUID();
        long now = player.level().getGameTime();
        Long last = lastDaggerAccrualTick.get(uuid);
        if (last != null && now - last < DAGGER_ACCRUAL_INTERVAL_TICKS) {
            return;
        }
        int stock = daggerCount.getOrDefault(uuid, 0);
        if (stock >= MAX_DAGGERS || !SpellCosts.hasItems(player, Items.IRON_INGOT, DAGGER_COST)) {
            lastDaggerAccrualTick.putIfAbsent(uuid, now);
            return;
        }
        SpellCosts.consumeItems(player, Items.IRON_INGOT, DAGGER_COST);
        daggerCount.put(uuid, stock + 1);
        lastDaggerAccrualTick.put(uuid, now);
    }

    @Override
    public boolean modifyIncomingDamage(ServerPlayer player, int level, DamageSource source, float amount) {
        Long end = shieldEndTick.get(player.getUUID());
        if (end == null || player.level().getGameTime() >= end) {
            return false;
        }
        float reduced = amount * (1 - SHIELD_REDUCTION);
        player.setHealth(Math.max(0.0f, player.getHealth() - reduced));
        return true;
    }

    private void sendNotEnoughIron(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("spell.effects.metallurgy_spell.not_enough_iron"), true);
    }
}

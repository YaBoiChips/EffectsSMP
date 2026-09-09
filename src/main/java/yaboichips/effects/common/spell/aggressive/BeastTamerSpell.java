package yaboichips.effects.common.spell.aggressive;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import yaboichips.effects.common.spell.AggressiveSpell;
import yaboichips.effects.common.spell.SpellTargeting;
import yaboichips.effects.common.trust.TrustHelper;

public class BeastTamerSpell extends AggressiveSpell {

    private static final int ABSORB_COOLDOWN_SECONDS = 180;
    private static final double ABILITY_RANGE = 32.0;
    private static final float SONIC_DAMAGE = 12.0f;
    private static final float SONIC_KNOCKBACK = 1.5f;
    private static final float ARROW_DAMAGE = 10.0f;
    private static final float CREEPER_BLAST_POWER = 3.0f;

    private static final int SUMMON_COOLDOWN_GROUP = 2;
    private static final int SUMMON_COOLDOWN_SECONDS = 270;
    private static final int MIN_WOLVES = 10;
    private static final int MAX_WOLVES_EXCLUSIVE = 16;

    private static final int BRUTE_COUNT = 3;
    private static final int BRUTE_LIFETIME_TICKS = 1200;
    private static final double BRUTE_TELEPORT_DISTANCE = 16.0;
    private static final double BRUTE_SEARCH_RANGE = 16.0;

    private final Map<UUID, EntityType<?>> loadedMob = new HashMap<>();
    private final Map<UUID, List<UUID>> summonedWolves = new HashMap<>();
    private final Map<UUID, BruteArmy> bruteArmies = new HashMap<>();

    public BeastTamerSpell(Identifier id, Identifier icon) {
        super(id, icon, 3);
    }

    @Override
    public void activate(ServerPlayer player, int level) {
        switch (level) {
            case 1 -> useAbsorbedAbility(player);
            case 2 -> summonWolfArmy(player);
            case 3 -> summonBruteArmy(player);
            default -> {
            }
        }
    }

    public InteractionResult tryAbsorb(ServerPlayer player, Entity entity) {
        EntityType<?> type = entity.getType();
        if (type != EntityType.WITHER_SKELETON && type != EntityType.WARDEN
                && type != EntityType.SKELETON && type != EntityType.CREEPER) {
            return InteractionResult.PASS;
        }
        loadedMob.put(player.getUUID(), type);
        entity.discard();
        player.displayClientMessage(
                Component.translatable("spell.effects.beast_tamer_spell.absorbed", entity.getType().getDescription()), true);
        return InteractionResult.SUCCESS;
    }

    private void useAbsorbedAbility(ServerPlayer player) {
        if (isOnCooldown(player, 1)) {
            sendCooldownMessage(player, 1);
            return;
        }
        EntityType<?> loaded = loadedMob.get(player.getUUID());
        if (loaded == null) {
            player.displayClientMessage(Component.translatable("spell.effects.beast_tamer_spell.no_mob_loaded"), true);
            return;
        }
        loadedMob.remove(player.getUUID());
        startCooldown(player, 1, ABSORB_COOLDOWN_SECONDS);

        if (loaded == EntityType.WITHER_SKELETON) {
            fireWitherSkulls(player);
        } else if (loaded == EntityType.WARDEN) {
            fireSonicBoom(player);
        } else if (loaded == EntityType.SKELETON) {
            firePowerArrow(player);
        } else if (loaded == EntityType.CREEPER) {
            fireContainedBlast(player);
        }
    }

    private void fireWitherSkulls(ServerPlayer player) {
        ServerLevel level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        for (int i = 0; i < 3; i++) {
            Vec3 jitter = new Vec3(
                    (player.getRandom().nextDouble() - 0.5) * 0.3,
                    (player.getRandom().nextDouble() - 0.5) * 0.3,
                    (player.getRandom().nextDouble() - 0.5) * 0.3);
            WitherSkull skull = new WitherSkull(level, player, look.scale(1.5).add(jitter));
            skull.setPos(eye.x, eye.y, eye.z);
            level.addFreshEntity(skull);
        }
    }

    private void fireSonicBoom(ServerPlayer player) {
        SpellTargeting.findLookTarget(player, ABILITY_RANGE).ifPresent(hit -> {
            if (!(hit instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof LivingEntity target)) {
                return;
            }
            ServerLevel level = player.level();
            target.hurtServer(level, level.damageSources().sonicBoom(player), SONIC_DAMAGE);
            target.knockback(SONIC_KNOCKBACK, player.getX() - target.getX(), player.getZ() - target.getZ());
            level.sendParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.0f, 1.0f);
        });
    }

    private void firePowerArrow(ServerPlayer player) {
        SpellTargeting.findLookTarget(player, ABILITY_RANGE).ifPresent(hit -> {
            if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target) {
                target.hurtServer(player.level(), player.level().damageSources().playerAttack(player), ARROW_DAMAGE);
            }
        });
    }

    private void fireContainedBlast(ServerPlayer player) {
        ServerLevel level = player.level();
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(ABILITY_RANGE));
        Vec3 blastPos = SpellTargeting.findLookTarget(player, ABILITY_RANGE).map(hit -> hit.getLocation()).orElse(end);
        level.explode(player, blastPos.x, blastPos.y, blastPos.z, CREEPER_BLAST_POWER, false, Level.ExplosionInteraction.NONE);
    }

    private void summonWolfArmy(ServerPlayer player) {
        if (isOnCooldown(player, SUMMON_COOLDOWN_GROUP)) {
            sendCooldownMessage(player, SUMMON_COOLDOWN_GROUP);
            return;
        }
        startCooldown(player, SUMMON_COOLDOWN_GROUP, SUMMON_COOLDOWN_SECONDS);

        ServerLevel level = player.level();
        discardTracked(level, summonedWolves.remove(player.getUUID()));

        int count = MIN_WOLVES + player.getRandom().nextInt(MAX_WOLVES_EXCLUSIVE - MIN_WOLVES);
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Wolf wolf = EntityType.WOLF.create(level, EntitySpawnReason.TRIGGERED);
            if (wolf == null) {
                continue;
            }
            double offsetX = (player.getRandom().nextDouble() - 0.5) * 4;
            double offsetZ = (player.getRandom().nextDouble() - 0.5) * 4;
            wolf.setPos(player.getX() + offsetX, player.getY(), player.getZ() + offsetZ);
            wolf.tame(player);
            wolf.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.WOLF_ARMOR));
            level.addFreshEntity(wolf);
            ids.add(wolf.getUUID());
        }
        summonedWolves.put(player.getUUID(), ids);
    }

    private void summonBruteArmy(ServerPlayer player) {
        if (isOnCooldown(player, SUMMON_COOLDOWN_GROUP)) {
            sendCooldownMessage(player, SUMMON_COOLDOWN_GROUP);
            return;
        }
        startCooldown(player, SUMMON_COOLDOWN_GROUP, SUMMON_COOLDOWN_SECONDS);

        ServerLevel level = player.level();
        BruteArmy previous = bruteArmies.remove(player.getUUID());
        if (previous != null) {
            discardTracked(level, previous.ids());
        }

        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < BRUTE_COUNT; i++) {
            PiglinBrute brute = EntityType.PIGLIN_BRUTE.create(level, EntitySpawnReason.TRIGGERED);
            if (brute == null) {
                continue;
            }
            double offsetX = (player.getRandom().nextDouble() - 0.5) * 4;
            double offsetZ = (player.getRandom().nextDouble() - 0.5) * 4;
            brute.setPos(player.getX() + offsetX, player.getY(), player.getZ() + offsetZ);
            brute.addEffect(new MobEffectInstance(MobEffects.SPEED, BRUTE_LIFETIME_TICKS + 20, 1, false, false));
            brute.setImmuneToZombification(true);
            brute.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_AXE));
            level.addFreshEntity(brute);
            ids.add(brute.getUUID());
        }
        bruteArmies.put(player.getUUID(), new BruteArmy(ids, level.getGameTime() + BRUTE_LIFETIME_TICKS));
    }

    @Override
    public void tickEffect(ServerPlayer player, int level) {
        BruteArmy army = bruteArmies.get(player.getUUID());
        if (army == null) {
            return;
        }
        ServerLevel serverLevel = player.level();
        if (serverLevel.getGameTime() >= army.expiryTick()) {
            discardTracked(serverLevel, army.ids());
            bruteArmies.remove(player.getUUID());
            return;
        }
        for (UUID bruteId : army.ids()) {
            if (serverLevel.getEntity(bruteId) instanceof PiglinBrute brute && brute.isAlive()) {
                manageBrute(brute, player, serverLevel);
            }
        }
    }

    private void manageBrute(PiglinBrute brute, ServerPlayer owner, ServerLevel level) {
        LivingEntity target = brute.getTarget();
        boolean invalidTarget = target == null || target == owner
                || (target instanceof ServerPlayer targetPlayer && TrustHelper.isAlly(owner, targetPlayer));
        if (invalidTarget) {
            brute.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            LivingEntity newTarget = findNearestFoe(brute, owner, level);
            if (newTarget != null) {
                brute.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, newTarget);
            }
        }

        if (brute.distanceToSqr(owner) > BRUTE_TELEPORT_DISTANCE * BRUTE_TELEPORT_DISTANCE) {
            Vec3 destination = SpellTargeting.findSafeTeleportSpot(level, owner.position(), 3.0);
            brute.teleportTo(destination.x, destination.y, destination.z);
        }
    }

    private LivingEntity findNearestFoe(PiglinBrute brute, ServerPlayer owner, ServerLevel level) {
        AABB area = AABB.ofSize(brute.position(), BRUTE_SEARCH_RANGE * 2, BRUTE_SEARCH_RANGE * 2, BRUTE_SEARCH_RANGE * 2);
        return level.getEntitiesOfClass(LivingEntity.class, area, candidate ->
                        candidate != brute && candidate != owner && candidate.isAlive()
                                && candidate.getType() != EntityType.PIGLIN_BRUTE
                                && !(candidate instanceof ServerPlayer candidatePlayer && TrustHelper.isAlly(owner, candidatePlayer))
                                && brute.hasLineOfSight(candidate))
                .stream()
                .min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(brute)))
                .orElse(null);
    }

    private void discardTracked(ServerLevel level, List<UUID> ids) {
        if (ids == null) {
            return;
        }
        for (UUID id : ids) {
            Entity entity = level.getEntity(id);
            if (entity != null) {
                entity.discard();
            }
        }
    }

    private record BruteArmy(List<UUID> ids, long expiryTick) {
    }
}

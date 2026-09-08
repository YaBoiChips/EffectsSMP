package yaboichips.effects.common.spell;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Shared "what is this caster looking at" / "find a safe place to teleport"
 * helpers, reused by Chorus, Beast Tamer's absorbed abilities, and Archer's
 * grappling hook - no equivalent existed elsewhere in the mod prior to this.
 */
public final class SpellTargeting {

    private static final int SAFE_SPOT_ATTEMPTS = 16;

    private SpellTargeting() {
    }

    /**
     * Combined block+entity raycast along the caster's look direction, up to
     * {@code range} blocks, returning whichever is closer.
     */
    public static Optional<HitResult> findLookTarget(ServerPlayer caster, double range) {
        Vec3 start = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        Vec3 end = start.add(look.scale(range));

        HitResult blockHit = caster.pick(range, 1.0f, false);

        AABB searchBox = caster.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(caster, start, end, searchBox,
                candidate -> candidate != caster && candidate.isPickable() && !candidate.isSpectator(), range * range);

        boolean blockHitFound = blockHit.getType() != HitResult.Type.MISS;
        if (entityHit == null) {
            return blockHitFound ? Optional.of(blockHit) : Optional.empty();
        }
        if (blockHitFound && blockHit.getLocation().distanceToSqr(start) < entityHit.getLocation().distanceToSqr(start)) {
            return Optional.of(blockHit);
        }
        return Optional.of(entityHit);
    }

    /**
     * Chorus-fruit-style random safe landing spot within {@code radius}
     * blocks of {@code center}: a handful of random attempts, falling back to
     * {@code center} itself if none land somewhere open with solid footing.
     */
    public static Vec3 findSafeTeleportSpot(ServerLevel level, Vec3 center, double radius) {
        RandomSource random = level.getRandom();
        for (int attempt = 0; attempt < SAFE_SPOT_ATTEMPTS; attempt++) {
            Vec3 candidate = center.add(
                    (random.nextDouble() * 2 - 1) * radius,
                    (random.nextDouble() * 2 - 1) * radius,
                    (random.nextDouble() * 2 - 1) * radius);
            if (isSafeLanding(level, candidate)) {
                return candidate;
            }
        }
        return center;
    }

    private static boolean isSafeLanding(ServerLevel level, Vec3 pos) {
        BlockPos feet = BlockPos.containing(pos);
        BlockPos head = feet.above();
        BlockPos ground = feet.below();
        return level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(head).getCollisionShape(level, head).isEmpty()
                && !level.getBlockState(ground).getCollisionShape(level, ground).isEmpty();
    }
}

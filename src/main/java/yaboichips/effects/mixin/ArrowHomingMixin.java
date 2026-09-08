package yaboichips.effects.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import yaboichips.effects.common.spell.SpellTargeting;
import yaboichips.effects.core.ModSpells;

/**
 * Archer Level 3's "aimbot" - there's no Fabric event for "steer this arrow",
 * so it's implemented the same way EnchanterSpell handles its own no-event
 * gaps: a mixin that checks the shooter's active spell state directly.
 */
@Mixin(AbstractArrow.class)
public abstract class ArrowHomingMixin {

    private static final double AIM_RANGE = 64.0;
    private static final double TURN_RATE = 0.2;

    @Inject(method = "tick", at = @At("HEAD"))
    private void effects$homeTowardTarget(CallbackInfo ci) {
        AbstractArrow self = (AbstractArrow) (Object) this;
        if (!(self.getOwner() instanceof ServerPlayer shooter) || !ModSpells.ARCHER_SPELL.hasAimbotActive(shooter)) {
            return;
        }

        Optional<HitResult> target = SpellTargeting.findLookTarget(shooter, AIM_RANGE);
        if (target.isEmpty() || !(target.get() instanceof EntityHitResult entityHit)) {
            return;
        }
        Entity targetEntity = entityHit.getEntity();
        if (targetEntity == shooter) {
            return;
        }

        Vec3 currentVelocity = self.getDeltaMovement();
        double speed = currentVelocity.length();
        if (speed < 1.0e-4) {
            return;
        }

        Vec3 toTarget = targetEntity.getEyePosition().subtract(self.position()).normalize();
        Vec3 newDirection = currentVelocity.normalize().scale(1 - TURN_RATE).add(toTarget.scale(TURN_RATE)).normalize();
        self.setDeltaMovement(newDirection.scale(speed));
    }
}

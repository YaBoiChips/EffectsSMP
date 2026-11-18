package yaboichips.effects.common.effect;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Crouching charges a jump (up to {@link #MAX_CHARGE_TICKS}); jumping while
 * charged launches the player based on how charged up they are. Fall damage
 * negation while this effect is active is handled centrally alongside the
 * other damage hooks in {@code Effects}, since that's where incoming damage
 * is already intercepted for other effects/spells.
 */
public class SpringMobEffect extends MobEffect {

    private static final int MAX_CHARGE_TICKS = 60;
    private static final double MAX_VERTICAL_VELOCITY = 2.6;
    private static final double MAX_HORIZONTAL_VELOCITY = 1.1;

    private final Map<UUID, Integer> chargeTicks = new HashMap<>();
    private final Map<UUID, Boolean> wasOnGround = new HashMap<>();

    public SpringMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x4CBB17);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (!(entity instanceof ServerPlayer player)) {
            return true;
        }

        UUID id = player.getUUID();
        boolean onGround = player.onGround();
        boolean wasGrounded = wasOnGround.getOrDefault(id, onGround);

        if (player.isShiftKeyDown() && onGround) {
            chargeTicks.merge(id, 1, (current, increment) -> Math.min(current + increment, MAX_CHARGE_TICKS));
        }

        int charge = chargeTicks.getOrDefault(id, 0);
        if (charge > 0 && wasGrounded && !onGround) {
            launch(player, charge / (double) MAX_CHARGE_TICKS);
            chargeTicks.put(id, 0);
        }

        wasOnGround.put(id, onGround);
        return true;
    }

    private void launch(ServerPlayer player, double chargeFraction) {
        Vec3 look = player.getLookAngle();
        double horizontalLenSq = look.x * look.x + look.z * look.z;
        Vec3 horizontal = horizontalLenSq > 1.0E-4
                ? new Vec3(look.x, 0, look.z).normalize().scale(MAX_HORIZONTAL_VELOCITY * chargeFraction)
                : Vec3.ZERO;

        player.setDeltaMovement(horizontal.x, MAX_VERTICAL_VELOCITY * chargeFraction, horizontal.z);
        player.hurtMarked = true;
    }
}

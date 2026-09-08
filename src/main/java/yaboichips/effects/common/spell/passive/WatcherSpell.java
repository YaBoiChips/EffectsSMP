package yaboichips.effects.common.spell.passive;

import java.util.Comparator;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import yaboichips.effects.common.spell.PassiveSpell;

/**
 * Level 1 (revealing invisible players' nametags while crouching) is purely
 * client-side, handled by {@code NameTagVisibilityMixin} - the client already
 * knows its own equipped spells, so no server logic is needed for it here.
 */
public class WatcherSpell extends PassiveSpell {

    private static final int ECHO_INTERVAL_TICKS = 600;
    private static final double ECHO_RANGE = 16.0;
    private static final int DARKNESS_DURATION_TICKS = 20;

    public WatcherSpell(Identifier id, Identifier icon) {
        super(id, icon, 2);
    }

    @Override
    public void tickEffect(ServerPlayer player, int level) {
        if (level < 2 || player.tickCount % ECHO_INTERVAL_TICKS != 0) {
            return;
        }
        ServerLevel serverLevel = player.level();
        AABB area = AABB.ofSize(player.position(), ECHO_RANGE * 2, ECHO_RANGE * 2, ECHO_RANGE * 2);
        LivingEntity nearest = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                        candidate -> candidate != player && candidate.position().distanceTo(player.position()) <= ECHO_RANGE)
                .stream()
                .min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(player)))
                .orElse(null);
        if (nearest == null) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DARKNESS_DURATION_TICKS, 0, false, false));
        serverLevel.playSound(null, nearest.getX(), nearest.getY(), nearest.getZ(),
                SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 1.0f, 1.0f);
    }
}

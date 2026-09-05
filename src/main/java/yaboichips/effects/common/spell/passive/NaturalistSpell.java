package yaboichips.effects.common.spell.passive;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import yaboichips.effects.common.spell.PassiveSpell;
import yaboichips.effects.common.trust.TrustHelper;

public class NaturalistSpell extends PassiveSpell {

    private static final float SPORE_PROC_CHANCE = 0.025f;
    private static final double SPORE_RADIUS = 5.0;
    private static final float SPORE_HEAL_AMOUNT = 2.0f;
    private static final int SPORE_COLOR = 0x33CC33;

    public NaturalistSpell(Identifier id, Identifier icon) {
        super(id, icon, 2);
    }

    @Override
    public void tickEffect(ServerPlayer player, int level) {
        if (!isExposedToSunlight(player)) {
            return;
        }
        MobEffectInstance current = player.getEffect(MobEffects.REGENERATION);
        if (current == null || current.getDuration() < 40) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, false));
        }
    }

    @Override
    public void onDamaged(ServerPlayer player, int level, DamageSource source, float damageTaken) {
        if (level < 2 || player.getRandom().nextFloat() >= SPORE_PROC_CHANCE) {
            return;
        }

        ServerLevel serverLevel = player.level();
        Vec3 pos = player.position();
        serverLevel.sendParticles(new DustParticleOptions(SPORE_COLOR, 1.5f),
                pos.x, pos.y + 1.0, pos.z, 40, 1.0, 0.5, 1.0, 0.05);

        AABB area = AABB.ofSize(pos, SPORE_RADIUS * 2, SPORE_RADIUS * 2, SPORE_RADIUS * 2);
        for (ServerPlayer nearby : serverLevel.getEntitiesOfClass(ServerPlayer.class, area,
                candidate -> candidate.position().distanceTo(pos) <= SPORE_RADIUS)) {
            if (TrustHelper.isAlly(player, nearby)) {
                nearby.heal(SPORE_HEAL_AMOUNT);
            }
        }
    }

    private boolean isExposedToSunlight(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos pos = player.blockPosition();
        return level.isBrightOutside() && level.canSeeSky(pos) && !level.isRainingAt(pos);
    }
}

package yaboichips.effects.common.spell.passive;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import yaboichips.effects.Effects;
import yaboichips.effects.common.spell.PassiveSpell;
import yaboichips.effects.common.trust.TrustHelper;

public class LavabenderSpell extends PassiveSpell {

    private static final Identifier ATTACK_BONUS_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(Effects.MOD_ID, "lavabender_attack_bonus");
    private static final double ATTACK_DAMAGE_BONUS = 1.0;
    private static final float ERUPTION_THRESHOLD = 10.0f;
    private static final double ERUPTION_RADIUS = 10.0;
    private static final float ERUPTION_DAMAGE = 10.0f;

    private final Map<UUID, Float> fireDamageAccumulator = new HashMap<>();

    public LavabenderSpell(Identifier id, Identifier icon) {
        super(id, icon, 2);
    }

    @Override
    public void tickEffect(ServerPlayer player, int level) {
        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage == null) {
            return;
        }
        boolean inLava = player.isInLava();
        boolean hasModifier = attackDamage.hasModifier(ATTACK_BONUS_MODIFIER_ID);
        if (inLava && !hasModifier) {
            attackDamage.addTransientModifier(new AttributeModifier(ATTACK_BONUS_MODIFIER_ID,
                    ATTACK_DAMAGE_BONUS, AttributeModifier.Operation.ADD_VALUE));
        } else if (!inLava && hasModifier) {
            attackDamage.removeModifier(ATTACK_BONUS_MODIFIER_ID);
        }
    }

    @Override
    public void onDamaged(ServerPlayer player, int level, DamageSource source, float damageTaken) {
        if (level < 2 || !source.is(DamageTypes.ON_FIRE)) {
            return;
        }

        float total = fireDamageAccumulator.merge(player.getUUID(), damageTaken, Float::sum);
        if (total < ERUPTION_THRESHOLD) {
            return;
        }
        fireDamageAccumulator.put(player.getUUID(), 0.0f);

        ServerLevel serverLevel = player.level();
        Vec3 pos = player.position();
        serverLevel.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.5, pos.z, 60, 2.0, 0.5, 2.0, 0.05);

        AABB area = AABB.ofSize(pos, ERUPTION_RADIUS * 2, ERUPTION_RADIUS * 2, ERUPTION_RADIUS * 2);
        for (ServerPlayer nearby : serverLevel.getEntitiesOfClass(ServerPlayer.class, area,
                candidate -> candidate.position().distanceTo(pos) <= ERUPTION_RADIUS)) {
            if (!TrustHelper.isAlly(player, nearby)) {
                nearby.hurtServer(serverLevel, serverLevel.damageSources().magic(), ERUPTION_DAMAGE);
            }
        }
    }
}

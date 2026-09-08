package yaboichips.effects.common.spell.passive;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import yaboichips.effects.common.spell.PassiveSpell;

public class LightningSpell extends PassiveSpell {

    private static final float TASE_CHANCE = 0.10f;
    private static final float LIGHTNING_ROD_CHANCE = 0.25f;
    private static final float LIGHTNING_ROD_DAMAGE = 4.0f;
    private static final int LIGHTNING_ROD_COOLDOWN_TICKS = 600;

    private final Map<UUID, Long> lightningRodCooldowns = new HashMap<>();

    public LightningSpell(Identifier id, Identifier icon) {
        super(id, icon, 2);
    }

    @Override
    public void tickEffect(ServerPlayer player, int level) {
        // Both levels are on-hit reactions; nothing to apply continuously.
    }

    @Override
    public void onDealtDamage(ServerPlayer attacker, int level, LivingEntity target, DamageSource source, float damageDealt) {
        if (target instanceof ServerPlayer targetPlayer) {
            tryTase(attacker, targetPlayer);
        }
        if (level >= 2) {
            tryLightningRod(attacker, target);
        }
    }

    private void tryTase(ServerPlayer attacker, ServerPlayer target) {
        if (attacker.getRandom().nextFloat() >= TASE_CHANCE) {
            return;
        }
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 9, false, true));
    }

    private void tryLightningRod(ServerPlayer attacker, LivingEntity target) {
        if (!attacker.getMainHandItem().is(ItemTags.AXES) || !(target instanceof ServerPlayer targetPlayer)) {
            return;
        }
        if (!justDisabledBlock(targetPlayer)) {
            return;
        }

        long now = attacker.tickCount;
        Long lastUse = lightningRodCooldowns.get(attacker.getUUID());
        if (lastUse != null && now - lastUse < LIGHTNING_ROD_COOLDOWN_TICKS) {
            return;
        }
        if (attacker.getRandom().nextFloat() >= LIGHTNING_ROD_CHANCE) {
            return;
        }
        lightningRodCooldowns.put(attacker.getUUID(), now);

        ServerLevel level = attacker.level();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt != null) {
            bolt.setVisualOnly(true);
            bolt.setPos(target.position());
            level.addFreshEntity(bolt);
        }
        target.hurtServer(level, level.damageSources().magic(), LIGHTNING_ROD_DAMAGE);
    }

    /**
     * Heuristic for "this hit just disabled the target's blocking item": a
     * blocking-capable item only ever enters cooldown via the block-disable
     * path, so seeing it on cooldown immediately after this hit lands is a
     * reliable proxy without needing a mixin into the disable internals.
     */
    private boolean justDisabledBlock(ServerPlayer target) {
        ItemStack mainHand = target.getMainHandItem();
        ItemStack offHand = target.getOffhandItem();
        ItemStack blockingItem = offHand.has(DataComponents.BLOCKS_ATTACKS) ? offHand
                : (mainHand.has(DataComponents.BLOCKS_ATTACKS) ? mainHand : ItemStack.EMPTY);
        return !blockingItem.isEmpty() && target.getCooldowns().isOnCooldown(blockingItem);
    }
}

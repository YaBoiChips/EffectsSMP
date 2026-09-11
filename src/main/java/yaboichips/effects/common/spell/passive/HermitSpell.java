package yaboichips.effects.common.spell.passive;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import yaboichips.effects.common.item.SpellBookItem;
import yaboichips.effects.common.spell.PassiveSpell;

public class HermitSpell extends PassiveSpell {

    private static final int IDLE_TICKS = 35;
    private static final int TICKS_PER_HASTE_LEVEL = 100;
    private static final int MAX_HASTE_AMPLIFIER = 3;
    private static final float DAMAGE_HALVE_CHANCE = 0.10f;

    private final Map<UUID, Long> lastBreakTick = new HashMap<>();
    private final Map<UUID, Long> streakStartTick = new HashMap<>();

    public HermitSpell(Identifier id, Identifier icon) {
        super(id, icon, 2);
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || !isEquipped(serverPlayer)) {
                return;
            }
            long now = serverPlayer.tickCount;
            Long last = lastBreakTick.get(serverPlayer.getUUID());
            if (last == null || now - last > IDLE_TICKS) {
                streakStartTick.put(serverPlayer.getUUID(), now);
            }
            lastBreakTick.put(serverPlayer.getUUID(), now);
        });
    }

    private boolean isEquipped(ServerPlayer player) {
        var data = SpellBookItem.getBoundBookData(player);
        return data.equippedPassive().filter(id()::equals).isPresent();
    }

    @Override
    public void tickEffect(ServerPlayer player, int level) {
        UUID uuid = player.getUUID();
        long now = player.tickCount;
        Long last = lastBreakTick.get(uuid);
        if (last == null || now - last > IDLE_TICKS) {
            return;
        }
        long start = streakStartTick.getOrDefault(uuid, now);
        int amplifier = (int) Math.min(MAX_HASTE_AMPLIFIER, (now - start) / TICKS_PER_HASTE_LEVEL);

        MobEffectInstance current = player.getEffect(MobEffects.HASTE);
        if (current == null || current.getAmplifier() != amplifier || current.getDuration() < 20) {
            player.addEffect(new MobEffectInstance(MobEffects.HASTE, 60, amplifier, true, false));
        }
    }

    @Override
    public boolean modifyIncomingDamage(ServerPlayer player, int level, DamageSource source, float amount) {
        if (level < 2 || player.getRandom().nextFloat() >= DAMAGE_HALVE_CHANCE) {
            return false;
        }
        float reduced = amount / 2.0f;
        player.setHealth(Math.max(0.0f, player.getHealth() - reduced));
        return true;
    }
}

package yaboichips.effects;

import java.util.Optional;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yaboichips.effects.common.effect.NumbnessMobEffect;
import yaboichips.effects.common.item.SpellBookItem;
import yaboichips.effects.common.spell.Spell;
import yaboichips.effects.core.ModAttachments;
import yaboichips.effects.core.ModBlocks;
import yaboichips.effects.core.ModCommands;
import yaboichips.effects.core.ModComponents;
import yaboichips.effects.core.ModItemGroups;
import yaboichips.effects.core.ModItems;
import yaboichips.effects.core.ModMenus;
import yaboichips.effects.core.ModMobEffects;
import yaboichips.effects.core.ModPayloads;
import yaboichips.effects.core.ModPotions;
import yaboichips.effects.core.ModSpells;
import yaboichips.effects.data.SpellBookData;

public class Effects implements ModInitializer {

    public static final String MOD_ID = "effects";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.registerModItems();
        ModBlocks.registerModBlocks();
        ModItemGroups.registerItemGroups();
        ModSpells.registerModSpells();
        ModAttachments.registerAttachments();
        ModComponents.registerModComponents();
        ModMobEffects.registerModMobEffects();
        ModPotions.registerModPotions();
        ModMenus.registerModMenus();
        ModPayloads.registerPayloads();
        ModPayloads.registerServerReceivers();
        ModCommands.registerCommands();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                var data = SpellBookItem.getBoundBookData(player);
                tickEquipped(player, data, data.equippedPassive());
                tickEquipped(player, data, data.equippedAggressive());
            }
        });

        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
            if (entity instanceof ServerPlayer victim) {
                var data = SpellBookItem.getBoundBookData(victim);
                notifyDamaged(victim, data, data.equippedPassive(), source, damageTaken);
                notifyDamaged(victim, data, data.equippedAggressive(), source, damageTaken);
            }

            if (source.getEntity() instanceof ServerPlayer attacker) {
                var data = SpellBookItem.getBoundBookData(attacker);
                notifyDealtDamage(attacker, data, data.equippedPassive(), entity, source, damageTaken);
                notifyDealtDamage(attacker, data, data.equippedAggressive(), entity, source, damageTaken);
            }
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer victim)) {
                return true;
            }

            if (source.is(DamageTypes.FALL) && victim.hasEffect(ModMobEffects.SPRING)) {
                return false;
            }

            if (victim.hasEffect(ModMobEffects.NUMBNESS)) {
                NumbnessMobEffect.bank(victim.getUUID(), amount);
                return false;
            }

            var victimData = SpellBookItem.getBoundBookData(victim);
            if (modifyIncoming(victim, victimData, victimData.equippedPassive(), source, amount)
                    || modifyIncoming(victim, victimData, victimData.equippedAggressive(), source, amount)) {
                return false;
            }

            if (source.getEntity() instanceof ServerPlayer attacker) {
                var attackerData = SpellBookItem.getBoundBookData(attacker);
                if (modifyOutgoing(attacker, attackerData, attackerData.equippedPassive(), victim, source, amount)
                        || modifyOutgoing(attacker, attackerData, attackerData.equippedAggressive(), victim, source, amount)) {
                    return false;
                }
            }

            return true;
        });

        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer) || !player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            var data = SpellBookItem.getBoundBookData(serverPlayer);
            boolean hasBeastTamer = data.equippedAggressive().filter(ModSpells.BEAST_TAMER_SPELL.id()::equals)
                    .map(data::levelOf).orElse(0) >= 1;
            if (!hasBeastTamer) {
                return InteractionResult.PASS;
            }
            return ModSpells.BEAST_TAMER_SPELL.tryAbsorb(serverPlayer, entity);
        });
    }

    private void tickEquipped(ServerPlayer player, SpellBookData data, Optional<Identifier> equipped) {
        equipped.ifPresent(spellId -> {
            int level = data.levelOf(spellId);
            Spell spell = ModSpells.get(spellId);
            if (level > 0 && spell != null) {
                spell.tickEffect(player, level);
            }
        });
    }

    private void notifyDamaged(ServerPlayer player, SpellBookData data, Optional<Identifier> equipped,
                                DamageSource source, float damageTaken) {
        equipped.ifPresent(spellId -> {
            int level = data.levelOf(spellId);
            Spell spell = ModSpells.get(spellId);
            if (level > 0 && spell != null) {
                spell.onDamaged(player, level, source, damageTaken);
            }
        });
    }

    private void notifyDealtDamage(ServerPlayer attacker, SpellBookData data, Optional<Identifier> equipped,
                                    LivingEntity target, DamageSource source, float damageDealt) {
        equipped.ifPresent(spellId -> {
            int level = data.levelOf(spellId);
            Spell spell = ModSpells.get(spellId);
            if (level > 0 && spell != null) {
                spell.onDealtDamage(attacker, level, target, source, damageDealt);
            }
        });
    }

    private boolean modifyIncoming(ServerPlayer player, SpellBookData data, Optional<Identifier> equipped,
                                    DamageSource source, float amount) {
        if (equipped.isEmpty()) {
            return false;
        }
        Identifier spellId = equipped.get();
        int level = data.levelOf(spellId);
        Spell spell = ModSpells.get(spellId);
        return level > 0 && spell != null && spell.modifyIncomingDamage(player, level, source, amount);
    }

    private boolean modifyOutgoing(ServerPlayer attacker, SpellBookData data, Optional<Identifier> equipped,
                                    ServerPlayer victim, DamageSource source, float amount) {
        if (equipped.isEmpty()) {
            return false;
        }
        Identifier spellId = equipped.get();
        int level = data.levelOf(spellId);
        Spell spell = ModSpells.get(spellId);
        if (level <= 0 || spell == null) {
            return false;
        }
        float adjusted = spell.modifyOutgoingDamage(attacker, level, victim, source, amount);
        if (adjusted == amount) {
            return false;
        }
        victim.setHealth(Math.max(0.0f, victim.getHealth() - adjusted));
        return true;
    }
}

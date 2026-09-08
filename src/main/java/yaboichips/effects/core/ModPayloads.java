package yaboichips.effects.core;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import yaboichips.effects.Effects;
import yaboichips.effects.common.spell.AggressiveSpell;
import yaboichips.effects.common.spell.Spell;
import yaboichips.effects.network.ActivateAggressiveSpellPayload;
import yaboichips.effects.network.EquipSpellPayload;
import yaboichips.effects.network.OpenSpellTablePayload;
import yaboichips.effects.network.SetDeafenedPayload;
import yaboichips.effects.network.UnlockSpellPayload;
import yaboichips.effects.network.UpgradeSpellPayload;

public class ModPayloads {

    public static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(OpenSpellTablePayload.TYPE, OpenSpellTablePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SetDeafenedPayload.TYPE, SetDeafenedPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(UnlockSpellPayload.TYPE, UnlockSpellPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(EquipSpellPayload.TYPE, EquipSpellPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(UpgradeSpellPayload.TYPE, UpgradeSpellPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ActivateAggressiveSpellPayload.TYPE, ActivateAggressiveSpellPayload.STREAM_CODEC);
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(UnlockSpellPayload.TYPE, (payload, context) -> {
            Spell spell = ModSpells.get(payload.spellId());
            context.player().modifyAttached(ModAttachments.SPELL_DATA, data -> data.tryUnlock(spell));
        });

        ServerPlayNetworking.registerGlobalReceiver(EquipSpellPayload.TYPE, (payload, context) -> {
            Spell spell = ModSpells.get(payload.spellId());
            context.player().modifyAttached(ModAttachments.SPELL_DATA, data -> data.withEquipped(spell));
        });

        ServerPlayNetworking.registerGlobalReceiver(UpgradeSpellPayload.TYPE, (payload, context) -> {
            Spell spell = ModSpells.get(payload.spellId());
            context.player().modifyAttached(ModAttachments.SPELL_DATA, data -> data.tryUpgrade(spell));
        });

        ServerPlayNetworking.registerGlobalReceiver(ActivateAggressiveSpellPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            var data = player.getAttachedOrCreate(ModAttachments.SPELL_DATA);
            data.equippedAggressive().ifPresent(spellId -> {
                if (data.levelOf(spellId) >= payload.level() && ModSpells.get(spellId) instanceof AggressiveSpell aggressiveSpell) {
                    aggressiveSpell.activate(player, payload.level());
                }
            });
        });
    }
}

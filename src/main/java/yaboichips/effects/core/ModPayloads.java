package yaboichips.effects.core;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import yaboichips.effects.common.item.SpellBookItem;
import yaboichips.effects.common.menu.SpellTableMenu;
import yaboichips.effects.common.spell.AggressiveSpell;
import yaboichips.effects.data.SpellBookData;
import yaboichips.effects.network.ActivateAggressiveSpellPayload;
import yaboichips.effects.network.EquipSpellPayload;
import yaboichips.effects.network.SetDeafenedPayload;
import yaboichips.effects.network.UnlockSpellPayload;
import yaboichips.effects.network.UpgradeSpellPayload;

public class ModPayloads {

    public static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(SetDeafenedPayload.TYPE, SetDeafenedPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(UnlockSpellPayload.TYPE, UnlockSpellPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(EquipSpellPayload.TYPE, EquipSpellPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(UpgradeSpellPayload.TYPE, UpgradeSpellPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ActivateAggressiveSpellPayload.TYPE, ActivateAggressiveSpellPayload.STREAM_CODEC);
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(UnlockSpellPayload.TYPE, (payload, context) -> {
            if (context.player().containerMenu instanceof SpellTableMenu menu) {
                menu.tryUnlockSpell(context.player(), ModSpells.get(payload.spellId()));
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(EquipSpellPayload.TYPE, (payload, context) -> {
            if (context.player().containerMenu instanceof SpellTableMenu menu) {
                menu.tryEquipSpell(context.player(), ModSpells.get(payload.spellId()));
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(UpgradeSpellPayload.TYPE, (payload, context) -> {
            if (context.player().containerMenu instanceof SpellTableMenu menu) {
                menu.tryUpgradeSpell(context.player(), ModSpells.get(payload.spellId()));
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(ActivateAggressiveSpellPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            int slotIndex = SpellBookItem.findBoundBookSlot(player);
            if (slotIndex < 0) {
                return;
            }
            ItemStack book = player.getInventory().getItem(slotIndex);
            SpellBookData data = book.getOrDefault(ModComponents.SPELL_BOOK_DATA, SpellBookData.DEFAULT);
            data.equippedAggressive().ifPresent(spellId -> {
                if (data.levelOf(spellId) >= payload.level() && ModSpells.get(spellId) instanceof AggressiveSpell aggressiveSpell) {
                    aggressiveSpell.activate(player, payload.level());
                }
            });
        });
    }
}

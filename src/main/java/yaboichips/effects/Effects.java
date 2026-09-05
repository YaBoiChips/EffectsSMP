package yaboichips.effects;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yaboichips.effects.common.spell.PassiveSpell;
import yaboichips.effects.core.ModAttachments;
import yaboichips.effects.core.ModBlocks;
import yaboichips.effects.core.ModItemGroups;
import yaboichips.effects.core.ModItems;
import yaboichips.effects.core.ModPayloads;
import yaboichips.effects.core.ModSpells;

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
        ModPayloads.registerPayloads();
        ModPayloads.registerServerReceivers();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                var data = player.getAttachedOrCreate(ModAttachments.SPELL_DATA);
                data.equippedPassive().ifPresent(spellId -> {
                    int level = data.levelOf(spellId);
                    if (level > 0 && ModSpells.get(spellId) instanceof PassiveSpell passiveSpell) {
                        passiveSpell.tickEffect(player, level);
                    }
                });
            }
        });
    }
}

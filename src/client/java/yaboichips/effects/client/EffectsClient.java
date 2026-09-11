package yaboichips.effects.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

import yaboichips.effects.client.core.ModKeyBindings;
import yaboichips.effects.client.core.ModPayloadsClient;
import yaboichips.effects.client.screen.SpellTableScreen;
import yaboichips.effects.core.ModMenus;

public class EffectsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModKeyBindings.register();
        ModPayloadsClient.registerClientReceivers();
        MenuScreens.register(ModMenus.SPELL_TABLE, SpellTableScreen::new);
    }
}

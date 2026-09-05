package yaboichips.effects.client;

import net.fabricmc.api.ClientModInitializer;

import yaboichips.effects.client.core.ModKeyBindings;
import yaboichips.effects.client.core.ModPayloadsClient;

public class EffectsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModKeyBindings.register();
        ModPayloadsClient.registerClientReceivers();
    }
}

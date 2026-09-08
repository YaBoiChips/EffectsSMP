package yaboichips.effects.client.core;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import yaboichips.effects.client.screen.SpellTableScreen;
import yaboichips.effects.network.OpenSpellTablePayload;
import yaboichips.effects.network.SetDeafenedPayload;

public class ModPayloadsClient {

    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(OpenSpellTablePayload.TYPE, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new SpellTableScreen())));

        ClientPlayNetworking.registerGlobalReceiver(SetDeafenedPayload.TYPE, (payload, context) ->
                context.client().execute(() -> DeafenState.refresh(payload.durationTicks())));
    }
}

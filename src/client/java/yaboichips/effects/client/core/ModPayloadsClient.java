package yaboichips.effects.client.core;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import yaboichips.effects.network.SetDeafenedPayload;

public class ModPayloadsClient {

    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(SetDeafenedPayload.TYPE, (payload, context) ->
                context.client().execute(() -> DeafenState.refresh(payload.durationTicks())));
    }
}

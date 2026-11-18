package yaboichips.effects.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

import yaboichips.effects.client.core.ModKeyBindings;
import yaboichips.effects.client.core.ModPayloadsClient;
import yaboichips.effects.client.screen.SpellTableScreen;
import yaboichips.effects.core.ModBlocks;
import yaboichips.effects.core.ModMenus;

public class EffectsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModKeyBindings.register();
        ModPayloadsClient.registerClientReceivers();
        MenuScreens.register(ModMenus.SPELL_TABLE, SpellTableScreen::new);

        // The spell table texture has fully transparent regions (outer shell layers), so it must not use the
        // default opaque layer, which draws transparent texels as solid black.
        BlockRenderLayerMap.putBlock(ModBlocks.SPELL_TABLE, ChunkSectionLayer.CUTOUT);
    }
}

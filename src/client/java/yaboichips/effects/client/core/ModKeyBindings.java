package yaboichips.effects.client.core;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import yaboichips.effects.Effects;
import yaboichips.effects.network.ActivateAggressiveSpellPayload;

public class ModKeyBindings {

    private static final KeyMapping.Category SPELLS_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Effects.MOD_ID, "spells"));

    public static final KeyMapping CAST_LEVEL_1 = new KeyMapping("key.effects.cast_level_1", GLFW.GLFW_KEY_V, SPELLS_CATEGORY);
    public static final KeyMapping CAST_LEVEL_2 = new KeyMapping("key.effects.cast_level_2", GLFW.GLFW_KEY_B, SPELLS_CATEGORY);
    public static final KeyMapping CAST_LEVEL_3 = new KeyMapping("key.effects.cast_level_3", GLFW.GLFW_KEY_N, SPELLS_CATEGORY);

    public static void register() {
        KeyBindingHelper.registerKeyBinding(CAST_LEVEL_1);
        KeyBindingHelper.registerKeyBinding(CAST_LEVEL_2);
        KeyBindingHelper.registerKeyBinding(CAST_LEVEL_3);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (CAST_LEVEL_1.consumeClick()) {
                ClientPlayNetworking.send(new ActivateAggressiveSpellPayload(1));
            }
            while (CAST_LEVEL_2.consumeClick()) {
                ClientPlayNetworking.send(new ActivateAggressiveSpellPayload(2));
            }
            while (CAST_LEVEL_3.consumeClick()) {
                ClientPlayNetworking.send(new ActivateAggressiveSpellPayload(3));
            }
        });
    }
}

package yaboichips.effects.core;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ModCommands {

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("trust")
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> {
                                ServerPlayer truster = context.getSource().getPlayerOrException();
                                ServerPlayer target = EntityArgument.getPlayer(context, "player");

                                if (truster.getUUID().equals(target.getUUID())) {
                                    context.getSource().sendFailure(Component.translatable("commands.effects.trust.self"));
                                    return 0;
                                }

                                truster.modifyAttached(ModAttachments.TRUST_DATA,
                                        data -> data.withTrusted(target.getUUID()));

                                context.getSource().sendSuccess(() ->
                                        Component.translatable("commands.effects.trust.success", target.getName()), false);
                                return 1;
                            })));

            dispatcher.register(Commands.literal("untrust")
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> {
                                ServerPlayer truster = context.getSource().getPlayerOrException();
                                ServerPlayer target = EntityArgument.getPlayer(context, "player");

                                boolean wasTrusted = truster.getAttachedOrCreate(ModAttachments.TRUST_DATA)
                                        .isTrusted(target.getUUID());
                                if (!wasTrusted) {
                                    context.getSource().sendFailure(
                                            Component.translatable("commands.effects.untrust.not_trusted", target.getName()));
                                    return 0;
                                }

                                truster.modifyAttached(ModAttachments.TRUST_DATA,
                                        data -> data.withUntrusted(target.getUUID()));

                                context.getSource().sendSuccess(() ->
                                        Component.translatable("commands.effects.untrust.success", target.getName()), false);
                                return 1;
                            })));
        });
    }
}

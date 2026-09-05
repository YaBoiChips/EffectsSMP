package yaboichips.effects.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import yaboichips.effects.Effects;

public record ActivateAggressiveSpellPayload(int level) implements CustomPacketPayload {

    public static final Type<ActivateAggressiveSpellPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Effects.MOD_ID, "activate_aggressive_spell"));

    public static final StreamCodec<ByteBuf, ActivateAggressiveSpellPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ActivateAggressiveSpellPayload::level, ActivateAggressiveSpellPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

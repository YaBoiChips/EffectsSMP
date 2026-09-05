package yaboichips.effects.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import yaboichips.effects.Effects;

public record UnlockSpellPayload(Identifier spellId) implements CustomPacketPayload {

    public static final Type<UnlockSpellPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Effects.MOD_ID, "unlock_spell"));

    public static final StreamCodec<ByteBuf, UnlockSpellPayload> STREAM_CODEC =
            StreamCodec.composite(Identifier.STREAM_CODEC, UnlockSpellPayload::spellId, UnlockSpellPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

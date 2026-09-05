package yaboichips.effects.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import yaboichips.effects.Effects;

public record UpgradeSpellPayload(Identifier spellId) implements CustomPacketPayload {

    public static final Type<UpgradeSpellPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Effects.MOD_ID, "upgrade_spell"));

    public static final StreamCodec<ByteBuf, UpgradeSpellPayload> STREAM_CODEC =
            StreamCodec.composite(Identifier.STREAM_CODEC, UpgradeSpellPayload::spellId, UpgradeSpellPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

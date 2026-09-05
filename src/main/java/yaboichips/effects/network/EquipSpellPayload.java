package yaboichips.effects.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import yaboichips.effects.Effects;

public record EquipSpellPayload(Identifier spellId) implements CustomPacketPayload {

    public static final Type<EquipSpellPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Effects.MOD_ID, "equip_spell"));

    public static final StreamCodec<ByteBuf, EquipSpellPayload> STREAM_CODEC =
            StreamCodec.composite(Identifier.STREAM_CODEC, EquipSpellPayload::spellId, EquipSpellPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package yaboichips.effects.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import yaboichips.effects.Effects;

/**
 * S2C marker payload telling the client to open the Spell Table screen.
 * Carries no data; the client reads current progress from its own synced
 * {@link yaboichips.effects.core.ModAttachments#SPELL_DATA} copy.
 */
public record OpenSpellTablePayload() implements CustomPacketPayload {

    public static final Type<OpenSpellTablePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Effects.MOD_ID, "open_spell_table"));

    public static final StreamCodec<ByteBuf, OpenSpellTablePayload> STREAM_CODEC =
            StreamCodec.unit(new OpenSpellTablePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package yaboichips.effects.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import yaboichips.effects.Effects;

/**
 * S2C: refreshes the client's local "deafened" window for
 * {@code durationTicks} more ticks, used by Stealth's Smokescreen to
 * approximate "deafness" for affected players. Self-expiring client-side (no
 * explicit "un-deafen" packet needed) so the effect naturally decays a couple
 * of seconds after the last refresh, whether that's because the zone expired
 * or because the player walked out of it.
 */
public record SetDeafenedPayload(int durationTicks) implements CustomPacketPayload {

    public static final Type<SetDeafenedPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Effects.MOD_ID, "set_deafened"));

    public static final StreamCodec<ByteBuf, SetDeafenedPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, SetDeafenedPayload::durationTicks, SetDeafenedPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

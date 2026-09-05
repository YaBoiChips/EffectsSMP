package yaboichips.effects.core;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

import yaboichips.effects.Effects;
import yaboichips.effects.data.PlayerSpellData;
import yaboichips.effects.data.TrustData;

public class ModAttachments {

    public static final AttachmentType<PlayerSpellData> SPELL_DATA = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(Effects.MOD_ID, "spell_data"),
            builder -> builder.persistent(PlayerSpellData.CODEC)
                    .initializer(() -> PlayerSpellData.DEFAULT)
                    .syncWith(PlayerSpellData.STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));

    /**
     * Unlike SPELL_DATA, trust relationships are social rather than progress,
     * so this survives death via copyOnDeath() instead of resetting.
     */
    public static final AttachmentType<TrustData> TRUST_DATA = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(Effects.MOD_ID, "trust_data"),
            builder -> builder.persistent(TrustData.CODEC)
                    .initializer(() -> TrustData.DEFAULT)
                    .copyOnDeath());

    public static void registerAttachments() {
        Effects.LOGGER.info("Registering attachments for {}", Effects.MOD_ID);
    }
}

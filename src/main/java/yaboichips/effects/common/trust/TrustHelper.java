package yaboichips.effects.common.trust;

import net.minecraft.server.level.ServerPlayer;

import yaboichips.effects.core.ModAttachments;
import yaboichips.effects.data.TrustData;

public class TrustHelper {

    private TrustHelper() {
    }

    public static boolean isAlly(ServerPlayer caster, ServerPlayer target) {
        if (caster.getUUID().equals(target.getUUID())) {
            return true;
        }
        TrustData data = caster.getAttachedOrCreate(ModAttachments.TRUST_DATA);
        return data.isTrusted(target.getUUID());
    }
}

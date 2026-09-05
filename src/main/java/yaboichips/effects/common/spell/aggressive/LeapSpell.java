package yaboichips.effects.common.spell.aggressive;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import yaboichips.effects.common.spell.AggressiveSpell;

public class LeapSpell extends AggressiveSpell {

    private static final double BASE_POWER = 1.5;

    public LeapSpell(Identifier id, Identifier icon) {
        super(id, icon, 3);
    }

    @Override
    public void activate(ServerPlayer player, int level) {
        double power = BASE_POWER * (1 << (level - 1));
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(look.scale(power));
        player.hurtMarked = true;
    }
}

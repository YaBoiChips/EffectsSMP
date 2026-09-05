package yaboichips.effects.common.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import yaboichips.effects.core.ModAttachments;

public class AmuletOfWillpowerItem extends Item {

    public AmuletOfWillpowerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            player.modifyAttached(ModAttachments.SPELL_DATA, data -> data.withSpellPoints(data.spellPoints() + 1));
        }
        return InteractionResult.SUCCESS;
    }
}

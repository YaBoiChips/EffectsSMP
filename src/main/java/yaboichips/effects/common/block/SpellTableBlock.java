package yaboichips.effects.common.block;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import yaboichips.effects.common.menu.SpellTableMenu;

public class SpellTableBlock extends Block {

    public SpellTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            player.openMenu(new ExtendedScreenHandlerFactory<Unit>() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("gui.effects.spell_table_title");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                    return new SpellTableMenu(containerId, playerInventory);
                }

                @Override
                public Unit getScreenOpeningData(ServerPlayer player) {
                    return Unit.INSTANCE;
                }
            });
        }
        return InteractionResult.SUCCESS;
    }
}

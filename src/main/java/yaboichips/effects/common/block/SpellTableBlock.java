package yaboichips.effects.common.block;

import java.util.Map;

import com.mojang.serialization.MapCodec;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import yaboichips.effects.common.menu.SpellTableMenu;

public class SpellTableBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<SpellTableBlock> CODEC = simpleCodec(SpellTableBlock::new);


    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            // Cabinet base (outer shell of the model)
            Block.box(-0.32, 0.0, -1.36, 17.36, 15.2, 16.32),
            // Back panel
            Block.box(0.0, 15.0, 13.0, 17.0, 20.0, 16.0),
            // Stand and tilted book (the pillar sits inside the book's bounds)
            Block.box(4.0, 15.0, 1.0, 13.0, 16.0, 10.0),
            Block.box(2.95, 16.0, 0.76, 14.06, 21.14, 10.28),
            // Rotated decoration on the back panel, left half
            Block.box(-0.28, 19.25, 11.25, 7.07, 25.56, 18.76),
            // Rotated decoration on the back panel, right half
            Block.box(8.91, 20.0, 12.59, 17.15, 27.0, 16.83)
    );

    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(NORTH_SHAPE);

    public SpellTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Face the player, i.e. the opposite of the direction they are looking when placing.
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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

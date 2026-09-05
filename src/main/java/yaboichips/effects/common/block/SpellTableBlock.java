package yaboichips.effects.common.block;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import yaboichips.effects.network.OpenSpellTablePayload;

public class SpellTableBlock extends Block {

    public SpellTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, new OpenSpellTablePayload());
        }
        return InteractionResult.SUCCESS;
    }
}

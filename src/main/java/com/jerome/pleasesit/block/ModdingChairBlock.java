package com.jerome.pleasesit.block;

import com.jerome.pleasesit.block.entity.ModdingChairBlockEntity;
import com.jerome.pleasesit.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import javax.annotation.Nullable;

public class ModdingChairBlock extends BaseEntityBlock {
    public static final MapCodec<ModdingChairBlock> CODEC = simpleCodec(ModdingChairBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Shapes.join(
            Shapes.or(
                    Block.box(2.0D, 0.0D, 2.0D, 14.0D, 2.0D, 14.0D),
                    Block.box(3.0D, 2.0D, 3.0D, 13.0D, 9.0D, 13.0D),
                    Block.box(3.0D, 9.0D, 11.0D, 13.0D, 16.0D, 13.0D)
            ),
            Shapes.or(
                    Block.box(3.0D, 0.0D, 3.0D, 5.0D, 9.0D, 5.0D),
                    Block.box(11.0D, 0.0D, 3.0D, 13.0D, 9.0D, 5.0D),
                    Block.box(3.0D, 0.0D, 11.0D, 5.0D, 9.0D, 13.0D),
                    Block.box(11.0D, 0.0D, 11.0D, 13.0D, 9.0D, 13.0D)
            ),
            BooleanOp.OR
    );

    public ModdingChairBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(POWERED, false)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()))
                .setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED, FACING);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);

        boolean isPowered = level.hasNeighborSignal(pos);
        if (state.getValue(POWERED) == isPowered) {
            return;
        }

        level.setBlock(pos, state.setValue(POWERED, isPowered), Block.UPDATE_ALL);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ModdingChairBlockEntity chair) {
            if (isPowered) {
                chair.activate((ServerLevel) level);
            } else {
                chair.releaseOccupant();
            }
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ModdingChairBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }

        return createTickerHelper(type, ModBlockEntities.MODDING_CHAIR.get(), ModdingChairBlockEntity::tick);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof ModdingChairBlockEntity chair) {
            chair.releaseOccupant();
        }

        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
}

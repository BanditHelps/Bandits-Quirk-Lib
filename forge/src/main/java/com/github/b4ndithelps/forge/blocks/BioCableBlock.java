package com.github.b4ndithelps.forge.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Simple cable block that visually connects to adjacent cables and indicates whether
 * the adjacent cable continues in the same direction (for model variants).
 */
public class BioCableBlock extends Block {
    public static final BooleanProperty CABLE_NORTH = BooleanProperty.create("cable_north");
    public static final BooleanProperty CABLE_SOUTH = BooleanProperty.create("cable_south");
    public static final BooleanProperty CABLE_EAST = BooleanProperty.create("cable_east");
    public static final BooleanProperty CABLE_WEST = BooleanProperty.create("cable_west");
    public static final BooleanProperty CABLE_UP = BooleanProperty.create("cable_up");
    public static final BooleanProperty CABLE_DOWN = BooleanProperty.create("cable_down");

    public static final BooleanProperty CONT_NORTH = BooleanProperty.create("cont_north");
    public static final BooleanProperty CONT_SOUTH = BooleanProperty.create("cont_south");
    public static final BooleanProperty CONT_EAST = BooleanProperty.create("cont_east");
    public static final BooleanProperty CONT_WEST = BooleanProperty.create("cont_west");
    public static final BooleanProperty CONT_UP = BooleanProperty.create("cont_up");
    public static final BooleanProperty CONT_DOWN = BooleanProperty.create("cont_down");

    private static final VoxelShape CORE = Block.box(6.0, 6.0, 6.0, 10.0, 10.0, 10.0);
    private static final VoxelShape ARM_N = Block.box(7.0, 7.0, 0.0, 9.0, 9.0, 6.0);
    private static final VoxelShape ARM_S = Block.box(7.0, 7.0, 10.0, 9.0, 9.0, 16.0);
    private static final VoxelShape ARM_W = Block.box(0.0, 7.0, 7.0, 6.0, 9.0, 9.0);
    private static final VoxelShape ARM_E = Block.box(10.0, 7.0, 7.0, 16.0, 9.0, 9.0);
    private static final VoxelShape ARM_U = Block.box(7.0, 10.0, 7.0, 9.0, 16.0, 9.0);
    private static final VoxelShape ARM_D = Block.box(7.0, 0.0, 7.0, 9.0, 6.0, 9.0);

    public BioCableBlock(Properties properties) {
        super(properties.noOcclusion());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(CABLE_NORTH, false)
                .setValue(CABLE_SOUTH, false)
                .setValue(CABLE_EAST, false)
                .setValue(CABLE_WEST, false)
                .setValue(CABLE_UP, false)
                .setValue(CABLE_DOWN, false)
                .setValue(CONT_NORTH, false)
                .setValue(CONT_SOUTH, false)
                .setValue(CONT_EAST, false)
                .setValue(CONT_WEST, false)
                .setValue(CONT_UP, false)
                .setValue(CONT_DOWN, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CABLE_NORTH, CABLE_SOUTH, CABLE_EAST, CABLE_WEST, CABLE_UP, CABLE_DOWN,
                CONT_NORTH, CONT_SOUTH, CONT_EAST, CONT_WEST, CONT_UP, CONT_DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return updateConnections(level, pos, this.defaultBlockState());
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        BlockState updated = updateConnections(level, pos, state);
        if (updated != state) {
            level.setBlock(pos, updated, 3);
        }
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
    }

    private BlockState updateConnections(Level level, BlockPos pos, BlockState state) {
        boolean n = isCable(level.getBlockState(pos.north()));
        boolean s = isCable(level.getBlockState(pos.south()));
        boolean e = isCable(level.getBlockState(pos.east()));
        boolean w = isCable(level.getBlockState(pos.west()));
        boolean u = isCable(level.getBlockState(pos.above()));
        boolean d = isCable(level.getBlockState(pos.below()));

        state = state
                .setValue(CABLE_NORTH, n)
                .setValue(CABLE_SOUTH, s)
                .setValue(CABLE_EAST, e)
                .setValue(CABLE_WEST, w)
                .setValue(CABLE_UP, u)
                .setValue(CABLE_DOWN, d);

        // Continuation means cable two blocks away in same direction
        boolean contN = n && isCable(level.getBlockState(pos.north(2)));
        boolean contS = s && isCable(level.getBlockState(pos.south(2)));
        boolean contE = e && isCable(level.getBlockState(pos.east(2)));
        boolean contW = w && isCable(level.getBlockState(pos.west(2)));
        boolean contU = u && isCable(level.getBlockState(pos.above(2)));
        boolean contD = d && isCable(level.getBlockState(pos.below(2)));

        state = state
                .setValue(CONT_NORTH, contN)
                .setValue(CONT_SOUTH, contS)
                .setValue(CONT_EAST, contE)
                .setValue(CONT_WEST, contW)
                .setValue(CONT_UP, contU)
                .setValue(CONT_DOWN, contD);

        return state;
    }

    private boolean isCable(BlockState state) {
        return state != null && state.getBlock() instanceof BioCableBlock;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        if (state.getValue(CABLE_NORTH)) shape = Shapes.or(shape, ARM_N);
        if (state.getValue(CABLE_SOUTH)) shape = Shapes.or(shape, ARM_S);
        if (state.getValue(CABLE_WEST)) shape = Shapes.or(shape, ARM_W);
        if (state.getValue(CABLE_EAST)) shape = Shapes.or(shape, ARM_E);
        if (state.getValue(CABLE_UP)) shape = Shapes.or(shape, ARM_U);
        if (state.getValue(CABLE_DOWN)) shape = Shapes.or(shape, ARM_D);
        return shape;
    }
}



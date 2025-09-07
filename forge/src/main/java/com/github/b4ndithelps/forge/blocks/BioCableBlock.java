package com.github.b4ndithelps.forge.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Simple cable block that visually connects to adjacent cables and indicates whether
 * the adjacent cable continues in the same direction (for model variants).
 */
public class BioCableBlock extends Block {
    public enum Connection implements StringRepresentable { none, cable, cont, cap; public String getSerializedName() { return name(); } }

    public static final EnumProperty<Connection> NORTH = EnumProperty.create("north", Connection.class);
    public static final EnumProperty<Connection> SOUTH = EnumProperty.create("south", Connection.class);
    public static final EnumProperty<Connection> EAST = EnumProperty.create("east", Connection.class);
    public static final EnumProperty<Connection> WEST = EnumProperty.create("west", Connection.class);
    public static final EnumProperty<Connection> UP = EnumProperty.create("up", Connection.class);
    public static final EnumProperty<Connection> DOWN = EnumProperty.create("down", Connection.class);

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
                .setValue(NORTH, Connection.none)
                .setValue(SOUTH, Connection.none)
                .setValue(EAST, Connection.none)
                .setValue(WEST, Connection.none)
                .setValue(UP, Connection.none)
                .setValue(DOWN, Connection.none)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
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
        state = state
                .setValue(NORTH, computeConnection(level, pos, Direction.NORTH))
                .setValue(SOUTH, computeConnection(level, pos, Direction.SOUTH))
                .setValue(EAST, computeConnection(level, pos, Direction.EAST))
                .setValue(WEST, computeConnection(level, pos, Direction.WEST))
                .setValue(UP, computeConnection(level, pos, Direction.UP))
                .setValue(DOWN, computeConnection(level, pos, Direction.DOWN));

        return state;
    }

    private boolean isCable(BlockState state) {
        return state != null && state.getBlock() instanceof BioCableBlock;
    }

    private boolean isDevice(BlockState state) {
        if (state == null) return false;
        Block b = state.getBlock();
        return b instanceof BioTerminalBlock
                || b instanceof GeneSequencerBlock
                || b instanceof GeneSlicerBlock
                || b instanceof SampleRefrigeratorBlock
                || b instanceof BioPrinterBlock
                || b instanceof GeneCombinerBlock
                || b instanceof ResearchTableBlock;
    }

    private Connection computeConnection(Level level, BlockPos pos, Direction dir) {
        BlockPos neighborPos = pos.relative(dir);
        BlockState neighbor = level.getBlockState(neighborPos);
        if (isCable(neighbor)) {
            // continuation if two away is also cable
            BlockPos two = neighborPos.relative(dir);
            return isCable(level.getBlockState(two)) ? Connection.cont : Connection.cable;
        } else if (isDevice(neighbor)) {
            return Connection.cap;
        }
        return Connection.none;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        if (state.getValue(NORTH) != Connection.none) shape = Shapes.or(shape, ARM_N);
        if (state.getValue(SOUTH) != Connection.none) shape = Shapes.or(shape, ARM_S);
        if (state.getValue(WEST) != Connection.none) shape = Shapes.or(shape, ARM_W);
        if (state.getValue(EAST) != Connection.none) shape = Shapes.or(shape, ARM_E);
        if (state.getValue(UP) != Connection.none) shape = Shapes.or(shape, ARM_U);
        if (state.getValue(DOWN) != Connection.none) shape = Shapes.or(shape, ARM_D);
        return shape;
    }
}



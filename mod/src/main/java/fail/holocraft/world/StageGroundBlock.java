package fail.holocraft.world;

import fail.holocraft.sheet.StagesRow;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;

/** A stage's floor. `cell` picks which piece of the stage's floor texture this block shows; it follows the block's position. */
public class StageGroundBlock extends Block {
    public static final int MAX_CELLS = 40;
    public static final IntProperty CELL = IntProperty.of("cell", 0, MAX_CELLS * MAX_CELLS - 1);
    private final StagesRow stage;

    public StageGroundBlock(StagesRow stage, Settings settings) {
        super(settings);
        this.stage = stage;
        setDefaultState(getDefaultState().with(CELL, 0));
    }

    public StagesRow stage() { return stage; }

    public BlockState at(int x, int z) {
        int n = Math.min(MAX_CELLS, stage.groundCells());
        return getDefaultState().with(CELL, Math.floorMod(x, n) + n * Math.floorMod(z, n));
    }

    public BlockState at(BlockPos pos) { return at(pos.getX(), pos.getZ()); }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) { builder.add(CELL); }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) { return at(ctx.getBlockPos()); }
}

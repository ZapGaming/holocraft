package fail.holocraft.world;

import com.mojang.serialization.MapCodec;
import fail.holocraft.sheet.PropsRow;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

/** One kind of stage prop (props.json row). Invisible as a block; PropRenderer draws its HoloCure sprite. */
public class PropBlock extends BlockWithEntity {
    private static final VoxelShape OUTLINE = createCuboidShape(3, 0, 3, 13, 14, 13);
    private final PropsRow row;

    public PropBlock(PropsRow row, Settings settings) {
        super(settings);
        this.row = row;
    }

    public PropsRow row() { return row; }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() { return createCodec(s -> new PropBlock(row, s)); }

    @Override
    protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.INVISIBLE; }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) { return OUTLINE; }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return row.solid() ? VoxelShapes.fullCube() : VoxelShapes.empty();
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return new PropBlockEntity(pos, state); }
}

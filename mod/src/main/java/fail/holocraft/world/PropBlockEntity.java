package fail.holocraft.world;

import fail.holocraft.registry.HcBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

/** Exists so the client has something to draw the prop's sprite for; it holds no data. */
public class PropBlockEntity extends BlockEntity {
    public PropBlockEntity(BlockPos pos, BlockState state) { super(HcBlocks.PROP_ENTITY, pos, state); }
}

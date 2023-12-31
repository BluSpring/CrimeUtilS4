package xyz.bluspring.crimeutils4;

import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

public class Workarounds {
    public static final DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> NO_COMBINE = DoubleBlockCombiner.Combiner::acceptNone;
}

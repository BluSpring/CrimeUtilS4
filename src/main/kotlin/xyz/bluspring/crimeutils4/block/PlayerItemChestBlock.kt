package xyz.bluspring.crimeutils4.block

import net.minecraft.core.BlockPos
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.ChestType
import net.minecraft.world.level.material.Fluids
import xyz.bluspring.crimeutils4.CrimeUtilS4
import xyz.bluspring.crimeutils4.block.entity.PlayerItemChestBlockEntity
import java.util.function.Supplier

class PlayerItemChestBlock : ChestBlock(Properties.ofFullCopy(Blocks.CHEST), Supplier { CrimeUtilS4.PLAYER_ITEM_CHEST_BLOCK_ENTITY }) {
    override fun newBlockEntity(blockPos: BlockPos, blockState: BlockState): BlockEntity {
        return PlayerItemChestBlockEntity(blockPos, blockState)
    }

    override fun getStateForPlacement(blockPlaceContext: BlockPlaceContext): BlockState? {
        val fluidState = blockPlaceContext.level.getFluidState(blockPlaceContext.clickedPos)
        return defaultBlockState().setValue(FACING, blockPlaceContext.horizontalDirection.opposite).setValue(WATERLOGGED, fluidState.type == Fluids.WATER)
            .setValue(TYPE, ChestType.SINGLE)
    }
}
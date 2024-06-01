package xyz.bluspring.crimeutils4.block.entity

import net.minecraft.core.BlockPos
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.block.state.BlockState
import xyz.bluspring.crimeutils4.CrimeUtilS4

class PlayerItemChestBlockEntity(pos: BlockPos, state: BlockState) : ChestBlockEntity(CrimeUtilS4.PLAYER_ITEM_CHEST_BLOCK_ENTITY, pos, state) {
    init {
        this.items = NonNullList.withSize(45, ItemStack.EMPTY);
    }

    override fun getContainerSize(): Int {
        return 45
    }

    override fun createMenu(i: Int, inventory: Inventory): AbstractContainerMenu {
        return ChestMenu(MenuType.GENERIC_9x5, i, inventory, this, 5)
    }

    var isUnbreakable = false

    override fun load(tag: CompoundTag) {
        super.load(tag)

        isUnbreakable = if (tag.contains("Unbreakable"))
            tag.getBoolean("Unbreakable")
        else false
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)

        tag.putBoolean("Unbreakable", isUnbreakable)
    }
}
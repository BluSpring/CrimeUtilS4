package xyz.bluspring.crimeutils4.block

import net.minecraft.sounds.SoundEvent
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ButtonBlock
import net.minecraft.world.level.block.state.properties.BlockSetType
import xyz.bluspring.crimeutils4.CrimeUtilS4

class BigRedButtonBlock : ButtonBlock(
    BlockSetType.STONE,
    35, Properties.ofFullCopy(Blocks.STONE_BUTTON)
) {
    override fun getSound(isOn: Boolean): SoundEvent {
        return if (isOn)
            CrimeUtilS4.BIG_RED_BUTTON_PRESS
        else
            CrimeUtilS4.ONE_PIECE
    }
}
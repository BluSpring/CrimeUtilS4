package xyz.bluspring.crimeutils4.client.renderer.blockentity

import net.minecraft.client.renderer.Sheets
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.blockentity.ChestRenderer
import net.minecraft.client.resources.model.Material
import net.minecraft.resources.ResourceLocation
import xyz.bluspring.crimeutils4.CrimeUtilS4
import xyz.bluspring.crimeutils4.block.entity.PlayerItemChestBlockEntity

class PlayerItemChestBlockEntityRenderer(ctx: BlockEntityRendererProvider.Context) : ChestRenderer<PlayerItemChestBlockEntity>(ctx) {
    companion object {
        @JvmField
        val PLAYER_ITEM_CHEST_LOCATION = Material(Sheets.CHEST_SHEET, ResourceLocation(CrimeUtilS4.MOD_ID, "entity/chest/player_item_chest"));
    }
}
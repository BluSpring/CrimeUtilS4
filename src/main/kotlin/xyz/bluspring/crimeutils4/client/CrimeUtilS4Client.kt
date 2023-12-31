package xyz.bluspring.crimeutils4.client

import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers
import xyz.bluspring.crimeutils4.CrimeUtilS4
import xyz.bluspring.crimeutils4.client.renderer.blockentity.PlayerItemChestBlockEntityRenderer

class CrimeUtilS4Client : ClientModInitializer {
    override fun onInitializeClient() {
        BlockEntityRenderers.register(CrimeUtilS4.PLAYER_ITEM_CHEST_BLOCK_ENTITY, ::PlayerItemChestBlockEntityRenderer)
    }
}
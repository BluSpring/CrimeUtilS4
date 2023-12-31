package xyz.bluspring.crimeutils4

import dev.architectury.event.EventResult
import dev.architectury.event.events.common.InteractionEvent
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.ChatFormatting
import net.minecraft.commands.Commands
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EnderChestBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.bluspring.crimeutils4.block.PlayerItemChestBlock
import xyz.bluspring.crimeutils4.block.entity.PlayerItemChestBlockEntity

class CrimeUtilS4 : ModInitializer {
    override fun onInitialize() {
        ServerPlayConnectionEvents.DISCONNECT.register { handler, server ->
            if (!handler.player.inventory.isEmpty)
                placeInventoryChest(handler.player.inventory, handler.player, searchForFreePos(handler.player.serverLevel(), handler.player.blockPosition()))
        }

        CommandRegistrationCallback.EVENT.register { dispatcher, registry, _ ->
            if (DEBUG) {
                dispatcher.register(
                    Commands.literal("testchest")
                        .executes { ctx ->
                            val player = ctx.source.playerOrException

                            placeInventoryChest(player.inventory, player, searchForFreePos(player.serverLevel(), player.blockPosition()))
                            1
                        }
                )
            }
        }

        InteractionEvent.RIGHT_CLICK_BLOCK.register { player, hand, pos, direction ->
            val state = player.level().getBlockState(pos)

            if (state.block is EnderChestBlock) {
                player.displayClientMessage(Component.literal("You're not allowed to use that block!").withStyle(ChatFormatting.RED), true)
                return@register EventResult.interruptFalse()
            }

            EventResult.pass()
        }
    }

    private fun placeInventoryChest(inventory: Inventory, player: Player, pos: BlockPos) {
        val level = player.level()

        val state = PLAYER_ITEM_CHEST_BLOCK.defaultBlockState()
        level.setBlockAndUpdate(pos, state)
        level.setBlockEntity(PLAYER_ITEM_CHEST_BLOCK_ENTITY.create(pos, state)!!.apply {
            for ((index, itemStack) in inventory.items.withIndex()) {
                this.setItem(index, itemStack)
            }

            this.setItem(36, inventory.offhand.firstOrNull() ?: ItemStack.EMPTY)
            for ((index, itemStack) in inventory.armor.withIndex()) {
                this.setItem(37 + index, itemStack)
            }

            this.customName = Component.empty()
                .append(player.name)
                .append("'s Chest")
        })

        inventory.clearContent()

        LOGGER.info("Placed ${player.name.string}'s items chest at ${pos.toShortString()}")
    }

    private fun searchForFreePos(level: ServerLevel, blockPos: BlockPos): BlockPos {
        val pos = BlockPos.MutableBlockPos()
        pos.set(blockPos)

        while (!level.getBlockState(pos).isAir) {
            if (pos.y >= level.maxBuildHeight) {
                pos.y = level.maxBuildHeight
                break
            } else if (pos.y <= level.minBuildHeight) {
                pos.y = level.minBuildHeight
                break
            }

            pos.y += 1
        }

        return pos
    }

    companion object {
        const val MOD_ID = "crimeutils"
        const val DEBUG = true

        @JvmField val LOGGER: Logger = LoggerFactory.getLogger(CrimeUtilS4::class.java)

        @JvmField val PLAYER_ITEM_CHEST_BLOCK = registerBlock("player_item_chest", PlayerItemChestBlock())
        @JvmField val PLAYER_ITEM_CHEST_BLOCK_ENTITY = registerBlockEntity("player_item_chest", FabricBlockEntityTypeBuilder.create(::PlayerItemChestBlockEntity, PLAYER_ITEM_CHEST_BLOCK).build())
        @JvmField val PLAYER_ITEM_CHEST_ITEM = registerItem("player_item_chest", BlockItem(PLAYER_ITEM_CHEST_BLOCK, Item.Properties()))

        @JvmField val DOUBLOON_ITEM = registerItem("doubloon", Item(Item.Properties().rarity(Rarity.RARE)))

        private fun <T : Block> registerBlock(id: String, value: T): T {
            return register<Block, T>(BuiltInRegistries.BLOCK, id, value)
        }

        private fun <T : Item> registerItem(id: String, value: T): T {
            return register<Item, T>(BuiltInRegistries.ITEM, id, value)
        }

        private fun <T : BlockEntityType<U>, U> registerBlockEntity(id: String, value: T): T {
            return register<BlockEntityType<*>, T>(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, value)
        }

        private fun <T, U : T> register(registry: Registry<T>, id: String, value: U): U {
            return Registry.register(registry, ResourceLocation(MOD_ID, id), value)
        }
    }
}
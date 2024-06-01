package xyz.bluspring.crimeutils4

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.brigadier.arguments.IntegerArgumentType
import dev.architectury.event.EventResult
import dev.architectury.event.events.common.EntityEvent
import dev.architectury.event.events.common.InteractionEvent
import dev.emi.trinkets.TrinketPlayerScreenHandler
import dev.emi.trinkets.api.TrinketsApi
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.ChatFormatting
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EnderChestBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.bluspring.crimeutils4.block.BigRedButtonBlock
import xyz.bluspring.crimeutils4.block.PlayerItemChestBlock
import xyz.bluspring.crimeutils4.block.entity.PlayerItemChestBlockEntity
import xyz.bluspring.crimeutils4.mixin.PlayerListAccessor
import java.io.File

class CrimeUtilS4 : ModInitializer {
    private val logger = LoggerFactory.getLogger("CrimeUtils")
    val gson = GsonBuilder().setPrettyPrinting().create()

    val protFile = File(FabricLoader.getInstance().configDir.toFile(), "crimeutils_prot.json")
    val protectedAreas = mutableListOf<AABB>()
    var isProtectionEnabled = false

    override fun onInitialize() {
        instance = this

        loadProtFile()

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

            dispatcher.register(
                Commands.literal("areaprot")
                    .requires { it.hasPermission(3) }
                    .then(
                        Commands.literal("toggle")
                            .executes {
                                isProtectionEnabled = !isProtectionEnabled
                                it.source.sendSystemMessage(Component.literal("Toggled block protection area status to ${isProtectionEnabled}."))

                                saveProtFile()

                                1
                            }
                    )
                    .then(
                        Commands.literal("add")
                            .then(
                                Commands.argument("pos1", BlockPosArgument.blockPos())
                                    .then(
                                        Commands.argument("pos2", BlockPosArgument.blockPos())
                                            .executes {
                                                val pos1 = BlockPosArgument.getLoadedBlockPos(it, "pos1")
                                                val pos2 = BlockPosArgument.getLoadedBlockPos(it, "pos2")

                                                val area = AABB(pos1, pos2).inflate(0.5)

                                                this.protectedAreas.add(area)
                                                it.source.sendSystemMessage(Component.literal("Added protected area from [${area.minX}, ${area.minY}, ${area.minZ}] to [${area.maxX}, ${area.maxY}, ${area.maxZ}] under index ${this.protectedAreas.size - 1}."))

                                                saveProtFile()

                                                1
                                            }
                                    )
                            )
                    )
                    .then(
                        Commands.literal("remove")
                            .then(
                                Commands.argument("index", IntegerArgumentType.integer(0))
                                    .executes {
                                        val index = IntegerArgumentType.getInteger(it, "index")

                                        if (index >= this.protectedAreas.size) {
                                            it.source.sendFailure(Component.literal("Index is out of range of a max ${this.protectedAreas.size - 1}!"))
                                            return@executes 0
                                        }

                                        val removed = this.protectedAreas.removeAt(index)
                                        it.source.sendSystemMessage(Component.literal("Removed protected area from [${removed.minX}, ${removed.minY}, ${removed.minZ}] to [${removed.maxX}, ${removed.maxY}, ${removed.maxZ}]."))

                                        1
                                    }
                            )
                    )
            )
        }

        InteractionEvent.RIGHT_CLICK_BLOCK.register { player, hand, pos, direction ->
            val state = player.level().getBlockState(pos)

            if (state.block is EnderChestBlock) {
                player.displayClientMessage(Component.literal("You're not allowed to use that block!").withStyle(ChatFormatting.RED), true)
                return@register EventResult.interruptFalse()
            }

            EventResult.pass()
        }

        EntityEvent.LIVING_DEATH.register { entity, source ->
            if (entity is ServerPlayer) {
                placeInventoryChest(entity.inventory, entity, entity.blockPosition())
            }

            EventResult.pass()
        }

        PlayerBlockBreakEvents.BEFORE.register { level, player, pos, state, blockEntity ->
            if (isProtectionEnabled) {
                for (area in this.protectedAreas) {
                    if (area.contains(Vec3.atCenterOf(pos))) {
                        player.sendSystemMessage(Component.literal("That block is protected!")
                            .withStyle(ChatFormatting.RED))
                        return@register false
                    }
                }
            }

            if (blockEntity != null && blockEntity is PlayerItemChestBlockEntity && blockEntity.isUnbreakable) {
                player.sendSystemMessage(Component.literal("That block is protected!")
                    .withStyle(ChatFormatting.RED))
                return@register false
            }

            true
        }
    }

    fun placeInventoryChest(inventory: Inventory, player: Player, pos: BlockPos) {
        val level = player.level()

        val state = PLAYER_ITEM_CHEST_BLOCK.defaultBlockState()
        level.setBlockAndUpdate(pos, state)
        level.setBlockEntity(PLAYER_ITEM_CHEST_BLOCK_ENTITY.create(pos, state)!!.apply {
            for ((index, itemStack) in inventory.items.withIndex()) {
                setNonVanishingItem(index, itemStack)
            }

            setNonVanishingItem(36, inventory.offhand.firstOrNull() ?: ItemStack.EMPTY)
            for ((index, itemStack) in inventory.armor.withIndex()) {
                setNonVanishingItem(37 + index, itemStack)
            }

            if (FabricLoader.getInstance().isModLoaded("trinkets")) {
                addTrinketsItems(40, player)
            }

            this.customName = Component.empty()
                .append(player.name)
                .append("'s Chest")
        })

        inventory.clearContent()
        ((player as ServerPlayer).server.playerList as PlayerListAccessor).callSave(player)

        LOGGER.info("Placed ${player.name.string}'s items chest at ${pos.toShortString()}")
    }

    private fun PlayerItemChestBlockEntity.addTrinketsItems(startIndex: Int, player: Player) {
        TrinketsApi.getTrinketComponent(player).ifPresent {
            var current = 0
            it.forEach { _, stack ->
                setNonVanishingItem(startIndex + current, stack.copy())
                current += 1
            }

            it.inventory.clear()
            it.groups.clear()
            it.trackingUpdates.clear()
            it.allEquipped.clear()

            it.update()
            (player.inventoryMenu as TrinketPlayerScreenHandler).`trinkets$updateTrinketSlots`(true)
        }
    }

    private fun PlayerItemChestBlockEntity.setNonVanishingItem(index: Int, stack: ItemStack) {
        if (EnchantmentHelper.hasVanishingCurse(stack))
            return

        this.setItem(index, stack)
    }

    private fun searchForFreePos(level: ServerLevel, blockPos: BlockPos): BlockPos {
        val pos = BlockPos.MutableBlockPos()
        pos.set(blockPos)

        while (level.getBlockState(pos).liquid()) {
            if (pos.y >= level.maxBuildHeight) {
                pos.y = level.maxBuildHeight
                break
            } else if (pos.y <= level.minBuildHeight) {
                pos.y = level.minBuildHeight
                break
            }

            pos.y += 1
        }

        if (!level.getBlockState(pos).isAir) {
            while (level.getBlockState(pos).hasBlockEntity()) {
                pos.y += 1
                if (pos.y >= level.maxBuildHeight) {
                    pos.y = level.maxBuildHeight
                    break
                } else if (pos.y <= level.minBuildHeight) {
                    pos.y = level.minBuildHeight
                    break
                }
            }

            level.destroyBlock(pos, true)
        }

        return pos
    }

    private fun saveProtFile() {
        if (!protFile.exists())
            protFile.createNewFile()

        val json = JsonObject()

        json.addProperty("enabled", this.isProtectionEnabled)

        val arr = JsonArray()
        for (area in this.protectedAreas) {
            val posList = JsonArray()
            posList.add(JsonObject().apply {
                this.addProperty("x", area.minX)
                this.addProperty("y", area.minY)
                this.addProperty("z", area.minZ)
            })
            posList.add(JsonObject().apply {
                this.addProperty("x", area.maxX)
                this.addProperty("y", area.maxY)
                this.addProperty("z", area.maxZ)
            })

            arr.add(posList)
        }

        json.add("areas", arr)

        protFile.writeText(gson.toJson(json))
    }

    private fun loadProtFile() {
        if (!protFile.exists())
            return

        try {
            val json = JsonParser.parseString(protFile.readText()).asJsonObject

            this.isProtectionEnabled = json.get("enabled").asBoolean

            json.getAsJsonArray("areas").forEach {
                val area = it.asJsonArray

                val pos1 = area.get(0).asJsonObject
                val pos2 = area.get(1).asJsonObject

                val boundingBox = AABB(pos1.get("x").asDouble, pos1.get("y").asDouble, pos1.get("z").asDouble, pos2.get("x").asDouble, pos2.get("y").asDouble, pos2.get("z").asDouble)

                this.protectedAreas.add(boundingBox)
            }
        } catch (e: Exception) {
            logger.error("Failed to load protections file!")
            e.printStackTrace()
        }
    }

    companion object {
        const val MOD_ID = "crimecraft"
        val DEBUG = FabricLoader.getInstance().isDevelopmentEnvironment
        lateinit var instance: CrimeUtilS4

        @JvmField val LOGGER: Logger = LoggerFactory.getLogger(CrimeUtilS4::class.java)

        @JvmField val PLAYER_ITEM_CHEST_BLOCK = registerBlock("player_item_chest", PlayerItemChestBlock())
        @JvmField val PLAYER_ITEM_CHEST_BLOCK_ENTITY = registerBlockEntity("player_item_chest", FabricBlockEntityTypeBuilder.create(::PlayerItemChestBlockEntity, PLAYER_ITEM_CHEST_BLOCK).build())
        @JvmField val PLAYER_ITEM_CHEST_ITEM = registerItem("player_item_chest", BlockItem(PLAYER_ITEM_CHEST_BLOCK, Item.Properties()))

        @JvmField val DOUBLOON_ITEM = registerItem("doubloon", Item(Item.Properties().rarity(Rarity.EPIC).food(
            FoodProperties.Builder()
                .fast()
                .effect(MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 1), 0.3f)
                .effect(MobEffectInstance(MobEffects.INVISIBILITY, 400, 1), 0.3f)
                .alwaysEat()
                .nutrition(2)
                .saturationMod(0.1f)
                .build()
        )))

        @JvmField val BIG_RED_BUTTON_BLOCK = registerBlock("big_red_button", BigRedButtonBlock())
        @JvmField val BIG_RED_BUTTON_ITEM = registerItem("big_red_button", BlockItem(
            BIG_RED_BUTTON_BLOCK, Item.Properties().stacksTo(64)
        ))

        @JvmField val ONE_PIECE = SoundEvent.createVariableRangeEvent(ResourceLocation(MOD_ID, "crimecraft.one_piece")).apply {
            Registry.register(BuiltInRegistries.SOUND_EVENT, this.location, this)
        }
        @JvmField val BIG_RED_BUTTON_PRESS = SoundEvent.createVariableRangeEvent(ResourceLocation(MOD_ID, "block.big_red_button.press")).apply {
            Registry.register(BuiltInRegistries.SOUND_EVENT, this.location, this)
        }

        @JvmField val CREATIVE_TAB = register(BuiltInRegistries.CREATIVE_MODE_TAB, "tab",
            FabricItemGroup.builder()
                .icon { ItemStack(DOUBLOON_ITEM) }
                .title(Component.translatable("itemGroup.${MOD_ID}"))
                .displayItems { ctx, entries ->
                    entries.accept(PLAYER_ITEM_CHEST_ITEM)
                    entries.accept(DOUBLOON_ITEM)
                    entries.accept(BIG_RED_BUTTON_ITEM)
                }
                .build()
        )

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
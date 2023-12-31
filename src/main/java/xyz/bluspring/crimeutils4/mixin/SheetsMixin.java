package xyz.bluspring.crimeutils4.mixin;

import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.bluspring.crimeutils4.block.entity.PlayerItemChestBlockEntity;
import xyz.bluspring.crimeutils4.client.renderer.blockentity.PlayerItemChestBlockEntityRenderer;

@Mixin(Sheets.class)
public class SheetsMixin {
    @Inject(method = "chooseMaterial(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/level/block/state/properties/ChestType;Z)Lnet/minecraft/client/resources/model/Material;", at = @At("HEAD"), cancellable = true)
    private static void crimeutils$giveCustomBedMaterial(BlockEntity blockEntity, ChestType chestType, boolean bl, CallbackInfoReturnable<Material> cir) {
        if (blockEntity instanceof PlayerItemChestBlockEntity) {
            cir.setReturnValue(PlayerItemChestBlockEntityRenderer.PLAYER_ITEM_CHEST_LOCATION);
        }
    }
}

package xyz.bluspring.crimeutils4.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.bluspring.crimeutils4.CrimeUtilS4;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Shadow public abstract PlayerList getPlayerList();

    @Inject(method = "stopServer", at = @At("HEAD"))
    private void disconnectAllPlayers(CallbackInfo ci) {
        for (ServerPlayer player : this.getPlayerList().getPlayers()) {
            CrimeUtilS4.Companion.getInstance().placeInventoryChest(player.getInventory(), player, player.blockPosition());
            player.disconnect();
        }
    }
}

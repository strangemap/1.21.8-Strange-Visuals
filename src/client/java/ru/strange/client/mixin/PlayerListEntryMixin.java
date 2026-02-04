package ru.strange.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.strange.client.utils.other.SkinUtil;

@Mixin(PlayerListEntry.class)
public class PlayerListEntryMixin {

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void strange$injectSkinUtil(CallbackInfoReturnable<SkinTextures> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.getNetworkHandler() == null) {
            return;
        }

        SkinTextures original = cir.getReturnValue();
        if (original == null) {
            return;
        }

        PlayerListEntry self = (PlayerListEntry) (Object) this;

        if (!self.getProfile().getId().equals(client.player.getUuid())) {
            return;
        }

        SkinTextures updated = SkinUtil.updatedPlayerSkin(original, client.player);
        if (updated != original) {
            cir.setReturnValue(updated);
        }
    }
}

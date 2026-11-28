package net.mehvahdjukaar.amendments.mixins.neoforge;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.mehvahdjukaar.amendments.common.SurfaceSwimming;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerSurfaceSprintMixin {

    //neo has its eyes underwate check
    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;canStartSwimming()Z"))
    private boolean amendments$swimEvenFromSurface(boolean canStart) {
        return canStart || SurfaceSwimming.canStartSwimming((LocalPlayer) (Object) this);
    }
}

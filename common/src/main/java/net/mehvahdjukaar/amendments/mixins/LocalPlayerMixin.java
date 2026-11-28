package net.mehvahdjukaar.amendments.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.mehvahdjukaar.amendments.common.SurfaceSwimming;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    //game normally only triggers swim pose when eyes are under water
    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isUnderWater()Z"))
    private boolean amendments$swimEvenFromSurface(boolean underWater) {
        return underWater || SurfaceSwimming.canStartSwimming((LocalPlayer) (Object) this);
    }
}

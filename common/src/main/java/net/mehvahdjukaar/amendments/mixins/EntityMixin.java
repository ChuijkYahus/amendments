package net.mehvahdjukaar.amendments.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.mehvahdjukaar.amendments.common.SurfaceSwimming;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @ModifyExpressionValue(method = "updateSwimming", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;isUnderWater()Z"))
    private boolean amendments$swimEvenFromSurface(boolean underWater) {
        return underWater || SurfaceSwimming.canStartSwimming((Entity) (Object) this);
    }
}

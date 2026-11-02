package net.mehvahdjukaar.amendments.mixins.neoforge;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.mehvahdjukaar.amendments.common.SurfaceSwimming;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityWaterJumpMixin {

    // neo skips jumpInLiquid for water
    @WrapWithCondition(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;jumpInFluid(Lnet/neoforged/neoforge/fluids/FluidType;)V"))
    private boolean amendments$needDepthToSwimUp(LivingEntity self, FluidType type) {
        return type != NeoForgeMod.WATER_TYPE.value() || SurfaceSwimming.canPushOffWater(self);
    }
}

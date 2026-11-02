package net.mehvahdjukaar.amendments.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.mehvahdjukaar.amendments.client.ThreadingWaterAnimation;
import net.mehvahdjukaar.amendments.common.SurfaceSwimming;
import net.mehvahdjukaar.amendments.common.entity.IThreadingWaterSwimmer;
import net.mehvahdjukaar.amendments.configs.CommonConfigs;
import org.jetbrains.annotations.Nullable;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements IThreadingWaterSwimmer {

    //client only, i hope,
    @Unique
    @Nullable
    private ThreadingWaterAnimation amendments$surfaceSwimAnimation;

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void amendments$noPassengerSuffocation(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (CommonConfigs.PASSENGER_NO_SUFFOCATION.get()
                && source.is(DamageTypes.IN_WALL)
                && ((LivingEntity) (Object) this).isPassenger()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "jumpInLiquid", at = @At("HEAD"), cancellable = true)
    private void amendments$needDepthToSwimUp(TagKey<Fluid> fluidTag, CallbackInfo ci) {
        if (fluidTag == FluidTags.WATER && !SurfaceSwimming.canPushOffWater((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }


    //vanilla checks spot above where you were before moving so falling in fast kicks you out with just your toes wet
    @ModifyExpressionValue(method = "travel", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/world/entity/LivingEntity;isFree(DDD)Z"))
    private boolean amendments$noWallKickFromToes(boolean free) {
        return free && SurfaceSwimming.canWallKick((LivingEntity) (Object) this);
    }

    @Inject(method = "updateSwimAmount", at = @At("TAIL"))
    private void amendments$tickSurfaceSwimAnimation(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide) {
            return;
        }
        if (amendments$surfaceSwimAnimation == null) {
            amendments$surfaceSwimAnimation = new ThreadingWaterAnimation();
        }

        amendments$surfaceSwimAnimation.tick(self);
    }

    @Override
    public @Nullable ThreadingWaterAnimation amendments$getThreadingWaterAnim() {
        return  amendments$surfaceSwimAnimation;
    }


}

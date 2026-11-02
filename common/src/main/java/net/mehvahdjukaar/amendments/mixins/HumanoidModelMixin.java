package net.mehvahdjukaar.amendments.mixins;

import net.mehvahdjukaar.amendments.client.ThreadingWaterAnimation;
import net.mehvahdjukaar.amendments.common.entity.IThreadingWaterSwimmer;
import net.mehvahdjukaar.amendments.configs.ClientConfigs;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> {

    @Unique
    @Nullable
    private ThreadingWaterAnimation amendments$surfaceSwimAnimation;
    @Unique
    private float amendments$partialTick;


    @Shadow
    protected abstract HumanoidArm getAttackArm(T entity);


    @Inject(method = "prepareMobModel(Lnet/minecraft/world/entity/LivingEntity;FFF)V", at = @At("TAIL"))
    private void amendments$cacheSurfaceSwimAmount(T entity, float limbSwing, float limbSwingAmount, float partialTick, CallbackInfo ci) {
        amendments$surfaceSwimAnimation = ClientConfigs.SURFACE_SWIM_ANIMATION.get() ?
                ((IThreadingWaterSwimmer) entity).amendments$getThreadingWaterAnim() : null;
        amendments$partialTick = partialTick;
    }


    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void amendments$animateSurfaceSwim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                               float netHeadYaw, float headPitch, CallbackInfo ci) {
        ThreadingWaterAnimation animation = amendments$surfaceSwimAnimation;
        //first person hand calls setupAnim again without prepareMobModel
        amendments$surfaceSwimAnimation = null;
        if (animation == null || animation.getAmount(amendments$partialTick) <= 0) {
            return;
        }

        HumanoidModel<T> model = (HumanoidModel<T>) (Object) this;
        HumanoidArm attackArm = model.attackTime > 0 ? getAttackArm(entity) : null;
        animation.apply(model, entity, attackArm, amendments$partialTick);
    }


}

package net.mehvahdjukaar.amendments.client;

import net.mehvahdjukaar.amendments.configs.ClientConfigs;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ThreadingWaterAnimation {
    private float amount;
    private float prevAmount;
    private float effort;
    private float prevEffort;
    private float phase;
    private float prevPhase;
    private float treading;
    @Nullable
    private Vec3 lastPos;

    public void tick(LivingEntity entity) {
        prevAmount = amount;
        prevEffort = effort;
        prevPhase = phase;

        float s = isSwimmingUpright(entity) ? 0.1F : -0.1f;
        amount = Mth.clamp(amount + s, 0, 1);

        Vec3 moved = lastPos == null ? Vec3.ZERO : entity.position().subtract(lastPos);
        lastPos = entity.position();

        float wantedEffort;
        if (entity instanceof LocalPlayer player) {
            Input input = player.input;
            boolean paddling = input.jumping || input.shiftKeyDown || input.forwardImpulse != 0 || input.leftImpulse != 0;
            wantedEffort = paddling ? 1 : 0;
        } else {
            //walkAnimation ignores vertical motion and water speed is way below walking speed
            double passiveSinkSpeed = 0.03;
            double v = Math.max(0, Math.abs(moved.y) - passiveSinkSpeed);
            wantedEffort = Mth.clamp((float) Math.sqrt(moved.horizontalDistanceSqr() + v * v) * 12, 0, 1);
        }

        effort += (wantedEffort - effort) * 0.2f;

        boolean sinking = moved.y < -0.015;
        treading += ((sinking ? 0 : 1) - treading) * 0.1f;
        phase += 0.08f + 0.2f * effort;
    }

    public float getAmount(float partialTicks) {
        return Mth.lerp(partialTicks, prevAmount, amount);
    }

    private static boolean isSwimmingUpright(LivingEntity entity) {
        return entity.isInWater() && !entity.onGround() && !entity.isPassenger()
                && !entity.isVisuallySwimming() && !entity.isFallFlying()
                && entity.getFluidHeight(FluidTags.WATER) > (entity.getBbHeight() * 0.5);
    }

    public void apply(HumanoidModel<?> model, LivingEntity entity, @Nullable HumanoidArm attackArm, float partialTicks) {
        float a = getAmount(partialTicks);
        float eff = Mth.lerp(partialTicks, prevEffort, this.effort);
        float ph = Mth.lerp(partialTicks, prevPhase, this.phase);

        if (!entity.isUsingItem()) {
            float armAmount = a * eff;
            float wave = 0.2f + 0.8f * eff;
            float pitch = -0.7f + 0.2f * wave*Mth.cos(ph);
            float armSweep = 0.3f - 0.4f * wave * Mth.sin(ph);
            if (isArmFree(model.rightArmPose) && attackArm != HumanoidArm.RIGHT) {
                lerpRotation(model.rightArm, armAmount, pitch, armSweep, 0.55f);
            }
            if (isArmFree(model.leftArmPose) && attackArm != HumanoidArm.LEFT) {
                lerpRotation(model.leftArm, armAmount, pitch, -armSweep, -0.55f);
            }
        }
        float kick = (0.1f * treading + 0.5f * eff) * Mth.sin(ph * 1.5f);
        float legTilt = -0.1f * treading + getForwardness(entity) * Math.min(entity.walkAnimation.speed(partialTicks), 1) * 0.5f;
        float splay = ClientConfigs.SURFACE_SWIM_LEG_SPLAY.get() ? 0.1F : 0;
        lerpRotation(model.rightLeg, a, legTilt + kick, splay * 0.05f, splay);
        lerpRotation(model.leftLeg, a, legTilt - kick, -splay * 0.05f, -splay);
    }

    private static float getForwardness(LivingEntity entity) {
        double dx = entity.getX() - entity.xo;
        double dz = entity.getZ() - entity.zo;
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d < 0.0001) return 0;
        float yaw = entity.yBodyRot * Mth.DEG_TO_RAD;
        return (float) ((dz*Mth.cos(yaw) - dx * Mth.sin(yaw)) / d);
    }


    //wins over lanter holding
    private static boolean isArmFree(HumanoidModel.ArmPose pose) {
        return pose == HumanoidModel.ArmPose.EMPTY || pose == HumanoidModel.ArmPose.ITEM;
    }

    private static void lerpRotation(ModelPart part, float amount, float xRot, float yRot, float zRot) {
        part.xRot = Mth.lerp(amount, part.xRot, xRot);
        part.yRot = Mth.lerp(amount, part.yRot, yRot);
        part.zRot = Mth.lerp(amount, part.zRot, zRot);
    }

}

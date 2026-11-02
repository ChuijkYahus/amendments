package net.mehvahdjukaar.amendments.common;

import net.mehvahdjukaar.amendments.configs.CommonConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public class SurfaceSwimming {

    public static double getDepthBelowWaterline(Entity entity) {
        double w = entity.getY() + entity.getEyeHeight() - 0.3;
        return getWaterSurfaceY(entity) - w;
    }

    // getFluidHeight stops at the top of hitbox so deep down it wraps around every block
    private static double getWaterSurfaceY(Entity entity) {
        Level level = entity.level();
        BlockPos.MutableBlockPos pos = entity.blockPosition().mutable();
        double surface = entity.getY();
        for (int i = 0; i < 4; i++) {
            FluidState fluid = level.getFluidState(pos);
            if (!fluid.is(FluidTags.WATER)) break;
            surface = pos.getY() + fluid.getHeight(level, pos);
            pos.move(Direction.UP);
        }
        return surface;
    }


    public static boolean isFloatingAtSurface(Entity entity) {
        if (!CommonConfigs.SURFACE_SWIMMING.get() || !entity.isInWater() || entity.isPassenger()) {
            return false;
        }
        double d = getDepthBelowWaterline(entity);
        return d > -0.4 && d < 0.8; //somewhat arbitrary
    }

    public static boolean canFloat(Player player) {
        return player.isControlledByLocalInstance() && player.isAffectedByFluids()
                && !player.isSwimming() && !player.getAbilities().flying && !player.isShiftKeyDown()
                && !player.onClimbable() && !player.isFallFlying()
                && !player.horizontalCollision;
    }

    //water isnt solid ground. no pushing off it with just your toes in!
    public static boolean canPushOffWater(LivingEntity entity) {
        if (!CommonConfigs.WATER_JUMP_NEEDS_DEPTH.get() || !(entity instanceof Player player)) return true;
        return getDepthBelowWaterline(player) > -0.4 || player.horizontalCollision;
    }

    public static boolean canWallKick(LivingEntity entity) {
        if (!CommonConfigs.WATER_JUMP_NEEDS_DEPTH.get() || !(entity instanceof Player)) return true;
        return entity.getFluidHeight(FluidTags.WATER) >= 0.3;
    }

    public static void holdAtSurface(Player player) {
        Vec3 motion = player.getDeltaMovement();
        // should be strong enough to also eat jump impulse thats what makes vanilla bob
        double wantedY = Mth.clamp(getDepthBelowWaterline(player) * 0.3, -0.1, 0.1);

        player.setDeltaMovement(motion.x, motion.y + (wantedY - motion.y) * 0.5, motion.z);
    }
}

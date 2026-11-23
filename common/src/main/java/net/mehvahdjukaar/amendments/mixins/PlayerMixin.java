package net.mehvahdjukaar.amendments.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.mehvahdjukaar.amendments.common.SurfaceSwimming;
import net.mehvahdjukaar.amendments.configs.CommonConfigs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {

    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "travel", at = @At("HEAD"))
    private void amendments$floatAtSurface(Vec3 travelVector, CallbackInfo ci) {
        Player self = (Player) (Object) this;

        boolean wantsToFloat = this.jumping || CommonConfigs.SURFACE_AUTO_FLOAT.get();
        if (wantsToFloat && SurfaceSwimming.canFloat(self) && SurfaceSwimming.isFloatingAtSurface(self)) {
            SurfaceSwimming.holdAtSurface(self);
        }
    }


    @ModifyExpressionValue(method = "travel", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/player/Player;jumping:Z"))
    private boolean amendments$noSwimBreach(boolean jumping) {
        return jumping && SurfaceSwimming.canSwimUp((Player) (Object) this);
    }

}

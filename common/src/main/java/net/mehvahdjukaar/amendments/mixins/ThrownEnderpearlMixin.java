package net.mehvahdjukaar.amendments.mixins;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mehvahdjukaar.amendments.common.FollowingPets;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ThrownEnderpearl.class)
public abstract class ThrownEnderpearlMixin {

    @WrapWithCondition(method = "onHit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;unRide()V"))
    private boolean amendments$stayMounted(Entity owner) {
        return !FollowingPets.keepsMountOnPearl(owner);
    }

    @WrapOperation(method = "onHit", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/world/entity/Entity;changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;"))
    private Entity amendments$pearlWithMount(Entity owner, DimensionTransition transition, Operation<Entity> original) {
        if (!FollowingPets.keepsMountOnPearl(owner)) {
            return original.call(owner, transition);
        }

        original.call(owner.getRootVehicle(), transition);
        return owner;
    }

}

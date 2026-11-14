package net.mehvahdjukaar.amendments.mixins;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.mehvahdjukaar.amendments.common.FollowingPets;
import net.mehvahdjukaar.amendments.configs.CommonConfigs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "changeDimension", at = @At("HEAD"))
    private void amendments$saveOldLevel(DimensionTransition transition,
                                                      CallbackInfoReturnable<Entity> cir,
                                                      @Share("oldLevel") LocalRef<ServerLevel> oldLevel,
                                                      @Share("oldPos") LocalRef<Vec3> oldPos,
                                                      @Share("oldVehicle") LocalRef<Entity> oldVehicle) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        oldLevel.set(self.serverLevel());
        oldPos.set(self.position());
        oldVehicle.set(self.getVehicle());
    }

    @Inject(method = "changeDimension", at = @At("RETURN"))
    private void amendments$tpPetsThroughPortal(DimensionTransition transition, CallbackInfoReturnable<Entity> cir,
                                                @Share("oldLevel") LocalRef<ServerLevel> oldLevel,
                                                @Share("oldPos") LocalRef<Vec3> oldPos,
                                                @Share("oldVehicle") LocalRef<Entity> oldVehicle) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (!CommonConfigs.PETS_FOLLOW_TELEPORTS.get() || oldLevel.get() == self.serverLevel()) return;
        FollowingPets.makePetsFollowDiffLevel(self, oldLevel.get(), oldPos.get(), oldVehicle.get());
    }
}
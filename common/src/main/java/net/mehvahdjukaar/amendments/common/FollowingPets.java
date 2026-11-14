package net.mehvahdjukaar.amendments.common;

import net.mehvahdjukaar.amendments.configs.CommonConfigs;
import net.mehvahdjukaar.amendments.mixins.MobAccessor;
import net.mehvahdjukaar.amendments.reg.ModTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class FollowingPets {
    public static void makePetsFollowSameLevel(ServerPlayer player, Vec3 oldPos) {
        for (TamableAnimal pet : findPetsAround(player, player.serverLevel(), oldPos)) {
            if (pet.shouldTryTeleportToOwner()) pet.tryToTeleportToOwner();
        }
    }

    public static void makePetsFollowDiffLevel(ServerPlayer player, ServerLevel oldLevel, Vec3 oldPos, Entity oldVehicle) {
        ServerLevel level = player.serverLevel();
        for (TamableAnimal pet : findPetsAround(player, oldLevel, oldPos)) {
            //the mount movs itself and would get duped
            if (pet == oldVehicle || !pet.canChangeDimensions(oldLevel, level)) continue;
            var moved = pet.changeDimension(new DimensionTransition(level, player.position(), Vec3.ZERO,
                    pet.getYRot(), pet.getXRot(), DimensionTransition.DO_NOTHING));
            if (moved instanceof TamableAnimal m) m.tryToTeleportToOwner();
        }
    }

    public static boolean keepsMountOnPearl(Entity owner) {
        return CommonConfigs.PETS_FOLLOW_TELEPORTS.get() && owner instanceof ServerPlayer
                && owner.getRootVehicle() instanceof LivingEntity m && !(m instanceof Player);
    }


    private static List<TamableAnimal> findPetsAround(ServerPlayer player, ServerLevel level, Vec3 pos) {
        //arbitrary range...
        return level.getEntitiesOfClass(TamableAnimal.class, AABB.ofSize(pos, 24, 12, 24),
                pet -> pet.isOwnedBy(player) && !pet.unableToMoveToOwner() && canFollowOwner(pet));
    }

    //for mods that have pets that cant follow meaning we probably sholdnt teleport them
    private static boolean canFollowOwner(TamableAnimal pet) {
        if (pet.getType().is(ModTags.PETS_FOLLOW_TELEPORTS_WHITELIST)) return true;
        for (var wrapped : ((MobAccessor) pet).amendments$getGoalSelector().getAvailableGoals()) {
            if (wrapped.getGoal() instanceof FollowOwnerGoal) return true;
        }
        return false;
    }
}

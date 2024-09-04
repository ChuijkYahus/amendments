package net.mehvahdjukaar.amendments.common.tile;

import net.mehvahdjukaar.amendments.common.block.HangingFlowerPotBlock;
import net.mehvahdjukaar.amendments.reg.ModRegistry;
import net.mehvahdjukaar.moonlight.api.block.IOwnerProtected;
import net.mehvahdjukaar.moonlight.api.block.MimicBlockTile;
import net.mehvahdjukaar.moonlight.api.misc.ForgeOverride;
import net.mehvahdjukaar.moonlight.api.platform.ForgeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class HangingFlowerPotBlockTile extends MimicBlockTile implements IOwnerProtected {

    private UUID owner = null;

    public HangingFlowerPotBlockTile(BlockPos pos, BlockState state) {
        super(ModRegistry.HANGING_FLOWER_POT_TILE.get(), pos, state);
    }

    @Override
    public boolean setHeldBlock(BlockState state) {
        super.setHeldBlock(state);
        if (this.level instanceof ServerLevel) {
            int newLight = ForgeHelper.getLightEmission(this.getHeldBlock(), level, worldPosition);
            this.level.setBlock(this.worldPosition, this.getBlockState().setValue(HangingFlowerPotBlock.LIGHT_LEVEL, newLight), 3);
        }
        return true;
    }

    @Nullable
    @Override
    public UUID getOwner() {
        return owner;
    }

    @Override
    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        this.saveOwner(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.loadOwner(tag);
    }

    @ForgeOverride
    public AABB getRenderBoundingBox() {
        return new AABB(this.worldPosition);
    }

}

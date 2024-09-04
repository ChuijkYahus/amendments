package net.mehvahdjukaar.amendments.common.tile;

import com.mojang.datafixers.util.Pair;
import net.mehvahdjukaar.amendments.common.block.CarpetSlabBlock;
import net.mehvahdjukaar.amendments.common.block.CarpetStairBlock;
import net.mehvahdjukaar.amendments.reg.ModBlockProperties;
import net.mehvahdjukaar.amendments.reg.ModRegistry;
import net.mehvahdjukaar.moonlight.api.block.MimicBlockTile;
import net.mehvahdjukaar.moonlight.api.client.model.ExtraModelData;
import net.mehvahdjukaar.moonlight.api.client.model.ModelDataKey;
import net.mehvahdjukaar.moonlight.api.platform.ForgeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class CarpetedBlockTile extends MimicBlockTile {

    private static final Map<Pair<SoundType, SoundType>, SoundType> MIXED_SOUND_MAP = new HashMap<>();

    public static final ModelDataKey<BlockState> CARPET_KEY = new ModelDataKey<>(BlockState.class);

    private BlockState carpet = Blocks.WHITE_CARPET.defaultBlockState();
    private SoundType soundType = null;

    public CarpetedBlockTile(BlockPos pos, BlockState state) {
        super(ModRegistry.CARPET_STAIRS_TILE.get(), pos, state);
    }

    @Override
    public void addExtraModelData(ExtraModelData.Builder builder) {
        super.addExtraModelData(builder);
        builder.with(CARPET_KEY, carpet);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        HolderGetter<Block> holderGetter = this.level != null ? this.level.holderLookup(Registries.BLOCK) : BuiltInRegistries.BLOCK.asLookup();
        this.setCarpet(NbtUtils.readBlockState(holderGetter, tag.getCompound("Carpet")));
    }

    public void setCarpet(BlockState carpet) {
        setHeldBlock(carpet, 1);
    }

    public BlockState getCarpet() {
        return getHeldBlock(1);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Carpet", NbtUtils.writeBlockState(carpet));
    }

    @Override
    public BlockState getHeldBlock(int index) {
        if (index == 1) return carpet;
        return super.getHeldBlock(index);
    }

    @Override
    public boolean setHeldBlock(BlockState state, int index) {
        if (this.level instanceof ServerLevel) {
            this.setChanged();
            int newLight = Math.max(ForgeHelper.getLightEmission(getCarpet(), level, worldPosition),
                    ForgeHelper.getLightEmission(getHeldBlock(), level, worldPosition));
            this.level.setBlock(this.worldPosition, this.getBlockState()
                    .setValue(ModBlockProperties.LIGHT_LEVEL, newLight).setValue(CarpetSlabBlock.SOLID,
                            getHeldBlock().canOcclude()), 3);
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        } else {
            this.requestModelReload();
        }

        if (index == 0) {
            this.mimic = state;
            return true;
        } else if (index == 1) {
            this.carpet = state;
            return true;
        }
        this.soundType = null;
        return false;
    }

    public void initialize(BlockState stairs, BlockState carpet) {
        this.setHeldBlock(carpet, 1);
        this.setHeldBlock(stairs, 0);
    }

    @Nullable
    public SoundType getSoundType() {
        if (soundType == null) {
            BlockState stairs = this.getHeldBlock();
            BlockState carpet = this.getHeldBlock(1);
            if (!stairs.isAir() && !carpet.isAir()) {
                SoundType stairsSound = stairs.getSoundType();
                SoundType carpetSound = carpet.getSoundType();
                soundType = MIXED_SOUND_MAP.computeIfAbsent(Pair.of(stairsSound, carpetSound), p -> new SoundType(
                        1, 1, stairsSound.getBreakSound(), carpetSound.getStepSound(),
                        stairsSound.getPlaceSound(), stairsSound.getHitSound(), carpetSound.getFallSound()));
            }
            // block is invalid. return default and try again
            else return null;
        }

        return soundType;
    }

}


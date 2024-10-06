package net.mehvahdjukaar.amendments.common.tile;

import net.mehvahdjukaar.amendments.common.block.WallLanternBlock;
import net.mehvahdjukaar.amendments.configs.ClientConfigs;
import net.mehvahdjukaar.amendments.integration.CompatHandler;
import net.mehvahdjukaar.amendments.integration.ThinAirCompat;
import net.mehvahdjukaar.amendments.reg.ModRegistry;
import net.mehvahdjukaar.moonlight.api.block.IBlockHolder;
import net.mehvahdjukaar.moonlight.api.block.MimicBlockTile;
import net.mehvahdjukaar.moonlight.api.client.model.ExtraModelData;
import net.mehvahdjukaar.moonlight.api.client.model.IExtraModelDataProvider;
import net.mehvahdjukaar.moonlight.api.client.model.ModelDataKey;
import net.mehvahdjukaar.moonlight.api.platform.ForgeHelper;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.TickPriority;
import org.joml.Vector3f;

public class WallLanternBlockTile extends SwayingBlockTile implements IBlockHolder, IExtraModelDataProvider {

    public static final ModelDataKey<BlockState> MIMIC_KEY = MimicBlockTile.MIMIC_KEY;

    private BlockState mimic = Blocks.LANTERN.defaultBlockState();
    protected double attachmentOffset = 0;

    //for charm compat
    protected boolean isRedstoneLantern = false;

    public WallLanternBlockTile(BlockPos pos, BlockState state) {
        super(ModRegistry.WALL_LANTERN_TILE.get(), pos, state);
    }

    @Override
    public boolean isNeverFancy() {
        return ClientConfigs.FAST_LANTERNS.get();
    }

    public boolean isRedstoneLantern() {
        return isRedstoneLantern;
    }

    public double getAttachmentOffset() {
        return attachmentOffset;
    }

    @Override
    public Vector3f getRotationAxis(BlockState state) {
        return state.getValue(WallLanternBlock.FACING).step();
    }

    @Override
    public void addExtraModelData(ExtraModelData.Builder builder) {
        super.addExtraModelData(builder);
        builder.with(MIMIC_KEY, this.getHeldBlock());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.setHeldBlock(Utils.readBlockState(tag.getCompound("Lantern"), level));
        this.isRedstoneLantern = tag.getBoolean("IsRedstone");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Lantern", NbtUtils.writeBlockState(mimic));
        tag.putBoolean("IsRedstone", this.isRedstoneLantern);
    }

    @Override
    public BlockState getHeldBlock(int index) {
        return this.mimic;
    }

    @Override
    public boolean setHeldBlock(BlockState state, int index) {
        if (state.hasProperty(LanternBlock.HANGING)) {
            state = state.setValue(LanternBlock.HANGING, false);
        }
        if (CompatHandler.THIN_AIR && this.level != null && ThinAirCompat.isAirLantern(state)) {
            var newState = ThinAirCompat.maybeSetAirQuality(state, Vec3.atCenterOf(this.worldPosition), this.level);
            if (newState != null) {
                state = newState;
            }
            level.scheduleTick(worldPosition, getBlockState().getBlock(), 20, TickPriority.NORMAL);
        }

        this.mimic = state;


        int light = ForgeHelper.getLightEmission(state, level, worldPosition);
        boolean lit = true;
        var res = Utils.getID(this.mimic.getBlock());
        if (res.toString().equals("charm:redstone_lantern")) {
            this.isRedstoneLantern = true;
            light = 15;
            lit = false;
        }

        if (this.level != null && !this.mimic.isAir()) {
            var shape = state.getShape(this.level, this.worldPosition);
            if (!shape.isEmpty() && !res.getNamespace().equals("twigs")) {
                this.attachmentOffset = (shape.bounds().maxY - (9 / 16d));
            }
            if (this.getBlockState().getValue(WallLanternBlock.LIGHT_LEVEL) != light) {
                if (light == 0) lit = false;
                BlockState newState = this.getBlockState().setValue(WallLanternBlock.LIT, lit)
                        .setValue(WallLanternBlock.LIGHT_LEVEL, Math.max(light, 5));
                this.getLevel().setBlock(this.worldPosition, newState, 4 | 16);
            }
        }
        return true;
    }

}
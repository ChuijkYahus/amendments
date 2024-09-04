package net.mehvahdjukaar.amendments.common.tile;

import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class EnhancedSkullBlockTile extends BlockEntity {

    @Nullable
    protected SkullBlockEntity innerTile = null;

    public EnhancedSkullBlockTile(BlockEntityType type, BlockPos pWorldPosition, BlockState pBlockState) {
        super(type, pWorldPosition, pBlockState);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        this.saveInnerTile("Skull", this.innerTile, tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.innerTile = loadInnerTile("Skull", this.innerTile, tag);
    }

    protected void saveInnerTile(String tagName, @Nullable SkullBlockEntity tile, CompoundTag tag, HolderLookup.Provider registries) {
        if (tile != null) {
            tag.put(tagName + "State", NbtUtils.writeBlockState(tile.getBlockState()));
            tag.put(tagName, tile.saveWithFullMetadata(registries));
        }
    }

    @Nullable
    protected SkullBlockEntity loadInnerTile(String tagName, @Nullable SkullBlockEntity tile, CompoundTag tag) {
        if (tag.contains(tagName)) {
            BlockState state = Utils.readBlockState(tag.getCompound(tagName + "State"), this.level);
            CompoundTag tileTag = tag.getCompound(tagName);
            if (tile == null) {
                BlockEntity newTile = BlockEntity.loadStatic(this.getBlockPos(), state, tileTag);
                if (newTile instanceof SkullBlockEntity skullTile) return skullTile;
            } else {
                tile.load(tileTag);
                return tile;
            }
        }
        return null;
    }

    @Override
    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    public ItemStack getSkullItem() {
        if (this.innerTile != null) {
            return new ItemStack(innerTile.getBlockState().getBlock());
        }
        return ItemStack.EMPTY;
    }

    public void initialize(SkullBlockEntity oldTile, ItemStack stack, Player player, InteractionHand hand) {
        // this.setOwner(oldTile.getOwnerProfile());
        this.innerTile = (SkullBlockEntity) oldTile.getType().create(this.getBlockPos(), oldTile.getBlockState());
        if (this.innerTile != null) this.innerTile.load(oldTile.saveWithoutMetadata());
    }

    @Nullable
    public BlockState getSkull() {
        if (innerTile != null) {
            return innerTile.getBlockState();
        }
        return null;
    }

    @Nullable
    public BlockEntity getSkullTile() {
        return innerTile;
    }

    protected void tick(Level level, BlockPos pos, BlockState state) {
        if (innerTile != null) {
            var b = innerTile.getBlockState();
            if (b instanceof EntityBlock eb) {
                eb.getTicker(level, b, innerTile.getType());
            }
        }
    }
}

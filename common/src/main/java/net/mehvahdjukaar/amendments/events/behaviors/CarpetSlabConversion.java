package net.mehvahdjukaar.amendments.events.behaviors;

import net.mehvahdjukaar.amendments.common.tile.CarpetedBlockTile;
import net.mehvahdjukaar.amendments.configs.CommonConfigs;
import net.mehvahdjukaar.amendments.reg.ModRegistry;
import net.mehvahdjukaar.amendments.reg.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

public class CarpetSlabConversion implements ItemUseOnBlock {

    @Override
    public boolean altersWorld() {
        return true;
    }

    @Override
    public boolean placesBlock() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return CommonConfigs.CARPETED_SLABS.get();
    }

    @Override
    public boolean appliesToItem(Item item) {
        if (item instanceof BlockItem bi) {
            Block b = bi.getBlock();
            return b instanceof CarpetBlock || b.builtInRegistryHolder().is(ModTags.STAIRS_CARPETS);
        }
        return false;
    }

    @Override
    public InteractionResult tryPerformingAction(Level level, Player player, InteractionHand hand,
                                                 ItemStack stack, BlockHitResult hit) {
        if (!player.isSecondaryUseActive()) {
            BlockPos pos = hit.getBlockPos();
            BlockState slabState = level.getBlockState(pos);
            Block block = slabState.getBlock();
            if (block instanceof SlabBlock && slabState.getValue(SlabBlock.TYPE) == SlabType.BOTTOM
                    && !(block instanceof EntityBlock)) {
                BlockState carpet = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();

                InteractionResult result = InteractEvents.replaceSimilarBlock(ModRegistry.CARPET_SLAB.get(),
                        player, stack, pos, level, slabState, carpet.getSoundType(),
                        false,true, SlabBlock.TYPE, SlabBlock.WATERLOGGED);

                if (result.consumesAction()) {
                    if (level.getBlockEntity(pos) instanceof CarpetedBlockTile tile) {
                        tile.initialize(slabState, carpet);
                        return result;
                    }
                }
            }
        }
        return InteractionResult.PASS;
    }

}


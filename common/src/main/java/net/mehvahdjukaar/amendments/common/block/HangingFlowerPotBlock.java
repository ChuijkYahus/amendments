package net.mehvahdjukaar.amendments.common.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.mehvahdjukaar.amendments.Amendments;
import net.mehvahdjukaar.amendments.common.FlowerPotHandler;
import net.mehvahdjukaar.amendments.common.tile.HangingFlowerPotBlockTile;
import net.mehvahdjukaar.amendments.integration.CompatHandler;
import net.mehvahdjukaar.amendments.integration.SuppCompat;
import net.mehvahdjukaar.amendments.reg.ModBlockProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class HangingFlowerPotBlock extends Block implements EntityBlock {

    public static final MapCodec<HangingFlowerPotBlock> CODEC = simpleCodec(HangingFlowerPotBlock::new);

    protected static final VoxelShape SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 6.0D, 11.0D);
    public static final IntegerProperty LIGHT_LEVEL = ModBlockProperties.LIGHT_LEVEL;

    public HangingFlowerPotBlock(Properties properties) {
        super(properties.lightLevel(state -> state.getValue(LIGHT_LEVEL)));
        this.registerDefaultState(this.stateDefinition.any().setValue(LIGHT_LEVEL, 0));
    }

    @Override
    protected MapCodec<? extends HangingFlowerPotBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIGHT_LEVEL);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        Item i = stack.getItem();
        if (world.getBlockEntity(pos) instanceof HangingFlowerPotBlockTile tile) {
            if (i instanceof BlockItem blockItem) {
                BlockState mimic = blockItem.getBlock().defaultBlockState();
                tile.setHeldBlock(mimic);
            }
            if (CompatHandler.SUPPLEMENTARIES)
                SuppCompat.addOptionalOwnership(tile.getLevel(), tile.getBlockPos(), entity);
        }
    }

    @Override
    public MutableComponent getName() {
        return Component.translatable("block.minecraft.flower_pot");
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return context.getClickedFace() == Direction.DOWN ? super.getStateForPlacement(context) : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof HangingFlowerPotBlockTile tile && tile.isAccessibleBy(player)) {
            Block pot = tile.getHeldBlock().getBlock();
            if (pot instanceof FlowerPotBlock flowerPot) {
                ItemStack itemstack = player.getItemInHand(hand); //&& FlowerPotHandler.isEmptyPot(flowerPot)
                Item item = itemstack.getItem();
                //mimics flowerPorBlock behavior for consistency
                Block newPot = item instanceof BlockItem bi ? FlowerPotHandler.getFullPot(flowerPot, bi.getBlock()) : Blocks.AIR;

                boolean isEmptyFlower = newPot == Blocks.AIR;
                boolean isPotEmpty = FlowerPotHandler.isEmptyPot(pot);

                if (isEmptyFlower != isPotEmpty) {
                    if (isPotEmpty) {
                        if (!level.isClientSide) {
                            tile.setHeldBlock(newPot.defaultBlockState());
                            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
                            tile.setChanged();
                        }
                        playPlantSound(level, pos, player);

                        player.awardStat(Stats.POT_FLOWER);
                        itemstack.consume(1, player);
                    } else {
                        //drop item
                        ItemStack flowerItem = pot.getCloneItemStack(level, pos, state);
                        if (!flowerItem.equals(new ItemStack(this))) {
                            if (itemstack.isEmpty()) {
                                player.setItemInHand(hand, flowerItem);
                            } else if (!player.addItem(flowerItem)) {
                                player.drop(flowerItem, false);
                            }
                        }
                        if (!level.isClientSide) {
                            tile.setHeldBlock(FlowerPotHandler.getEmptyPot(flowerPot).defaultBlockState());
                            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
                            tile.setChanged();
                        }
                    }

                    level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
                    return ItemInteractionResult.sidedSuccess(level.isClientSide);
                } else {
                    return ItemInteractionResult.CONSUME;
                }
            }
        }
        return InteractionResult.PASS;
    }

    public static void playPlantSound(Level level, BlockPos pos, Player player) {
        level.playSound(player, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1,
                level.random.nextFloat() * 0.1F + 0.95F);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new HangingFlowerPotBlockTile(pPos, pState);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof HangingFlowerPotBlockTile te) {
            if (te.getHeldBlock().getBlock() instanceof FlowerPotBlock b) {
                Block flower = b.getPotted();
                if (flower == Blocks.AIR) return new ItemStack(Blocks.FLOWER_POT, 1);
                return new ItemStack(flower);
            }
        }
        return new ItemStack(Blocks.FLOWER_POT, 1);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof HangingFlowerPotBlockTile tile) {
            if (tile.getHeldBlock().getBlock() instanceof FlowerPotBlock flowerPotBlock)
                return Arrays.asList(new ItemStack(flowerPotBlock.getPotted()), new ItemStack(Items.FLOWER_POT));
        }
        return super.getDrops(state, builder);
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos) {
        return facing == Direction.UP && !this.canSurvive(stateIn, worldIn, currentPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader worldIn, BlockPos pos) {
        return Amendments.isSupportingCeiling(pos.relative(Direction.UP), worldIn);
    }
}
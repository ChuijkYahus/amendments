package net.mehvahdjukaar.amendments.common.block;

import net.mehvahdjukaar.amendments.common.tile.LiquidCauldronBlockTile;
import net.mehvahdjukaar.amendments.reg.ModBlockProperties;
import net.mehvahdjukaar.amendments.reg.ModRegistry;
import net.mehvahdjukaar.amendments.reg.ModTags;
import net.mehvahdjukaar.moonlight.api.fluids.BuiltInSoftFluids;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluidRegistry;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluidStack;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluidTank;
import net.mehvahdjukaar.moonlight.api.util.PotionNBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.TippedArrowRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LiquidCauldronBlock extends ModCauldronBlock {
    public static final IntegerProperty LEVEL = ModBlockProperties.LEVEL_1_4;
    public static final IntegerProperty LIGHT_LEVEL = ModBlockProperties.LIGHT_LEVEL;
    public static final BooleanProperty BOILING = ModBlockProperties.BOILING;

    public LiquidCauldronBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(LEVEL, 1).setValue(LIGHT_LEVEL, 0).setValue(BOILING, false));
    }

    @Override
    public IntegerProperty getLevelProperty() {
        return null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LEVEL, LIGHT_LEVEL, BOILING);
    }

    @Override
    protected boolean canReceiveStalactiteDrip(Fluid fluid) {
        return fluid != Fluids.WATER && fluid != Fluids.LAVA;
    }

    @Override
    public void receiveStalactiteDrip(BlockState state, Level level, BlockPos pos, Fluid fluid) {
        if (!isFull(state) && level.getBlockEntity(pos) instanceof LiquidCauldronBlockTile te) {
            var sf = SoftFluidRegistry.fromVanillaFluid(fluid);
            if (sf != null && te.getSoftFluidTank().addFluid(new SoftFluidStack(sf, 1))) {
                level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(state));
                level.levelEvent(1047, pos, 0);
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof LiquidCauldronBlockTile te) {
            if (te.handleInteraction(player, hand)) {
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            SoftFluidTank tank = te.getSoftFluidTank();
            SoftFluidStack fluid = tank.getFluid();
            ItemStack stack = player.getItemInHand(hand);

            //TODO: add recipe system for these?
            // craft with any item
            if (stack.is(Items.ARROW) && fluid.is(ModRegistry.DYE_SOFT_FLUID.get())) {
                //try tipping arrows
                ItemStack recolored = PotionUtils.setPotion(stack, PotionUtils.getPotion(fluid.getTag()));
                this.doCraftItem(level, pos, player, hand, te, fluid, stack, recolored);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isFull(BlockState state) {
        return state.getValue(LEVEL) == 4;
    }

    @Override
    protected double getContentHeight(BlockState state) {
        return 0.4375 + 0.125 * state.getValue(LEVEL);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        var s = super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
        if (direction == Direction.DOWN) {
            if (level.getBlockEntity(currentPos) instanceof LiquidCauldronBlockTile te) {
                boolean isFire = shouldBoil(neighborState, te.getSoftFluidTank().getFluid());
                s = s.setValue(BOILING, isFire);
            }
        }
        return s;
    }

    public static boolean shouldBoil(BlockState belowState, SoftFluidStack fluid) {
        return belowState.is(ModTags.HEAT_SOURCES) && fluid.is(BuiltInSoftFluids.POTION.get());
    }

    @Override
    protected boolean handleEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (state.getValue(BOILING)) {
            entity.hurt(new DamageSource(ModRegistry.BOILING_DAMAGE.getHolder()), 1.0F);
        }
        if (entity instanceof LivingEntity living && entity.mayInteract(level, pos) &&
                level.getBlockEntity(pos) instanceof LiquidCauldronBlockTile tile) {

            SoftFluidStack stack = tile.getSoftFluidTank().getFluid();
            if (getSplashOrLingeringPotType(stack) != null && applyPotionFluidEffects(level, pos, living, stack)) {
                return true;
            }
        }
        return false;
    }

    private boolean applyPotionFluidEffects(Level level, BlockPos pos, LivingEntity living, SoftFluidStack stack) {
        List<MobEffectInstance> effects = PotionUtils.getAllEffects(stack.getTag());
        boolean success = false;
        for (MobEffectInstance effect : effects) {
            MobEffect ef = effect.getEffect();
            if (living.hasEffect(ef)) continue;
            if (ef.isInstantenous()) {
                ef.applyInstantenousEffect(null, null, living,
                        effect.getAmplifier(), 1.0D);
            } else {
                living.addEffect(new MobEffectInstance(effect));
            }
            success = true;
        }
        if (success) {
            level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        return success;
    }

    //Spawn potion when removed

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {

        if (level.getBlockEntity(pos) instanceof LiquidCauldronBlockTile te) {
            if (state.getValue(BOILING)) {
                int color = te.getSoftFluidTank().getParticleColor(level, pos);
                addBubblingParticles(ModRegistry.BOILING_PARTICLE.get(), level, pos, 2,
                        getContentHeight(state), rand, color);

                if (level.random.nextInt(4) == 0) {
                    level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS,
                            0.4F + level.random.nextFloat() * 0.2F,
                            0.35F + level.random.nextFloat() * 0.2F, false);
                }
            }

            if (level.random.nextInt(4) == 0) {
                PotionNBTHelper.Type type = getSplashOrLingeringPotType(te.getSoftFluidTank().getFluid());
                if (type != null) {
                    ParticleOptions particle = type == PotionNBTHelper.Type.SPLASH ?
                            ParticleTypes.AMBIENT_ENTITY_EFFECT : ParticleTypes.ENTITY_EFFECT;

                    int color = te.getSoftFluidTank().getParticleColor(level, pos);
                    addPotionParticles(particle, level, pos, 1,
                            getContentHeight(state), rand, color);
                }
            }
        }
    }

    @Nullable
    private PotionNBTHelper.Type getSplashOrLingeringPotType(SoftFluidStack stack) {
        if (stack.is(BuiltInSoftFluids.POTION.get()) && stack.hasTag()) {
            var type = PotionNBTHelper.getPotionType(stack.getTag());
            if (type == PotionNBTHelper.Type.REGULAR) return null;
            else return type;
        }
        return null;
    }


    private void addBubblingParticles(ParticleOptions type, Level level, BlockPos pos, int count,
                                      double surface, RandomSource rand, int color) {

        for (int i = 0; i < count; i++) {
            double x = pos.getX() + 0.1875D + (rand.nextFloat() * 0.625D);
            double y = pos.getY() + 5 / 16f;
            double z = pos.getZ() + 0.1875D + (rand.nextFloat() * 0.625D);
            level.addParticle(type, x, y, z, color, pos.getY() + surface, 0);
        }
    }

    private void addPotionParticles(ParticleOptions type, Level level, BlockPos pos, int count,
                                    double surface, RandomSource rand, int color) {

        float r = FastColor.ARGB32.red(color) / 255f;
        float g = FastColor.ARGB32.green(color) / 255f;
        float b = FastColor.ARGB32.blue(color) / 255f;

        for (int i = 0; i < count; i++) {
            double x = pos.getX() + 0.1875D + (rand.nextFloat() * 0.625D);
            double y = pos.getY() + surface;
            double z = pos.getZ() + 0.1875D + (rand.nextFloat() * 0.625D);
            level.addParticle(type, x, y, z, r, g, b);
        }
    }

    @Override
    public BlockState updateStateOnFluidChange(BlockState state, SoftFluidStack fluid) {
        int light = fluid.getFluid().value().getLuminosity();
        if (light != state.getValue(ModBlockProperties.LIGHT_LEVEL)) {
            state = state.setValue(ModBlockProperties.LIGHT_LEVEL, light);
        }
        int height = fluid.getCount();
        if (fluid.isEmpty()) {
            state = Blocks.CAULDRON.defaultBlockState();
        } else {
            state = state.setValue(LiquidCauldronBlock.LEVEL, height);
        }
        return state;
    }


}

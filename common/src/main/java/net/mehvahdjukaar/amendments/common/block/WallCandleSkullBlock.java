package net.mehvahdjukaar.amendments.common.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.mehvahdjukaar.amendments.common.tile.CandleSkullBlockTile;
import net.mehvahdjukaar.moonlight.api.block.IRecolorable;
import net.mehvahdjukaar.moonlight.api.set.BlocksColorAPI;
import net.mehvahdjukaar.moonlight.api.util.math.MthUtils;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class WallCandleSkullBlock extends AbstractCandleSkullBlock implements IRecolorable {
    public static final MapCodec<WallCandleSkullBlock> CODEC = simpleCodec(WallCandleSkullBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final Map<Direction, VoxelShape[]> SHAPES = Util.make(() -> {
        Map<Direction, VoxelShape[]> m = new HashMap<>();

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            m.put(dir, new VoxelShape[]{
                            MthUtils.rotateVoxelShape(ONE_AABB.move(0, 0, 0.25), dir),
                            MthUtils.rotateVoxelShape(TWO_AABB.move(0, 0, 0.25), dir),
                            MthUtils.rotateVoxelShape(THREE_AABB.move(0, 0, 0.25), dir),
                            MthUtils.rotateVoxelShape(FOUR_AABB.move(0, 0, 0.25), dir),
                    }
            );
        }
        return m;
    });

    protected static final Map<Direction, Int2ObjectMap<List<Vec3>>> H_PARTICLE_OFFSETS = Util.make(() -> {
        Map<Direction, Int2ObjectMap<List<Vec3>>> temp = new Object2ObjectOpenHashMap<>(4);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            temp.put(dir, new Int2ObjectArrayMap<>(4));
            PARTICLE_OFFSETS.forEach((key, value) -> {
                List<Vec3> transformedList = new ArrayList<>();
                for (Vec3 v : value) {
                    transformedList.add(MthUtils.rotateVec3(
                                    new Vec3(v.x - 0.5, v.y, v.z + 0.25 - 0.5), dir)
                            .add(0.5, 0, 0.5));
                }
                temp.get(dir).put(key, transformedList);
            });
        }
        return temp;
    });

    public WallCandleSkullBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends WallCandleSkullBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(FACING);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPES.get(pState.getValue(FACING))[pState.getValue(CANDLES) - 1];
    }

    @Override
    protected Iterable<Vec3> getParticleOffsets(BlockState pState) {
        return H_PARTICLE_OFFSETS.get(pState.getValue(FACING)).get(pState.getValue(CANDLES));
    }

    @Override
    public boolean tryRecolor(Level level, BlockPos blockPos, BlockState blockState, @Nullable DyeColor dyeColor) {
        if(level.getBlockEntity(blockPos) instanceof CandleSkullBlockTile tile){
            var c = tile.getCandle();
            if(!c.isAir()){
                Block otherCandle = BlocksColorAPI.changeColor(c.getBlock(), dyeColor);
                if(otherCandle != null && !c.is(otherCandle)){
                    tile.setCandle(otherCandle.withPropertiesOf(c));
                    tile.setChanged();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isDefaultColor(Level level, BlockPos blockPos, BlockState blockState) {
        if(level.getBlockEntity(blockPos) instanceof CandleSkullBlockTile tile) {
            var c = tile.getCandle();
            return BlocksColorAPI.isDefaultColor(c.getBlock());
        }
        return false;
    }
}

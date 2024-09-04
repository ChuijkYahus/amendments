package net.mehvahdjukaar.amendments.client.colors;

import net.mehvahdjukaar.amendments.configs.ClientConfigs;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BrewingStandColor implements BlockColor {

    @Override
    public int getColor(BlockState state, @Nullable BlockAndTintGetter world, @Nullable BlockPos pos, int tint) {
        if (tint < 1 || tint > 3) return -1;
        if (world != null && pos != null) {
            BlockEntity te = world.getBlockEntity(pos);
            if (te instanceof BrewingStandBlockEntity br) {
                ItemStack item = br.getItem(tint - 1);
                PotionContents contents = item.get(DataComponents.POTION_CONTENTS);
                if (contents != null) {
                    //TODO: use dynamic pack
                    if (!ClientConfigs.COLORED_BREWING_STAND.get()) return 0xff3434;
                    return contents.getColor();
                } else return -1;
            }
        }
        return 0xffffff;
    }
}


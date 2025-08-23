package net.mehvahdjukaar.amendments.common;

import net.mehvahdjukaar.amendments.reg.ModRegistry;
import net.mehvahdjukaar.moonlight.api.misc.TileOrEntityTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.LecternBlockEntity;

import java.util.List;

public class LecternEditMenu extends LecternMenu {

    private final BlockPos pos;

    public static LecternEditMenu of(int id, Inventory playerInventory, FriendlyByteBuf packetBuffer) {
        var tile = TileOrEntityTarget.read(packetBuffer);
        return new LecternEditMenu(id, tile.getBlockEntityOrThrow(playerInventory.player.level(),
                BlockEntityType.LECTERN), new SimpleContainerData(1));
    }

    public LecternEditMenu(int i, LecternBlockEntity container, ContainerData containerData) {
        super(i, (Container) container, containerData);
        this.pos = container.getBlockPos();
    }

    @Override
    public MenuType<?> getType() {
        return ModRegistry.LECTERN_EDIT_MENU.get();
    }

    public BlockPos getPos() {
        return this.pos;
    }

    @Override
    public void initializeContents(int stateId, List<ItemStack> items, ItemStack carried) {
        super.initializeContents(stateId, items, carried);
    }


}

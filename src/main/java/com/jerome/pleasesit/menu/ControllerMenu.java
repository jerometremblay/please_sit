package com.jerome.pleasesit.menu;

import com.jerome.pleasesit.PleaseSitMod;
import com.jerome.pleasesit.block.entity.ModdingChairBlockEntity;
import com.jerome.pleasesit.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.IContainerFactory;

public class ControllerMenu extends AbstractContainerMenu {
    private final ContainerData data;
    private final BlockEntity blockEntity;

    public ControllerMenu(int id, Inventory playerInventory, BlockEntity blockEntity) {
        super(ModBlockEntities.CONTROLLER.get(), id);
        this.blockEntity = blockEntity;
        this.data = new SimpleContainerData(1) {
            @Override
            public int get(int index) {
                if (blockEntity instanceof ModdingChairBlockEntity chair) {
                    return chair.getSearchRadius();
                }
                return 16;
            }

            @Override
            public void set(int index, int value) {
                if (blockEntity instanceof ModdingChairBlockEntity chair) {
                    chair.setSearchRadius(value);
                }
            }

            @Override
            public int getCount() {
                return 1;
            }
        };
        addDataSlots(this.data);
    }

    public ControllerMenu(int id, Inventory playerInventory, net.minecraft.network.RegistryFriendlyByteBuf buffer) {
        this(id, playerInventory, getBlockEntity(playerInventory, buffer));
    }

    private static BlockEntity getBlockEntity(Inventory playerInventory, net.minecraft.network.RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity entity = playerInventory.player.level().getBlockEntity(pos);
        if (entity instanceof ModdingChairBlockEntity) {
            return entity;
        }
        throw new IllegalStateException("Block entity is not a ModdingChairBlockEntity");
    }

    public int getSearchRadius() {
        return data.get(0);
    }

    public void setSearchRadius(int radius) {
        data.set(0, radius);
    }

    public BlockPos getBlockPos() {
        return blockEntity != null ? blockEntity.getBlockPos() : BlockPos.ZERO;
    }

     @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) {
            return false;
        }
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}

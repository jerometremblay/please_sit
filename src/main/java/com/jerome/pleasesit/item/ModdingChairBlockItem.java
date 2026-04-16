package com.jerome.pleasesit.item;

import com.jerome.pleasesit.block.entity.ModdingChairBlockEntity;
import java.util.function.Consumer;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ModdingChairBlockItem extends BlockItem {
    private static final String TARGET_POS_KEY = "target_pos";
    private static final String LOCKED_VILLAGER_UUID_KEY = "locked_villager_uuid";

    public ModdingChairBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();

        if (player == null) {
            return super.useOn(context);
        }

        if (player.isShiftKeyDown() || !hasStoredTarget(stack)) {
            storeTarget(stack, context.getClickedPos());
            player.displayClientMessage(Component.translatable("item.pleasesit.modding_chair.target_set"), true);
            return InteractionResult.SUCCESS;
        }

        return super.useOn(context);
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, @Nullable Player player, ItemStack stack, BlockState state) {
        boolean updated = super.updateCustomBlockEntityTag(pos, level, player, stack, state);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ModdingChairBlockEntity chair) {
            chair.applyPlacementData(getStoredTarget(stack), getStoredLockedVillagerUuid(stack));
            updated = true;
        }
        if (updated) {
            stack.remove(DataComponents.BLOCK_ENTITY_DATA);
        }
        return updated;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltipComponents,
            TooltipFlag isAdvanced
    ) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipComponents, isAdvanced);

        BlockPos targetPos = getStoredTarget(stack);
        if (targetPos == null) {
            tooltipComponents.accept(Component.translatable("item.pleasesit.modding_chair.target_missing")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        tooltipComponents.accept(Component.translatable("item.pleasesit.modding_chair.target_ready", formatBlockPos(targetPos))
                .withStyle(ChatFormatting.GRAY));
        if (getStoredLockedVillagerUuid(stack) != null) {
            tooltipComponents.accept(Component.translatable("item.pleasesit.modding_chair.villager_locked_tooltip")
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltipComponents.accept(Component.translatable("item.pleasesit.modding_chair.target_reset_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static boolean hasStoredTarget(ItemStack stack) {
        return getStoredTarget(stack) != null;
    }

    public static @Nullable BlockPos getStoredTarget(ItemStack stack) {
        CompoundTag tag = getStoredBlockEntityTag(stack);
        if (!tag.contains(TARGET_POS_KEY)) {
            return null;
        }

        return tag.getLong(TARGET_POS_KEY).map(BlockPos::of).orElse(null);
    }

    public static @Nullable UUID getStoredLockedVillagerUuid(ItemStack stack) {
        CompoundTag tag = getStoredBlockEntityTag(stack);
        return tag.read(LOCKED_VILLAGER_UUID_KEY, UUIDUtil.CODEC).orElse(null);
    }

    public static void bindToVillager(ItemStack stack, Player player, Villager villager) {
        if (!hasStoredTarget(stack)) {
            storeTarget(stack, villager.blockPosition());
        }
        storeLockedVillager(stack, villager.getUUID());
        player.displayClientMessage(Component.translatable("item.pleasesit.modding_chair.villager_locked"), true);
    }

    private static void storeTarget(ItemStack stack, BlockPos targetPos) {
        CompoundTag tag = getStoredBlockEntityTag(stack);
        tag.putLong(TARGET_POS_KEY, targetPos.asLong());
        setBlockEntityData(stack, tag);
    }

    private static void storeLockedVillager(ItemStack stack, UUID villagerUuid) {
        CompoundTag tag = getStoredBlockEntityTag(stack);
        tag.store(LOCKED_VILLAGER_UUID_KEY, UUIDUtil.CODEC, villagerUuid);
        setBlockEntityData(stack, tag);
    }

    private static void setBlockEntityData(ItemStack stack, CompoundTag tag) {
        if (tag.isEmpty()) {
            stack.remove(DataComponents.BLOCK_ENTITY_DATA);
            return;
        }

        CustomData.set(DataComponents.BLOCK_ENTITY_DATA, stack, tag);
    }

    private static CompoundTag getStoredBlockEntityTag(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        return customData.copyTag();
    }

    private static Component formatBlockPos(BlockPos pos) {
        return Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
    }
}

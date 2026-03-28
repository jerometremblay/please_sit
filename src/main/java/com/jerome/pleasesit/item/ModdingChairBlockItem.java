package com.jerome.pleasesit.item;

import com.jerome.pleasesit.registry.ModBlockEntities;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ModdingChairBlockItem extends BlockItem {
    private static final String TARGET_POS_KEY = "target_pos";

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
        if (updated) {
            stack.remove(DataComponents.BLOCK_ENTITY_DATA);
        }
        return updated;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, context, tooltipComponents, isAdvanced);

        BlockPos targetPos = getStoredTarget(stack);
        if (targetPos == null) {
            tooltipComponents.add(Component.translatable("item.pleasesit.modding_chair.target_missing")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        tooltipComponents.add(Component.translatable("item.pleasesit.modding_chair.target_ready", formatBlockPos(targetPos))
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.pleasesit.modding_chair.target_reset_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static boolean hasStoredTarget(ItemStack stack) {
        return getStoredTarget(stack) != null;
    }

    public static @Nullable BlockPos getStoredTarget(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(TARGET_POS_KEY)) {
            return null;
        }

        return BlockPos.of(tag.getLong(TARGET_POS_KEY));
    }

    private static void storeTarget(ItemStack stack, BlockPos targetPos) {
        CompoundTag tag = new CompoundTag();
        tag.putLong(TARGET_POS_KEY, targetPos.asLong());
        BlockItem.setBlockEntityData(stack, getBlockEntityType(), tag);
    }

    @SuppressWarnings("unchecked")
    private static BlockEntityType<?> getBlockEntityType() {
        return ModBlockEntities.MODDING_CHAIR.get();
    }

    private static Component formatBlockPos(BlockPos pos) {
        return Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
    }
}

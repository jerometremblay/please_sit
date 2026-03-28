package com.jerome.pleasesit.client;

import com.jerome.pleasesit.PleaseSitMod;
import com.jerome.pleasesit.item.ModdingChairBlockItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

@EventBusSubscriber(modid = PleaseSitMod.MOD_ID, value = Dist.CLIENT)
public final class ModdingChairTargetHandler {
    private static final float TARGET_RED = 1.0F;
    private static final float TARGET_GREEN = 0.7843F;
    private static final float TARGET_BLUE = 0.3294F;
    private static final float TARGET_ALPHA = 1.0F;

    private ModdingChairTargetHandler() {
    }

    @SubscribeEvent
    public static void onRenderHighlight(RenderHighlightEvent.Block event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ModdingChairBlockItem)) {
            return;
        }

        BlockPos targetPos = ModdingChairBlockItem.getStoredTarget(stack);
        if (targetPos == null) {
            return;
        }

        BlockState state = minecraft.level.getBlockState(targetPos);
        VoxelShape shape = state.getShape(minecraft.level, targetPos);
        AABB box = shape.isEmpty() ? new AABB(BlockPos.ZERO) : shape.bounds();
        AABB worldBox = box.move(targetPos);
        AABB renderBox = worldBox.move(
                -event.getCamera().getPosition().x,
                -event.getCamera().getPosition().y,
                -event.getCamera().getPosition().z
        );

        LevelRenderer.renderLineBox(
                event.getPoseStack(),
                event.getMultiBufferSource().getBuffer(RenderType.lines()),
                renderBox,
                TARGET_RED,
                TARGET_GREEN,
                TARGET_BLUE,
                TARGET_ALPHA
        );
    }
}

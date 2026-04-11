package com.jerome.pleasesit.client;

import com.jerome.pleasesit.PleaseSitMod;
import com.jerome.pleasesit.item.ModdingChairBlockItem;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = PleaseSitMod.MOD_ID, value = Dist.CLIENT)
public final class ModdingChairTargetHandler {
    private static final float TARGET_RED = 1.0F;
    private static final float TARGET_GREEN = 0.7843F;
    private static final float TARGET_BLUE = 0.3294F;
    private static final float TARGET_ALPHA = 1.0F;
    private static final double SELECTED_VILLAGER_BOX_INFLATION = 0.1D;

    private ModdingChairTargetHandler() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterEntities event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ModdingChairBlockItem)) {
            return;
        }

        Vec3 cameraPosition = minecraft.gameRenderer.getMainCamera().position();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        BlockPos targetPos = ModdingChairBlockItem.getStoredTarget(stack);
        if (targetPos != null) {
            BlockState state = minecraft.level.getBlockState(targetPos);
            VoxelShape shape = state.getShape(minecraft.level, targetPos);
            if (shape.isEmpty()) {
                shape = Shapes.block();
            }

            ShapeRenderer.renderShape(
                    event.getPoseStack(),
                    bufferSource.getBuffer(RenderTypes.lines()),
                    shape,
                    targetPos.getX() - cameraPosition.x,
                    targetPos.getY() - cameraPosition.y,
                    targetPos.getZ() - cameraPosition.z,
                    color(),
                    1.0F
            );
        }

        UUID lockedVillagerUuid = ModdingChairBlockItem.getStoredLockedVillagerUuid(stack);
        if (lockedVillagerUuid == null) {
            bufferSource.endBatch(RenderTypes.lines());
            return;
        }

        Entity lockedVillager = null;
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (lockedVillagerUuid.equals(entity.getUUID())) {
                lockedVillager = entity;
                break;
            }
        }

        if (lockedVillager == null || !lockedVillager.isAlive()) {
            bufferSource.endBatch(RenderTypes.lines());
            return;
        }

        AABB villagerRenderBox = lockedVillager.getBoundingBox().inflate(SELECTED_VILLAGER_BOX_INFLATION);
        ShapeRenderer.renderShape(
                event.getPoseStack(),
                bufferSource.getBuffer(RenderTypes.lines()),
                Shapes.create(villagerRenderBox),
                -cameraPosition.x,
                -cameraPosition.y,
                -cameraPosition.z,
                color(),
                1.0F
        );
        bufferSource.endBatch(RenderTypes.lines());
    }

    private static int color() {
        return ((int) (TARGET_ALPHA * 255.0F) << 24)
                | ((int) (TARGET_RED * 255.0F) << 16)
                | ((int) (TARGET_GREEN * 255.0F) << 8)
                | (int) (TARGET_BLUE * 255.0F);
    }
}

package com.jerome.pleasesit.client;

import com.jerome.pleasesit.PleaseSitMod;
import com.jerome.pleasesit.item.ModdingChairBlockItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
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

        renderLineBox(
                event.getPoseStack(),
                event.getMultiBufferSource().getBuffer(RenderType.lines()),
                renderBox,
                TARGET_RED,
                TARGET_GREEN,
                TARGET_BLUE,
                TARGET_ALPHA
        );
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ModdingChairBlockItem)) {
            return;
        }

        UUID lockedVillagerUuid = ModdingChairBlockItem.getStoredLockedVillagerUuid(stack);
        if (lockedVillagerUuid == null) {
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
            return;
        }

        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        AABB villagerRenderBox = lockedVillager.getBoundingBox()
                .inflate(SELECTED_VILLAGER_BOX_INFLATION)
                .move(
                        -event.getCamera().getPosition().x,
                        -event.getCamera().getPosition().y,
                        -event.getCamera().getPosition().z
                );

        renderLineBox(
                event.getPoseStack(),
                bufferSource.getBuffer(RenderType.lines()),
                villagerRenderBox,
                TARGET_RED,
                TARGET_GREEN,
                TARGET_BLUE,
                TARGET_ALPHA
        );
        bufferSource.endBatch(RenderType.lines());
    }

    private static void renderLineBox(PoseStack poseStack, VertexConsumer consumer, AABB box, float red, float green, float blue, float alpha) {
        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;

        line(poseStack, consumer, minX, minY, minZ, maxX, minY, minZ, red, green, blue, alpha);
        line(poseStack, consumer, minX, minY, maxZ, maxX, minY, maxZ, red, green, blue, alpha);
        line(poseStack, consumer, minX, maxY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
        line(poseStack, consumer, minX, maxY, maxZ, maxX, maxY, maxZ, red, green, blue, alpha);

        line(poseStack, consumer, minX, minY, minZ, minX, minY, maxZ, red, green, blue, alpha);
        line(poseStack, consumer, maxX, minY, minZ, maxX, minY, maxZ, red, green, blue, alpha);
        line(poseStack, consumer, minX, maxY, minZ, minX, maxY, maxZ, red, green, blue, alpha);
        line(poseStack, consumer, maxX, maxY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);

        line(poseStack, consumer, minX, minY, minZ, minX, maxY, minZ, red, green, blue, alpha);
        line(poseStack, consumer, maxX, minY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
        line(poseStack, consumer, minX, minY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
        line(poseStack, consumer, maxX, minY, maxZ, maxX, maxY, maxZ, red, green, blue, alpha);
    }

    private static void line(PoseStack poseStack, VertexConsumer consumer, double x1, double y1, double z1, double x2, double y2, double z2, float red, float green, float blue, float alpha) {
        float nx = (float) (x2 - x1);
        float ny = (float) (y2 - y1);
        float nz = (float) (z2 - z1);
        PoseStack.Pose pose = poseStack.last();
        consumer.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(red, green, blue, alpha).setNormal(pose, nx, ny, nz);
        consumer.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(red, green, blue, alpha).setNormal(pose, nx, ny, nz);
    }
}

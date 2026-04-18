package com.jerome.pleasesit.client.screen;

import com.jerome.pleasesit.menu.ControllerMenu;
import com.jerome.pleasesit.network.SetSearchRadiusPayload;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.world.entity.player.Inventory;

public class ControllerScreen extends AbstractContainerScreen<ControllerMenu> {
    private EditBox radiusField;
    private Button increaseButton;
    private Button decreaseButton;

    public ControllerScreen(ControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        
        radiusField = new EditBox(
            font,
            leftPos + 62,
            topPos + 35,
            52,
            12,
            Component.translatable("container.pleasesit.search_radius")
        );
        radiusField.setMaxLength(2);
        radiusField.setFilter(s -> s.matches("\\d*"));
        radiusField.setValue(String.valueOf(menu.getSearchRadius()));
        radiusField.setResponder(s -> {
            try {
                onRadiusChanged(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                radiusField.setValue(String.valueOf(menu.getSearchRadius()));
            }
        });
        addRenderableWidget(radiusField);
        
        increaseButton = Button.builder(
            Component.literal("+"),
            button -> onRadiusChanged(Math.min(menu.getSearchRadius() + 1, 64))
        ).size(20, 20).build();
        increaseButton.setPosition(leftPos + 116, topPos + 33);
        addRenderableWidget(increaseButton);
        
        decreaseButton = Button.builder(
            Component.literal("-"),
            button -> onRadiusChanged(Math.max(menu.getSearchRadius() - 1, 1))
        ).size(20, 20).build();
        decreaseButton.setPosition(leftPos + 42, topPos + 33);
        addRenderableWidget(decreaseButton);
    }

    @Override
    public void removed() {
        super.removed();
        try {
            sendRadius(Integer.parseInt(radiusField.getValue()));
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        guiGraphics.drawString(
            font,
            String.valueOf(menu.getSearchRadius()),
            leftPos + 84,
            topPos + 39,
            4210752,
            false
        );
    }

    private void onRadiusChanged(int newRadius) {
        String value = String.valueOf(Math.clamp(newRadius, 1, 64));
        if (!value.equals(radiusField.getValue())) {
            radiusField.setValue(value);
        }
        sendRadius(Integer.parseInt(value));
    }

    private void sendRadius(int radius) {
        int clamped = Math.clamp(radius, 1, 64);
        menu.setSearchRadius(clamped);
        Minecraft.getInstance().getConnection().send(new ServerboundCustomPayloadPacket(new SetSearchRadiusPayload(menu.getBlockPos(), clamped)));
    }
}

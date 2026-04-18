package com.jerome.pleasesit.client;

import com.jerome.pleasesit.PleaseSitMod;
import com.jerome.pleasesit.client.screen.ControllerScreen;
import com.jerome.pleasesit.menu.ControllerMenu;
import com.jerome.pleasesit.registry.ModBlockEntities;
import net.minecraft.client.gui.screens.MenuScreens;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = PleaseSitMod.MOD_ID)
public final class ModScreenFactories {
    private ModScreenFactories() {
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModBlockEntities.CONTROLLER.get(), ControllerScreen::new);
    }
}
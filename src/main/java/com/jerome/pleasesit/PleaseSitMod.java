package com.jerome.pleasesit;

import com.jerome.pleasesit.config.PleaseSitConfig;
import com.mojang.logging.LogUtils;
import com.jerome.pleasesit.registry.ModBlockEntities;
import com.jerome.pleasesit.registry.ModBlocks;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;

@Mod(PleaseSitMod.MOD_ID)
public final class PleaseSitMod {
    public static final String MOD_ID = "pleasesit";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PleaseSitMod(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        modEventBus.addListener(this::addCreativeTabEntries);
        modContainer.registerConfig(ModConfig.Type.COMMON, PleaseSitConfig.COMMON_SPEC);
        if (FMLEnvironment.getDist().isClient()) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, (container, parent) -> new ConfigurationScreen(modContainer, parent));
        }
        LOGGER.info("Loading {}", MOD_ID);
    }

    @SubscribeEvent
    private void addCreativeTabEntries(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.MODDING_CHAIR_ITEM);
        }
    }
}

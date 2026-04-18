package com.jerome.pleasesit.registry;

import com.jerome.pleasesit.PleaseSitMod;
import com.jerome.pleasesit.block.entity.ModdingChairBlockEntity;
import com.jerome.pleasesit.menu.ControllerMenu;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PleaseSitMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ModdingChairBlockEntity>> MODDING_CHAIR =
            BLOCK_ENTITY_TYPES.register("modding_chair",
                    () -> new BlockEntityType<>(ModdingChairBlockEntity::new, Set.of(ModBlocks.MODDING_CHAIR.get())));

    private static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, PleaseSitMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ControllerMenu>> CONTROLLER =
            MENU_TYPES.register("controller", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(ControllerMenu::new));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENU_TYPES.register(modEventBus);
    }
}

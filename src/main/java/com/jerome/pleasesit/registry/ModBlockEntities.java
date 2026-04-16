package com.jerome.pleasesit.registry;

import com.jerome.pleasesit.PleaseSitMod;
import com.jerome.pleasesit.block.entity.ModdingChairBlockEntity;
import java.util.Set;
import net.minecraft.core.registries.Registries;
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

    private ModBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}

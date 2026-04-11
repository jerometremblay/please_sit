package com.jerome.pleasesit.registry;

import com.jerome.pleasesit.PleaseSitMod;
import com.jerome.pleasesit.block.ModdingChairBlock;
import com.jerome.pleasesit.item.ModdingChairBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(PleaseSitMod.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PleaseSitMod.MOD_ID);

    public static final DeferredBlock<ModdingChairBlock> MODDING_CHAIR = BLOCKS.registerBlock("modding_chair",
            ModdingChairBlock::new,
            properties -> properties
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(2.0F)
                    .sound(SoundType.WOOD));

    public static final DeferredItem<BlockItem> MODDING_CHAIR_ITEM = ITEMS.registerItem("modding_chair",
            properties -> new ModdingChairBlockItem(ModBlocks.MODDING_CHAIR.get(), properties));

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}

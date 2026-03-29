package com.jerome.pleasesit.event;

import com.jerome.pleasesit.PleaseSitMod;
import com.jerome.pleasesit.item.ModdingChairBlockItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = PleaseSitMod.MOD_ID)
public final class ModInteractionHandler {
    private ModInteractionHandler() {
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ModdingChairBlockItem)
                || !(event.getTarget() instanceof Villager villager)
                || !player.isShiftKeyDown()) {
            return;
        }

        ModdingChairBlockItem.bindToVillager(stack, player, villager);
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));
        event.setCanceled(true);
    }
}

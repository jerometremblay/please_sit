package com.jerome.pleasesit.network;

import com.jerome.pleasesit.PleaseSitMod;
import com.jerome.pleasesit.block.entity.ModdingChairBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetSearchRadiusPayload(BlockPos pos, int radius) implements CustomPacketPayload {
    public static final Type<SetSearchRadiusPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PleaseSitMod.MOD_ID, "set_search_radius"));

    public static final StreamCodec<FriendlyByteBuf, SetSearchRadiusPayload> STREAM_CODEC =
            StreamCodec.of(SetSearchRadiusPayload::write, SetSearchRadiusPayload::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static SetSearchRadiusPayload read(FriendlyByteBuf buffer) {
        return new SetSearchRadiusPayload(buffer.readBlockPos(), buffer.readVarInt());
    }

    private static void write(FriendlyByteBuf buffer, SetSearchRadiusPayload payload) {
        buffer.writeBlockPos(payload.pos);
        buffer.writeVarInt(payload.radius);
    }

    public static void handle(SetSearchRadiusPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player == null) {
                return;
            }
            if (!payload.pos.closerToCenterThan(player.position(), 8.0D)) {
                return;
            }
            if (player.level().getBlockEntity(payload.pos) instanceof ModdingChairBlockEntity chair) {
                chair.setSearchRadius(payload.radius);
            }
        });
    }
}

package com.jerome.pleasesit.network;

import com.jerome.pleasesit.PleaseSitMod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                SetSearchRadiusPayload.TYPE,
                SetSearchRadiusPayload.STREAM_CODEC,
                SetSearchRadiusPayload::handle
        );
    }
}

package com.plusultra.client;

import com.plusultra.PlusUltra;
import com.plusultra.core.ModParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = PlusUltra.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientParticles {
    private ClientParticles() {}

    @SubscribeEvent
    public static void providers(RegisterParticleProvidersEvent event) {
        for (ModParticles.Def def : ModParticles.DEFS) {
            event.registerSpriteSet(ModParticles.type(def.id()), set -> new StyledParticle.Provider(set, def));
        }
    }
}

package com.plusultra.client;

import com.plusultra.PlusUltra;
import com.plusultra.core.ModEntities;
import com.plusultra.entity.VillainEntity.Kind;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = PlusUltra.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent event) {
        for (var k : ClientKeys.ABILITY) event.register(k);
        event.register(ClientKeys.TALENTS);
        event.register(ClientKeys.SWAP);
        event.register(ClientKeys.FUSION);
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (Kind k : Kind.values()) {
            event.registerLayerDefinition(VillainModel.layer(k), () -> VillainModel.createLayer(k));
        }
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.VILLAIN.get(), c -> new VillainRenderer(c, Kind.DECAY_LORD));
        event.registerEntityRenderer(ModEntities.NOMU.get(), c -> new VillainRenderer(c, Kind.NOMU));
        event.registerEntityRenderer(ModEntities.HERO_KILLER.get(), c -> new VillainRenderer(c, Kind.HERO_KILLER));
        event.registerEntityRenderer(ModEntities.BLUE_FLAME.get(), c -> new VillainRenderer(c, Kind.BLUE_FLAME));
        event.registerEntityRenderer(ModEntities.THUG.get(), c -> new VillainRenderer(c, Kind.THUG));
    }

    @SubscribeEvent
    public static void gui(RegisterGuiLayersEvent event) {
        event.registerAboveAll(PlusUltra.id("quirk_hud"), QuirkHud::render);
    }
}

package com.plusultra.client;

import com.plusultra.PlusUltra;
import com.plusultra.net.UseAbilityPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = PlusUltra.MODID, value = Dist.CLIENT)
public final class ClientGameEvents {
    private ClientGameEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientData.tick++;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        for (int i = 0; i < 3; i++) {
            while (ClientKeys.ABILITY[i].consumeClick()) {
                if (mc.screen == null) PacketDistributor.sendToServer(new UseAbilityPayload(i));
            }
        }
        while (ClientKeys.FUSION.consumeClick()) {
            if (mc.screen == null) PacketDistributor.sendToServer(new UseAbilityPayload(3));
        }
        while (ClientKeys.SWAP.consumeClick()) {
            if (mc.screen == null) PacketDistributor.sendToServer(new UseAbilityPayload(4));
        }
        while (ClientKeys.TALENTS.consumeClick()) {
            if (mc.screen == null && ClientData.hasQuirk()) mc.setScreen(new TalentScreen());
        }
    }
}

package com.vodmordia.enoughfolders.forge.client.event;

import com.vodmordia.enoughfolders.EnoughFoldersCommon;
import com.vodmordia.enoughfolders.client.event.ClientEventHandler;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge-side screen / tick event subscribers. Forwards every event into the
 * common-side {@link ClientEventHandler} and applies the returned cancel
 * verdict via {@code event.setCanceled(...)}.
 */
@Mod.EventBusSubscriber(modid = EnoughFoldersCommon.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeClientEventHandler {

    private ForgeClientEventHandler() {}

    @SubscribeEvent
    public static void onScreenOpened(ScreenEvent.Opening event) {
        ClientEventHandler.onScreenOpened(event.getScreen());
    }

    @SubscribeEvent
    public static void onScreenClosed(ScreenEvent.Closing event) {
        ClientEventHandler.onScreenClosed(event.getScreen());
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        // 1.19.2 Forge 43: ScreenEvent.Render.Post carries a PoseStack
        // (1.20.1 Forge 47 renamed this to getGuiGraphics()).
        ClientEventHandler.onScreenRender(
            event.getScreen(),
            event.getPoseStack(),
            event.getMouseX(),
            event.getMouseY(),
            event.getPartialTick());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onScreenMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        if (ClientEventHandler.onScreenMouseClicked(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (ClientEventHandler.onScreenKeyPressed(event.getScreen(), event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onScreenCharTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (ClientEventHandler.onScreenCharTyped(event.getScreen(), event.getCodePoint(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        ClientEventHandler.onClientPlayerLogin();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ClientEventHandler.onClientTick();
        }
    }
}

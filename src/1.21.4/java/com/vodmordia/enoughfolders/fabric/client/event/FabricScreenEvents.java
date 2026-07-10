package com.vodmordia.enoughfolders.fabric.client.event;

import com.vodmordia.enoughfolders.client.event.ClientEventHandler;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;

/**
 * Wires Fabric API screen events into the common-side
 * {@link ClientEventHandler} dispatcher.
 *
 * <p>The "allow" callbacks for mouse / keyboard input are how Fabric API
 * expresses event cancellation — return {@code false} to swallow the input.
 * The render and remove streams do not support cancellation, which we don't
 * need here either.
 *
 * <p>Fabric API does not expose a {@code charTyped} screen event, so that
 * one path is handled via a Mixin
 * ({@code com.vodmordia.enoughfolders.fabric.mixin.ScreenMixin}).
 */
public final class FabricScreenEvents {

    private FabricScreenEvents() {}

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ClientEventHandler.onScreenOpened(screen);

            ScreenEvents.afterRender(screen).register((scr, graphics, mouseX, mouseY, tickDelta) ->
                ClientEventHandler.onScreenRender(scr, graphics, mouseX, mouseY, tickDelta));

            ScreenEvents.remove(screen).register(ClientEventHandler::onScreenClosed);

            ScreenMouseEvents.allowMouseClick(screen).register((scr, mouseX, mouseY, button) ->
                !ClientEventHandler.onScreenMouseClicked(scr, mouseX, mouseY, button));

            ScreenKeyboardEvents.allowKeyPress(screen).register((scr, keyCode, scanCode, modifiers) ->
                !ClientEventHandler.onScreenKeyPressed(scr, keyCode, scanCode, modifiers));
        });
    }
}

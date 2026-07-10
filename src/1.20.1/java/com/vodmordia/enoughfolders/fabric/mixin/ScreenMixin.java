package com.vodmordia.enoughfolders.fabric.mixin;

import com.vodmordia.enoughfolders.client.event.ClientEventHandler;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Routes vanilla character input through the common-side
 * {@link ClientEventHandler} dispatcher. The natural target ({@code Screen.charTyped})
 * is a default interface method inherited from {@code ContainerEventHandler},
 * so it isn't directly mixin-targetable. Instead, intercept the upstream
 * dispatch site in {@link KeyboardHandler#charTyped}: if the common-side
 * handler swallows the char, cancel before vanilla forwards to the screen.
 *
 * <p>Fabric API exposes events for key press/release but not char typed,
 * which is why this mixin exists on the Fabric side only — Forge gives us
 * the equivalent via {@code ScreenEvent.CharacterTyped.Pre}.
 */
@Mixin(KeyboardHandler.class)
public abstract class ScreenMixin {

    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "charTyped(JII)V", at = @At("HEAD"), cancellable = true)
    private void enoughfolders$charTyped(long window, int codePoint, int modifiers, CallbackInfo ci) {
        if (window != this.minecraft.getWindow().getWindow()) {
            return;
        }
        Screen screen = this.minecraft.screen;
        if (screen == null) {
            return;
        }
        if (ClientEventHandler.onScreenCharTyped(screen, (char) codePoint, modifiers)) {
            ci.cancel();
        }
    }
}

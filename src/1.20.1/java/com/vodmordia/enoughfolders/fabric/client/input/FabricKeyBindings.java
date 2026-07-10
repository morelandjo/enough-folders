package com.vodmordia.enoughfolders.fabric.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.vodmordia.enoughfolders.EnoughFoldersCommon;
import com.vodmordia.enoughfolders.client.input.JEIAddToFolderHandler;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.KeyMapping;

/**
 * Fabric keybinding registration for "add hovered JEI ingredient to active
 * folder".
 *
 * <p>Two Fabric-specific deviations from the Forge build:
 *
 * <ul>
 *   <li>Default binding is {@link InputConstants#UNKNOWN} (unbound). The
 *       Forge build defaults to {@code Shift+A} via {@code KeyModifier.SHIFT}
 *       + {@code KeyConflictContext.GUI}; neither concept exists in Fabric's
 *       vanilla {@link KeyMapping}. Defaulting to plain {@code A} would
 *       silently collide with JEI's bookmark hotkey, so we leave it unbound
 *       and let the user pick a non-conflicting key in the controls menu.
 *   <li>Dispatch happens in {@link ScreenKeyboardEvents#allowKeyPress},
 *       returning {@code false} on match to cancel propagation. The earlier
 *       implementation used {@code afterKeyPress}, which is skipped when
 *       another mod's screen handler consumes the key first (e.g. JEI for
 *       its bookmark hotkey), so our handler never fired for the default key.
 * </ul>
 */
public final class FabricKeyBindings {

    private static final String CATEGORY = "key.categories." + EnoughFoldersCommon.MOD_ID;

    public static final KeyMapping ADD_TO_FOLDER = new KeyMapping(
            "key." + EnoughFoldersCommon.MOD_ID + ".add_to_folder",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );

    private FabricKeyBindings() {}

    public static void init() {
        EnoughFoldersCommon.LOGGER.info("Registering keybindings (Fabric)");
        KeyBindingHelper.registerKeyBinding(ADD_TO_FOLDER);

        ScreenEvents.AFTER_INIT.register((client, screen, w, h) ->
            ScreenKeyboardEvents.allowKeyPress(screen).register((scr, keyCode, scanCode, modifiers) -> {
                if (ADD_TO_FOLDER.matches(keyCode, scanCode)) {
                    JEIAddToFolderHandler.handleAddToFolderKeyPress();
                    return false;
                }
                return true;
            }));
    }
}

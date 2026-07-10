package com.vodmordia.enoughfolders.forge.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.vodmordia.enoughfolders.EnoughFoldersCommon;
import com.vodmordia.enoughfolders.client.input.JEIAddToFolderHandler;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Forge keybinding registration. Mirrors the original NeoForge setup —
 * GUI-only conflict context + Shift modifier on the add-to-folder key.
 */
public final class ForgeKeyBindings {

    private static final String CATEGORY = "key.categories." + EnoughFoldersCommon.MOD_ID;

    public static final KeyMapping ADD_TO_FOLDER = new KeyMapping(
            "key." + EnoughFoldersCommon.MOD_ID + ".add_to_folder",
            KeyConflictContext.GUI,
            KeyModifier.SHIFT,
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_A),
            CATEGORY
    );

    private ForgeKeyBindings() {}

    public static void init() {
        EnoughFoldersCommon.LOGGER.info("Registering keyboard event handler (Forge)");
        MinecraftForge.EVENT_BUS.register(KeyInputHandler.class);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        EnoughFoldersCommon.LOGGER.info("Registering add to folder key mapping (Forge)");
        event.register(ADD_TO_FOLDER);
    }

    public static final class KeyInputHandler {
        private KeyInputHandler() {}

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (event.getAction() != GLFW.GLFW_PRESS) {
                return;
            }
            InputConstants.Key key = InputConstants.getKey(event.getKey(), event.getScanCode());
            if (ADD_TO_FOLDER.isActiveAndMatches(key)) {
                JEIAddToFolderHandler.handleAddToFolderKeyPress();
            }
        }
    }
}

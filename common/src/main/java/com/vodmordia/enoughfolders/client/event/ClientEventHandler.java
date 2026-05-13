package com.vodmordia.enoughfolders.client.event;

import com.vodmordia.enoughfolders.EnoughFoldersCommon;
import com.vodmordia.enoughfolders.client.gui.FolderScreen;
import com.vodmordia.enoughfolders.integrations.jei.drag.managers.RecipeGuiManager;
import com.vodmordia.enoughfolders.integrations.jei.gui.handlers.JEIRecipeGuiHandler;

import mezz.jei.api.runtime.IRecipesGui;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Common-side dispatcher for the folder overlay. Platform-specific event
 * subscribers (Forge {@code ScreenEvent.*}, Fabric {@code ScreenEvents.*})
 * forward into the static methods here. Methods that may want to cancel the
 * triggering platform event return {@code boolean} (true = cancel).
 *
 * <p>Owns the per-container-screen {@link FolderScreen} overlay map and the
 * IRecipesGui-overlay dispatch that used to live in a separate
 * JEIRecipeScreenRenderer event class.
 */
@Environment(EnvType.CLIENT)
public final class ClientEventHandler {
    private static final Map<AbstractContainerScreen<?>, FolderScreen> FOLDER_SCREENS = new HashMap<>();

    // Track recipe-screen size to detect resize → forces folder screen reinit.
    private static int lastRecipeScreenWidth = 0;
    private static int lastRecipeScreenHeight = 0;

    private ClientEventHandler() {}

    /**
     * Looks up the folder overlay attached to the given inventory screen.
     */
    public static Optional<FolderScreen> getFolderScreen(AbstractContainerScreen<?> screen) {
        return Optional.ofNullable(FOLDER_SCREENS.get(screen));
    }

    /**
     * Called when any screen opens. Builds a new folder overlay for container
     * screens; no-op for everything else.
     */
    public static void onScreenOpened(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return;
        }
        EnoughFoldersCommon.LOGGER.debug("Container screen opened: {}", containerScreen.getClass().getName());

        FolderScreen folderScreen = new FolderScreen(containerScreen);
        FOLDER_SCREENS.put(containerScreen, folderScreen);

        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        folderScreen.init(width, height);

        EnoughFoldersCommon.LOGGER.debug("Folder screen initialized with width: {}, height: {}", width, height);
    }

    /**
     * Called when any screen closes. Tears down the folder overlay for
     * container screens, and clears the saved recipe-screen overlay unless
     * we just transitioned into a JEI recipe view (which deliberately keeps
     * it alive).
     */
    public static void onScreenClosed(Screen screen) {
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            FOLDER_SCREENS.remove(containerScreen);
        }

        // If we're closing the inventory because JEI is about to open a recipe
        // GUI, JEIRecipeGuiHandler.saveLastFolderScreen has just set a flag —
        // keep the saved overlay so it can render over the recipe view. Any
        // other close path (Escape, world unload, etc.) clears it.
        if (!JEIRecipeGuiHandler.consumeTransitioningToRecipe()) {
            JEIRecipeGuiHandler.clearLastFolderScreen();
        }
    }

    /**
     * Called after the screen has rendered. Reinitializes the overlay if the
     * window size changed, then draws it.
     */
    public static void onScreenRender(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            FolderScreen folderScreen = FOLDER_SCREENS.get(containerScreen);
            if (folderScreen != null) {
                Minecraft minecraft = Minecraft.getInstance();
                folderScreen.reinitIfNeeded(
                    minecraft.getWindow().getGuiScaledWidth(),
                    minecraft.getWindow().getGuiScaledHeight());
                folderScreen.render(graphics, mouseX, mouseY, partialTick);
            }
            return;
        }

        if (screen instanceof IRecipesGui) {
            int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            if (screenWidth != lastRecipeScreenWidth || screenHeight != lastRecipeScreenHeight) {
                JEIRecipeGuiHandler.reinitLastFolderScreen();
                lastRecipeScreenWidth = screenWidth;
                lastRecipeScreenHeight = screenHeight;
            }
            JEIRecipeGuiHandler.getLastFolderScreen().ifPresent(folderScreen ->
                folderScreen.render(graphics, mouseX, mouseY, partialTick));
        }
    }

    /**
     * Called on screen mouse-click. Returns true if the click was consumed
     * by the folder overlay and the platform-level event should be cancelled.
     */
    public static boolean onScreenMouseClicked(Screen screen, double mouseX, double mouseY, int button) {
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            FolderScreen folderScreen = FOLDER_SCREENS.get(containerScreen);
            if (folderScreen != null && folderScreen.isVisible(mouseX, mouseY)) {
                return folderScreen.mouseClicked(mouseX, mouseY, button);
            }
            return false;
        }

        if (screen instanceof IRecipesGui && JEIRecipeGuiHandler.getLastFolderScreen().isPresent()) {
            return RecipeGuiManager.handleMouseClick(screen, mouseX, mouseY, button);
        }
        return false;
    }

    /**
     * Called on screen key-press. Returns true to cancel.
     */
    public static boolean onScreenKeyPressed(Screen screen, int keyCode, int scanCode, int modifiers) {
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            FolderScreen folderScreen = FOLDER_SCREENS.get(containerScreen);
            if (folderScreen == null) {
                return false;
            }
            // While typing a new folder name, swallow E and Escape so vanilla
            // doesn't close the inventory while the EditBox has focus.
            if (folderScreen.isAddingFolder() && folderScreen.isInputFocused()
                && (keyCode == GLFW.GLFW_KEY_E || keyCode == GLFW.GLFW_KEY_ESCAPE)) {
                return true;
            }
            return folderScreen.keyPressed(keyCode, scanCode, modifiers);
        }

        if (screen instanceof IRecipesGui) {
            return JEIRecipeGuiHandler.getLastFolderScreen()
                .map(fs -> fs.keyPressed(keyCode, scanCode, modifiers))
                .orElse(false);
        }
        return false;
    }

    /**
     * Called on screen character-typed. Returns true to cancel.
     */
    public static boolean onScreenCharTyped(Screen screen, char codePoint, int modifiers) {
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            FolderScreen folderScreen = FOLDER_SCREENS.get(containerScreen);
            return folderScreen != null && folderScreen.charTyped(codePoint, modifiers);
        }

        if (screen instanceof IRecipesGui) {
            return JEIRecipeGuiHandler.getLastFolderScreen()
                .map(fs -> fs.charTyped(codePoint, modifiers))
                .orElse(false);
        }
        return false;
    }

    /**
     * Called when the client player has fully logged in. Reloads folder data
     * for the new world. Must be invoked on (or marshalled to) the main thread.
     */
    public static void onClientPlayerLogin() {
        EnoughFoldersCommon.LOGGER.debug("Client player fully logged in, world is now ready");
        Minecraft.getInstance().execute(() -> EnoughFoldersCommon.getFolderManager().reloadFolders());
    }

    /**
     * Called each client tick. Clears the per-screen overlay map when the
     * client leaves the world, so it doesn't leak stale references.
     */
    public static void onClientTick() {
        if (Minecraft.getInstance().level == null && !FOLDER_SCREENS.isEmpty()) {
            FOLDER_SCREENS.clear();
        }
    }
}

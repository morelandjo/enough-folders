package com.vodmordia.enoughfolders.integrations.jei.gui.handlers;
import com.vodmordia.enoughfolders.EnoughFoldersCommon;

import com.vodmordia.enoughfolders.client.gui.FolderScreen;
import mezz.jei.api.runtime.IRecipesGui;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.Optional;

/**
 * Static state holder for the {@link FolderScreen} that should overlay JEI's
 * recipe GUI. Populated when the user opens recipes from inside the overlay,
 * cleared when the inventory screen is closed without transitioning to JEI.
 */
@Environment(EnvType.CLIENT)
public class JEIRecipeGuiHandler {

    private JEIRecipeGuiHandler() {}

    /**
     * Set when {@link #saveLastFolderScreen} runs (just before JEI opens its
     * recipe GUI). The next call to {@link #consumeTransitioningToRecipe}
     * reads and clears it. Lets {@code ClientEventHandler.onScreenClosed}
     * tell "user closed inventory because we're showing a recipe" from
     * "user closed inventory for any other reason" without walking the
     * thread stack for {@code mezz.jei} class names.
     */
    private static boolean transitioningToRecipe = false;

    /**
     * Reads and clears the recipe-transition flag. Returns true exactly
     * once after each {@link #saveLastFolderScreen} call.
     */
    public static boolean consumeTransitioningToRecipe() {
        boolean v = transitioningToRecipe;
        transitioningToRecipe = false;
        return v;
    }

    // Keep track of the last used folder screen
    private static FolderScreen lastFolderScreen = null;
    // Keep track if the folder screen has been initialized for the recipe screen
    private static boolean folderScreenInitialized = false;
    // Keep track of the last screen size to detect changes
    private static int lastScreenWidth = 0;
    private static int lastScreenHeight = 0;

    /**
     * Save the last active folder screen before opening a recipe view
     * @param folderScreen The folder screen to save
     */
    public static void saveLastFolderScreen(FolderScreen folderScreen) {
        lastFolderScreen = folderScreen;
        folderScreenInitialized = false;
        transitioningToRecipe = true;
        EnoughFoldersCommon.LOGGER.debug("Saved folder screen for recipe view: {}x{} at {},{}",
            folderScreen.getScreenArea().getWidth(), folderScreen.getScreenArea().getHeight(),
            folderScreen.getScreenArea().getX(), folderScreen.getScreenArea().getY());
    }

    /**
     * Clear the saved folder screen when no longer needed
     */
    public static void clearLastFolderScreen() {
        if (lastFolderScreen != null) {
            EnoughFoldersCommon.LOGGER.debug("Clearing saved folder screen");
        }
        lastFolderScreen = null;
        folderScreenInitialized = false;
        lastScreenWidth = 0;
        lastScreenHeight = 0;
    }

    /**
     * Force a reinitialization of the folder screen with current dimensions.
     */
    public static void reinitLastFolderScreen() {
        if (lastFolderScreen == null) {
            return;
        }

        // Get current screen dimensions
        Screen currentScreen = Minecraft.getInstance().screen;
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        // Only reinit if the screen is a JEI recipe GUI
        if (currentScreen instanceof IRecipesGui) {
            EnoughFoldersCommon.LOGGER.debug("Forcing reinitialization of folder screen with dimensions {}x{}",
                screenWidth, screenHeight);

            // Reset the initialization flag to force a full reinit
            folderScreenInitialized = false;

            // Initialize the folder screen with current dimensions
            lastFolderScreen.init(screenWidth, screenHeight);
            folderScreenInitialized = true;
            lastScreenWidth = screenWidth;
            lastScreenHeight = screenHeight;
        }
    }

    /**
     * Get the currently saved folder screen, initializing it for the recipe screen if needed
     */
    public static Optional<FolderScreen> getLastFolderScreen() {
        if (lastFolderScreen == null) {
            return Optional.empty();
        }

        // Get current screen and dimensions
        Screen currentScreen = Minecraft.getInstance().screen;
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        // Initialize the folder screen for the recipe GUI if needed or if screen dimensions changed
        if (currentScreen instanceof IRecipesGui &&
            (!folderScreenInitialized || screenWidth != lastScreenWidth || screenHeight != lastScreenHeight)) {

            // Log the current screen type to help with debugging
            EnoughFoldersCommon.LOGGER.debug("Current screen type for JEI exclusion areas: {}", currentScreen.getClass().getName());

            lastFolderScreen.init(screenWidth, screenHeight);
            folderScreenInitialized = true;
            lastScreenWidth = screenWidth;
            lastScreenHeight = screenHeight;
        }

        return Optional.of(lastFolderScreen);
    }
}
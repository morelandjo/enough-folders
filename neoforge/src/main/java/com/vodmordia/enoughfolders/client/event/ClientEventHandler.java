package com.vodmordia.enoughfolders.client.event;
import com.vodmordia.enoughfolders.EnoughFoldersCommon;

import com.vodmordia.enoughfolders.neoforge.EnoughFolders;
import com.vodmordia.enoughfolders.client.gui.FolderScreen;
import com.vodmordia.enoughfolders.integrations.jei.core.JEIIntegration;
import com.vodmordia.enoughfolders.integrations.jei.gui.handlers.JEIRecipeGuiHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Handles client-side events.
 */
@EventBusSubscriber(modid = EnoughFoldersCommon.MOD_ID, value = Dist.CLIENT, bus = Bus.GAME)
public class ClientEventHandler {
    private static final Map<AbstractContainerScreen<?>, FolderScreen> FOLDER_SCREENS = new HashMap<>();
    
    /**
     * Gets the FolderScreen associated with the given container screen.
     * 
     * @param screen The container screen to get the folder screen for
     * @return Optional containing the folder screen if it exists, or empty if none exists for the given screen
     */
    public static Optional<FolderScreen> getFolderScreen(AbstractContainerScreen<?> screen) {
        return Optional.ofNullable(FOLDER_SCREENS.get(screen));
    }
    
    /**
     * Event handler for screen opening.
     * 
     * @param event The screen opening event
     */
    @SubscribeEvent
    public static void onScreenOpened(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            EnoughFoldersCommon.LOGGER.debug("Container screen opened: {}", containerScreen.getClass().getName());
            
            // Create a folder screen for container screen
            FOLDER_SCREENS.put(containerScreen, new FolderScreen(containerScreen));
            
            // Initialize the folder screen
            Minecraft minecraft = Minecraft.getInstance();
            int width = minecraft.getWindow().getGuiScaledWidth();
            int height = minecraft.getWindow().getGuiScaledHeight();
            FOLDER_SCREENS.get(containerScreen).init(width, height);
            
            EnoughFoldersCommon.LOGGER.debug("Folder screen initialized with width: {}, height: {}", width, height);
        }
    }
    
    /**
     * Event handler for screen closing.
     * 
     * @param event The screen closing event
     */
    @SubscribeEvent
    public static void onScreenClosed(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
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
     * Event handler for screen rendering (post). Reinitializes the folder
     * screen if the window size changed, then draws the overlay.
     *
     * @param event The screen render post event
     */
    @SubscribeEvent
    public static void onScreenDrawForeground(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            FolderScreen folderScreen = FOLDER_SCREENS.get(containerScreen);
            if (folderScreen != null) {
                Minecraft minecraft = Minecraft.getInstance();
                folderScreen.reinitIfNeeded(
                    minecraft.getWindow().getGuiScaledWidth(),
                    minecraft.getWindow().getGuiScaledHeight());
                folderScreen.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
            }
        }
    }
    
    /**
     * Event handler for mouse clicks on screens.
     * 
     * @param event The mouse button pressed pre event
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onScreenMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            FolderScreen folderScreen = FOLDER_SCREENS.get(containerScreen);
            if (folderScreen != null && folderScreen.isVisible(event.getMouseX(), event.getMouseY())) {
                if (folderScreen.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) {
                    event.setCanceled(true);
                }
            }
        }
        else if (event.getScreen() instanceof mezz.jei.api.runtime.IRecipesGui &&
                JEIRecipeGuiHandler.getLastFolderScreen().isPresent()) {
            boolean handled = com.vodmordia.enoughfolders.integrations.jei.drag.managers.RecipeGuiManager.handleMouseClick(
                event.getScreen(), event.getMouseX(), event.getMouseY(), event.getButton());
            if (handled) {
                event.setCanceled(true);
            }
        }
    }
    
   
    
    /**
     * Event handler for keyboard input.
     * 
     * @param event The key pressed pre event
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            FolderScreen folderScreen = FOLDER_SCREENS.get(containerScreen);
            if (folderScreen != null) {
                // If input box is active (adding folder mode and input box focused),
                // cancel all key events to prevent inventory from closing
                // While typing a new folder name, swallow E and Escape so vanilla
                // doesn't close the inventory while the EditBox has focus.
                if (folderScreen.isAddingFolder() && folderScreen.isInputFocused()) {
                    if (event.getKeyCode() == GLFW.GLFW_KEY_E || event.getKeyCode() == GLFW.GLFW_KEY_ESCAPE) {
                        event.setCanceled(true);
                    }
                }

                // Process the key event in the folder screen
                if (folderScreen.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
                    event.setCanceled(true);
                }
            }
        }
    }

    /**
     * Event handler for character typed events.
     *
     * @param event The character typed pre event
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onScreenCharTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            FolderScreen folderScreen = FOLDER_SCREENS.get(containerScreen);
            if (folderScreen != null) {
                if (folderScreen.charTyped(event.getCodePoint(), event.getModifiers())) {
                    event.setCanceled(true);
                }
            }
        }
    }
    
    /**
     * Event handler for when the client player fully joins a world.
     * 
     * @param event The client player network logging in event
     */
    @SubscribeEvent
    public static void onClientPlayerLogin(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) {
        EnoughFoldersCommon.LOGGER.debug("ClientEventHandler: Client player fully logged in, world is now ready"
        );
        
        Minecraft.getInstance().execute(() -> {
            EnoughFolders.getInstance().getFolderManager().reloadFolders();
        });
    }
    
    /**
     * Event handler for client tick events.
     * 
     * @param event The client tick post event
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        // Static state moved out to avoid potential issues
        Minecraft minecraft = Minecraft.getInstance();
        
        // Clear screens when leaving a world
        if (minecraft.level == null && !FOLDER_SCREENS.isEmpty()) {
            FOLDER_SCREENS.clear();
        }
    }
}

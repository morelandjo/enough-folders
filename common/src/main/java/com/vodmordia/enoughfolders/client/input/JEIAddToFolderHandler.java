package com.vodmordia.enoughfolders.client.input;
import com.vodmordia.enoughfolders.EnoughFoldersCommon;

import com.vodmordia.enoughfolders.client.event.ClientEventHandler;
import com.vodmordia.enoughfolders.client.gui.FolderScreen;
import com.vodmordia.enoughfolders.data.Folder;
import com.vodmordia.enoughfolders.data.FolderManager;
import com.vodmordia.enoughfolders.data.StoredIngredient;
import com.vodmordia.enoughfolders.integrations.jei.core.JEIIntegration;
import com.vodmordia.enoughfolders.integrations.jei.gui.handlers.JEIRecipeGuiHandler;

import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.runtime.IRecipesGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * Handles the "add hovered JEI ingredient to active folder" keybind.
 */
public class JEIAddToFolderHandler {

    private JEIAddToFolderHandler() {}

    /**
     * Handles the key press to add the JEI ingredient currently under the
     * mouse to the active folder. No-op if there is no player, no active
     * folder, or no JEI runtime.
     */
    public static void handleAddToFolderKeyPress() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        FolderManager folderManager = EnoughFoldersCommon.getFolderManager();
        Optional<Folder> activeFolder = folderManager.getActiveFolder();
        if (activeFolder.isEmpty()) {
            player.displayClientMessage(Component.translatable("enoughfolders.message.no_active_folder"), false);
            return;
        }

        JEIIntegration.get().getJeiRuntime().ifPresent(jeiRuntime ->
            tryAddIngredientFromJei(jeiRuntime, activeFolder.get(), folderManager, player));
    }

    private static void tryAddIngredientFromJei(IJeiRuntime jeiRuntime, Folder folder, FolderManager folderManager, LocalPlayer player) {
        if (!jeiRuntime.getIngredientListOverlay().isListDisplayed()) {
            player.displayClientMessage(Component.translatable("enoughfolders.message.jei_not_visible"), false);
            return;
        }

        Optional<ITypedIngredient<?>> ingredient = jeiRuntime.getIngredientListOverlay().getIngredientUnderMouse();
        if (ingredient.isEmpty()) {
            ingredient = jeiRuntime.getBookmarkOverlay().getIngredientUnderMouse();
        }
        if (ingredient.isEmpty()) {
            player.displayClientMessage(Component.translatable("enoughfolders.message.no_ingredient_under_cursor"), false);
            return;
        }

        Object rawIngredient = ingredient.get().getIngredient();
        Optional<StoredIngredient> stored = JEIIntegration.get().storeIngredient(rawIngredient);
        if (stored.isEmpty()) {
            EnoughFoldersCommon.LOGGER.error("ADD_TO_FOLDER: storeIngredient returned empty for {}", rawIngredient.getClass().getName());
            return;
        }

        folderManager.addIngredient(folder, stored.get());
        // Refresh the open folder UI so the new ingredient appears immediately —
        // FolderManager only updates the data; the FolderScreen needs to rebuild
        // its slot list. The drag-drop path does this via notifyTarget.onIngredientAdded;
        // the keybind path has to look the screen up itself.
        currentFolderScreen().ifPresent(FolderScreen::onIngredientAdded);
    }

    /**
     * Finds the FolderScreen overlay attached to whichever screen is currently open:
     * looks it up via {@link ClientEventHandler#getFolderScreen} for inventory screens,
     * or via {@link JEIRecipeGuiHandler#getLastFolderScreen} for JEI's recipe GUI.
     */
    private static Optional<FolderScreen> currentFolderScreen() {
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            return ClientEventHandler.getFolderScreen(containerScreen);
        }
        if (screen instanceof IRecipesGui) {
            return JEIRecipeGuiHandler.getLastFolderScreen();
        }
        return Optional.empty();
    }
}

package com.vodmordia.enoughfolders.integrations.jei.core;
import com.vodmordia.enoughfolders.EnoughFoldersCommon;

import com.vodmordia.enoughfolders.client.event.ClientEventHandler;
import com.vodmordia.enoughfolders.integrations.jei.gui.handlers.FolderGhostHandler;
import com.vodmordia.enoughfolders.integrations.jei.gui.handlers.FolderScreenHandler;
import com.vodmordia.enoughfolders.integrations.jei.gui.handlers.JEIRecipeGuiHandler;
import com.vodmordia.enoughfolders.integrations.jei.gui.handlers.RecipeScreenHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.runtime.IRecipesGui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * JEI Plugin for Enough Folders.
 */
@JeiPlugin
@Environment(EnvType.CLIENT)
public class JEIPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_ID = new ResourceLocation(EnoughFoldersCommon.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        JEIIntegration.get().setJeiRuntime(jeiRuntime);
        EnoughFoldersCommon.LOGGER.info("JEI runtime available; integration active");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // Inventory screens: exclude the folder overlay from JEI's sidebar,
        // and accept ghost-ingredient drops onto folders.
        registration.addGuiContainerHandler((Class) AbstractContainerScreen.class, new FolderScreenHandler());
        registration.addGhostIngredientHandler(
            (Class) AbstractContainerScreen.class,
            new FolderGhostHandler<AbstractContainerScreen<?>>(ClientEventHandler::getFolderScreen));

        // Recipe screens: same, but the folder overlay is held in
        // JEIRecipeGuiHandler's static state instead of the per-screen map.
        registration.addGlobalGuiHandler(new RecipeScreenHandler());
        Optional<Class<? extends Screen>> recipesGuiClass = resolveRecipesGuiClass();
        recipesGuiClass.ifPresent(cls -> registration.addGhostIngredientHandler(
            (Class) cls,
            new FolderGhostHandler<Screen>(s -> JEIRecipeGuiHandler.getLastFolderScreen())));
    }

    /**
     * JEI's {@code RecipesGui} class is in {@code mezz.jei.gui.*}, not the
     * public {@code mezz.jei.api.*} surface — the only public handle is the
     * {@link IRecipesGui} interface, which can't be passed to
     * {@code addGhostIngredientHandler}. Resolve the concrete class
     * reflectively at register time.
     */
    @SuppressWarnings("unchecked")
    private static Optional<Class<? extends Screen>> resolveRecipesGuiClass() {
        try {
            Class<?> cls = Class.forName("mezz.jei.gui.recipes.RecipesGui");
            if (Screen.class.isAssignableFrom(cls) && IRecipesGui.class.isAssignableFrom(cls)) {
                return Optional.of((Class<? extends Screen>) cls);
            }
            EnoughFoldersCommon.LOGGER.warn("Found mezz.jei.gui.recipes.RecipesGui but it is not a Screen+IRecipesGui");
        } catch (ClassNotFoundException e) {
            EnoughFoldersCommon.LOGGER.warn("Could not find JEI's RecipesGui class; recipe-screen drag-drop disabled");
        }
        return Optional.empty();
    }
}

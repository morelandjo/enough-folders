package com.vodmordia.enoughfolders.integrations.jei.gui.handlers;

import com.vodmordia.enoughfolders.client.gui.FolderScreen;
import com.vodmordia.enoughfolders.integrations.jei.core.JEIIntegration;
import com.vodmordia.enoughfolders.integrations.jei.gui.targets.FolderButtonTarget;
import com.vodmordia.enoughfolders.integrations.jei.gui.targets.FolderGhostIngredientTarget;
import com.vodmordia.enoughfolders.integrations.jei.gui.targets.FolderTargetFactory;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Generic JEI ghost-ingredient handler. Parameterized by how to find the
 * {@link FolderScreen} attached to a given {@link Screen} subtype, so the
 * same logic serves both inventory screens (lookup via the per-screen map)
 * and JEI's recipe GUI (lookup via the saved-screen state).
 */
@OnlyIn(Dist.CLIENT)
public class FolderGhostHandler<S extends Screen> implements IGhostIngredientHandler<S>, FolderGhostIngredientTarget {
    private final Function<S, Optional<FolderScreen>> folderScreenLookup;
    private S currentScreen;

    public FolderGhostHandler(Function<S, Optional<FolderScreen>> folderScreenLookup) {
        this.folderScreenLookup = folderScreenLookup;
    }

    @Override
    @Nonnull
    public <I> List<Target<I>> getTargetsTyped(@Nonnull S gui, @Nonnull ITypedIngredient<I> ingredient, boolean doStart) {
        currentScreen = gui;
        if (doStart) {
            JEIIntegration.get().setCurrentDraggedObject(ingredient.getIngredient());
        }
        return folderScreenLookup.apply(gui)
            .map(fs -> FolderTargetFactory.<I>createAllTargets(fs, this))
            .orElseGet(ArrayList::new);
    }

    @Override
    public void onComplete() {
        JEIIntegration.get().clearCurrentDraggedObject();
    }

    @Override
    public boolean shouldHighlightTargets() {
        // Only let JEI paint its target-highlight overlay when an actual drag is
        // in progress — JEI also queries this during pre-drag hover previews,
        // which would otherwise turn the folder backgrounds green just from
        // hovering over a JEI ingredient.
        return JEIIntegration.get().getDraggedIngredient().isPresent();
    }

    @Override
    public Rect2i getContentDropArea() {
        return currentFolderScreen()
            .map(FolderScreen::getContentDropArea)
            .orElseGet(() -> new Rect2i(0, 0, 0, 0));
    }

    @Override
    public List<FolderButtonTarget> getFolderButtonTargets() {
        return currentFolderScreen()
            .<List<FolderButtonTarget>>map(FolderScreen::getFolderButtonTargets)
            .orElseGet(ArrayList::new);
    }

    @Override
    public void onIngredientAdded() {
        currentFolderScreen().ifPresent(FolderScreen::onIngredientAdded);
    }

    private Optional<FolderScreen> currentFolderScreen() {
        return currentScreen == null ? Optional.empty() : folderScreenLookup.apply(currentScreen);
    }
}

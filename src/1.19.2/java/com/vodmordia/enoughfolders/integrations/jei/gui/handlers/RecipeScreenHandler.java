package com.vodmordia.enoughfolders.integrations.jei.gui.handlers;

import com.vodmordia.enoughfolders.client.gui.FolderScreen;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.runtime.IRecipesGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.Rect2i;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/**
 * Reports the folder overlay's bounds to JEI as an excluded area while
 * a recipe GUI is open, so JEI doesn't overlap the overlay there either.
 */
@Environment(EnvType.CLIENT)
public class RecipeScreenHandler implements IGlobalGuiHandler {
    private static final int BUFFER = 2;

    @Override
    public Collection<Rect2i> getGuiExtraAreas() {
        Screen currentScreen = Minecraft.getInstance().screen;
        if (!(currentScreen instanceof IRecipesGui)) {
            return new ArrayList<>();
        }
        Optional<FolderScreen> folderScreenOpt = JEIRecipeGuiHandler.getLastFolderScreen();
        if (folderScreenOpt.isEmpty()) {
            return new ArrayList<>();
        }
        Rect2i a = folderScreenOpt.get().getScreenArea();
        Collection<Rect2i> areas = new ArrayList<>();
        areas.add(new Rect2i(
            a.getX() - BUFFER,
            a.getY() - BUFFER,
            a.getWidth() + BUFFER * 2,
            a.getHeight() + BUFFER * 2));
        return areas;
    }
}

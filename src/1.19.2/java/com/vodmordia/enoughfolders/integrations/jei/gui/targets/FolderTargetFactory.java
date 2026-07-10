package com.vodmordia.enoughfolders.integrations.jei.gui.targets;
import com.vodmordia.enoughfolders.EnoughFoldersCommon;

import com.vodmordia.enoughfolders.client.gui.FolderScreen;
import com.vodmordia.enoughfolders.data.Folder;
import com.vodmordia.enoughfolders.integrations.jei.core.JEIIntegration;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler.Target;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.Rect2i;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for {@link Target}s exposed to JEI's ghost-ingredient drag system.
 */
@Environment(EnvType.CLIENT)
public final class FolderTargetFactory {

    private FolderTargetFactory() {}

    /**
     * Creates the full set of drop targets for a folder screen: one per
     * folder button, plus (if a folder is active) one for the content grid.
     */
    public static <I> List<Target<I>> createAllTargets(FolderScreen folderScreen, FolderGhostIngredientTarget notifyTarget) {
        List<Target<I>> targets = new ArrayList<>();

        EnoughFoldersCommon.getFolderManager().getActiveFolder().ifPresent(folder ->
            targets.add(createTarget(folderScreen.getContentDropArea(), folder, notifyTarget)));

        for (FolderButtonTarget folderTarget : folderScreen.getFolderButtonTargets()) {
            targets.add(createTarget(folderTarget.getArea(), folderTarget.getFolder(), notifyTarget));
        }
        return targets;
    }

    private static <I> Target<I> createTarget(Rect2i area, Folder folder, FolderGhostIngredientTarget notifyTarget) {
        return new Target<I>() {
            @Override
            public Rect2i getArea() {
                return area;
            }

            @Override
            public void accept(I ingredientObj) {
                JEIIntegration.get().storeIngredient(ingredientObj).ifPresent(stored -> {
                    try {
                        EnoughFoldersCommon.getFolderManager().addIngredient(folder, stored);
                        notifyTarget.onIngredientAdded();
                        EnoughFoldersCommon.LOGGER.debug("Added ingredient to folder: {}", folder.getName());
                    } catch (Exception e) {
                        EnoughFoldersCommon.LOGGER.error("Error adding ingredient to folder", e);
                    }
                });
            }
        };
    }
}

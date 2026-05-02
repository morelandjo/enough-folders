package com.vodmordia.enoughfolders.integrations.jei.gui.handlers;

import com.vodmordia.enoughfolders.client.event.ClientEventHandler;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Reports the folder overlay's bounds to JEI as an excluded area, so JEI's
 * sidebar avoids overlapping it.
 */
@OnlyIn(Dist.CLIENT)
public class FolderScreenHandler implements IGuiContainerHandler<AbstractContainerScreen<?>> {
    private static final int BUFFER = 2;

    @Override
    @Nonnull
    public List<Rect2i> getGuiExtraAreas(@Nonnull AbstractContainerScreen<?> screen) {
        List<Rect2i> areas = new ArrayList<>();
        ClientEventHandler.getFolderScreen(screen).ifPresent(folderScreen -> {
            Rect2i a = folderScreen.getScreenArea();
            areas.add(new Rect2i(
                a.getX() - BUFFER,
                a.getY() - BUFFER,
                a.getWidth() + BUFFER * 2,
                a.getHeight() + BUFFER * 2));
        });
        return areas;
    }
}

package com.vodmordia.enoughfolders.client.gui;
import com.vodmordia.enoughfolders.EnoughFoldersCommon;

import com.vodmordia.enoughfolders.data.Folder;
import com.vodmordia.enoughfolders.data.StoredIngredient;
import com.vodmordia.enoughfolders.integrations.jei.core.JEIIntegration;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * Button that represents a folder in the GUI.
 */
public class FolderButton extends Button {
    /**
     * Resource location for the folder button textures
     */
    private static final ResourceLocation TEXTURE = new ResourceLocation(EnoughFoldersCommon.MOD_ID, "textures/gui/folders_gray.png");
    
    /**
     * The folder that this button represents
     */
    private final Folder folder;

    /**
     * Cached pixel width of the short label, computed lazily on first paint.
     * Buttons are rebuilt by {@code FolderButtonManager.initFolderButtons}
     * whenever folder state changes, so the cache lives for one button's
     * lifetime without needing explicit invalidation.
     */
    private int cachedShortNameWidth = -1;
    
    /**
     * Creates a new folder button.
     *
     * @param x The x position of the button
     * @param y The y position of the button
     * @param width The width of the button
     * @param height The height of the button
     * @param folder The folder that this button represents
     * @param onPress The action to perform when the button is pressed
     */
    public FolderButton(int x, int y, int width, int height, Folder folder, OnPress onPress) {
        super(x, y, width, height, Component.literal(folder.getShortName()), onPress, DEFAULT_NARRATION);
        this.folder = folder;
    }
    
    /**
     * Renders the folder button.
     *
     * @param guiGraphics The graphics context to render with
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     * @param partialTick The partial tick time
     */
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.setShaderTexture(0, TEXTURE);

        // Use the highlighted texture variant only when a JEI item is being
        // dragged over this folder — plain mouse-hover keeps the normal sprite.
        // isHovered() (the inherited AbstractWidget field) is only updated by
        // render(), which we bypass by calling renderWidget directly — so use
        // isPointInButton against the live mouse coords instead.
        boolean dragHover = isPointInButton(mouseX, mouseY)
            && JEIIntegration.get().getDraggedIngredient().isPresent();
        int textureU = dragHover ? 16 : 0;
        int textureV = folder.isActive() ? 48 : 32;

        // Scale the full 16x16 sprite cell down to SPRITE_SIZE (the 11-arg blit
        // overload takes destination size and source-region size separately;
        // the 9-arg version we'd been using treats them as one value, which
        // clips instead of scales).
        int spriteSize = FolderLayout.SPRITE_SIZE;
        int spriteX = getX() + (width - spriteSize) / 2;
        int spriteY = getY() + (height - spriteSize) / 2;

        // Tint the grayscale folder sprite with the folder's chosen color.
        int color = folder.getColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        RenderSystem.setShaderColor(r, g, b, 1f);
        guiGraphics.blit(TEXTURE, spriteX, spriteY, spriteSize, spriteSize, textureU, textureV, 16, 16, 64, 64);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        
        // Folder label, scaled down to fit beneath the smaller icon. Translate to
        // the desired anchor (centered horizontally, just below the button), then
        // scale, then draw centered around (0,0) in the scaled coordinate system.
        String shortName = folder.getShortName();
        var font = Minecraft.getInstance().font;
        int unscaledWidth = cachedShortNameWidth;
        if (unscaledWidth < 0) {
            unscaledWidth = font.width(shortName);
            cachedShortNameWidth = unscaledWidth;
        }
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(getX() + width / 2f, getY() + height + FolderLayout.LABEL_Y_OFFSET, 0);
        pose.scale(FolderLayout.LABEL_SCALE, FolderLayout.LABEL_SCALE, 1.0f);
        guiGraphics.drawString(font, shortName, -unscaledWidth / 2, 0, 0xFFFFFF);
        pose.popPose();
        
        // Highlight if an ingredient is being dragged over
        highlightForDrag(guiGraphics, mouseX, mouseY);
    }
    
    /**
     * Highlights the folder button if an ingredient is being dragged over it.
     *
     * @param graphics The graphics context to render with
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     */
    private void highlightForDrag(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!isPointInButton(mouseX, mouseY)) {
            return;
        }
        JEIIntegration.get().getDraggedIngredient().ifPresent(ingredient -> {
            int highlightColor = 0x80FFFFFF;
            graphics.fill(getX() - 1, getY() - 1, getX() + width + 1, getY() + height + 1, highlightColor);
        });
    }
    
    /**
     * Gets the folder that this button represents.
     *
     * @return The folder object
     */
    public Folder getFolder() {
        return folder;
    }
    
    /**
     * Checks if a point is within the button's bounds.
     *
     * @param mouseX The X coordinate to check
     * @param mouseY The Y coordinate to check
     * @return True if the point is within the button
     */
    public boolean isPointInButton(int mouseX, int mouseY) {
        return mouseX >= getX() && mouseX < getX() + width && 
               mouseY >= getY() && mouseY < getY() + height;
    }
    
    /**
     * Checks if the button is currently being hovered over by the mouse.
     * 
     * @return True if the mouse is hovering over this button
     */
    @Override
    public boolean isHovered() {
        return super.isHovered();
    }
    
    /**
     * Triggers the button's click action.
     */
    public void onClick() {
        onPress.onPress(this);
    }
    
    /**
     * Tries to handle a JEI ingredient drop on this folder button.
     *
     * @param mouseX The mouse X coordinate
     * @param mouseY The mouse Y coordinate
     * @return True if a drop was handled, false otherwise
     */
    public boolean tryHandleDrop(int mouseX, int mouseY) {
        if (!isPointInButton(mouseX, mouseY)) {
            return false;
        }
        JEIIntegration jei = JEIIntegration.get();
        Optional<Object> draggedIngredient = jei.getDraggedIngredient();
        if (draggedIngredient.isEmpty()) {
            return false;
        }
        Optional<StoredIngredient> storedIngredient = jei.storeIngredient(draggedIngredient.get());
        if (storedIngredient.isEmpty()) {
            return false;
        }
        EnoughFoldersCommon.getFolderManager().addIngredient(folder, storedIngredient.get());
        EnoughFoldersCommon.LOGGER.info("Added ingredient to folder: {}", folder.getName());
        return true;
    }
}

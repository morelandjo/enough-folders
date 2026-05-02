package com.vodmordia.enoughfolders.client.gui;
import com.vodmordia.enoughfolders.EnoughFoldersCommon;

import com.vodmordia.enoughfolders.data.Folder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Handles rendering of the folder screen components.
 */
public class FolderScreenRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(EnoughFoldersCommon.MOD_ID, "textures/gui/folders.png");
    private static final int FOLDER_AREA_HEIGHT = FolderLayout.FOLDER_AREA_HEIGHT;
    
    // The screen that this renderer is rendering for
    private final AbstractContainerScreen<?> parentScreen;
    
    // Screen position and dimensions
    private int leftPos;
    private int topPos;
    private int width;
    private int height;
    
    // Dependencies
    private final Supplier<Optional<Folder>> activeFolder;
    
    /**
     * Creates a new folder screen renderer.
     *
     * @param parentScreen The parent container screen
     * @param activeFolderSupplier Supplier for the active folder
     */
    public FolderScreenRenderer(
            AbstractContainerScreen<?> parentScreen,
            Supplier<Optional<Folder>> activeFolderSupplier) {
        this.parentScreen = parentScreen;
        this.activeFolder = activeFolderSupplier;
    }
    
    /**
     * Sets the position and dimensions for rendering.
     *
     * @param leftPos Left position
     * @param topPos Top position
     * @param width Width of the container
     * @param height Height of the container
     */
    public void setPositionAndDimensions(int leftPos, int topPos, int width, int height) {
        this.leftPos = leftPos;
        this.topPos = topPos;
        this.width = width;
        this.height = height;
    }
    
    /**
     * Updates the height dimension.
     *
     * @param height New height
     */
    public void updateHeight(int height) {
        this.height = height;
    }
    
    /**
     * Renders the folder screen.
     *
     * @param graphics The graphics context to render with
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     * @param partialTick The partial tick time
     * @param folderButtons The folder buttons to render
     * @param ingredientSlots The ingredient slots to render
     * @param addFolderButton The add folder button
     * @param deleteButton The delete button
     * @param prevPageButton The previous page button
     * @param nextPageButton The next page button
     * @param newFolderNameInput The new folder name input field
     * @param isAddingFolder Whether we're currently adding a folder
     * @param currentPage Current page number (0-based)
     * @param totalPages Total number of pages
     * @param folderRowsCount Number of folder button rows
     */
    public void render(
            GuiGraphics graphics, 
            int mouseX, 
            int mouseY, 
            float partialTick,
            List<FolderButton> folderButtons,
            List<IngredientSlot> ingredientSlots,
            Button addFolderButton,
            Button deleteButton,
            Button prevPageButton,
            Button nextPageButton,
            EditBox newFolderNameInput,
            boolean isAddingFolder,
            int currentPage,
            int totalPages,
            int folderRowsCount) {
        
        renderBackground(graphics);
        
        // Render folder buttons
        for (FolderButton button : folderButtons) {
            button.renderWidget(graphics, mouseX, mouseY, partialTick);
        }
        
        EnoughFoldersCommon.LOGGER.debug("Rendered {} folder buttons", folderButtons.size());
        
        // Render add folder button
        renderAddFolderButton(graphics, mouseX, mouseY, partialTick, addFolderButton);
        
        // Render active folder content if there is one
        activeFolder.get().ifPresent(folder -> {
            EnoughFoldersCommon.LOGGER.debug("Rendering active folder: {}", folder.getName());
            
            int verticalOffset = FolderLayout.verticalOffset(isAddingFolder, folderRowsCount);
            
            String name = folder.getTruncatedName();
            graphics.drawString(
                    parentScreen.getMinecraft().font, 
                    name, 
                    leftPos + 5, 
                    topPos + FOLDER_AREA_HEIGHT + 17 + verticalOffset, 
                    0xFFFFFF
            );
            
            deleteButton.setPosition(leftPos + width - 25, topPos + FOLDER_AREA_HEIGHT + 12 + verticalOffset);
            
            renderDeleteButton(graphics, mouseX, mouseY, partialTick, deleteButton);
            
            prevPageButton.render(graphics, mouseX, mouseY, partialTick);
            nextPageButton.render(graphics, mouseX, mouseY, partialTick);
            
            // Render page count
            String pageText = (currentPage + 1) + "/" + totalPages;
            int pageTextWidth = parentScreen.getMinecraft().font.width(pageText);
            
            int centerX = leftPos + (width - pageTextWidth) / 2;
            int centerY = prevPageButton.getY() + prevPageButton.getHeight() / 2 - 4;
            
            graphics.drawString(
                    parentScreen.getMinecraft().font,
                    pageText,
                    centerX,
                    centerY,
                    0xFFFFFF
            );
            
            // Render ingredient slots
            for (IngredientSlot slot : ingredientSlots) {
                slot.render(graphics, mouseX, mouseY);
            }
            
            EnoughFoldersCommon.LOGGER.debug("Rendered {} ingredient slots", ingredientSlots.size());
        });
        
        // Render folder name input if adding a folder
        if (isAddingFolder) {
            newFolderNameInput.render(graphics, mouseX, mouseY, partialTick);
        }
        
        // Render tooltips
        renderTooltips(graphics, mouseX, mouseY, folderButtons);
    }
    
    /**
     * Renders the semi-transparent background of the folder screen.
     *
     * @param graphics The graphics context to render with
     */
    private void renderBackground(GuiGraphics graphics) {
        EnoughFoldersCommon.LOGGER.debug("FolderScreen.renderBackground at position: {},{} with dimensions: {}x{}", 
            leftPos, topPos, width, height);
        
        graphics.fill(leftPos, topPos, leftPos + width, topPos + height, 0x80404040);
    }
    
    /**
     * Renders tooltips for elements under the mouse cursor.
     *
     * @param graphics The graphics context to render with
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     * @param folderButtons The folder buttons
     */
    private void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY, List<FolderButton> folderButtons) {
        for (FolderButton button : folderButtons) {
            if (button.isPointInButton(mouseX, mouseY)) {
                graphics.renderTooltip(
                        parentScreen.getMinecraft().font, 
                        Component.literal(button.getFolder().getName()),
                        mouseX, 
                        mouseY
                );
            }
        }
    }
    
    /**
     * Renders the add folder button and textures.
     *
     * @param graphics The graphics context to render with
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     * @param partialTick The partial tick time
     * @param addFolderButton The add folder button
     */
    private void renderAddFolderButton(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, Button addFolderButton) {
        int x = addFolderButton.getX();
        int y = addFolderButton.getY();
        int frameW = addFolderButton.getWidth();
        int frameH = addFolderButton.getHeight();
        boolean isHovered = mouseX >= x && mouseX < x + frameW && mouseY >= y && mouseY < y + frameH;

        int spriteSize = FolderLayout.SPRITE_SIZE;
        int spriteX = x + (frameW - spriteSize) / 2;
        int spriteY = y + (frameH - spriteSize) / 2;

        int textureU = 0;
        int textureV = isHovered ? 16 : 0;
        graphics.blit(TEXTURE, spriteX, spriteY, spriteSize, spriteSize, textureU, textureV, 16, 16, 64, 64);
    }

    /**
     * Renders the delete folder button and textures.
     *
     * @param graphics The graphics context to render with
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     * @param partialTick The partial tick time
     * @param deleteButton The delete button
     */
    private void renderDeleteButton(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, Button deleteButton) {
        int x = deleteButton.getX();
        int y = deleteButton.getY();
        int frameW = deleteButton.getWidth();
        int frameH = deleteButton.getHeight();

        int spriteSize = FolderLayout.SPRITE_SIZE;
        int spriteX = x + (frameW - spriteSize) / 2;
        int spriteY = y + (frameH - spriteSize) / 2;

        int textureU = 16;
        int textureV = 0;
        graphics.blit(TEXTURE, spriteX, spriteY, spriteSize, spriteSize, textureU, textureV, 16, 16, 64, 64);
    }
}

package com.vodmordia.enoughfolders.client.gui;
import com.vodmordia.enoughfolders.EnoughFoldersCommon;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.vodmordia.enoughfolders.data.Folder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
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
    private static final ResourceLocation TEXTURE = new ResourceLocation(EnoughFoldersCommon.MOD_ID, "textures/gui/folders.png");
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
     */
    public FolderScreenRenderer(
            AbstractContainerScreen<?> parentScreen,
            Supplier<Optional<Folder>> activeFolderSupplier) {
        this.parentScreen = parentScreen;
        this.activeFolder = activeFolderSupplier;
    }

    public void setPositionAndDimensions(int leftPos, int topPos, int width, int height) {
        this.leftPos = leftPos;
        this.topPos = topPos;
        this.width = width;
        this.height = height;
    }

    public void updateHeight(int height) {
        this.height = height;
    }

    /**
     * Renders the folder screen.
     */
    public void render(
            PoseStack poseStack,
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

        renderBackground(poseStack);

        // Render folder buttons
        for (FolderButton button : folderButtons) {
            button.renderButton(poseStack, mouseX, mouseY, partialTick);
        }

        // Render add folder button
        renderAddFolderButton(poseStack, mouseX, mouseY, partialTick, addFolderButton);

        // Render active folder content if there is one. Avoid Optional.ifPresent
        // here — the lambda captures poseStack/mouseX/etc. and allocates per frame.
        Optional<Folder> active = activeFolder.get();
        if (active.isPresent()) {
            Folder folder = active.get();
            int verticalOffset = FolderLayout.verticalOffset(isAddingFolder, folderRowsCount);

            String name = folder.getTruncatedName();
            var font = Minecraft.getInstance().font;
            font.draw(
                    poseStack,
                    name,
                    leftPos + 5f,
                    topPos + FOLDER_AREA_HEIGHT + 17f + verticalOffset,
                    0xFFFFFF
            );

            // 1.19.2: AbstractWidget has no setPosition/setX/setY — the x/y
            // fields are public (added later in 1.19.x).
            deleteButton.x = leftPos + width - 25;
            deleteButton.y = topPos + FOLDER_AREA_HEIGHT + 12 + verticalOffset;

            renderDeleteButton(poseStack, mouseX, mouseY, partialTick, deleteButton);

            prevPageButton.render(poseStack, mouseX, mouseY, partialTick);
            nextPageButton.render(poseStack, mouseX, mouseY, partialTick);

            // Render page count
            String pageText = (currentPage + 1) + "/" + totalPages;
            int pageTextWidth = font.width(pageText);

            int centerX = leftPos + (width - pageTextWidth) / 2;
            int centerY = prevPageButton.y + prevPageButton.getHeight() / 2 - 4;

            font.draw(
                    poseStack,
                    pageText,
                    centerX,
                    centerY,
                    0xFFFFFF
            );

            // Render ingredient slots
            for (IngredientSlot slot : ingredientSlots) {
                slot.render(poseStack, mouseX, mouseY);
            }
        }

        // Render folder name input if adding a folder
        if (isAddingFolder) {
            newFolderNameInput.render(poseStack, mouseX, mouseY, partialTick);
        }

        // Render tooltips
        renderTooltips(poseStack, mouseX, mouseY, folderButtons);
    }

    /**
     * Renders the semi-transparent background of the folder screen.
     */
    private void renderBackground(PoseStack poseStack) {
        GuiComponent.fill(poseStack, leftPos, topPos, leftPos + width, topPos + height, 0x80404040);
    }

    /**
     * Renders tooltips for elements under the mouse cursor.
     */
    private void renderTooltips(PoseStack poseStack, int mouseX, int mouseY, List<FolderButton> folderButtons) {
        // Skip the per-button rect tests when the mouse isn't even over the panel.
        if (mouseX < leftPos || mouseX >= leftPos + width || mouseY < topPos || mouseY >= topPos + height) {
            return;
        }
        for (FolderButton button : folderButtons) {
            if (button.isPointInButton(mouseX, mouseY)) {
                // Screen.renderTooltip(PoseStack, Component, int, int) is public in 1.19.2.
                parentScreen.renderTooltip(
                        poseStack,
                        Component.literal(button.getFolder().getName()),
                        mouseX,
                        mouseY
                );
            }
        }
    }

    /**
     * Renders the add folder button and textures.
     */
    private void renderAddFolderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick, Button addFolderButton) {
        int x = addFolderButton.x;
        int y = addFolderButton.y;
        int frameW = addFolderButton.getWidth();
        int frameH = addFolderButton.getHeight();
        boolean isHovered = mouseX >= x && mouseX < x + frameW && mouseY >= y && mouseY < y + frameH;

        int spriteSize = FolderLayout.SPRITE_SIZE;
        int spriteX = x + (frameW - spriteSize) / 2;
        int spriteY = y + (frameH - spriteSize) / 2;

        int textureU = 0;
        int textureV = isHovered ? 16 : 0;
        RenderSystem.setShaderTexture(0, TEXTURE);
        GuiComponent.blit(poseStack, spriteX, spriteY, spriteSize, spriteSize, textureU, textureV, 16, 16, 64, 64);
    }

    /**
     * Renders the delete folder button and textures.
     */
    private void renderDeleteButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick, Button deleteButton) {
        int x = deleteButton.x;
        int y = deleteButton.y;
        int frameW = deleteButton.getWidth();
        int frameH = deleteButton.getHeight();

        int spriteSize = FolderLayout.SPRITE_SIZE;
        int spriteX = x + (frameW - spriteSize) / 2;
        int spriteY = y + (frameH - spriteSize) / 2;

        int textureU = 16;
        int textureV = 0;
        RenderSystem.setShaderTexture(0, TEXTURE);
        GuiComponent.blit(poseStack, spriteX, spriteY, spriteSize, spriteSize, textureU, textureV, 16, 16, 64, 64);
    }
}

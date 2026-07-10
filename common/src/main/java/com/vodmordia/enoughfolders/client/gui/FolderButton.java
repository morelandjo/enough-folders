package com.vodmordia.enoughfolders.client.gui;
import com.vodmordia.enoughfolders.EnoughFoldersCommon;

import com.vodmordia.enoughfolders.data.Folder;
import com.vodmordia.enoughfolders.data.StoredIngredient;
import com.vodmordia.enoughfolders.integrations.jei.core.JEIIntegration;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
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
        // 1.19.2 Button has no CreateNarration parameter; the 6-arg form is the
        // most expressive. The 1.20.1 build used a 7-arg ctor with DEFAULT_NARRATION.
        super(x, y, width, height, Component.literal(folder.getShortName()), onPress);
        this.folder = folder;
    }

    /**
     * Renders the folder button. In 1.19.2 the override hook on AbstractWidget
     * is {@code renderButton(PoseStack, int, int, float)} (renamed to
     * {@code renderWidget(GuiGraphics, ...)} in 1.20+).
     */
    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        RenderSystem.setShaderTexture(0, TEXTURE);

        // Use the highlighted texture variant only when a JEI item is being
        // dragged over this folder — plain mouse-hover keeps the normal sprite.
        // isHovered() (the inherited AbstractWidget field) is only updated by
        // render(), which we bypass by calling renderButton directly — so use
        // isPointInButton against the live mouse coords instead.
        boolean dragHover = isPointInButton(mouseX, mouseY)
            && JEIIntegration.get().getDraggedIngredient().isPresent();
        int textureU = dragHover ? 16 : 0;
        int textureV = folder.isActive() ? 48 : 32;

        // Scale the full 16x16 sprite cell down to SPRITE_SIZE (the 10-arg blit
        // overload takes destination size and source-region size separately;
        // a shorter overload would treat them as one value and clip).
        int spriteSize = FolderLayout.SPRITE_SIZE;
        int spriteX = this.x + (width - spriteSize) / 2;
        int spriteY = this.y + (height - spriteSize) / 2;

        // Tint the grayscale folder sprite with the folder's chosen color.
        int color = folder.getColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        RenderSystem.setShaderColor(r, g, b, 1f);
        GuiComponent.blit(poseStack, spriteX, spriteY, spriteSize, spriteSize, textureU, textureV, 16, 16, 64, 64);
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
        poseStack.pushPose();
        poseStack.translate(this.x + width / 2f, this.y + height + FolderLayout.LABEL_Y_OFFSET, 0);
        poseStack.scale(FolderLayout.LABEL_SCALE, FolderLayout.LABEL_SCALE, 1.0f);
        font.draw(poseStack, shortName, -unscaledWidth / 2f, 0, 0xFFFFFF);
        poseStack.popPose();

        // Highlight if an ingredient is being dragged over
        highlightForDrag(poseStack, mouseX, mouseY);
    }

    /**
     * Highlights the folder button if an ingredient is being dragged over it.
     */
    private void highlightForDrag(PoseStack poseStack, int mouseX, int mouseY) {
        if (!isPointInButton(mouseX, mouseY)) {
            return;
        }
        JEIIntegration.get().getDraggedIngredient().ifPresent(ingredient -> {
            int highlightColor = 0x80FFFFFF;
            GuiComponent.fill(poseStack, this.x - 1, this.y - 1, this.x + width + 1, this.y + height + 1, highlightColor);
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
        return mouseX >= this.x && mouseX < this.x + width &&
               mouseY >= this.y && mouseY < this.y + height;
    }

    /**
     * Checks if the button is currently being hovered over by the mouse.
     * 1.19.2's AbstractWidget exposes hover state as the protected field
     * {@code isHovered}, not a method (the {@code isHovered()} accessor was
     * added in a later version).
     */
    public boolean isHovered() {
        return this.isHovered;
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

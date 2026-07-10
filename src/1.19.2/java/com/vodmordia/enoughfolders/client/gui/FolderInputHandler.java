package com.vodmordia.enoughfolders.client.gui;

import com.vodmordia.enoughfolders.data.FolderManager;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import org.lwjgl.glfw.GLFW;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Handles input events for the folder screen. Holds references to the
 * managers and suppliers it needs so the per-event methods take only the
 * dynamic mouse/key arguments instead of being passed FolderScreen's full
 * widget set on every call.
 */
public class FolderInputHandler {
    // Layout — set by FolderScreen.init()
    private int leftPos;
    private int topPos;
    private int width;
    private int height;

    // Stable dependencies
    private final FolderManager folderManager;
    private final FolderButtonManager buttonManager;
    private final IngredientGridManager gridManager;

    // Suppliers for state recreated on each FolderScreen.init() (resize)
    private final Supplier<Button> deleteButton;
    private final Supplier<EditBox> newFolderNameInput;
    private final BooleanSupplier isAddingFolder;

    // Callbacks to FolderScreen
    private final Consumer<String> createFolderCallback;
    private final Runnable toggleAddFolderModeCallback;
    private final Runnable onIngredientAddedCallback;
    private final Consumer<FolderButton> openColorPickerCallback;

    public FolderInputHandler(
            FolderManager folderManager,
            FolderButtonManager buttonManager,
            IngredientGridManager gridManager,
            Supplier<Button> deleteButton,
            Supplier<EditBox> newFolderNameInput,
            BooleanSupplier isAddingFolder,
            Consumer<String> createFolderCallback,
            Runnable toggleAddFolderModeCallback,
            Runnable onIngredientAddedCallback,
            Consumer<FolderButton> openColorPickerCallback) {
        this.folderManager = folderManager;
        this.buttonManager = buttonManager;
        this.gridManager = gridManager;
        this.deleteButton = deleteButton;
        this.newFolderNameInput = newFolderNameInput;
        this.isAddingFolder = isAddingFolder;
        this.createFolderCallback = createFolderCallback;
        this.toggleAddFolderModeCallback = toggleAddFolderModeCallback;
        this.onIngredientAddedCallback = onIngredientAddedCallback;
        this.openColorPickerCallback = openColorPickerCallback;
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

    public boolean isVisible(double mouseX, double mouseY) {
        return mouseX >= leftPos && mouseX < leftPos + width &&
               mouseY >= topPos && mouseY < topPos + height;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible(mouseX, mouseY)) {
            return false;
        }

        Button addBtn = buttonManager.getAddFolderButton();
        if (addBtn.isMouseOver(mouseX, mouseY)) {
            addBtn.onClick(mouseX, mouseY);
            return true;
        }

        for (FolderButton folderButton : buttonManager.getFolderButtons()) {
            if (folderButton.isPointInButton((int) mouseX, (int) mouseY)) {
                if (button == 1) {
                    openColorPickerCallback.accept(folderButton);
                } else {
                    folderButton.onClick();
                }
                return true;
            }
        }

        for (IngredientSlot slot : gridManager.getIngredientSlots()) {
            if (slot.mouseClicked((int) mouseX, (int) mouseY, button)) {
                return true;
            }
        }

        EditBox input = newFolderNameInput.get();
        if (isAddingFolder.getAsBoolean() && input.isMouseOver(mouseX, mouseY)) {
            // 1.19.2: AbstractWidget#setFocused is protected; EditBox exposes
            // the public setFocus(boolean) (renamed to setFocused in 1.19.4+).
            input.setFocus(true);
            return input.mouseClicked(mouseX, mouseY, button);
        }

        if (folderManager.getActiveFolder().isPresent()) {
            Button delBtn = deleteButton.get();
            if (delBtn.isMouseOver(mouseX, mouseY)) {
                delBtn.onClick(mouseX, mouseY);
                return true;
            }
            Button prevBtn = gridManager.getPrevPageButton();
            if (prevBtn.isMouseOver(mouseX, mouseY)) {
                prevBtn.onClick(mouseX, mouseY);
                return true;
            }
            Button nextBtn = gridManager.getNextPageButton();
            if (nextBtn.isMouseOver(mouseX, mouseY)) {
                nextBtn.onClick(mouseX, mouseY);
                return true;
            }
        }

        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!isVisible(mouseX, mouseY)) {
            return false;
        }
        for (FolderButton folderButton : buttonManager.getFolderButtons()) {
            if (folderButton.tryHandleDrop((int) mouseX, (int) mouseY)) {
                onIngredientAddedCallback.run();
                return true;
            }
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        EditBox input = newFolderNameInput.get();
        if (isAddingFolder.getAsBoolean() && input.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                createFolderCallback.accept(input.getValue());
                input.setValue("");
                toggleAddFolderModeCallback.run();
                return true;
            }
            return input.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        EditBox input = newFolderNameInput.get();
        if (isAddingFolder.getAsBoolean() && input.isFocused()) {
            return input.charTyped(codePoint, modifiers);
        }
        return false;
    }
}

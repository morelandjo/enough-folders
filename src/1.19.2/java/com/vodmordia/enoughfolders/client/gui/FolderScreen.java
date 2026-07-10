package com.vodmordia.enoughfolders.client.gui;
import com.vodmordia.enoughfolders.EnoughFoldersCommon;

import com.vodmordia.enoughfolders.data.Folder;
import com.vodmordia.enoughfolders.data.FolderManager;
import com.vodmordia.enoughfolders.integrations.jei.core.JEIIntegration;
import com.vodmordia.enoughfolders.integrations.jei.gui.targets.FolderButtonTarget;
import com.vodmordia.enoughfolders.integrations.jei.gui.targets.FolderGhostIngredientTarget;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

/**
 * The main folder screen overlay that displays folders and their contents.
 *
 */
public class FolderScreen implements FolderGhostIngredientTarget {
    private static final int FOLDER_AREA_HEIGHT = FolderLayout.FOLDER_AREA_HEIGHT;

    /**
     * The container screen that this folder screen is overlaying
     */
    private final AbstractContainerScreen<?> parentScreen;

    /**
     * Reference to the folder manager for data access
     */
    private final FolderManager folderManager;

    /**
     * Component managers
     */
    private final FolderButtonManager buttonManager;
    private final IngredientGridManager gridManager;
    private final FolderScreenRenderer renderer;
    private final FolderInputHandler inputHandler;

    /**
     * UI controls for folder management
     */
    private Button deleteButton;
    private EditBox newFolderNameInput;

    /**
     * State variables for the folder UI
     */
    private boolean isAddingFolder = false;

    /** Color picker popup, non-null while it is open. */
    private ColorPickerOverlay colorPicker = null;

    /**
     * Position and size of the folder screen
     */
    private int leftPos;
    private int topPos;
    private int width;
    private int height;

    /**
     * Last parent dimensions init() ran with; -1 means uninitialized
     */
    private int lastParentWidth = -1;
    private int lastParentHeight = -1;

    /**
     * Creates a new folder screen overlay for the given parent screen.
     *
     * @param parentScreen The container screen to overlay
     */
    public FolderScreen(AbstractContainerScreen<?> parentScreen) {

        EnoughFoldersCommon.LOGGER.debug("FolderScreen constructor called with: " + parentScreen.getClass().getName());

        this.parentScreen = parentScreen;
        this.folderManager = EnoughFoldersCommon.getFolderManager();

        // Initialize component managers
        this.buttonManager = new FolderButtonManager(folderManager, this::onFolderClicked);
        this.gridManager = new IngredientGridManager(() -> folderManager.getActiveFolder());
        this.renderer = new FolderScreenRenderer(parentScreen, () -> folderManager.getActiveFolder());
        this.inputHandler = new FolderInputHandler(
            folderManager,
            buttonManager,
            gridManager,
            () -> deleteButton,
            () -> newFolderNameInput,
            () -> isAddingFolder,
            this::createNewFolder,
            this::toggleAddFolderMode,
            this::onIngredientAdded,
            this::openColorPicker);
    }

    /**
     * Opens the color picker, preferring to anchor it to the right of the
     * folder button. Falls back to the left if the right side would clip the
     * screen, and finally clamps within the screen bounds so the popup is
     * always fully visible.
     */
    private void openColorPicker(FolderButton folderButton) {
        ColorPickerOverlay picker = new ColorPickerOverlay(0, 0, folderButton.getFolder());
        int pickerW = picker.getWidth();
        int pickerH = picker.getHeight();
        var window = Minecraft.getInstance().getWindow();
        int screenW = window.getGuiScaledWidth();
        int screenH = window.getGuiScaledHeight();

        // 1.19.2: AbstractWidget exposes x/y as public fields, no getX/getY.
        int anchorX = folderButton.x + folderButton.getWidth() + 2;
        if (anchorX + pickerW > screenW) {
            anchorX = folderButton.x - pickerW - 2;
        }
        anchorX = Math.max(0, Math.min(anchorX, screenW - pickerW));

        int anchorY = folderButton.y;
        anchorY = Math.max(0, Math.min(anchorY, screenH - pickerH));

        colorPicker = new ColorPickerOverlay(anchorX, anchorY, folderButton.getFolder());
    }

    /**
     * Initializes or reinitializes the folder screen with the given dimensions.
     *
     * @param parentWidth The width of the parent screen
     * @param parentHeight The height of the parent screen
     */
    public void init(int parentWidth, int parentHeight) {

        EnoughFoldersCommon.LOGGER.debug("FolderScreen.init called with width: {}, height: {}",
            parentWidth, parentHeight);

        boolean wasAddingFolder = isAddingFolder;
        String currentInputText = newFolderNameInput != null ? newFolderNameInput.getValue() : "";
        boolean inputHadFocus = newFolderNameInput != null && newFolderNameInput.isFocused();

        int parentLeftPos = (parentWidth - FolderLayout.STANDARD_CONTAINER_WIDTH) / 2;
        boolean isJeiRecipeGuiOpen = JEIIntegration.get().isRecipeGuiOpen();

        int maxWidth = Math.min(parentWidth - FolderLayout.PANEL_RIGHT_MARGIN, FolderLayout.MAX_PANEL_WIDTH);
        if (parentLeftPos > 0) {
            maxWidth = Math.min(maxWidth, parentLeftPos - FolderLayout.JEI_WIDTH_REDUCTION);
        }
        if (isJeiRecipeGuiOpen) {
            maxWidth = Math.max(FolderLayout.MIN_PANEL_WIDTH, maxWidth - FolderLayout.JEI_WIDTH_REDUCTION);
        }

        width = maxWidth;
        leftPos = FolderLayout.PADDING;
        topPos = FolderLayout.PADDING;

        // Set position and dimensions for all component managers
        buttonManager.setPositionAndDimensions(leftPos, width);
        gridManager.setPositionAndDimensions(leftPos, topPos, width);
        renderer.setPositionAndDimensions(leftPos, topPos, width, 0); // Height updated below
        inputHandler.setPositionAndDimensions(leftPos, topPos, width, 0); // Height updated below

        // Create the add folder button
        buttonManager.createAddFolderButton(
                leftPos + FolderLayout.PADDING,
                topPos + FolderLayout.PADDING,
                button -> toggleAddFolderMode());

        // Initialize folder buttons
        int folderRowsCount = buttonManager.initFolderButtons(topPos, isAddingFolder);

        // Calculate base dimensions
        boolean hasActiveFolder = folderManager.getActiveFolder().isPresent();
        int folderRowsHeight = FOLDER_AREA_HEIGHT;
        if (folderRowsCount > 1) {
            folderRowsHeight += (folderRowsCount - 1) * FolderLayout.FOLDER_ROW_HEIGHT;
        }

        if (hasActiveFolder) {
            height = folderRowsHeight + FolderLayout.INPUT_FIELD_HEIGHT + FolderLayout.CONTENT_AREA_HEIGHT + FolderLayout.PADDING;
        } else {
            height = folderRowsHeight + 2 * FolderLayout.PADDING;
        }

        if (wasAddingFolder) {
            height += FolderLayout.INPUT_FIELD_HEIGHT;
        }

        // Create the delete button (1.19.2 Button has no fluent Builder).
        deleteButton = new Button(
                leftPos + width - 25,
                topPos + FOLDER_AREA_HEIGHT + FolderLayout.PADDING,
                FolderLayout.BUTTON_SIZE,
                FolderLayout.BUTTON_SIZE,
                Component.literal("X"),
                button -> deleteCurrentFolder());

        // Create pagination buttons for ingredient grid
        gridManager.createPaginationButtons(
                button -> {
                    gridManager.previousPage();
                    refreshIngredientSlots();
                },
                button -> {
                    gridManager.nextPage();
                    refreshIngredientSlots();
                });

        // Create the folder name input field
        newFolderNameInput = new EditBox(
                Minecraft.getInstance().font,
                leftPos + 30,
                topPos + 7,
                width - 35,
                16,
                Component.literal("Folder Name")
        );
        newFolderNameInput.setMaxLength(20);

        isAddingFolder = wasAddingFolder;
        newFolderNameInput.setValue(currentInputText);
        newFolderNameInput.setVisible(isAddingFolder);
        if (inputHadFocus) {
            newFolderNameInput.setFocus(true);
        }

        EnoughFoldersCommon.LOGGER.debug("Input box state restored: visible={}, text={}", isAddingFolder, currentInputText);

        // Initialize ingredient slots and update final dimensions
        refreshIngredientSlots();

        lastParentWidth = parentWidth;
        lastParentHeight = parentHeight;
    }

    /**
     * Re-runs init() only when the parent dimensions changed since the last init.
     *
     * @param parentWidth The current width of the parent screen
     * @param parentHeight The current height of the parent screen
     */
    public void reinitIfNeeded(int parentWidth, int parentHeight) {
        if (parentWidth != lastParentWidth || parentHeight != lastParentHeight) {
            init(parentWidth, parentHeight);
        }
    }

    /**
     * Refreshes the ingredient slots for the active folder.
     */
    private void refreshIngredientSlots() {
        int newHeight = gridManager.refreshIngredientSlots(isAddingFolder, buttonManager.getFolderRowsCount());

        // Update height for all components
        height = newHeight;
        renderer.updateHeight(height);
        inputHandler.updateHeight(height);
    }

    /**
     * Toggles the folder creation mode on or off.
     */
    private void toggleAddFolderMode() {
        isAddingFolder = !isAddingFolder;
        newFolderNameInput.setVisible(isAddingFolder);

        if (isAddingFolder) {
            newFolderNameInput.setFocus(true);
        }

        EnoughFoldersCommon.LOGGER.debug("Add folder mode toggled to: {}, input box visibility: {}",
            isAddingFolder, newFolderNameInput.isVisible());

        if (!isAddingFolder && !newFolderNameInput.getValue().isEmpty()) {
            createNewFolder(newFolderNameInput.getValue());
            newFolderNameInput.setValue("");
        }

        // Re-initialize buttons with new layout
        buttonManager.initFolderButtons(topPos, isAddingFolder);
        refreshIngredientSlots();
    }

    /**
     * Creates a new folder with the given name.
     *
     * @param name The name for the new folder
     */
    private void createNewFolder(String name) {
        if (name == null || name.trim().isEmpty()) {
            return;
        }

        folderManager.createFolder(name.trim());
        buttonManager.initFolderButtons(topPos, isAddingFolder);
    }

    /**
     * Handles clicks on folder buttons.
     *
     * @param folder The folder that was clicked
     */
    private void onFolderClicked(Folder folder) {
        if (folder.isActive()) {
            folderManager.clearActiveFolder();
            EnoughFoldersCommon.LOGGER.debug("Active folder deactivated: " + folder.getName());
        } else {
            folderManager.setActiveFolder(folder);
            EnoughFoldersCommon.LOGGER.debug("Folder activated: " + folder.getName());
        }
        refreshIngredientSlots();
    }

    /**
     * Deletes the currently active folder.
     */
    private void deleteCurrentFolder() {
        folderManager.getActiveFolder().ifPresent(folder -> {
            Minecraft.getInstance().setScreen(new ConfirmScreen(confirmed -> {
                if (confirmed) {
                    folderManager.deleteFolder(folder);
                    folderManager.clearActiveFolder();
                    buttonManager.initFolderButtons(topPos, isAddingFolder);
                    refreshIngredientSlots();
                }
                Minecraft.getInstance().setScreen(parentScreen);
            },
                Component.translatable("enoughfolders.folder.delete.confirm.title"),
                Component.translatable("enoughfolders.folder.delete.confirm.message", folder.getName())));
        });
    }

    /**
     * Renders the folder screen.
     */
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {

        renderer.render(
            poseStack,
            mouseX,
            mouseY,
            partialTick,
            buttonManager.getFolderButtons(),
            gridManager.getIngredientSlots(),
            buttonManager.getAddFolderButton(),
            deleteButton,
            gridManager.getPrevPageButton(),
            gridManager.getNextPageButton(),
            newFolderNameInput,
            isAddingFolder,
            gridManager.getCurrentPage(),
            gridManager.getTotalPages(),
            buttonManager.getFolderRowsCount()
        );

        if (colorPicker != null) {
            colorPicker.render(poseStack, mouseX, mouseY);
        }
    }

    /**
     * Handles mouse click events on the folder screen.
     *
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     * @param button The mouse button that was clicked
     * @return true if the click was handled, false otherwise
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (colorPicker != null) {
            if (colorPicker.isOver(mouseX, mouseY)) {
                // Inside the popup. Picker.mouseClicked returns true only when
                // a swatch was actually picked; gap clicks keep the picker open.
                if (colorPicker.mouseClicked(mouseX, mouseY, button, folderManager)) {
                    colorPicker = null;
                }
            } else {
                // Click outside the popup dismisses without applying.
                colorPicker = null;
            }
            return true;
        }
        return inputHandler.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return inputHandler.mouseReleased(mouseX, mouseY, button);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return inputHandler.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return inputHandler.charTyped(codePoint, modifiers);
    }

    /**
     * Checks if the folder creation mode is active.
     *
     * @return true if the user is currently adding a new folder, false otherwise
     */
    public boolean isAddingFolder() {
        return isAddingFolder;
    }

    /**
     * Checks if the folder name input field has focus.
     *
     * @return true if the input field is focused, false otherwise
     */
    public boolean isInputFocused() {
        return newFolderNameInput != null && newFolderNameInput.isFocused();
    }

    /**
     * Checks if a point is within the folder screen's bounds.
     *
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     * @return true if the point is within the folder screen, false otherwise
     */
    public boolean isVisible(double mouseX, double mouseY) {
        // While the color picker is open, capture clicks anywhere on the screen
        // — the picker often overflows the panel bounds onto the parent
        // inventory area, and clicks on those overflow swatches must still
        // route through to mouseClicked.
        if (colorPicker != null) {
            return true;
        }
        return inputHandler.isVisible(mouseX, mouseY);
    }

    /**
     * Gets the area where ingredients can be dropped in the active folder.
     *
     * @return A rectangle representing the drop area
     */
    @Override
    public Rect2i getContentDropArea() {
        return gridManager.getContentDropArea(isAddingFolder, buttonManager.getFolderRowsCount());
    }

    /**
     * Called when an ingredient is added to a folder via drag-and-drop.
     */
    @Override
    public void onIngredientAdded() {
        refreshIngredientSlots();
    }

    /**
     * Gets the total area occupied by the folder screen.
     *
     * @return A rectangle representing the screen's bounds
     */
    public Rect2i getScreenArea() {
        return new Rect2i(leftPos, topPos, width, height);
    }

    /**
     * Gets all folder buttons for drop handling.
     *
     * @return List of all folder buttons
     */
    public List<FolderButton> getFolderButtons() {
        return buttonManager.getFolderButtons();
    }

    /**
     * Gets drop targets for all folder buttons.
     *
     * @return List of folder button targets for drag-and-drop
     */
    @Override
    public List<FolderButtonTarget> getFolderButtonTargets() {
        return buttonManager.getFolderButtonTargets();
    }
}

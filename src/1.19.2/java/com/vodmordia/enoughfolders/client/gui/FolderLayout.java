package com.vodmordia.enoughfolders.client.gui;

/**
 * Layout constants for the folder overlay. Centralized so adjustments only
 * need to happen in one place — previously these magic numbers were copy-pasted
 * across {@link FolderScreen}, {@link FolderScreenRenderer},
 * {@link FolderButtonManager}, and {@link IngredientGridManager}.
 */
public final class FolderLayout {
    private FolderLayout() {}

    /** Vanilla {@code AbstractContainerScreen} image width — used to estimate where the parent panel ends. */
    public static final int STANDARD_CONTAINER_WIDTH = 176;

    /** Maximum width of the folder overlay panel. */
    public static final int MAX_PANEL_WIDTH = 387;

    /** Minimum margin between the panel right edge and the screen edge. */
    public static final int PANEL_RIGHT_MARGIN = 40;

    /** Width reduction applied when the JEI recipe GUI is open. */
    public static final int JEI_WIDTH_REDUCTION = 20;

    /** Minimum panel width when the JEI reduction kicks in. */
    public static final int MIN_PANEL_WIDTH = 70;

    /** Edge padding inside the panel. */
    public static final int PADDING = 5;

    /** Width/height of stock vanilla buttons (add, delete, pagination). */
    public static final int BUTTON_SIZE = 20;

    /**
     * Visual sprite size for every icon in the folder panel (folder buttons,
     * add (+), delete (X)). Centered within its containing button frame.
     */
    public static final int SPRITE_SIZE = 12;

    /** Scale factor applied to the 3-letter folder label rendered under each icon. */
    public static final float LABEL_SCALE = 0.7f;

    /**
     * Y offset for the folder label, measured from the bottom of the button
     * frame. Negative values pull the label up into the bottom edge of the
     * frame (the sprite leaves padding inside the frame, so this can be
     * negative without overlapping the icon).
     */
    public static final int LABEL_Y_OFFSET = -1;

    /** Width of each folder button. */
    public static final int FOLDER_WIDTH = 16;

    /** Height of each folder button — matches the 16x16 sprite. */
    public static final int FOLDER_HEIGHT = 16;

    /** Horizontal spacing between folder buttons in a row. */
    public static final int FOLDER_COLUMN_SPACING = 4;

    /** Vertical spacing between rows of folder buttons. */
    public static final int FOLDER_ROW_HEIGHT = 25;

    /** Height of the top folder area (incl. the add-folder button). */
    public static final int FOLDER_AREA_HEIGHT = 20;

    /** Height of the EditBox shown while creating a new folder. */
    public static final int INPUT_FIELD_HEIGHT = 20;

    /** Width/height of one ingredient slot in the content grid. */
    public static final int CONTENT_SLOT_SIZE = 18;

    /** Default number of ingredient rows in the content grid. */
    public static final int INGREDIENT_ROWS = 4;

    /** Vertical offset from {@link #FOLDER_AREA_HEIGHT} to the pagination row. */
    public static final int PAGINATION_OFFSET_Y = 32;

    /** Vertical offset from {@link #FOLDER_AREA_HEIGHT} to the start of the ingredient grid. */
    public static final int CONTENT_OFFSET_Y = 55;

    /** Default content area height used when sizing the panel for an active folder. */
    public static final int CONTENT_AREA_HEIGHT = 72;

    /**
     * Vertical offset added to ingredient/pagination positions when the input
     * box is visible and/or there are extra folder rows. Centralizes the math
     * that used to be copy-pasted in three files.
     */
    public static int verticalOffset(boolean isAddingFolder, int folderRowsCount) {
        int offset = isAddingFolder ? INPUT_FIELD_HEIGHT : 0;
        if (folderRowsCount > 1) {
            offset += (folderRowsCount - 1) * FOLDER_ROW_HEIGHT;
        }
        return offset;
    }
}

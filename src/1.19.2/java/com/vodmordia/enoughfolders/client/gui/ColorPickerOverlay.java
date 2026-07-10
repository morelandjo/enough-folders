package com.vodmordia.enoughfolders.client.gui;
import com.vodmordia.enoughfolders.EnoughFoldersCommon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vodmordia.enoughfolders.data.Folder;
import com.vodmordia.enoughfolders.data.FolderManager;
import net.minecraft.client.gui.GuiComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.item.DyeColor;

/**
 * Small popup that anchors next to a folder button and shows a 4x4 grid of
 * vanilla {@link DyeColor} swatches. Click a swatch to recolor the target
 * folder; click outside to dismiss.
 */
@Environment(EnvType.CLIENT)
public class ColorPickerOverlay {
    private static final DyeColor[] PALETTE = DyeColor.values(); // 16 colors
    private static final int COLS = 4;
    private static final int ROWS = 4;
    private static final int SWATCH = 12;
    private static final int PAD = 2;
    private static final int WIDTH = COLS * SWATCH + (COLS + 1) * PAD;
    private static final int HEIGHT = ROWS * SWATCH + (ROWS + 1) * PAD;
    private static final int BG_COLOR = 0xE0202020;
    private static final int BORDER_COLOR = 0xFF555555;
    private static final int HOVER_BORDER = 0xFFFFFFFF;

    private final int x;
    private final int y;
    private final Folder targetFolder;

    public ColorPickerOverlay(int anchorX, int anchorY, Folder targetFolder) {
        this.x = anchorX;
        this.y = anchorY;
        this.targetFolder = targetFolder;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return WIDTH; }
    public int getHeight() { return HEIGHT; }

    public boolean isOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEIGHT;
    }

    public void render(PoseStack poseStack, int mouseX, int mouseY) {
        GuiComponent.fill(poseStack, x, y, x + WIDTH, y + HEIGHT, BG_COLOR);
        GuiComponent.fill(poseStack, x, y, x + WIDTH, y + 1, BORDER_COLOR);
        GuiComponent.fill(poseStack, x, y + HEIGHT - 1, x + WIDTH, y + HEIGHT, BORDER_COLOR);
        GuiComponent.fill(poseStack, x, y, x + 1, y + HEIGHT, BORDER_COLOR);
        GuiComponent.fill(poseStack, x + WIDTH - 1, y, x + WIDTH, y + HEIGHT, BORDER_COLOR);

        for (int i = 0; i < PALETTE.length; i++) {
            int sx = swatchX(i);
            int sy = swatchY(i);
            int rgb = packDyeColor(PALETTE[i]) | 0xFF000000;
            GuiComponent.fill(poseStack, sx, sy, sx + SWATCH, sy + SWATCH, rgb);

            boolean hovered = mouseX >= sx && mouseX < sx + SWATCH && mouseY >= sy && mouseY < sy + SWATCH;
            int border = hovered ? HOVER_BORDER : BORDER_COLOR;
            GuiComponent.fill(poseStack, sx - 1, sy - 1, sx + SWATCH + 1, sy, border);
            GuiComponent.fill(poseStack, sx - 1, sy + SWATCH, sx + SWATCH + 1, sy + SWATCH + 1, border);
            GuiComponent.fill(poseStack, sx - 1, sy, sx, sy + SWATCH, border);
            GuiComponent.fill(poseStack, sx + SWATCH, sy, sx + SWATCH + 1, sy + SWATCH, border);
        }
    }

    /**
     * Returns true only if the click landed on a swatch and a color was
     * applied — the caller dismisses the picker on true. Returns false for
     * clicks in the gap between swatches (caller leaves the picker open).
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button, FolderManager folderManager) {
        if (button != 0) {
            return false;
        }
        for (int i = 0; i < PALETTE.length; i++) {
            int sx = swatchX(i);
            int sy = swatchY(i);
            if (mouseX >= sx && mouseX < sx + SWATCH && mouseY >= sy && mouseY < sy + SWATCH) {
                int rgb = packDyeColor(PALETTE[i]) & 0xFFFFFF;
                folderManager.setFolderColor(targetFolder, rgb);
                EnoughFoldersCommon.LOGGER.debug("Recolored folder '{}' to {} ({})",
                    targetFolder.getName(), PALETTE[i].getName(), Integer.toHexString(rgb));
                return true;
            }
        }
        return false;
    }

    /**
     * Packs {@link DyeColor#getTextureDiffuseColors()} (an RGB float triple)
     * into an 0xRRGGBB int. 1.21+ has {@code DyeColor.getTextureDiffuseColor()}
     * that returns the packed int directly; 1.19.2 and 1.20.1 only expose the float[].
     */
    private static int packDyeColor(DyeColor color) {
        float[] rgb = color.getTextureDiffuseColors();
        int r = Math.round(rgb[0] * 255f) & 0xFF;
        int g = Math.round(rgb[1] * 255f) & 0xFF;
        int b = Math.round(rgb[2] * 255f) & 0xFF;
        return (r << 16) | (g << 8) | b;
    }

    private int swatchX(int i) {
        int col = i % COLS;
        return x + PAD + col * (SWATCH + PAD);
    }

    private int swatchY(int i) {
        int row = i / COLS;
        return y + PAD + row * (SWATCH + PAD);
    }
}

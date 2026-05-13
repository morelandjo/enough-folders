package com.vodmordia.enoughfolders.client.gui;

import com.vodmordia.enoughfolders.data.StoredIngredient;
import com.vodmordia.enoughfolders.integrations.jei.core.JEIIntegration;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Optional;

/**
 * Represents a slot for displaying an ingredient in the active folder's view.
 */
public class IngredientSlot {
    /**
     * Position and content of this slot
     */
    private final int x;
    private final int y;
    private final StoredIngredient ingredient;
    private static final int SIZE = 18;

    /**
     * Cached resolution of the stored ingredient → live JEI object + renderer.
     * Resolving requires iterating all registered ingredient types, so we do
     * it once on first paint. Slot instances are rebuilt every time
     * {@code IngredientGridManager.refreshIngredientSlots} runs, so this cache
     * is naturally invalidated on any folder state change.
     */
    private boolean resolved;
    private Object cachedIngredient;
    private IIngredientRenderer<Object> cachedRenderer;

    /**
     * Creates a new ingredient slot.
     *
     * @param x The x position of the slot
     * @param y The y position of the slot
     * @param ingredient The ingredient to display in this slot
     */
    public IngredientSlot(int x, int y, StoredIngredient ingredient) {
        this.x = x;
        this.y = y;
        this.ingredient = ingredient;
    }

    /**
     * Renders this ingredient slot and its contents.
     *
     * @param graphics The graphics context to render with
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isHovered(mouseX, mouseY)) {
            graphics.fill(x, y, x + SIZE, y + SIZE, 0x80FFFFFF);
        }

        if (!resolved) {
            resolved = true;
            Optional<JEIIntegration.ResolvedRender> opt = JEIIntegration.get().resolveForRender(ingredient);
            if (opt.isPresent()) {
                cachedIngredient = opt.get().ingredient();
                cachedRenderer = opt.get().renderer();
            }
        }

        if (cachedRenderer != null) {
            cachedRenderer.render(graphics, cachedIngredient, x + 1, y + 1);
        }
    }

    /**
     * Handles mouse clicks on this ingredient slot.
     *
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     * @param button The mouse button that was clicked
     * @return true if the click was handled, false otherwise
     */
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!isHovered(mouseX, mouseY)) {
            return false;
        }

        if (button == 0) {
            showRecipes();
            return true;
        } else if (button == 1) {
            showUses();
            return true;
        }

        return false;
    }

    private void showRecipes() {
        JEIIntegration jei = JEIIntegration.get();
        if (cachedIngredient != null) {
            jei.showRecipes(cachedIngredient);
        } else {
            jei.getIngredientFromStored(ingredient).ifPresent(jei::showRecipes);
        }
    }

    private void showUses() {
        JEIIntegration jei = JEIIntegration.get();
        if (cachedIngredient != null) {
            jei.showUses(cachedIngredient);
        } else {
            jei.getIngredientFromStored(ingredient).ifPresent(jei::showUses);
        }
    }

    /**
     * Checks if the mouse is hovering over this ingredient slot.
     *
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     * @return true if the mouse is hovering over this slot, false otherwise
     */
    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + SIZE &&
               mouseY >= y && mouseY < y + SIZE;
    }

    /**
     * Gets the ingredient displayed in this slot.
     *
     * @return The stored ingredient
     */
    public StoredIngredient getIngredient() {
        return ingredient;
    }
}

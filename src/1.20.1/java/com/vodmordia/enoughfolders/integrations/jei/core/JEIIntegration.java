package com.vodmordia.enoughfolders.integrations.jei.core;
import com.vodmordia.enoughfolders.EnoughFoldersCommon;

import com.vodmordia.enoughfolders.data.StoredIngredient;
import com.vodmordia.enoughfolders.integrations.jei.gui.handlers.JEIRecipeGuiHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.runtime.IRecipesGui;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusFactory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

/**
 * Singleton integration with JEI. Reachable everywhere via {@link #get()};
 * methods that depend on the JEI runtime degrade gracefully (return empty
 * / no-op) until {@link #setJeiRuntime} is called by JEI's plugin lifecycle.
 */
@Environment(EnvType.CLIENT)
public final class JEIIntegration {
    private static final JEIIntegration INSTANCE = new JEIIntegration();

    public static JEIIntegration get() {
        return INSTANCE;
    }

    private JEIIntegration() {}

    /**
     * Resolved render pair for a {@link StoredIngredient}: the live ingredient
     * object plus the renderer that knows how to draw it. Cache this in the
     * slot — looking either up requires iterating every registered JEI
     * ingredient type, which is way too expensive to do per frame.
     */
    public record ResolvedRender(Object ingredient, IIngredientRenderer<Object> renderer) {}

    /**
     * Resolves the live ingredient object and its renderer in a single pass
     * over registered JEI ingredient types. Callers should cache the result
     * for the lifetime of the slot — if the JEI runtime is reloaded
     * mid-session (rare; once per game start), the player needs to reopen the
     * screen.
     */
    public Optional<ResolvedRender> resolveForRender(StoredIngredient stored) {
        if (jeiRuntime == null) {
            return Optional.empty();
        }
        try {
            String typeName = stored.getType();
            String value = stored.getValue();
            for (IIngredientType<?> type : jeiRuntime.getIngredientManager().getRegisteredIngredientTypes()) {
                if (!type.getIngredientClass().getName().equals(typeName)) {
                    continue;
                }
                @SuppressWarnings({"deprecation", "unchecked"})
                Optional<? extends ITypedIngredient<?>> typedIngredient =
                        jeiRuntime.getIngredientManager().getTypedIngredientByUid((IIngredientType) type, value);
                if (typedIngredient.isEmpty()) {
                    return Optional.empty();
                }
                Object ingredient = typedIngredient.get().getIngredient();
                @SuppressWarnings("unchecked")
                IIngredientRenderer<Object> renderer = (IIngredientRenderer<Object>)
                        jeiRuntime.getIngredientManager().getIngredientRenderer((IIngredientType<Object>) type);
                if (ingredient == null || renderer == null) {
                    return Optional.empty();
                }
                return Optional.of(new ResolvedRender(ingredient, renderer));
            }
        } catch (Exception e) {
            EnoughFoldersCommon.LOGGER.error("Failed to resolve ingredient for render", e);
        }
        return Optional.empty();
    }

    /**
     * The JEI runtime, obtained from the JEIPlugin
     */
    private IJeiRuntime jeiRuntime;

    /**
     * Currently tracked dragged ingredient
     */
    private Object currentDraggedObject = null;

    /**
     * Whether an ingredient is currently being dragged
     */
    private boolean isDragging = false;

    public Optional<?> getIngredientFromStored(StoredIngredient storedIngredient) {
        if (jeiRuntime == null) {
            return Optional.empty();
        }

        try {
            String typeName = storedIngredient.getType();
            String value = storedIngredient.getValue();

            for (IIngredientType<?> type : jeiRuntime.getIngredientManager().getRegisteredIngredientTypes()) {
                if (type.getIngredientClass().getName().equals(typeName)) {
                    @SuppressWarnings({"deprecation", "unchecked"})
                    Optional<? extends ITypedIngredient<?>> typedIngredient =
                            jeiRuntime.getIngredientManager().getTypedIngredientByUid((IIngredientType) type, value);

                    if (typedIngredient.isPresent()) {
                        return Optional.ofNullable(typedIngredient.get().getIngredient());
                    }
                }
            }
        } catch (Exception e) {
            EnoughFoldersCommon.LOGGER.error("Failed to get ingredient from stored data", e);
        }

        return Optional.empty();
    }

    public Optional<StoredIngredient> storeIngredient(Object ingredient) {
        if (jeiRuntime == null) {
            return Optional.empty();
        }

        try {
            Optional<? extends ITypedIngredient<?>> optTypedIngredient = jeiRuntime.getIngredientManager()
                    .createTypedIngredient(ingredient);

            if (optTypedIngredient.isPresent()) {
                ITypedIngredient<?> typedIngredient = optTypedIngredient.get();
                IIngredientType<?> ingredientType = typedIngredient.getType();
                IIngredientHelper<Object> helper = getHelperForType(ingredientType);

                if (helper != null) {
                    String typeClass = ingredientType.getIngredientClass().getName();
                    // JEI 15.x (1.20.1): `getUniqueId(I, UidContext) -> String`.
                    // Renamed to `getUid` returning Object in JEI 19+ (1.21+).
                    String uid = helper.getUniqueId(ingredient, UidContext.Ingredient);

                    return Optional.of(new StoredIngredient(typeClass, uid));
                }
            }
        } catch (Exception e) {
            EnoughFoldersCommon.LOGGER.error("Failed to store ingredient", e);
        }

        return Optional.empty();
    }

    /**
     * @deprecated re-resolves the ingredient and renderer on every call,
     * which iterates all registered JEI ingredient types twice. Prefer
     * {@link #resolveForRender(StoredIngredient)} cached on the call site.
     */
    @Deprecated
    public void renderIngredient(GuiGraphics graphics, StoredIngredient ingredient, int x, int y, int width, int height) {
        Optional<ResolvedRender> resolved = resolveForRender(ingredient);
        if (resolved.isPresent()) {
            ResolvedRender r = resolved.get();
            r.renderer().render(graphics, r.ingredient(), x, y);
        }
    }

    /**
     * Shows recipes for the provided ingredient in the JEI recipe GUI.
     *
     * @param ingredient The ingredient to show recipes for
     */
    public void showRecipes(Object ingredient) {
        if (jeiRuntime == null) {
            EnoughFoldersCommon.LOGGER.error("Cannot show recipes: JEI runtime is not available");
            return;
        }

        try {
            saveCurrentFolderScreen();

            IRecipesGui recipesGui = jeiRuntime.getRecipesGui();
            if (recipesGui != null) {
                Optional<? extends ITypedIngredient<?>> typedIngredient =
                    jeiRuntime.getIngredientManager().createTypedIngredient(ingredient);

                if (typedIngredient.isPresent()) {
                    IFocusFactory focusFactory = jeiRuntime.getJeiHelpers().getFocusFactory();

                    @SuppressWarnings("unchecked")
                    IFocus<?> focus = focusFactory.createFocus(
                        RecipeIngredientRole.OUTPUT,
                        (ITypedIngredient) typedIngredient.get()
                    );

                    recipesGui.show(focus);
                    EnoughFoldersCommon.LOGGER.debug("Successfully showed recipes for ingredient");
                } else {
                    EnoughFoldersCommon.LOGGER.error("Failed to create typed ingredient for showing recipes");
                }
            }
        } catch (Exception e) {
            EnoughFoldersCommon.LOGGER.error("Error showing recipes for ingredient", e);
        }
    }

    /**
     * Shows usages for the provided ingredient in the JEI recipe GUI.
     *
     * @param ingredient The ingredient to show usages for
     */
    public void showUses(Object ingredient) {
        if (jeiRuntime == null) {
            EnoughFoldersCommon.LOGGER.error("Cannot show uses: JEI runtime is not available");
            return;
        }

        try {
            saveCurrentFolderScreen();

            IRecipesGui recipesGui = jeiRuntime.getRecipesGui();
            if (recipesGui != null) {
                Optional<? extends ITypedIngredient<?>> typedIngredient =
                    jeiRuntime.getIngredientManager().createTypedIngredient(ingredient);

                if (typedIngredient.isPresent()) {
                    IFocusFactory focusFactory = jeiRuntime.getJeiHelpers().getFocusFactory();

                    @SuppressWarnings("unchecked")
                    IFocus<?> focus = focusFactory.createFocus(
                        RecipeIngredientRole.INPUT,
                        (ITypedIngredient) typedIngredient.get()
                    );

                    recipesGui.show(focus);
                    EnoughFoldersCommon.LOGGER.debug("Successfully showed uses for ingredient");
                } else {
                    EnoughFoldersCommon.LOGGER.error("Failed to create typed ingredient for showing uses");
                }
            }
        } catch (Exception e) {
            EnoughFoldersCommon.LOGGER.error("Error showing uses for ingredient", e);
        }
    }

    /**
     * Saves the current folder screen so it can be displayed on recipe screens.
     */
    private void saveCurrentFolderScreen() {
        Screen currentScreen = Minecraft.getInstance().screen;
        if (currentScreen instanceof AbstractContainerScreen<?>) {
            AbstractContainerScreen<?> containerScreen = (AbstractContainerScreen<?>) currentScreen;
            com.vodmordia.enoughfolders.client.event.ClientEventHandler.getFolderScreen(containerScreen)
                .ifPresent(folderScreen -> {
                    // Force a reinit of the folder screen before saving it
                    int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
                    int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
                    folderScreen.init(screenWidth, screenHeight);

                    JEIRecipeGuiHandler.saveLastFolderScreen(folderScreen);
                    EnoughFoldersCommon.LOGGER.debug("Saved folder screen for recipe/usage view");
                });
        }
    }

    /**
     * Helper method to get the ingredient helper for a specific ingredient type.
     *
     * @param <T> The type of ingredient
     * @param type The ingredient type class
     * @return The ingredient helper, or null if not found
     */
    @SuppressWarnings("unchecked")
    private <T> IIngredientHelper<T> getHelperForType(IIngredientType<?> type) {
        try {
            return (IIngredientHelper<T>) jeiRuntime.getIngredientManager().getIngredientHelper((IIngredientType<T>) type);
        } catch (Exception e) {
            EnoughFoldersCommon.LOGGER.error("Failed to get ingredient helper for type: " + type, e);
            return null;
        }
    }

    /**
     * Sets the JEI runtime reference.
     *
     * @param jeiRuntime The JEI runtime instance
     */
    public void setJeiRuntime(IJeiRuntime jeiRuntime) {
        this.jeiRuntime = jeiRuntime;
        EnoughFoldersCommon.LOGGER.info("JEI Runtime available, integration active");
    }

    /**
     * Gets the JEI runtime reference.
     *
     * @return Optional containing the JEI runtime, or empty if not available
     */
    public Optional<IJeiRuntime> getJeiRuntime() {
        return Optional.ofNullable(jeiRuntime);
    }

    /**
     * Checks if the JEI recipe GUI is currently open.
     *
     * @return true if the JEI recipe GUI is the current screen
     */
    public boolean isRecipeGuiOpen() {
        if (jeiRuntime == null) {
            return false;
        }

        try {
            IRecipesGui recipesGui = jeiRuntime.getRecipesGui();
            if (recipesGui == null) {
                return false;
            }

            // Check if the recipe GUI is the current screen
            Screen currentScreen = Minecraft.getInstance().screen;
            return currentScreen != null && currentScreen.equals(recipesGui);
        } catch (Exception e) {
            EnoughFoldersCommon.LOGGER.error("Error checking if JEI recipe GUI is open", e);
            return false;
        }
    }

    /**
     * Gets the ingredient currently being dragged in JEI, if any. Self-corrects
     * if no mouse button is held — JEI's onComplete hook is normally what
     * clears the state, but it can be missed (drop on a non-target screen,
     * world transition mid-drag, etc.).
     */
    public Optional<Object> getDraggedIngredient() {
        if (!isDragging || currentDraggedObject == null) {
            return Optional.empty();
        }
        long handle = Minecraft.getInstance().getWindow().getWindow();
        boolean anyButtonHeld =
            GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS
            || GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (!anyButtonHeld) {
            clearCurrentDraggedObject();
            return Optional.empty();
        }
        return Optional.of(currentDraggedObject);
    }

    public void setCurrentDraggedObject(Object ingredient) {
        this.currentDraggedObject = ingredient;
        this.isDragging = true;
    }

    public void clearCurrentDraggedObject() {
        this.currentDraggedObject = null;
        this.isDragging = false;
    }
}

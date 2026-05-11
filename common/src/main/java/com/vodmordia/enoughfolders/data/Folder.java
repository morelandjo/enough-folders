package com.vodmordia.enoughfolders.data;

import com.vodmordia.enoughfolders.EnoughFoldersCommon;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a folder
 */
public class Folder {
    /**
     * Unique identifier for this folder, used for serialization and equality checks.
     */
    private final String id;
    
    /**
     * Display name of the folder, shown in the UI.
     */
    private volatile String name;

    /**
     * Lazily computed truncated display name. Cleared on rename. Marked
     * transient so Gson doesn't try to serialize it.
     */
    private transient volatile String cachedTruncatedName;

    /**
     * List of ingredients stored in this folder. Always accessed under
     * {@code this}'s monitor.
     */
    private final List<StoredIngredient> ingredients;

    /**
     * Whether this folder is the currently active folder.
     */
    private volatile boolean active;

    /**
     * Tint color applied to the folder sprite, RGB packed (e.g. 0xFFAA00).
     * Field is left at the Java default (0) when missing from JSON; the
     * {@link #getColor} accessor maps that sentinel to {@link #DEFAULT_COLOR}.
     */
    private volatile int color;

    /**
     * Tint color used when no color is explicitly set on a folder. Matches
     * {@code DyeColor.YELLOW.getTextureDiffuseColor()} so newly-created folders
     * look like classic manila folders.
     */
    public static final int DEFAULT_COLOR = 0xFED83D;
    
    /**
     * Creates a new folder with the specified name.
     *
     * @param name The display name for the folder
     */
    public Folder(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.ingredients = new ArrayList<>();
        this.active = false;
        this.color = DEFAULT_COLOR;
        EnoughFoldersCommon.LOGGER.debug("Created new folder: " + name + " with ID: " + id);
    }
    
    /**
     * Creates a folder with specified properties.
     *
     * @param id The unique identifier for the folder
     * @param name The display name for the folder
     * @param ingredients The list of ingredients in the folder
     * @param active Whether the folder is active
     */
    public Folder(String id, String name, List<StoredIngredient> ingredients, boolean active) {
        this.id = id;
        this.name = name;
        this.ingredients = ingredients != null ? new ArrayList<>(ingredients) : new ArrayList<>();
        this.active = active;
        EnoughFoldersCommon.LOGGER.debug("Loaded existing folder: " + name + " with ID: " + id);
    }
    
    /**
     * Gets the unique identifier for this folder.
     *
     * @return The folder's UUID as a string
     */
    public String getId() {
        return id;
    }
    
    /**
     * Gets the display name of this folder.
     *
     * @return The folder's name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Changes the display name of this folder.
     *
     * @param name The new name for the folder
     */
    public void setName(String name) {
        String oldName = this.name;
        this.name = name;
        this.cachedTruncatedName = null;
        EnoughFoldersCommon.LOGGER.debug("Renamed folder from '" + oldName + "' to '" + name + "'");
    }
    
    /**
     * Gets an immutable snapshot of the ingredients stored in this folder.
     *
     * @return An immutable copy of the ingredient list
     */
    public synchronized List<StoredIngredient> getIngredients() {
        return List.copyOf(ingredients);
    }

    /**
     * Adds an ingredient to this folder if it's not already present.
     *
     * @param ingredient The ingredient to add to the folder
     */
    public synchronized void addIngredient(StoredIngredient ingredient) {
        if (!ingredients.contains(ingredient)) {
            ingredients.add(ingredient);
            EnoughFoldersCommon.LOGGER.debug("Added ingredient {}/{} to folder '{}'", ingredient.getType(), ingredient.getValue(), name);
        }
    }

    /**
     * Removes an ingredient from this folder.
     *
     * @param ingredient The ingredient to remove from the folder
     */
    public synchronized void removeIngredient(StoredIngredient ingredient) {
        ingredients.remove(ingredient);
        EnoughFoldersCommon.LOGGER.debug("Removed ingredient {}/{} from folder '{}'", ingredient.getType(), ingredient.getValue(), name);
    }
    
    /**
     * Checks if this folder is currently active.
     *
     * @return true if this folder is active, false otherwise
     */
    public boolean isActive() {
        return active;
    }
    
    /**
     * Sets whether this folder is active or inactive.
     *
     * @param active true to make this folder active, false to make it inactive
     */
    public void setActive(boolean active) {
        this.active = active;
        EnoughFoldersCommon.LOGGER.debug("Folder '{}' active state changed to: {}", name, active);
    }

    /**
     * Gets the tint color for this folder's sprite, defaulting to
     * {@link #DEFAULT_COLOR} if not set (e.g. for folders saved before the
     * color field existed).
     */
    public int getColor() {
        return color == 0 ? DEFAULT_COLOR : color;
    }

    public void setColor(int color) {
        this.color = color;
    }
    
    /**
     * Gets a shortened version of the folder name.
     *
     * @return The short name for this folder
     */
    public String getShortName() {
        if (name.length() <= 3) {
            return name;
        }
        return name.substring(0, 3);
    }
    
    /**
     * Gets a truncated version of the folder name for medium-sized UI elements.
     *
     * @return The truncated name for this folder
     */
    public String getTruncatedName() {
        String cached = cachedTruncatedName;
        if (cached != null) {
            return cached;
        }
        String current = name;
        String result = current.length() <= 12 ? current : current.substring(0, 13) + "...";
        cachedTruncatedName = result;
        return result;
    }
    
    /**
     * Checks if this folder is equal to another object.
     *
     * @param obj The object to compare with
     * @return true if the folders are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Folder folder = (Folder) obj;
        return id.equals(folder.id);
    }
    
    /**
     * Generates a hash code for this folder based on its ID.
     *
     * @return The hash code value
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

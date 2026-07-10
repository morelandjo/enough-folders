package com.vodmordia.enoughfolders.data;

import com.vodmordia.enoughfolders.EnoughFoldersCommon;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages all folders and their contents. All public methods are synchronized;
 * the returned folder list is an immutable snapshot so callers may iterate
 * without risk of {@link java.util.ConcurrentModificationException}.
 *
 * <p>Client-only: {@link #getCurrentWorldName} reaches into
 * {@link Minecraft#getInstance()} to derive the world identifier from the
 * active singleplayer server / connected multiplayer server, which only
 * exists on the client.
 */
@Environment(EnvType.CLIENT)
public class FolderManager {
    /**
     * The in-memory list of all folders for the current world. Always accessed
     * under {@code this}'s monitor.
     */
    private final List<Folder> folders = new ArrayList<>();

    private final StorageManager storageManager;

    /**
     * Constructs a new FolderManager and loads any existing folders from storage.
     */
    public FolderManager() {
        this.storageManager = new StorageManager();
        loadFolders();
    }

    /**
     * Gets all folders managed by this FolderManager.
     *
     * @return An immutable snapshot of the folder list
     */
    public synchronized List<Folder> getFolders() {
        return List.copyOf(folders);
    }

    /**
     * Gets the currently active folder.
     *
     * @return Optional containing the active folder, or empty if no folder is active
     */
    public synchronized Optional<Folder> getActiveFolder() {
        return folders.stream().filter(Folder::isActive).findFirst();
    }

    /**
     * Sets a folder as active and deactivates all other folders.
     *
     * @param folder The folder to set as active
     */
    public synchronized void setActiveFolder(Folder folder) {
        folders.forEach(f -> f.setActive(f.equals(folder)));
        saveFolders();
    }

    /**
     * Deactivates all folders.
     */
    public synchronized void clearActiveFolder() {
        folders.forEach(f -> f.setActive(false));
        saveFolders();
    }

    /**
     * Creates a new folder with the given name.
     *
     * @param name The name of the folder to create
     * @return The newly created folder
     */
    public synchronized Folder createFolder(String name) {
        Folder folder = new Folder(name);
        folders.add(folder);
        saveFolders();
        return folder;
    }

    /**
     * Deletes a folder and all its contents.
     *
     * @param folder The folder to delete
     */
    public synchronized void deleteFolder(Folder folder) {
        folders.remove(folder);
        saveFolders();
    }

    /**
     * Adds an ingredient to a specific folder.
     *
     * @param folder The folder to add the ingredient to
     * @param ingredient The ingredient to add
     */
    public synchronized void addIngredient(Folder folder, StoredIngredient ingredient) {
        folder.addIngredient(ingredient);
        saveFolders();
    }

    /**
     * Removes an ingredient from a specific folder.
     *
     * @param folder The folder to remove the ingredient from
     * @param ingredient The ingredient to remove
     */
    public synchronized void removeIngredient(Folder folder, StoredIngredient ingredient) {
        folder.removeIngredient(ingredient);
        saveFolders();
    }

    /**
     * Sets a folder's tint color and persists.
     */
    public synchronized void setFolderColor(Folder folder, int color) {
        folder.setColor(color);
        saveFolders();
    }

    /**
     * Reloads folders from disk based on the current world.
     */
    public synchronized void reloadFolders() {
        loadFolders();
    }

    private void loadFolders() {
        EnoughFoldersCommon.LOGGER.debug("Loading folders...");
        try {
            String worldName = getCurrentWorldName();
            List<Folder> loadedFolders = storageManager.loadFolders(worldName);
            folders.clear();
            folders.addAll(loadedFolders);
            EnoughFoldersCommon.LOGGER.debug("Loaded {} folders for world {}", folders.size(), worldName);
        } catch (Exception e) {
            EnoughFoldersCommon.LOGGER.error("Failed to load folders; keeping existing in-memory state", e);
        }
    }

    private void saveFolders() {
        try {
            storageManager.saveFolders(getCurrentWorldName(), folders);
        } catch (Exception e) {
            EnoughFoldersCommon.LOGGER.error("Failed to save folders", e);
        }
    }

    /**
     * Determines the current world name to use for saving/loading folders.
     *
     * @return A unique identifier for the current world
     */
    private String getCurrentWorldName() {
        Minecraft minecraft = Minecraft.getInstance();

        // If we're not in a world yet or the player isn't loaded, don't try to load folders
        if (minecraft.level == null || minecraft.player == null) {
            EnoughFoldersCommon.LOGGER.debug("Level or player not loaded yet, using default"
            );
            return "default";
        }

        // For multiplayer, use server name/ip
        if (!minecraft.isLocalServer()) {
            String serverName = minecraft.getCurrentServer() != null ?
                minecraft.getCurrentServer().name : "unknown_server";
            String worldName = "mp_" + serverName.replaceAll("[^a-zA-Z0-9_-]", "_");
            EnoughFoldersCommon.LOGGER.debug("Using multiplayer world name: {}",
                worldName
            );
            return worldName;
        }

        // For single player, use world name
        try {
            if (minecraft.getSingleplayerServer() != null) {
                // Get the save directory path
                java.nio.file.Path savesDir = minecraft.getSingleplayerServer().getWorldPath(
                    net.minecraft.world.level.storage.LevelResource.ROOT).getParent();

                if (savesDir != null) {
                    // The actual world name is the last part of the path
                    String worldName = savesDir.getFileName().toString();
                    EnoughFoldersCommon.LOGGER.debug("Using singleplayer world name: {}",
                        worldName
                    );
                    return worldName;
                }
            }

            // Last resort - use a unique identifier based on dimension and spawn point
            if (minecraft.level != null) {
                String dimensionKey = minecraft.level.dimension().location().toString();
                int spawnX = (int) minecraft.player.getX();
                int spawnZ = (int) minecraft.player.getZ();
                String levelId = "world_" + dimensionKey + "_" + Math.abs((spawnX * 31 + spawnZ) % 1000000);
                EnoughFoldersCommon.LOGGER.debug("Using derived level ID: {}",
                    levelId
                );
                return levelId;
            }

            EnoughFoldersCommon.LOGGER.debug("Could not determine world name, using default"
            );
            return "default";
        } catch (Exception e) {
            EnoughFoldersCommon.LOGGER.error("Failed to get world name", e);
            return "default";
        }
    }
}

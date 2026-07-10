package com.vodmordia.enoughfolders.data;

import com.vodmordia.enoughfolders.EnoughFoldersCommon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import dev.architectury.platform.Platform;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles saving and loading folder data to/from disk.
 */
public class StorageManager {
    /**
     * Current on-disk schema version. Bump when the JSON shape changes in
     * a non-backward-compatible way.
     */
    public static final int SCHEMA_VERSION = 1;

    private static final String CONFIG_DIR = "enough_folders";
    private static final String WORLDS_DIR = "worlds";
    private static final String FOLDERS_FILE = "folders.json";
    private static final String VERSION_KEY = "version";
    private static final String FOLDERS_KEY = "folders";

    private static final Type FOLDER_LIST_TYPE = new TypeToken<ArrayList<Folder>>(){}.getType();

    private final Gson gson;

    public StorageManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Loads folders for a specific world from disk. Accepts both the current
     * versioned envelope format and the legacy bare-array format; legacy files
     * are silently migrated on the next save.
     *
     * @param worldName The name or identifier for the world
     * @return List of folder objects loaded from disk; empty if no file exists
     * @throws IOException If the file exists but cannot be read
     */
    public List<Folder> loadFolders(String worldName) throws IOException {
        Path file = getFoldersFile(worldName);

        if (!Files.exists(file)) {
            return new ArrayList<>();
        }

        String content = Files.readString(file, StandardCharsets.UTF_8);
        if (content.isBlank()) {
            return new ArrayList<>();
        }

        JsonElement root = JsonParser.parseString(content);
        JsonArray foldersArray;
        if (root.isJsonObject() && root.getAsJsonObject().has(FOLDERS_KEY)) {
            foldersArray = root.getAsJsonObject().getAsJsonArray(FOLDERS_KEY);
        } else if (root.isJsonArray()) {
            // Legacy unversioned format — will be migrated on next save.
            foldersArray = root.getAsJsonArray();
        } else {
            EnoughFoldersCommon.LOGGER.warn("Unrecognized folders.json shape for world {}; starting empty", worldName);
            return new ArrayList<>();
        }

        List<Folder> folders = gson.fromJson(foldersArray, FOLDER_LIST_TYPE);
        return folders != null ? folders : new ArrayList<>();
    }

    /**
     * Saves folders atomically: writes to a sibling .tmp file and then
     * replaces the target via {@link StandardCopyOption#ATOMIC_MOVE}, so a
     * crash mid-write cannot corrupt the existing file.
     *
     * @param worldName The name or identifier for the world
     * @param folders The list of folders to save
     * @throws IOException If the directory cannot be created or the file cannot be written
     */
    public void saveFolders(String worldName, List<Folder> folders) throws IOException {
        Path file = getFoldersFile(worldName);
        Files.createDirectories(file.getParent());

        JsonObject envelope = new JsonObject();
        envelope.addProperty(VERSION_KEY, SCHEMA_VERSION);
        envelope.add(FOLDERS_KEY, gson.toJsonTree(folders));
        String json = gson.toJson(envelope);

        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, json, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            // Filesystem doesn't support atomic moves (e.g. some network mounts,
            // cross-device temp dirs). The exception itself IS the signal to fall
            // back to a non-atomic replace; we lose the crash-safety guarantee
            // for this single write but folder data is still saved.
            EnoughFoldersCommon.LOGGER.debug("ATOMIC_MOVE not supported for {}; falling back to non-atomic replace", file);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path getFoldersFile(String worldName) {
        Path configPath = Platform.getConfigFolder();
        return configPath.resolve(CONFIG_DIR).resolve(WORLDS_DIR).resolve(worldName).resolve(FOLDERS_FILE);
    }
}

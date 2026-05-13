package com.vodmordia.enoughfolders;

import com.vodmordia.enoughfolders.data.FolderManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Platform-agnostic entry point and shared constants. Platform-specific entry
 * points (e.g. the Forge {@code @Mod} class, the Fabric {@code ClientModInitializer})
 * call {@link #init()} during mod construction and, on client, set the
 * {@link FolderManager} via {@link #setFolderManager}.
 */
public final class EnoughFoldersCommon {
    public static final String MOD_ID = "enoughfolders";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Environment(EnvType.CLIENT)
    private static FolderManager folderManager;

    private EnoughFoldersCommon() {}

    public static void init() {
        LOGGER.info("Enough Folders common initialization");
    }

    /**
     * Installs the client-only {@link FolderManager} singleton. Called from
     * each platform's client entry point during mod construction.
     */
    @Environment(EnvType.CLIENT)
    public static void setFolderManager(FolderManager manager) {
        folderManager = manager;
    }

    /**
     * Returns the client-only {@link FolderManager}. Will throw NPE if accessed
     * before the platform entry point has installed it — which only happens if
     * common-side client code runs at server-only init time. Don't.
     */
    @Environment(EnvType.CLIENT)
    public static FolderManager getFolderManager() {
        return folderManager;
    }
}

package com.vodmordia.enoughfolders;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Platform-agnostic entry point and shared constants. Platform-specific entry
 * points (e.g. the NeoForge {@code @Mod} class) call {@link #init()} during
 * mod construction.
 */
public final class EnoughFoldersCommon {
    public static final String MOD_ID = "enoughfolders";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private EnoughFoldersCommon() {}

    public static void init() {
        LOGGER.info("Enough Folders common initialization");
    }
}

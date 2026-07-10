package com.vodmordia.enoughfolders.neoforge;

import com.vodmordia.enoughfolders.EnoughFoldersCommon;
import com.vodmordia.enoughfolders.client.input.KeyBindings;
import com.vodmordia.enoughfolders.data.FolderManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.apache.logging.log4j.Logger;

/**
 * NeoForge entry point for Enough Folders.
 */
@Mod(EnoughFolders.MOD_ID)
public class EnoughFolders {
    public static final String MOD_ID = EnoughFoldersCommon.MOD_ID;
    public static final Logger LOGGER = EnoughFoldersCommon.LOGGER;

    private static EnoughFolders instance;

    @OnlyIn(Dist.CLIENT)
    private FolderManager folderManager;

    public EnoughFolders(IEventBus modEventBus) {
        instance = this;

        EnoughFoldersCommon.init();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            LOGGER.info("Initializing Enough Folders client components");

            folderManager = new FolderManager();
            LOGGER.debug("Folder manager created");

            modEventBus.register(KeyBindings.class);
            KeyBindings.init();
            LOGGER.debug("Key bindings initialized");

            NeoForge.EVENT_BUS.register(this);
        }

        LOGGER.info("Enough Folders initialized");
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onRegisterCommands(RegisterCommandsEvent event) {
        // Command registration code (if any) goes here
    }

    public static EnoughFolders getInstance() {
        return instance;
    }

    @OnlyIn(Dist.CLIENT)
    public FolderManager getFolderManager() {
        return folderManager;
    }
}

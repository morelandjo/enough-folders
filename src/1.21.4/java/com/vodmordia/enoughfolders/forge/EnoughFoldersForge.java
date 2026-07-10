package com.vodmordia.enoughfolders.forge;

import com.vodmordia.enoughfolders.EnoughFoldersCommon;
import com.vodmordia.enoughfolders.data.FolderManager;
import com.vodmordia.enoughfolders.forge.client.input.ForgeKeyBindings;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Forge entry point for Enough Folders.
 */
@Mod(EnoughFoldersCommon.MOD_ID)
public class EnoughFoldersForge {

    public EnoughFoldersForge() {
        EnoughFoldersCommon.init();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            EnoughFoldersCommon.LOGGER.info("Initializing Enough Folders client components (Forge)");

            EnoughFoldersCommon.setFolderManager(new FolderManager());

            IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
            modEventBus.register(ForgeKeyBindings.class);
            ForgeKeyBindings.init();
        }

        EnoughFoldersCommon.LOGGER.info("Enough Folders initialized (Forge)");
    }
}

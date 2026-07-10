package com.vodmordia.enoughfolders.fabric;

import com.vodmordia.enoughfolders.EnoughFoldersCommon;
import com.vodmordia.enoughfolders.client.event.ClientEventHandler;
import com.vodmordia.enoughfolders.data.FolderManager;
import com.vodmordia.enoughfolders.fabric.client.event.FabricScreenEvents;
import com.vodmordia.enoughfolders.fabric.client.input.FabricKeyBindings;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/**
 * Fabric client entry point. Wires up the common-side
 * {@link ClientEventHandler} dispatcher to Fabric API events.
 */
public final class EnoughFoldersFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EnoughFoldersCommon.init();
        EnoughFoldersCommon.LOGGER.info("Initializing Enough Folders client components (Fabric)");

        EnoughFoldersCommon.setFolderManager(new FolderManager());

        FabricKeyBindings.init();
        FabricScreenEvents.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientEventHandler.onClientTick());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ClientEventHandler.onClientPlayerLogin());

        EnoughFoldersCommon.LOGGER.info("Enough Folders initialized (Fabric)");
    }
}

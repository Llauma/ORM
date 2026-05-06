package com.lauma;

import com.lauma.client.input.ItemSelectionHandler;
import com.lauma.client.resource.ORMModelLoadingPlugin;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OverrideResourceManager implements ModInitializer, ClientModInitializer {
    public static final String MOD_ID = "orm";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("ORM initialized.");
    }

    @Override
    public void onInitializeClient() {
        ItemSelectionHandler.register();
        ORMModelLoadingPlugin.register();
    }
}
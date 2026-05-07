package com.lauma;

import com.lauma.client.input.ItemSelectionHandler;
import com.lauma.client.input.ORMKeyBindings;
import com.lauma.client.resource.ORMModelLoadingPlugin;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OverrideResourceManager implements ModInitializer {
    public static final String MOD_ID = "orm";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ORMKeyBindings.register();
        ItemSelectionHandler.register();
        ORMModelLoadingPlugin.register();
        LOGGER.info("ORM initialized.");
    }

}
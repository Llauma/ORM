package com.lauma.client.resource;

import com.lauma.OverrideResourceManager;
import com.lauma.config.ORMConfigManager;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Registers user-supplied JSON models from {@code config/orm/models/} (recursively) so the
 * BakedModelManager loads and bakes them.
 *
 * <p>Layout convention:
 * <ul>
 *   <li>{@code config/orm/models/foo.json}              registers {@code orm:item/foo}     (flat, legacy)</li>
 *   <li>{@code config/orm/models/item/foo.json}         registers {@code orm:item/foo}     (mirrors resource pack)</li>
 *   <li>{@code config/orm/models/weapons/foo.json}      registers {@code orm:weapons/foo}  (custom subfolder)</li>
 * </ul>
 *
 * <p>Reference these in {@code overrides.json} via the same identifier (e.g. {@code "model": "orm:weapons/foo"}).
 * For backward compatibility, an unqualified path (e.g. {@code "orm:foo"}) resolves to {@code orm:item/foo}.
 */
public class ORMModelLoadingPlugin implements ModelLoadingPlugin {

    public static void register() {
        ModelLoadingPlugin.register(new ORMModelLoadingPlugin());
    }

    @Override
    public void initialize(Context context) {
        List<Identifier> ids = collectUserModelIds();
        if (ids.isEmpty()) {
            OverrideResourceManager.LOGGER.info("ORM: no user models in {}", ORMConfigManager.MODEL_DIR);
            return;
        }
        for (Identifier id : ids) {
            OverrideResourceManager.LOGGER.info("ORM: registering user model {}", id);
        }
        context.addModels(ids);
    }

    private static List<Identifier> collectUserModelIds() {
        List<Identifier> ids = new ArrayList<>();
        Path root = ORMConfigManager.MODEL_DIR;
        if (!Files.isDirectory(root)) return ids;
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .forEach(p -> {
                        Path rel = root.relativize(p);
                        String relStr = rel.toString().replace('\\', '/');
                        String stem = relStr.substring(0, relStr.length() - ".json".length());

                        if (stem.contains("/")) {
                            // Nested file: mirror the path 1:1 (e.g. weapons/foo -> orm:weapons/foo).
                            ids.add(Identifier.of("orm", stem));
                        } else {
                            // Flat file: register under the conventional item/ subpath
                            // so the user can reference it as "orm:foo" or "orm:item/foo".
                            ids.add(Identifier.of("orm", "item/" + stem));
                        }
                    });
        } catch (IOException e) {
            OverrideResourceManager.LOGGER.warn("ORM: failed to scan models dir for plugin", e);
        }
        return ids;
    }
}

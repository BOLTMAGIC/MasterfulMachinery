package io.ticticboom.mods.mm.setup.loader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ticticboom.mods.mm.Ref;
import lombok.SneakyThrows;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class AbstractConfigLoader<TModel> {

    protected abstract String getConfigPath();

    protected abstract List<TModel> parseModels(JsonObject json);

    protected abstract String getModelId(TModel model);

    protected abstract void registerModels(List<TModel> models);

    /**
     * Everything here happens per file so that a failure can name the file that caused it. A pack
     * with a typo used to surface as a bare exception from somewhere inside parsing, with nothing
     * saying which of the author's files was at fault.
     */
    public void load() {
        var models = new ArrayList<TModel>();
        var sources = new HashMap<String, Path>();
        for (Path path : findConfigFiles()) {
            JsonObject json = parseJson(path);
            List<TModel> parsed;
            try {
                parsed = parseModels(json);
            } catch (Exception e) {
                throw describe(path, e);
            }
            for (TModel model : parsed) {
                String id = getModelId(model);
                Path previous = sources.put(id, path);
                if (previous != null) {
                    throw duplicate(id, previous, path);
                }
            }
            models.addAll(parsed);
        }
        registerModels(models);
    }

    @SneakyThrows
    private List<Path> findConfigFiles() {
        Path root = FMLPaths.CONFIGDIR.get().resolve("mm").resolve(getConfigPath());
        if (!Files.exists(root)) {
            Files.createDirectories(root);
        }
        try (var files = Files.walk(root, FileVisitOption.FOLLOW_LINKS)) {
            return files.filter(x -> x.toString().endsWith(".json")).sorted().toList();
        }
    }

    private static JsonObject parseJson(Path path) {
        try {
            var file = Files.readString(path);
            var parsed = JsonParser.parseString(file);
            if (!parsed.isJsonObject()) {
                throw new RuntimeException("expected a JSON object at the top level, found " + parsed);
            }
            return parsed.getAsJsonObject();
        } catch (Exception e) {
            throw describe(path, e);
        }
    }

    private static RuntimeException duplicate(String id, Path first, Path second) {
        var message = "Two Masterful Machinery config files both define '" + id + "'. Remove one of them, "
                + "or give it an id of its own: " + first + " and " + second;
        Ref.LOG.fatal(message);
        return new RuntimeException(message);
    }

    private static RuntimeException describe(Path path, Exception cause) {
        var message = "Failed to load Masterful Machinery config file " + path + ": " + cause.getMessage();
        Ref.LOG.fatal(message);
        return new RuntimeException(message, cause);
    }
}

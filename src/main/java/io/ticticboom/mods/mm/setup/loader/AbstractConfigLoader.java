package io.ticticboom.mods.mm.setup.loader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ticticboom.mods.mm.Ref;
import lombok.SneakyThrows;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public abstract class AbstractConfigLoader<TModel> {

    protected abstract String getConfigPath();

    protected abstract List<TModel> parseModels(List<JsonObject> jsons);

    protected abstract void registerModels(List<TModel> models);

    public void load() {
        var jsons = getJsons();
        var models = parseModels(jsons);
        registerModels(models);
    }

    @SneakyThrows
    private List<JsonObject> getJsons() {
        Path root = FMLPaths.CONFIGDIR.get().resolve("mm").resolve(getConfigPath());
        if (!Files.exists(root)) {
            Files.createDirectories(root);
        }
        return Files.walk(root, FileVisitOption.FOLLOW_LINKS).filter(x -> x.toString().endsWith(".json")).map(AbstractConfigLoader::parseJson).toList();
    }

    private static JsonObject parseJson(Path path) {
        try {
            var file = Files.readString(path);
            return JsonParser.parseString(file).getAsJsonObject();
        } catch (IOException e) {
            Ref.LOG.fatal("Failed to read file {}", path);
            throw new RuntimeException(e);
        }
    }
}

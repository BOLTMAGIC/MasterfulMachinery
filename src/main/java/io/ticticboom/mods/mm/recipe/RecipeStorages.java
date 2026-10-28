package io.ticticboom.mods.mm.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

public final class RecipeStorages {

    private final List<IPortStorage> inputStorages;
    private final List<IPortStorage> outputStorages;
    private final List<IPortBlockEntity> sources;
    private final Map<Class<?>, List<?>> inputsByType = new HashMap<>();
    private final Map<Class<?>, List<?>> outputsByType = new HashMap<>();
    private final ItemPortIndex inputItemPortIndex;
    private final ItemPortIndex outputItemPortIndex;
    private final LongSupplier changes;

    public RecipeStorages(List<IPortStorage> inputStorages, List<IPortStorage> outputStorages) {
        this(inputStorages, outputStorages, List.of());
    }

    public RecipeStorages(List<IPortStorage> inputStorages, List<IPortStorage> outputStorages, List<IPortBlockEntity> sources) {
        this(inputStorages, outputStorages, sources, () -> 0);
    }

    public RecipeStorages(List<IPortStorage> inputStorages, List<IPortStorage> outputStorages, LongSupplier changes) {
        this(inputStorages, outputStorages, List.of(), changes);
    }

    private RecipeStorages(List<IPortStorage> inputStorages, List<IPortStorage> outputStorages, List<IPortBlockEntity> sources, LongSupplier changes) {
        this.changes = changes;
        this.inputStorages = List.copyOf(inputStorages);
        this.outputStorages = List.copyOf(outputStorages);
        this.sources = List.copyOf(sources);
        this.inputItemPortIndex = new ItemPortIndex(this.inputStorages);
        this.outputItemPortIndex = new ItemPortIndex(this.outputStorages);
    }

    public List<IPortStorage> inputStorages() {
        return inputStorages;
    }

    public List<IPortStorage> outputStorages() {
        return outputStorages;
    }

    public List<IPortBlockEntity> sources() {
        return sources;
    }

    public List<ItemPortStorage> getInputItemStorages(Item item) {
        return inputItemPortIndex.forItem(item);
    }

    public List<ItemPortStorage> getOutputItemStorages(Item item) {
        var candidates = new LinkedHashSet<>(outputItemPortIndex.forItem(item));
        candidates.addAll(outputItemPortIndex.withEmptySlots());
        return new ArrayList<>(candidates);
    }

    public <T extends IPortStorage> List<T> getInputStorages(Class<T> clz) {
        return getStorages(clz, inputStorages, inputsByType);
    }

    public <T extends IPortStorage> List<T> getOutputStorages(Class<T> clz) {
        return getStorages(clz, outputStorages, outputsByType);
    }

    public <T extends IPortStorage> List<T> getOutputStoragesByPriority(Class<T> clz) {
        var storages = getOutputStorages(clz);
        if (storages.size() < 2) {
            return storages;
        }
        var sorted = new ArrayList<>(storages);
        sorted.sort(Comparator.comparingInt(IPortStorage::getPriority).reversed());
        return sorted;
    }

    public long changeCount() {
        long total = changes.getAsLong();
        for (IPortBlockEntity source : sources) {
            total += source.changeCount();
        }
        return total;
    }

    @SuppressWarnings("unchecked")
    private static <T extends IPortStorage> List<T> getStorages(Class<T> clz, List<IPortStorage> storages, Map<Class<?>, List<?>> cache) {
        var cached = cache.get(clz);
        if (cached != null) {
            return (List<T>) cached;
        }
        var result = new ArrayList<T>();
        for (IPortStorage storage : storages) {
            if (clz.isInstance(storage)) {
                result.add((T) storage);
            }
        }
        var frozen = List.copyOf(result);
        cache.put(clz, frozen);
        return frozen;
    }

    public JsonObject debug() {
        var inputsJson = new JsonArray();
        var outputsJson = new JsonArray();
        for (IPortStorage inputStorage : inputStorages) {
            inputsJson.add(inputStorage.debugDump());
        }
        for (IPortStorage outputStorage : outputStorages) {
            outputsJson.add(outputStorage.debugDump());
        }
        var json = new JsonObject();
        json.add("inputs", inputsJson);
        json.add("outputs", outputsJson);
        return json;
    }
}

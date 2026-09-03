package io.ticticboom.mods.mm.controller.machine.register;

import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.port.fluid.FluidPortStorage;
import io.ticticboom.mods.mm.port.energy.EnergyPortStorage;
import io.ticticboom.mods.mm.port.botania.mana.BotaniaManaPortStorage;
import io.ticticboom.mods.mm.port.pneumaticcraft.air.PneumaticAirPortStorage;
import io.ticticboom.mods.mm.port.kinetic.CreateKineticPortStorage;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorage;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages caching of available resources in controller storages.
 * Caches item IDs, fluid IDs, energy availability, and other resource types to avoid expensive
 * repeated scans through large inventories.
 */
public class StorageCacheManager {

    // Cache expiration intervals
    public static final int ACTIVE_RECIPE_CACHE_INTERVAL_TICKS = 2;
    public static final int IDLE_RESOURCE_SCAN_INTERVAL_TICKS = 5;

    /**
     * Container for all cached storage information
     */
    public static class StorageCache {
        public Set<ResourceLocation> availableItemIds;
        public Set<ResourceLocation> availableFluidIds;
        public Set<ResourceLocation> availableMekanismIds;
        public Map<ResourceLocation, List<ResourceLocation>> availableItemStackKeys;
        public boolean hasEnergyAvailable;
        public boolean hasManaAvailable;
        public boolean hasPneumaticAir;
        public boolean hasKinetic;
        public boolean hasMekanismChemical;
        public boolean isValid;

        public StorageCache() {
            this.availableItemIds = new HashSet<>();
            this.availableFluidIds = new HashSet<>();
            this.availableMekanismIds = new HashSet<>();
            this.availableItemStackKeys = new HashMap<>();
            this.isValid = false;
        }

        public void clear() {
            availableItemIds.clear();
            availableFluidIds.clear();
            availableMekanismIds.clear();
            availableItemStackKeys.clear();
            hasEnergyAvailable = false;
            hasManaAvailable = false;
            hasPneumaticAir = false;
            hasKinetic = false;
            hasMekanismChemical = false;
            isValid = false;
        }
    }

    /**
     * Rebuild the storage cache from current port storages
     */
    public static void rebuildStorageCache(RecipeStorages portStorages, StorageCache cache) {
        cache.clear();

        if (portStorages == null) {
            cache.isValid = true;
            return;
        }

        // Scan items
        var itemStorages = portStorages.getInputStorages(ItemPortStorage.class);
        for (ItemPortStorage s : itemStorages) {
            var handler = s.getHandler();
            if (handler == null) continue;
            for (int i = 0; i < handler.getSlots(); i++) {
                var stack = handler.getStackInSlot(i);
                int actual = handler.getActualCount(i);
                if (!stack.isEmpty() && actual > 0) {
                    var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
                    if (key != null) {
                        cache.availableItemIds.add(key);
                        // Compute NBT fingerprint for this exact stack
                        ResourceLocation composed = key;
                        if (stack.hasTag()) {
                            try {
                                String json = io.ticticboom.mods.mm.util.NbtMatchUtils.toJson(stack.getTag()).toString();
                                String hex = Integer.toHexString(json.hashCode());
                                String namespaced = key.getNamespace() + ":" + key.getPath() + "__W__" + hex;
                                var parsed = ResourceLocation.tryParse(namespaced);
                                if (parsed != null) composed = parsed;
                            } catch (Throwable ignored) { }
                        }
                        cache.availableItemStackKeys.computeIfAbsent(key, k -> new ArrayList<>()).add(composed);
                    }
                }
            }
        }

        // Scan fluids
        var fluidStorages = portStorages.getInputStorages(FluidPortStorage.class);
        for (FluidPortStorage s : fluidStorages) {
            var handler = s.getHandler();
            if (handler == null) continue;
            for (int i = 0; i < handler.getTanks(); i++) {
                var fs = handler.getFluidInTank(i);
                if (fs.getAmount() > 0) {
                    var key = ForgeRegistries.FLUIDS.getKey(fs.getFluid());
                    if (key != null) cache.availableFluidIds.add(key);
                }
            }
        }

        // Scan energy
        var energyStorages = portStorages.getInputStorages(EnergyPortStorage.class);
        for (EnergyPortStorage s : energyStorages) {
            if (s.getStoredEnergy() > 0) { cache.hasEnergyAvailable = true; break; }
        }

        // Scan mana
        var manaStorages = portStorages.getInputStorages(BotaniaManaPortStorage.class);
        for (BotaniaManaPortStorage s : manaStorages) {
            if (s.getStored() > 0) { cache.hasManaAvailable = true; break; }
        }

        // Scan pneumatic air
        var pneuStorages = portStorages.getInputStorages(PneumaticAirPortStorage.class);
        for (PneumaticAirPortStorage s : pneuStorages) {
            if (s.getAir() > 0) { cache.hasPneumaticAir = true; break; }
        }

        // Scan kinetic
        var kineticStorages = portStorages.getInputStorages(CreateKineticPortStorage.class);
        for (CreateKineticPortStorage s : kineticStorages) {
            if (s.getSpeed() > 0) { cache.hasKinetic = true; break; }
        }

        // Scan mekanism chemicals
        //noinspection rawtypes
        for (MekanismChemicalPortStorage s : portStorages.getInputStorages(MekanismChemicalPortStorage.class)) {
            try {
                var stack = s.chemicalTank.getStack();
                if (!stack.isEmpty() && stack.getAmount() > 0) {
                    try {
                        var rl = stack.getType().getRegistryName();
                        cache.availableMekanismIds.add(rl);
                    } catch (Throwable ignored) { }
                    cache.hasMekanismChemical = true;
                }
            } catch (Throwable ignored) { }
        }

        cache.isValid = true;
    }
}


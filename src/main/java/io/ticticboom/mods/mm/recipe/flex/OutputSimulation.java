package io.ticticboom.mods.mm.recipe.flex;

import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Puts outputs into copies of the machine's output ports, to know whether several outputs fit together
 * (checking each alone would count the same empty slot or tank for all of them). The real ports aren't touched.
 * Only outputs with {@link IPortIngredient#canSimulateOutput()} may be added: ports that can't be copied stay the
 * real ones here.
 */
public class OutputSimulation {

    private final List<IPortStorage> copies = new ArrayList<>();
    private final RecipeStorages storages;
    private final HolderLookup.Provider registries;

    public OutputSimulation(Level level, RecipeStorages real) {
        registries = level.registryAccess();
        var outputs = new ArrayList<IPortStorage>();
        for (IPortStorage storage : real.outputStorages()) {
            IPortStorage copy = storage.simulationCopy(registries);
            if (copy != null) {
                copies.add(copy);
                outputs.add(copy);
            } else {
                outputs.add(storage);
            }
        }
        storages = new RecipeStorages(real.inputStorages(), outputs);
    }

    /**
     * Adds these outputs if all of them fit next to what was added before. {@code state} is the run's own state:
     * amounts and chances rolled into it decide what is added, as they will when the run finishes.
     *
     * @return false (and nothing added) if one doesn't fit
     */
    public boolean tryAdd(Level level, List<? extends IPortIngredient> outputs, RecipeStateModel state) {
        var before = new ArrayList<CompoundTag>();
        for (IPortStorage copy : copies) {
            before.add(copy.save(new CompoundTag(), registries));
        }
        for (IPortIngredient output : outputs) {
            if (!output.canOutput(level, storages, state)) {
                for (int i = 0; i < copies.size(); i++) {
                    copies.get(i).load(before.get(i), registries);
                }
                return false;
            }
            output.output(level, storages, state);
        }
        return true;
    }
}

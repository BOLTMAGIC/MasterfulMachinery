package io.ticticboom.mods.mm.recipe.flex;

import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
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
    private final RecipeStateModel scratch = new RecipeStateModel();

    public OutputSimulation(RecipeStorages real) {
        var outputs = new ArrayList<IPortStorage>();
        for (IPortStorage storage : real.outputStorages()) {
            IPortStorage copy = storage.simulationCopy();
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
     * Adds these outputs if all of them fit next to what was added before.
     *
     * @return false (and nothing added) if one doesn't fit
     */
    public boolean tryAdd(Level level, List<? extends IPortIngredient> outputs) {
        var before = new ArrayList<CompoundTag>();
        for (IPortStorage copy : copies) {
            before.add(copy.save(new CompoundTag()));
        }
        for (IPortIngredient output : outputs) {
            if (!output.canOutput(level, storages, scratch)) {
                for (int i = 0; i < copies.size(); i++) {
                    copies.get(i).load(before.get(i));
                }
                return false;
            }
            output.output(level, storages, scratch);
        }
        return true;
    }
}

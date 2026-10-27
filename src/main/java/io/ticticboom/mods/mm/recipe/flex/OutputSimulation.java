package io.ticticboom.mods.mm.recipe.flex;

import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.recipe.ItemPortIndex;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
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

    private final RecipeStorages real;
    // output ports in the real order: a copy where the port can be copied, else the real port
    private List<IPortStorage> outputs = new ArrayList<>();
    private final List<Boolean> copied = new ArrayList<>();
    private final RecipeStateModel scratch = new RecipeStateModel();

    public OutputSimulation(RecipeStorages real) {
        this.real = real;
        for (IPortStorage storage : real.outputStorages()) {
            IPortStorage copy = storage.simulationCopy();
            copied.add(copy != null);
            outputs.add(copy != null ? copy : storage);
        }
    }

    /**
     * Adds these outputs if all of them fit next to what was added before.
     *
     * @return false (and nothing added) if one doesn't fit
     */
    public boolean tryAdd(Level level, List<? extends IPortIngredient> outputs) {
        // the outputs go into fresh copies of the copies, kept only if all of them fit
        var trial = new ArrayList<IPortStorage>(this.outputs.size());
        for (int i = 0; i < this.outputs.size(); i++) {
            IPortStorage storage = this.outputs.get(i);
            trial.add(copied.get(i) ? storage.simulationCopy() : storage);
        }
        var trialStorages = new RecipeStorages(real.inputStorages(), trial, real.inputItemPortIndex(), new ItemPortIndex(trial));
        for (IPortIngredient output : outputs) {
            if (!output.canOutput(level, trialStorages, scratch)) {
                return false;
            }
            output.output(level, trialStorages, scratch);
        }
        this.outputs = trial;
        return true;
    }
}

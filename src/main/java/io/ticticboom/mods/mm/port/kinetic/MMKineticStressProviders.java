package io.ticticboom.mods.mm.port.kinetic;

import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortBlock;
import net.minecraft.world.level.block.Block;
import org.apache.commons.lang3.tuple.Pair;

import java.util.function.DoubleSupplier;

public class MMKineticStressProviders {
    private static Pair<PortModel, CreateKineticPortStorageModel> getModel(Block block) {
        if (block instanceof IPortBlock pb) {
            var storageModel = pb.getModel().config().getModel();
            if (storageModel instanceof CreateKineticPortStorageModel psm) {
                return Pair.of(pb.getModel(), psm);
            }
        }
        return null;
    }

    public static double getImpact(Block block) {
        var models = getModel(block);
        if (models == null) {
            return 0d;
        }
        return models.getLeft().input() ? models.getRight().stress() : 0d;
    }

    public static double getCapacity(Block block) {
        var models = getModel(block);
        if (models == null) {
            return 0;
        }
        return !models.getLeft().input() ? models.getRight().stress() : 0;
    }

    public static DoubleSupplier impactSupplier(Block block) {
        return () -> getImpact(block);
    }

    public static DoubleSupplier capacitySupplier(Block block) {
        return () -> getCapacity(block);
    }
}

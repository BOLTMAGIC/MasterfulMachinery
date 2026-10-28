package io.ticticboom.mods.mm.compat.ae2;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import com.buuz135.replication.api.IMatterType;
import net.minecraft.world.item.Item;
import net.unfamily.repae2bridge.item.ModItems;
import net.unfamily.repae2bridge.item.UniversalMatterItem;
import net.unfamily.repae2bridge.util.MatterTypeUtil;
import org.jetbrains.annotations.Nullable;

public class MatterKeys {

    public static @Nullable AEKey of(IMatterType type) {
        var item = knownItem(type);
        if (item != null) {
            return AEItemKey.of(item);
        }
        var stack = UniversalMatterItem.createMatterStack(type, 1);
        return stack.isEmpty() ? null : AEItemKey.of(stack);
    }

    public static @Nullable IMatterType typeOf(AEKey key) {
        if (!(key instanceof AEItemKey item)) {
            return null;
        }
        return MatterTypeUtil.getMatterTypeFromItemStack(item.toStack(1));
    }

    public static boolean isMatter(AEKey key) {
        return typeOf(key) != null;
    }

    private static @Nullable Item knownItem(IMatterType type) {
        var name = type.getName();
        if (name == null) {
            return null;
        }
        if (name.equalsIgnoreCase("earth")) return ModItems.EARTH_MATTER.get();
        if (name.equalsIgnoreCase("nether")) return ModItems.NETHER_MATTER.get();
        if (name.equalsIgnoreCase("organic")) return ModItems.ORGANIC_MATTER.get();
        if (name.equalsIgnoreCase("ender")) return ModItems.ENDER_MATTER.get();
        if (name.equalsIgnoreCase("metallic")) return ModItems.METALLIC_MATTER.get();
        if (name.equalsIgnoreCase("precious")) return ModItems.PRECIOUS_MATTER.get();
        if (name.equalsIgnoreCase("living")) return ModItems.LIVING_MATTER.get();
        if (name.equalsIgnoreCase("quantum")) return ModItems.QUANTUM_MATTER.get();
        return null;
    }
}

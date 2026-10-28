package io.ticticboom.mods.mm.port.replication.matter;

import com.buuz135.replication.ReplicationRegistry;
import com.buuz135.replication.api.IMatterType;
import net.minecraft.resources.Identifier;

public class MatterTypes {

    public static IMatterType get(Identifier id) {
        if (id == null || ReplicationRegistry.MATTER_TYPES_REGISTRY == null) {
            return null;
        }
        return ReplicationRegistry.MATTER_TYPES_REGISTRY.getValue(id);
    }

    public static Identifier idOf(IMatterType type) {
        if (type == null || ReplicationRegistry.MATTER_TYPES_REGISTRY == null) {
            return null;
        }
        return ReplicationRegistry.MATTER_TYPES_REGISTRY.getKey(type);
    }

    public static int colorOf(IMatterType type) {
        if (type == null) {
            return 0xFFFFFFFF;
        }
        var color = type.getColor().get();
        int a = channel(color, 3, 1f);
        int r = channel(color, 0, 1f);
        int g = channel(color, 1, 1f);
        int b = channel(color, 2, 1f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int channel(float[] color, int index, float fallback) {
        float value = index < color.length ? color[index] : fallback;
        return Math.max(0, Math.min(255, Math.round(value * 255f)));
    }
}

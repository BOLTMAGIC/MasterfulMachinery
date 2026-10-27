package io.ticticboom.mods.mm.compat.mysticalagriculture;

import io.ticticboom.mods.mm.Ref;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Optional Mystical Agriculture support for flex recipes: what MA itself knows about a seed (its essence, crux,
 * tier and whether the crop is enabled), so recipes don't have to guess it from item names. Read through
 * reflection from MA's public API, so MM has no build dependency on MA; without MA every lookup returns null.
 */
public final class MACrops {

    /**
     * @param crux null if the crop needs no crux
     */
    public record Info(Item seeds, Item essence, @Nullable Item crux, int tier, boolean enabled) {
    }

    private static volatile Map<Item, Info> bySeeds;

    private MACrops() {
    }

    public static boolean isLoaded() {
        return ModList.get() != null && ModList.get().isLoaded("mysticalagriculture");
    }

    /** @return MA's data for these seeds, or null if MA isn't loaded or the item isn't an MA seed */
    @Nullable
    public static Info forSeeds(Item seeds) {
        if (!isLoaded()) {
            return null;
        }
        var map = bySeeds;
        if (map == null) {
            map = load();
            bySeeds = map;
        }
        return map.get(seeds);
    }

    private static Map<Item, Info> load() {
        var map = new HashMap<Item, Info>();
        try {
            Class<?> api = Class.forName("com.blakebr0.mysticalagriculture.api.MysticalAgricultureAPI");
            Object registry = api.getMethod("getCropRegistry").invoke(null);
            List<?> crops = (List<?>) registry.getClass().getMethod("getCrops").invoke(registry);
            for (Object crop : crops) {
                var type = crop.getClass();
                Item seeds = (Item) type.getMethod("getSeedsItem").invoke(crop);
                Item essence = (Item) type.getMethod("getEssenceItem").invoke(crop);
                Block cruxBlock = (Block) type.getMethod("getCruxBlock").invoke(crop);
                Object tier = type.getMethod("getTier").invoke(crop);
                int tierValue = (int) tier.getClass().getMethod("getValue").invoke(tier);
                boolean enabled = (boolean) type.getMethod("isEnabled").invoke(crop);
                Item crux = cruxBlock == null || cruxBlock.asItem() == Items.AIR ? null : cruxBlock.asItem();
                if (seeds != null && essence != null) {
                    map.put(seeds, new Info(seeds, essence, crux, tierValue, enabled));
                }
            }
            Ref.LOG.info("Flex recipes: read {} Mystical Agriculture crops", map.size());
        } catch (ReflectiveOperationException | RuntimeException e) {
            Ref.LOG.error("Flex recipes: couldn't read Mystical Agriculture crops, {ma:...} won't work", e);
        }
        return map;
    }
}

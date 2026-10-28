package io.ticticboom.mods.mm.datagen;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.world.flag.FeatureFlagSet;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 1.20.5 reworked the pack API. Pack.create with its long argument list is gone, replaced by a
 * constructor taking three records: PackLocationInfo (id, title, source), Pack.Metadata
 * (description, compatibility, features) and PackSelectionConfig (required, position, fixed).
 * Pack.Info no longer exists, and ResourcesSupplier is now handed a PackLocationInfo rather than
 * the pack id string, with separate primary and full openers.
 */
public class MMRepositorySource implements RepositorySource {

    private final MMRepoType type;
    public static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("mm/pack");

    public MMRepositorySource(MMRepoType type) {
        this.type = type;
    }

    @Override
    public void loadPacks(Consumer<Pack> consumer) {
        var location = new PackLocationInfo(
                type.getNameId(), type.nameComponent(), PackSource.DEFAULT, Optional.empty());
        var selection = new PackSelectionConfig(true, Pack.Position.BOTTOM, false);
        consumer.accept(new Pack(location, createPackSupplier(CONFIG_DIR), createPackMetadata(), selection));
    }

    private Pack.ResourcesSupplier createPackSupplier(Path configDir) {
        return new Pack.ResourcesSupplier() {
            @Override
            public PackResources openPrimary(PackLocationInfo info) {
                return open();
            }

            @Override
            public PackResources openFull(PackLocationInfo info, Pack.Metadata metadata) {
                return open();
            }

            private PackResources open() {
                DataGenManager.generate();
                return new GeneratedPack(type.getNameId(), false, type.getPath(configDir));
            }
        };
    }

    private Pack.Metadata createPackMetadata() {
        return new Pack.Metadata(type.nameComponent(), PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), List.of());
    }
}

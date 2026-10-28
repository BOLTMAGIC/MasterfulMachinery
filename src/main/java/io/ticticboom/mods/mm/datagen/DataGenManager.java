package io.ticticboom.mods.mm.datagen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.datagen.provider.*;
import net.minecraft.DetectedVersion;
import net.minecraft.WorldVersion;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.server.packs.repository.PackRepository;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.resource.ResourcePackLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DataGenManager {

    private static DataGenerator generator;
    private static boolean hasGenerated = false;

    public static void generate() {
        if (!hasGenerated) {
            try {
                // ModLoader.isLoadingStateValid is gone in NeoForge; ModLoader.hasErrors covers it.
                if (ModLoader.hasErrors()) {
                    return;
                }
                generator.run();
                hasGenerated = true;
            } catch (Exception e) {
                Ref.LOG.error("Data generation failed", e);
            }
        }
    }

    public static void registerDataProviders() {
        generator = createDataGenerator();
        CompletableFuture<HolderLookup.Provider> lookupProvider = CompletableFuture.supplyAsync(() -> RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        generator.addProvider(true, new MMLootTableProvider(generator, lookupProvider));
        generator.addProvider(true, new MMLangProvider(generator, "en_us"));
        generator.addProvider(true, new MMBlockTagsProvider(generator, lookupProvider));
        if (FMLEnvironment.getDist() != Dist.DEDICATED_SERVER) {
            generator.addProvider(true, new MMBlockstateProvider(generator));
        }
    }

    public static DataGenerator createDataGenerator() {
        generator = new DataGenerator.Cached(MMRepositorySource.CONFIG_DIR, DetectedVersion.tryDetectVersion(), true);
        return generator;
    }
}

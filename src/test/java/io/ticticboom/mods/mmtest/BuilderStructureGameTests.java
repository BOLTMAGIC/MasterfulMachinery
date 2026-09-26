package io.ticticboom.mods.mmtest;

import io.netty.buffer.Unpooled;
import io.ticticboom.mods.mm.builder.structure.BuildableStructure;
import io.ticticboom.mods.mm.builder.structure.BuildableStructureParser;
import io.ticticboom.mods.mm.builder.structure.BuildableStructureRegistry;
import io.ticticboom.mods.mm.builder.structure.BuildableStructureSync;
import io.ticticboom.mods.mm.net.packet.BuildableStructureSyncPkt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Non-MM structures for the Multiblock Tool: loading the test {@code .nbt} files from mm_builder_structures,
 * mbtool_structures and spatial_structures, their names, groups and buildability, and the chunked client sync.
 */
@GameTestHolder("mmtest")
@PrefixGameTestTemplate(false)
public class BuilderStructureGameTests {
    private static final String TEMPLATE = "empty";

    private static ResourceLocation id(String path) {
        return ResourceLocation.tryBuild("mmtest", path);
    }

    @GameTest(template = TEMPLATE)
    public static void loadsVanillaStructureWithStates(GameTestHelper helper) {
        BuildableStructure tower = BuildableStructureRegistry.SERVER.get(id("test_tower"));
        check(helper, tower != null, "mmtest:test_tower should be loaded");
        // the mm_builder_structures file wins over the mbtool_structures one with the same id (a single dirt block)
        check(helper, tower.blockCount() == 10, "expected 9 stone + 1 stairs (air and structure void skipped), got " + tower.blockCount());
        check(helper, tower.size().equals(new Vec3i(3, 2, 3)), "expected size 3x2x3, got " + tower.size());
        BlockState stairs = tower.blocks().stream().filter(p -> p.pos().equals(new BlockPos(1, 1, 1)))
                .map(BuildableStructure.Placement::state).findFirst().orElse(null);
        check(helper, stairs != null && stairs.is(Blocks.OAK_STAIRS), "expected oak stairs at 1,1,1, got " + stairs);
        check(helper, stairs.getValue(StairBlock.FACING) == Direction.EAST && stairs.getValue(StairBlock.HALF) == Half.TOP,
                "known properties should be applied, got " + stairs);
        // the unknown "bogus" property and the invalid shape value are ignored
        check(helper, stairs.getValue(StairBlock.SHAPE) == StairsShape.STRAIGHT, "an invalid value should keep the default, got " + stairs);
        check(helper, tower.buildable() && tower.unbuildableBlock() == null, "a stone/stairs tower should be buildable");
        check(helper, tower.group().equals("mmtest"), "an all-vanilla structure is grouped by its folder namespace, got " + tower.group());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void skipsStructuresWithUnknownBlocks(GameTestHelper helper) {
        check(helper, BuildableStructureRegistry.SERVER.get(id("unknown_block")) == null, "a structure with an unknown block should be skipped");
        // a real Multi Builder Tool file for a mod that isn't in the dev runtime
        check(helper, BuildableStructureRegistry.SERVER.get(id("nuclearcraft_fission_reactor")) == null,
                "the NuclearCraft reactor should be skipped without NuclearCraft");
        BuildableStructure boiler = BuildableStructureRegistry.SERVER.get(id("mekanism_boiler"));
        check(helper, boiler != null, "the Mekanism boiler from mbtool_structures should load with Mekanism present");
        check(helper, boiler.blockCount() == 128, "expected 128 non-air blocks in the boiler, got " + boiler.blockCount());
        check(helper, boiler.buildable(), "every boiler block has an item form");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void detectsUnbuildableBlocks(GameTestHelper helper) {
        BuildableStructure structure = BuildableStructureRegistry.SERVER.get(id("unbuildable"));
        check(helper, structure != null, "an unbuildable structure is still listed");
        check(helper, !structure.buildable(), "fire and water have no item form");
        check(helper, structure.unbuildableBlock() == Blocks.FIRE, "the first unbuildable block should be fire, got " + structure.unbuildableBlock());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void resolvesNamesAndGroups(GameTestHelper helper) {
        BuildableStructure named = BuildableStructureRegistry.SERVER.get(id("named/lang_named"));
        check(helper, named != null, "a structure in a sub folder should load as mmtest:named/lang_named");
        check(helper, BuildableStructure.langKey(named.id()).equals("structure.mmtest.named.lang_named"), "unexpected lang key " + BuildableStructure.langKey(named.id()));
        check(helper, named.displayName().getString().equals("Lang Named Tower"), "the lang key should name it, got " + named.displayName().getString());

        BuildableStructure mixed = BuildableStructureRegistry.SERVER.get(id("mixed_mods"));
        check(helper, mixed.displayName().getString().equals("Mixed Mods"), "without a lang key the file name is title-cased, got " + mixed.displayName().getString());
        // 10 stone, 3 Immersive Engineering, 2 Mekanism: the most common non-vanilla namespace wins
        check(helper, mixed.group().equals("immersiveengineering"), "expected group immersiveengineering, got " + mixed.group());
        check(helper, mixed.groupName().equals("Immersive Engineering"), "expected the mod's display name, got " + mixed.groupName());

        BuildableStructure boiler = BuildableStructureRegistry.SERVER.get(id("mekanism_boiler"));
        check(helper, boiler.group().equals("mekanism") && boiler.groupName().equals("Mekanism"), "expected group Mekanism, got " + boiler.groupName());
        check(helper, boiler.displayName().getString().equals("Mekanism Boiler"), "got " + boiler.displayName().getString());
        check(helper, BuildableStructure.fallbackName(ResourceLocation.tryBuild("mbtool", "woot/woot_tier_1_copper")).equals("Woot Tier 1 Copper"), "title case of a nested path");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void parserRejectsMalformedFiles(GameTestHelper helper) {
        CompoundTag badIndex = template(List.of("minecraft:stone"), List.of(new int[]{0, 0, 0, 5}));
        check(helper, BuildableStructureParser.parse(id("bad"), badIndex).structure() == null, "a palette index out of range should skip the file");

        CompoundTag onlyAir = template(List.of("minecraft:air"), List.of(new int[]{0, 0, 0, 0}));
        check(helper, BuildableStructureParser.parse(id("air"), onlyAir).structure() == null, "a structure with only air should be skipped");

        // the multi-palette form uses its first palette
        CompoundTag multi = template(List.of("minecraft:stone"), List.of(new int[]{0, 0, 0, 0}));
        ListTag palettes = new ListTag();
        palettes.add(multi.getList("palette", 10));
        multi.remove("palette");
        multi.put("palettes", palettes);
        var result = BuildableStructureParser.parse(id("multi"), multi);
        check(helper, result.structure() != null && result.structure().blocks().get(0).state().is(Blocks.STONE), "palettes[0] should be used, got " + result);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void syncRoundTripsInChunks(GameTestHelper helper) {
        List<BuildableStructure> structures = new ArrayList<>(BuildableStructureRegistry.SERVER.all());
        // three large structures (~440 KB encoded each) force one packet per structure at the real limit
        for (int i = 0; i < 3; i++) {
            structures.add(big(id("big_" + i), 48));
        }
        List<BuildableStructureSyncPkt> packets = BuildableStructureSync.split(structures, 7, BuildableStructureSync.MAX_PACKET_BYTES);
        check(helper, packets.size() >= 3, "expected the big structures to be split over several packets, got " + packets.size());

        var assembler = new BuildableStructureSync.Assembler();
        List<BuildableStructure> received = null;
        for (int i = 0; i < packets.size(); i++) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            BuildableStructureSyncPkt.encode(packets.get(i), buf);
            check(helper, buf.readableBytes() < BuildableStructureSync.MAX_PACKET_BYTES, "packet " + i + " is " + buf.readableBytes() + " bytes");
            BuildableStructureSyncPkt decoded = BuildableStructureSyncPkt.decode(buf);
            check(helper, buf.readableBytes() == 0, "decode should consume the whole packet");
            buf.release();
            List<BuildableStructure> result = assembler.accept(decoded);
            check(helper, (result != null) == (i == packets.size() - 1), "the list completes only with the last chunk (chunk " + i + ")");
            received = result;
        }
        check(helper, received.equals(structures), "the decoded structures should equal the sent ones");
        BuildableStructure tower = received.stream().filter(s -> s.id().equals(id("test_tower"))).findFirst().orElseThrow();
        check(helper, tower.group().equals("mmtest") && tower.buildable(), "derived fields are recomputed after decoding");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void syncSplitsSmallBudgetsAndDropsStaleBatches(GameTestHelper helper) {
        // only the small test files: bigger ones (e.g. the bundled Multi Builder Tool structures) don't fit 800 bytes
        List<BuildableStructure> structures = BuildableStructureRegistry.SERVER.all().stream()
                .filter(s -> s.id().getNamespace().equals("mmtest") && s.blockCount() <= 20)
                .toList();
        check(helper, structures.size() >= 3, "expected the small mmtest structures, got " + structures.size());
        List<BuildableStructureSyncPkt> small = BuildableStructureSync.split(structures, 2, 800);
        check(helper, small.size() > 1, "an 800 byte budget should need several packets, got " + small.size());

        var assembler = new BuildableStructureSync.Assembler();
        // the first chunk of an older batch is abandoned when a newer batch starts
        List<BuildableStructureSyncPkt> older = BuildableStructureSync.split(structures, 1, 800);
        check(helper, assembler.accept(roundTrip(older.get(0))) == null, "one chunk of several is not complete");
        List<BuildableStructure> result = null;
        for (BuildableStructureSyncPkt pkt : small) {
            result = assembler.accept(roundTrip(pkt));
        }
        check(helper, result != null && result.equals(structures), "the newer batch should assemble on its own");

        // an empty registry still sends one packet so the client clears its list
        List<BuildableStructureSyncPkt> empty = BuildableStructureSync.split(List.of(), 3, 1200);
        check(helper, empty.size() == 1 && empty.get(0).structures().isEmpty(), "expected one empty packet");
        check(helper, assembler.accept(roundTrip(empty.get(0))).isEmpty(), "an empty batch completes as an empty list");

        // a structure larger than one packet is left out instead of breaking the sync
        List<BuildableStructureSyncPkt> oversized = BuildableStructureSync.split(List.of(big(id("huge"), 12)), 4, 1200);
        check(helper, oversized.size() == 1 && oversized.get(0).structures().isEmpty(), "an oversized structure should be dropped");
        helper.succeed();
    }

    private static BuildableStructureSyncPkt roundTrip(BuildableStructureSyncPkt pkt) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        BuildableStructureSyncPkt.encode(pkt, buf);
        BuildableStructureSyncPkt decoded = BuildableStructureSyncPkt.decode(buf);
        buf.release();
        return decoded;
    }

    /** A solid cube of mixed states, so the palette has several entries. */
    private static BuildableStructure big(ResourceLocation id, int edge) {
        BlockState[] states = {
                Blocks.STONE.defaultBlockState(),
                Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST),
                Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.HALF, Half.TOP),
                Blocks.GLASS.defaultBlockState()
        };
        List<BuildableStructure.Placement> blocks = new ArrayList<>(edge * edge * edge);
        for (int x = 0; x < edge; x++) {
            for (int y = 0; y < edge; y++) {
                for (int z = 0; z < edge; z++) {
                    blocks.add(new BuildableStructure.Placement(new BlockPos(x, y, z), states[(x + y * 3 + z * 7) % states.length]));
                }
            }
        }
        return BuildableStructure.of(id, new Vec3i(edge, edge, edge), blocks);
    }

    /** A minimal structure template; each block is {x, y, z, paletteIndex}. */
    private static CompoundTag template(List<String> palette, List<int[]> blocks) {
        CompoundTag nbt = new CompoundTag();
        ListTag paletteTag = new ListTag();
        for (String name : palette) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Name", name);
            paletteTag.add(entry);
        }
        nbt.put("palette", paletteTag);
        ListTag blocksTag = new ListTag();
        for (int[] block : blocks) {
            CompoundTag entry = new CompoundTag();
            ListTag pos = new ListTag();
            pos.add(IntTag.valueOf(block[0]));
            pos.add(IntTag.valueOf(block[1]));
            pos.add(IntTag.valueOf(block[2]));
            entry.put("pos", pos);
            entry.putInt("state", block[3]);
            blocksTag.add(entry);
        }
        nbt.put("blocks", blocksTag);
        ListTag size = new ListTag();
        size.add(IntTag.valueOf(1));
        size.add(IntTag.valueOf(1));
        size.add(IntTag.valueOf(1));
        nbt.put("size", size);
        return nbt;
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}

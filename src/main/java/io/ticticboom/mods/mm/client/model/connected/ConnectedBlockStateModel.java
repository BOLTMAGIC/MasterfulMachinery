package io.ticticboom.mods.mm.client.model.connected;

import com.mojang.math.Quadrant;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.extra.IExtraBlock;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Connected textures for MM's extra blocks: every face is four quarters, and each quarter picks one of five tiles from
 * whether the block joins its neighbours (same connect group). A blockstate variant names it with
 * {"type": "mm:connected", "overlay": "&lt;texture&gt;"}.
 * <p>
 * Up to 1.21.1 the tiles were chosen through ModelData and a dynamic baked model; since 1.21.5 block state models
 * collect their parts per position, so the choice is made there.
 */
public class ConnectedBlockStateModel implements DynamicBlockStateModel {
    public static final int TILE_COUNT = 5;

    private static final int ALONE = 0;
    private static final int JOINED = 1;
    private static final int JOINED_ALONG_U = 2;
    private static final int JOINED_ALONG_V = 3;
    private static final int INNER_CORNER = 4;
    private static final Direction[] FACES = Direction.values();

    private final BakedQuad[][][] base;
    private final BakedQuad[][] overlay;
    private final Material.Baked particle;
    private final Map<Long, BlockStateModelPart> parts = new ConcurrentHashMap<>();

    public record Unbaked(Identifier overlayTexture) implements CustomUnbakedBlockStateModel {
        public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Identifier.CODEC.fieldOf("overlay").forGetter(Unbaked::overlayTexture)
        ).apply(i, Unbaked::new));

        @Override
        public MapCodec<Unbaked> codec() {
            return CODEC;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
        }

        @Override
        public BlockStateModel bake(ModelBaker baker) {
            var tiles = new Material.Baked[TILE_COUNT];
            for (int i = 0; i < TILE_COUNT; i++) {
                var tile = Ref.id("block/base_block_ctm" + i);
                tiles[i] = baker.materials().get(new Material(tile), () -> "mm:connected " + tile);
            }
            var overlay = baker.materials().get(new Material(overlayTexture, true), () -> "mm:connected overlay " + overlayTexture);
            return new ConnectedBlockStateModel(baker, tiles, overlay);
        }
    }

    private ConnectedBlockStateModel(ModelBaker baker, Material.Baked[] tiles, Material.Baked overlaySprite) {
        this.particle = tiles[ALONE];
        this.base = new BakedQuad[6][4][TILE_COUNT];
        this.overlay = new BakedQuad[6][4];
        for (Direction face : FACES) {
            for (int quarter = 0; quarter < 4; quarter++) {
                var from = faceFrom(face);
                var to = faceTo(face);
                int a = quarter / 2;
                int b = quarter % 2;
                from[uAxis(face)] = a * 8;
                to[uAxis(face)] = a * 8 + 8;
                from[vAxis(face)] = b * 8;
                to[vAxis(face)] = b * 8 + 8;
                for (int tile = 0; tile < TILE_COUNT; tile++) {
                    base[face.ordinal()][quarter][tile] = bake(baker, face, from, to, tiles[tile]);
                }
                overlay[face.ordinal()][quarter] = bake(baker, face, from, to, overlaySprite);
            }
        }
    }

    private static BakedQuad bake(ModelBaker baker, Direction face, float[] from, float[] to, Material.Baked material) {
        float[] uv = uvs(face, from, to);
        var cuboidFace = new CuboidFace(face, CuboidFace.NO_TINT, "", new CuboidFace.UVs(uv[0], uv[1], uv[2], uv[3]), Quadrant.R0);
        return FaceBakery.bakeQuad(baker, new Vector3f(from[0], from[1], from[2]), new Vector3f(to[0], to[1], to[2]), cuboidFace, material, face,
                BlockModelRotation.IDENTITY, null, true, 0);
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> output) {
        long key = 0;
        if (state.getBlock() instanceof IExtraBlock self) {
            var group = self.getModel().resolvedConnectGroup();
            for (Direction face : FACES) {
                for (int quarter = 0; quarter < 4; quarter++) {
                    key = key * TILE_COUNT + tileFor(level, pos, group, face, quarter);
                }
            }
        }
        output.add(parts.computeIfAbsent(key, this::part));
    }

    /** The part for one tile choice, the 24 quarters packed base-5 into key (first face first). */
    private BlockStateModelPart part(long key) {
        var tiles = new int[24];
        for (int i = 23; i >= 0; i--) {
            tiles[i] = (int) (key % TILE_COUNT);
            key /= TILE_COUNT;
        }
        var quads = new QuadCollection.Builder();
        for (Direction face : FACES) {
            for (int quarter = 0; quarter < 4; quarter++) {
                quads.addCulledFace(face, base[face.ordinal()][quarter][tiles[face.ordinal() * 4 + quarter]]);
                quads.addCulledFace(face, overlay[face.ordinal()][quarter]);
            }
        }
        return new SimpleModelWrapper(quads.build(), true, particle);
    }

    @Override
    public Material.Baked particleMaterial() {
        return particle;
    }

    @Override
    public int materialFlags() {
        return part(0).materialFlags();
    }

    private static int tileFor(BlockAndTintGetter level, BlockPos pos, String group, Direction face, int quarter) {
        var u = uNeighbour(face, quarter / 2);
        var v = vNeighbour(face, quarter % 2);
        boolean alongU = joins(level, pos.relative(u), group);
        boolean alongV = joins(level, pos.relative(v), group);
        if (!alongU && !alongV) {
            return ALONE;
        }
        if (alongU && !alongV) {
            return JOINED_ALONG_U;
        }
        if (!alongU) {
            return JOINED_ALONG_V;
        }
        return joins(level, pos.relative(u).relative(v), group) ? JOINED : INNER_CORNER;
    }

    private static boolean joins(BlockAndTintGetter level, BlockPos pos, String group) {
        return level.getBlockState(pos).getBlock() instanceof IExtraBlock other
                && group.equals(other.getModel().resolvedConnectGroup());
    }

    private static float[] faceFrom(Direction face) {
        return switch (face) {
            case UP -> new float[]{0, 16, 0};
            case SOUTH -> new float[]{0, 0, 16};
            case EAST -> new float[]{16, 0, 0};
            default -> new float[]{0, 0, 0};
        };
    }

    private static float[] faceTo(Direction face) {
        return switch (face) {
            case DOWN -> new float[]{16, 0, 16};
            case NORTH -> new float[]{16, 16, 0};
            case WEST -> new float[]{0, 16, 16};
            default -> new float[]{16, 16, 16};
        };
    }

    private static int uAxis(Direction face) {
        return face.getAxis() == Direction.Axis.X ? 2 : 0;
    }

    private static int vAxis(Direction face) {
        return face.getAxis() == Direction.Axis.Y ? 2 : 1;
    }

    private static Direction uNeighbour(Direction face, int half) {
        if (face.getAxis() == Direction.Axis.X) {
            return half == 0 ? Direction.NORTH : Direction.SOUTH;
        }
        return half == 0 ? Direction.WEST : Direction.EAST;
    }

    private static Direction vNeighbour(Direction face, int half) {
        if (face.getAxis() == Direction.Axis.Y) {
            return half == 0 ? Direction.NORTH : Direction.SOUTH;
        }
        return half == 0 ? Direction.DOWN : Direction.UP;
    }

    private static float[] uvs(Direction face, float[] from, float[] to) {
        return switch (face) {
            case DOWN -> new float[]{from[0], 16 - to[2], to[0], 16 - from[2]};
            case UP -> new float[]{from[0], from[2], to[0], to[2]};
            case NORTH -> new float[]{16 - to[0], 16 - to[1], 16 - from[0], 16 - from[1]};
            case SOUTH -> new float[]{from[0], 16 - to[1], to[0], 16 - from[1]};
            case WEST -> new float[]{from[2], 16 - to[1], to[2], 16 - from[1]};
            case EAST -> new float[]{16 - to[2], 16 - to[1], 16 - from[2], 16 - from[1]};
        };
    }
}

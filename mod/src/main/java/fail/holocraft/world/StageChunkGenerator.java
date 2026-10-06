package fail.holocraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fail.holocraft.registry.HcBlocks;
import fail.holocraft.sheet.StagesRow;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.FixedBiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** systems: stage_worldgen. A HoloCure stage as a world: flat stage floor over stone, props where the stage puts them. */
public class StageChunkGenerator extends ChunkGenerator {
    public static final MapCodec<StageChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("stage").forGetter(g -> g.stageId),
            Biome.REGISTRY_CODEC.fieldOf("biome").forGetter(g -> g.biome)
    ).apply(i, i.stable(StageChunkGenerator::new)));

    private final String stageId;
    private final RegistryEntry<Biome> biome;

    public StageChunkGenerator(String stageId, RegistryEntry<Biome> biome) {
        super(new FixedBiomeSource(biome));
        this.stageId = stageId;
        this.biome = biome;
    }

    private StagesRow stage() {
        for (StagesRow s : StagesRow.ALL) if (s.id().equals(stageId)) return s;
        return StagesRow.ALL.get(0);
    }

    private int floorY() { return stage().surfaceY(); }

    private BlockState layer(int y, int x, int z) {
        int f = floorY();
        if (y == -64) return Blocks.BEDROCK.getDefaultState();
        if (y < f - 3) return Blocks.STONE.getDefaultState();
        if (y < f) return Blocks.DIRT.getDefaultState();
        if (y == f) return HcBlocks.GROUND.get(stage().id()).at(x, z);
        return null;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> getCodec() { return CODEC; }

    @Override
    public CompletableFuture<Chunk> populateNoise(Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
        BlockPos.Mutable m = new BlockPos.Mutable();
        Heightmap h1 = chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG), h2 = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);
        int bx = chunk.getPos().getStartX(), bz = chunk.getPos().getStartZ();
        for (int y = Math.max(-64, chunk.getBottomY()); y <= floorY(); y++)
            for (int x = 0; x < 16; x++)
                for (int z = 0; z < 16; z++) {
                    BlockState s = layer(y, bx + x, bz + z);
                    if (s == null) continue;
                    chunk.setBlockState(m.set(x, y, z), s, false);
                    h1.trackUpdate(x, y, z, s);
                    h2.trackUpdate(x, y, z, s);
                }
        return CompletableFuture.completedFuture(chunk);
    }

    /** Props go in here rather than in populateNoise so their block entities are created properly. */
    @Override
    public void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor) {
        StageLayouts.Layout l = StageLayouts.get(stageId);
        if (l.props().isEmpty()) return;
        int x0 = chunk.getPos().getStartX(), z0 = chunk.getPos().getStartZ();
        int w = l.widthBlocks(), h = l.heightBlocks();
        int y = floorY() + 1;
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (StageLayouts.Prop p : l.props()) {
            PropBlock block = HcBlocks.PROPS.get(p.id());
            if (block == null) continue;
            int px = x0 + Math.floorMod(p.x() - x0, w), pz = z0 + Math.floorMod(p.z() - z0, h);
            if (px >= x0 + 16 || pz >= z0 + 16) continue;
            m.set(px, y, pz);
            if (world.getBlockState(m).isAir()) world.setBlockState(m, block.getDefaultState(), Block.NOTIFY_LISTENERS);
        }
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {}

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk) {}

    @Override
    public void populateEntities(ChunkRegion region) {}

    @Override
    public int getWorldHeight() { return 384; }

    @Override
    public int getSeaLevel() { return -63; }

    @Override
    public int getMinimumY() { return -64; }

    @Override
    public int getSpawnHeight(HeightLimitView world) { return floorY() + 1; }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) { return floorY() + 1; }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        BlockState[] col = new BlockState[world.getHeight()];
        for (int i = 0; i < col.length; i++) {
            BlockState s = layer(world.getBottomY() + i, x, z);
            col[i] = s == null ? Blocks.AIR.getDefaultState() : s;
        }
        return new VerticalBlockSample(world.getBottomY(), col);
    }

    @Override
    public void appendDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        text.add("HoloCure stage: " + stageId);
    }
}

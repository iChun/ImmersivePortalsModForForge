package com.qouteall.immersive_portals.alternate_dimension;

import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.EndChunkGenerator;
import net.minecraft.world.gen.EndGenerationSettings;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.template.TemplateManager;

public class VoidChunkGenerator extends EndChunkGenerator {
    public VoidChunkGenerator(
        IWorld iWorld,
        BiomeProvider biomeSource,
        EndGenerationSettings floatingIslandsChunkGeneratorConfig
    ) {
        super(iWorld, biomeSource, floatingIslandsChunkGeneratorConfig);
    }
    
    @Override
    public void makeBase(IWorld world, IChunk chunk) {
        //nothing
    }
    
    @Override
    public boolean hasStructure(
        Biome biome, Structure<? extends IFeatureConfig> structureFeature
    ) {
        return false;
    }
    
    @Override
    public void generateStructures(
        BiomeManager biomeAccess,
        IChunk chunk,
        ChunkGenerator<?> chunkGenerator,
        TemplateManager structureManager
    ) {
        //nothing
    }
}

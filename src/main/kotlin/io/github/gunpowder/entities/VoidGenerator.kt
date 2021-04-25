/*
 * MIT License
 *
 * Copyright (c) 2020 GunpowderMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.gunpowder.entities

import com.mojang.serialization.Codec
import io.github.gunpowder.GunpowderIslandsModule
import net.minecraft.block.Blocks
import net.minecraft.server.world.ServerWorld
import net.minecraft.structure.StructureManager
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.registry.BuiltinRegistries
import net.minecraft.util.registry.DynamicRegistryManager
import net.minecraft.world.*
import net.minecraft.world.biome.source.BiomeAccess
import net.minecraft.world.biome.source.MultiNoiseBiomeSource
import net.minecraft.world.biome.source.VanillaLayeredBiomeSource
import net.minecraft.world.chunk.Chunk
import net.minecraft.world.gen.GenerationStep
import net.minecraft.world.gen.StructureAccessor
import net.minecraft.world.gen.chunk.*
import net.minecraft.world.gen.feature.StructureFeature
import java.util.*

class VoidGenerator(seed: Long, overworld: Boolean) : ChunkGenerator(
    if (overworld) VanillaLayeredBiomeSource(seed, false, false, GunpowderIslandsModule.BIOME_REGISTRY)
    else MultiNoiseBiomeSource.Preset.NETHER.getBiomeSource(GunpowderIslandsModule.BIOME_REGISTRY, seed),
    StructuresConfig(Optional.empty(), mapOf())
) {
    override fun getCodec(): Codec<out ChunkGenerator>? {
        return null
    }

    override fun withSeed(seed: Long): ChunkGenerator {
        return this;
    }

    override fun buildSurface(region: ChunkRegion?, chunk: Chunk?) {

    }

    override fun populateNoise(world: WorldAccess?, accessor: StructureAccessor?, chunk: Chunk?) {

    }

    override fun addStructureReferences(world: StructureWorldAccess?, accessor: StructureAccessor?, chunk: Chunk?) {

    }

    override fun generateFeatures(region: ChunkRegion?, accessor: StructureAccessor?) {

    }

    override fun locateStructure(
        world: ServerWorld?,
        feature: StructureFeature<*>?,
        center: BlockPos?,
        radius: Int,
        skipExistingChunks: Boolean
    ): BlockPos? {
        return null
    }

    override fun carve(seed: Long, access: BiomeAccess?, chunk: Chunk?, carver: GenerationStep.Carver?) {

    }

    override fun setStructureStarts(
        dynamicRegistryManager: DynamicRegistryManager?,
        structureAccessor: StructureAccessor?,
        chunk: Chunk?,
        structureManager: StructureManager?,
        worldSeed: Long
    ) {

    }

    override fun getHeight(x: Int, z: Int, heightmapType: Heightmap.Type?): Int {
        return 1
    }

    override fun getColumnSample(x: Int, z: Int): BlockView {
        return VerticalBlockSample(arrayOf(Blocks.AIR.defaultState))
    }
}
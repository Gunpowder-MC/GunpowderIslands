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

import io.github.gunpowder.GunpowderIslandsModule
import net.minecraft.util.registry.BuiltinRegistries
import net.minecraft.world.biome.source.MultiNoiseBiomeSource
import net.minecraft.world.biome.source.VanillaLayeredBiomeSource
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings
import net.minecraft.world.gen.chunk.NoiseChunkGenerator
import java.util.function.Supplier

class ProtoGenerator(seed: Long, overworld: Boolean) : NoiseChunkGenerator(
    if (overworld) VanillaLayeredBiomeSource(seed, false, false, GunpowderIslandsModule.BIOME_REGISTRY)
    else MultiNoiseBiomeSource.Preset.NETHER.getBiomeSource(GunpowderIslandsModule.BIOME_REGISTRY, seed),
    seed,
    { BuiltinRegistries.CHUNK_GENERATOR_SETTINGS.get(if (overworld) ChunkGeneratorSettings.OVERWORLD else ChunkGeneratorSettings.NETHER) }
) {

}

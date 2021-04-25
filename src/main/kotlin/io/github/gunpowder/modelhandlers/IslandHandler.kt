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

package io.github.gunpowder.modelhandlers

import com.google.common.collect.ImmutableList
import com.mojang.authlib.GameProfile
import com.mojang.serialization.Lifecycle
import io.github.gunpowder.api.GunpowderMod
import io.github.gunpowder.configs.IslandsConfig
import io.github.gunpowder.entities.Island
import io.github.gunpowder.entities.ProtoGenerator
import io.github.gunpowder.entities.VoidGenerator
import io.github.gunpowder.models.IslandTable
import io.github.gunpowder.models.TeamsTable
import net.minecraft.nbt.NbtOps
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.structure.StructurePlacementData
import net.minecraft.tag.BlockTags
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.Registry
import net.minecraft.util.registry.RegistryKey
import net.minecraft.world.ChunkRegion
import net.minecraft.world.GameMode
import net.minecraft.world.World
import net.minecraft.world.biome.source.VoronoiBiomeAccessType
import net.minecraft.world.dimension.DimensionType
import net.minecraft.world.gen.GeneratorOptions
import net.minecraft.world.level.LevelInfo
import net.minecraft.world.level.LevelProperties
import org.jetbrains.exposed.sql.*
import java.util.*
import java.util.Random


object IslandHandler {
    private val overworldId = RegistryKey.of(Registry.DIMENSION_TYPE_KEY, Identifier("gpislands:overworld"))
    private val netherId = RegistryKey.of(Registry.DIMENSION_TYPE_KEY, Identifier("gpislands:nether"))

    private val db by lazy {
        GunpowderMod.instance.database
    }
    private val dimManager by lazy {
        GunpowderMod.instance.dimensionManager
    }
    private val config by lazy {
        GunpowderMod.instance.registry.getConfig(IslandsConfig::class.java)
    }
    private val serverProps by lazy {
        GunpowderMod.instance.server.saveProperties
    }

    private val rand = Random()

    fun init() {
        db.transaction {
            IslandTable.selectAll().map {
                val seed = it[IslandTable.seed]
                val dimName = it[IslandTable.dimensionName]
                return@map Pair(seed, dimName)
            }
        }.thenAccept {
            it.forEach { itt ->
                GunpowderMod.instance.server.execute {
                    loadIsland(itt.second, itt.first)
                }
            }
        }
    }

    init {
        // Load islands
        dimManager.addDimensionType(
            overworldId,
            DimensionType(
                OptionalLong.empty(),
                true,
                false,
                false,
                true,
                1.0,
                false,
                false,
                true,
                false,
                true,
                256,
                VoronoiBiomeAccessType.INSTANCE,
                BlockTags.INFINIBURN_OVERWORLD.id,
                DimensionType.OVERWORLD_ID,
                0.0f
            )
        )

        dimManager.addDimensionType(
            netherId,
            DimensionType(
                OptionalLong.of(18000L),
                false,
                true,
                true,
                false,
                8.0,
                false,
                true,
                false,
                true,
                false,
                128,
                VoronoiBiomeAccessType.INSTANCE,
                BlockTags.INFINIBURN_NETHER.id,
                DimensionType.THE_NETHER_ID,
                0.1f
            )
        )
    }

    private fun generateRandomName(): String {
        return UUID.randomUUID().toString()
    }

    private fun getIslandOrNull(player: ServerPlayerEntity): Island? {
        return db.transaction {
            val item = IslandTable.select { IslandTable.owner.eq(player.uuid) }.firstOrNull()
            return@transaction if (item != null) {
                Island(player.uuid, RegistryKey.of(Registry.DIMENSION, Identifier(item[IslandTable.dimensionName])))
            } else {
                val team = TeamsTable.select { TeamsTable.allowed.eq(player.uuid) }.firstOrNull()
                if (team != null) {
                    val itemOther = IslandTable.select { IslandTable.owner.eq(team[TeamsTable.owner]) }.first()
                    Island(itemOther[IslandTable.owner], RegistryKey.of(Registry.DIMENSION, Identifier(itemOther[IslandTable.dimensionName])))
                } else {
                    null
                }
            }
        }.get()
    }

    fun hasIsland(player: ServerPlayerEntity): Boolean {
        return getIslandOrNull(player) != null
    }

    fun getIsland(player: ServerPlayerEntity): Island {
         return getIslandOrNull(player) ?: error("NULL ISLAND")
    }

    fun createNewIsland(player: ServerPlayerEntity): Island {
        val seed = rand.nextLong()
        val dimName = "gpislands:${generateRandomName()}"
        val owKey = RegistryKey.of(Registry.DIMENSION, Identifier(dimName))
        val netKey = RegistryKey.of(Registry.DIMENSION, Identifier("$dimName-nether"))
        val owProps = LevelProperties(
            LevelInfo(dimName, GameMode.SURVIVAL, false, serverProps.difficulty, true, serverProps.gameRules, serverProps.dataPackSettings),
            GeneratorOptions(seed, true, false, serverProps.generatorOptions.dimensions),
            Lifecycle.experimental()
        )

        val ow = dimManager.addWorld(
            owKey,
            overworldId,
            if (config.protoskyMode) ProtoGenerator(seed, true) else VoidGenerator(seed, true),
            owProps
        )

        val net = dimManager.addWorld(
            netKey,
            netherId,
            if (config.protoskyMode) ProtoGenerator(seed, false) else VoidGenerator(seed, false),
            LevelProperties(
                LevelInfo("$dimName-nether", GameMode.SURVIVAL, false, serverProps.difficulty, true, serverProps.gameRules, serverProps.dataPackSettings),
                GeneratorOptions(seed, true, false, serverProps.generatorOptions.dimensions),
                Lifecycle.experimental()
            )
        )

        ow.worldBorder.size = config.maxIslandSize.toDouble()
        net.worldBorder.size = config.maxIslandSize.toDouble()

        val man = ow.structureManager
        val s = man.getStructure(Identifier("gunpowder-islands:spawn_platform"))!!
        val blockPos = BlockPos(0, 64, 0)
        s.place(
            ChunkRegion(ow, ImmutableList.of(ow.getChunk(BlockPos.ORIGIN))),
            blockPos,
            StructurePlacementData().setUpdateNeighbors(true),
            Random()
        )
        val homePos = blockPos.add(s.size.x.toDouble() / 2, s.size.y.toDouble() + 1, s.size.z.toDouble() / 2)
        ow.setSpawnPos(homePos, 0f)
        owProps.setSpawnPos(homePos, 0f)
        dimManager.linkNether(owKey, netKey)

        db.transaction {
            IslandTable.insert {
                it[IslandTable.dimensionName] = dimName
                it[IslandTable.owner] = player.uuid
                it[IslandTable.seed] = seed
            }
        }

        return Island(player.uuid, owKey)
    }

    fun loadIsland(dimName: String, seed: Long) {
        val owKey = RegistryKey.of(Registry.DIMENSION, Identifier(dimName))
        val netKey = RegistryKey.of(Registry.DIMENSION, Identifier("$dimName-nether"))
        val owProps = LevelProperties(
            LevelInfo(dimName, GameMode.SURVIVAL, false, serverProps.difficulty, true, serverProps.gameRules, serverProps.dataPackSettings),
            GeneratorOptions(seed, true, false, serverProps.generatorOptions.dimensions),
            Lifecycle.experimental()
        )

        val ow = dimManager.addWorld(
            owKey,
            overworldId,
            if (config.protoskyMode) ProtoGenerator(seed, true) else VoidGenerator(seed, true),
            owProps
        )

        val net = dimManager.addWorld(
            netKey,
            netherId,
            if (config.protoskyMode) ProtoGenerator(seed, false) else VoidGenerator(seed, false),
            LevelProperties(
                LevelInfo("$dimName-nether", GameMode.SURVIVAL, false, serverProps.difficulty, true, serverProps.gameRules, serverProps.dataPackSettings),
                GeneratorOptions(seed, true, false, serverProps.generatorOptions.dimensions),
                Lifecycle.experimental()
            )
        )

        ow.worldBorder.size = config.maxIslandSize.toDouble()
        net.worldBorder.size = config.maxIslandSize.toDouble()

        dimManager.linkNether(owKey, netKey)
    }

    fun hasOwnIsland(player: ServerPlayerEntity): Boolean {
        return db.transaction {
            IslandTable.select { IslandTable.owner.eq(player.uuid) }.firstOrNull()
        }.get() != null
    }

    fun deleteIsland(player: ServerPlayerEntity) {
        db.transaction {
            val row = IslandTable.select { IslandTable.owner.eq(player.uuid) }.first()

            // TODO: Unlink
            GunpowderMod.instance.server.execute {
                dimManager.removeWorld(RegistryKey.of(Registry.DIMENSION, Identifier(row[IslandTable.dimensionName])))
                dimManager.removeWorld(RegistryKey.of(Registry.DIMENSION, Identifier("${row[IslandTable.dimensionName]}-nether")))
            }

            TeamsTable.deleteWhere {
                TeamsTable.owner.eq(player.uuid)
            }
            IslandTable.deleteWhere {
                IslandTable.owner.eq(player.uuid)
            }
        }
    }

    fun leaveIsland(player: ServerPlayerEntity) {
        db.transaction {
            TeamsTable.deleteWhere {
                TeamsTable.allowed.eq(player.uuid)
            }
        }
    }

    fun transferIsland(player: ServerPlayerEntity, newOwner: GameProfile) {
        db.transaction {
            val uuids = TeamsTable.select { TeamsTable.owner.eq(player.uuid) }.map { it[TeamsTable.allowed] }

            TeamsTable.deleteWhere { TeamsTable.owner.eq(player.uuid) }

            IslandTable.update({ IslandTable.owner.eq(player.uuid) }) {
                it[IslandTable.owner] = newOwner.id
            }
            for (uuid in uuids) {
                if (uuid == newOwner.id) continue

                TeamsTable.insert {
                    it[TeamsTable.owner] = newOwner.id
                    it[TeamsTable.allowed] = uuid
                }
            }

            TeamsTable.insert {
                it[TeamsTable.owner] = newOwner.id
                it[TeamsTable.allowed] = player.uuid
            }
        }
    }

    fun addMember(owner: UUID, player: ServerPlayerEntity) {
        db.transaction {
            TeamsTable.insert {
                it[TeamsTable.owner] = owner
                it[TeamsTable.allowed] = player.uuid
            }
        }
    }

    fun removeMember(player: ServerPlayerEntity, target: UUID) {
        db.transaction {
            TeamsTable.deleteWhere {
                TeamsTable.owner.eq(player.uuid).and(TeamsTable.allowed.eq(target))
            }
        }
    }

    fun teamFull(id: UUID): Boolean {
        return config.maxTeamSize != 0 && db.transaction {
            TeamsTable.select { TeamsTable.owner.eq(id) }.count()
        }.get() >= config.maxTeamSize
    }
}

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

package io.github.gunpowder

import io.github.gunpowder.api.GunpowderMod
import io.github.gunpowder.api.GunpowderModule
import io.github.gunpowder.commands.IslandCommand
import io.github.gunpowder.configs.IslandsConfig
import io.github.gunpowder.modelhandlers.IslandHandler
import io.github.gunpowder.models.IslandTable
import io.github.gunpowder.models.TeamsTable
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.ResourcePackActivationType
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import net.minecraft.world.biome.Biome

object GunpowderIslandsModule : GunpowderModule {
    override val name = "islands"
    override val toggleable = true
    val gunpowder: GunpowderMod
        get() = GunpowderMod.instance

    val BIOME_REGISTRY: Registry<Biome> by lazy {
        gunpowder.server.registryManager.get(Registry.BIOME_KEY)
    }

    override fun registerConfigs() {
        gunpowder.registry.registerConfig("gunpowder-islands.yaml", IslandsConfig::class.java, "gunpowder-islands.yaml")
    }

    override fun registerCommands() {
        gunpowder.registry.registerCommand(IslandCommand::register)
    }

    override fun onInitialize() {
        gunpowder.registry.registerTable(IslandTable)
        gunpowder.registry.registerTable(TeamsTable)
    }

    override fun registerEvents() {
        ServerLifecycleEvents.SERVER_STARTED.register {
            IslandHandler.init()
        }

        ResourceManagerHelper.registerBuiltinResourcePack(
            Identifier("gunpowder-islands:original"),
            FabricLoader.getInstance().getModContainer("gunpowder-islands").get(),
            ResourcePackActivationType.NORMAL
        )
    }
}

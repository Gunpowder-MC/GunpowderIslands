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

package io.github.gunpowder.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import io.github.gunpowder.api.GunpowderMod
import io.github.gunpowder.api.builders.Command
import io.github.gunpowder.api.util.TranslatedText
import io.github.gunpowder.modelhandlers.IslandHandler
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.command.argument.GameProfileArgumentType
import net.minecraft.server.command.ServerCommandSource
import java.util.*

object IslandCommand {
    private val ALREADY_ON_ISLAND = TranslatedText("gunpowder.island.already_on_island")
    private val ALREADY_ON_OTHER_ISLAND = TranslatedText("gunpowder.island.already_on_other_island")
    private val NO_ISLAND_OWNED = TranslatedText("gunpowder.island.no_island_owned")
    private val ISLAND_OWNED = TranslatedText("gunpowder.island.island_owned")
    private val NO_ISLAND = TranslatedText("gunpowder.island.no_island")
    private val ISLAND_TRANSFERRED = TranslatedText("gunpowder.island.island_transferred")
    private val ISLAND_DELETED = TranslatedText("gunpowder.island.island_deleted")
    private val USER_INVITED = TranslatedText("gunpowder.island.user_invited")
    private val NO_INVITE = TranslatedText("gunpowder.island.no_invite")
    private val LEAVE_ISLAND = TranslatedText("gunpowder.island.leave_island")
    private val ACCEPT_INVITE = TranslatedText("gunpowder.island.accept_invite")
    private val ISLAND_FULL = TranslatedText("gunpowder.island.island_full")
    private val HOME_SET = TranslatedText("gunpowder.island.home_set")

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        Command.builder(dispatcher) {
            command("island", "is") {
                literal("home") {
                    // Teleport to island
                    executes(::homeIsland)

                    literal("set") {
                        executes(::setHomeIsland)
                    }
                }

                literal("team") {
                    // Configure island team
                    literal("add") {
                        argument("player", EntityArgumentType.player()) {
                            executes(::inviteMember)
                        }
                    }

                    literal("remove") {
                        argument("player", GameProfileArgumentType.gameProfile()) {
                            executes(::removeMember)
                        }
                    }

                    literal("accept") {
                        argument("player", GameProfileArgumentType.gameProfile()) {
                            executes(::acceptInvite)
                        }
                    }
                }

                literal("transfer") {
                    argument("player", GameProfileArgumentType.gameProfile()) {
                        executes(::transferIsland)
                    }
                }

                literal("leave") {
                    executes(::leaveIsland)
                }

                literal("create") {
                    // Create island
                    executes(::createIsland)
                }

                literal("delete") {
                    // Delete island
                    executes(::deleteIsland)
                }

                literal("seed") {
                    executes(::islandSeed)
                }
            }
        }
    }

    private fun islandSeed(context: CommandContext<ServerCommandSource>): Int {
        if (!IslandHandler.hasIsland(context.source.player)) {
            context.reply(NO_ISLAND)
            return -1
        }

        val seed = GunpowderMod.instance.server.getWorld(IslandHandler.getIsland(context.source.player).dim)!!.seed
        context.reply(TranslatedText("gunpowder.island.seed", seed))

        return 0
    }

    private inline fun CommandContext<ServerCommandSource>.reply(text: TranslatedText) {
        source.sendFeedback(text.translateTextForPlayer(source.player), false)
    }

    private val invited = mutableMapOf<UUID, MutableList<UUID>>()

    private fun setHomeIsland(context: CommandContext<ServerCommandSource>): Int {
        if (!IslandHandler.hasOwnIsland(context.source.player)) {
            context.reply(NO_ISLAND_OWNED)
            return -1
        }

        val world = GunpowderMod.instance.server.getWorld(IslandHandler.getIsland(context.source.player).dim)
        world?.setSpawnPos(context.source.player.blockPos, 0f)
        context.reply(HOME_SET)

        return 1
    }

    private fun transferIsland(context: CommandContext<ServerCommandSource>): Int {
        if (!IslandHandler.hasOwnIsland(context.source.player)) {
            context.reply(NO_ISLAND_OWNED)
            return -1
        }

        IslandHandler.transferIsland(
            context.source.player,
            GameProfileArgumentType.getProfileArgument(context, "player").first()
        )
        context.reply(ISLAND_TRANSFERRED)
        return 1
    }

    private fun leaveIsland(context: CommandContext<ServerCommandSource>): Int {
        if (!IslandHandler.hasIsland(context.source.player)) {
            context.reply(NO_ISLAND)
            return -1
        }

        if (IslandHandler.hasOwnIsland(context.source.player)) {
            context.reply(ISLAND_OWNED)
            return -1
        }

        IslandHandler.leaveIsland(context.source.player)
        context.reply(LEAVE_ISLAND)

        return 1
    }

    private fun acceptInvite(context: CommandContext<ServerCommandSource>): Int {
        val from = GameProfileArgumentType.getProfileArgument(context, "player").first()
        if (from.id !in invited.getOrDefault(context.source.player.uuid, mutableListOf())) {
            context.reply(NO_INVITE)
            return -1
        }

        if (IslandHandler.teamFull(from.id)) {
            context.reply(ISLAND_FULL)
            return -1
        }

        invited.remove(context.source.player.uuid)
        IslandHandler.addMember(from.id, context.source.player)

        context.reply(ACCEPT_INVITE)
        GunpowderMod.instance.server.playerManager.getPlayer(from.id)?.let {
            it.sendMessage(
                TranslatedText("gunpowder.island.invite_accepted", context.source.player).translateTextForPlayer(it),
                false
            )
        }

        return 1
    }

    private fun removeMember(context: CommandContext<ServerCommandSource>): Int {
        if (!IslandHandler.hasOwnIsland(context.source.player)) {
            context.reply(NO_ISLAND_OWNED)
            return -1
        }

        val target = GameProfileArgumentType.getProfileArgument(context, "player").first()
        IslandHandler.removeMember(context.source.player, target.id)

        context.reply(TranslatedText("gunpowder.island.remove_player", target.name))
        GunpowderMod.instance.server.playerManager.getPlayer(target.id)?.let {
            it.sendMessage(
                TranslatedText("gunpowder.island.island_remove", context.source.player.entityName).translateTextForPlayer(it),
                false
            )
        }

        return 1
    }

    private fun inviteMember(context: CommandContext<ServerCommandSource>): Int {
        if (!IslandHandler.hasOwnIsland(context.source.player)) {
            context.reply(NO_ISLAND_OWNED)
            return -1
        }

        val target = EntityArgumentType.getPlayer(context, "player")

        if (IslandHandler.hasIsland(target)) {
            context.reply(ALREADY_ON_OTHER_ISLAND)
            return -1
        }

        invited.getOrPut(target.uuid) { mutableListOf() }.add(context.source.player.uuid)
        target.sendMessage(
            TranslatedText("gunpowder.island.invite_from", context.source.player.entityName).translateTextForPlayer(target),
            false
        )
        context.reply(USER_INVITED)

        return 1
    }

    private fun homeIsland(context: CommandContext<ServerCommandSource>): Int {
        if (!IslandHandler.hasIsland(context.source.player)) {
            context.reply(NO_ISLAND)
            return -1
        }
        IslandHandler.getIsland(context.source.player).teleport(context.source.player)
        return 1
    }

    private fun deleteIsland(context: CommandContext<ServerCommandSource>): Int {
        if (!IslandHandler.hasOwnIsland(context.source.player)) {
            context.reply(NO_ISLAND_OWNED)
            return -1
        }
        context.source.player.inventory.clear()
        IslandHandler.deleteIsland(context.source.player)
        context.reply(ISLAND_DELETED)
        return 1
    }

    private fun createIsland(context: CommandContext<ServerCommandSource>): Int {
        if (IslandHandler.hasIsland(context.source.player)) {
            context.reply(ALREADY_ON_ISLAND)
            return -1
        }
        val island = IslandHandler.createNewIsland(context.source.player)
        island.teleport(context.source.player)
        return 1
    }
}

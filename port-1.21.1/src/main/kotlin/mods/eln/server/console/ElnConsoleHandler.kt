package mods.eln.server.console

import mods.eln.misc.FC
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.neoforged.neoforge.event.RegisterCommandsEvent

val ElnConsoleCommandList = mutableListOf<IConsoleCommand>()

internal fun findConsoleCommand(
    name: String,
    commands: Iterable<IConsoleCommand> = ElnConsoleCommandList
): IConsoleCommand? {
    return commands.firstOrNull { it.name.equals(name, ignoreCase = true) }
}

/** The 1.7.10 /eln console: one Brigadier command whose words are handed to the IConsoleCommand of that name. */
class ElnConsoleCommands {
    companion object {
        init {
            ElnConsoleCommandList.addAll(listOf(
                ElnLsCommand(),
                ElnConfigCommand(),
                ElnAboutCommand(),
                ElnVersionCommand(),
                ElnNewWindDirectionCommand(),
                ElnMatrixCommand(),
                ElnManCommand(),
                ElnWailaEasyModeCommand(),
                ElnDebugCommand(),
                ElnSimSnapshotCommand(),
                ElnWatchdogCommand(),
                ElnIconsCommand(),
                ElnPoleMapCommand(),
                ElnStopShaftCommand(),
                ElnResetAmbientTempsCommand(),
                ElnResetLampLifeCommand(),
                ElnZoneDumpCommand(),
                ElnZoneCleanCommand(),
                ElnZoneRemoveCommand(),
                ElnZoneDestroyCommand()
            ))
        }

        fun register(event: RegisterCommandsEvent) {
            event.dispatcher.register(
                Commands.literal("eln")
                    .executes { context -> processCommand(context.source, emptyList()); 1 }
                    .then(Commands.argument("args", StringArgumentType.greedyString())
                        .suggests { context, builder ->
                            val typed = builder.remaining.split(' ')
                            val start = builder.start + builder.remaining.lastIndexOf(' ') + 1
                            val words = SharedSuggestionProvider.suggest(tabCompletion(typed), builder.createOffset(start))
                            words
                        }
                        .executes { context ->
                            processCommand(context.source, StringArgumentType.getString(context, "args").split(' ').filter { it.isNotEmpty() })
                            1
                        })
            )
        }

        fun cprint(ics: CommandSourceStack, text: String, indent: Int = 0) {
            printIndented(text, indent).forEach {
                ics.sendSuccess({ Component.literal(it) }, false)
            }
        }

        fun cprint(ics: CommandSourceStack, text: String, url: String) {
            val msg = Component.literal(FC.BRIGHT_GREY + text).withStyle { it.withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, url)) }
            ics.sendSuccess({ msg }, false)
        }

        fun printIndented(text: String, indent: Int): List<String> {
            val lineLength = 60 - indent * 2
            val list = mutableListOf<String>()
            val finalLine = text
                .split(' ')
                .fold("", { acc, next ->
                    if (acc.length + next.length > lineLength) {
                        list.add(acc)
                        next
                    } else {
                        "$acc $next"
                    }
                })
            list.add(finalLine)
            var mostRecentColor = '7' // default color is light gray
            val list2 = list.map  {
                line ->
                val lastLine = "§$mostRecentColor$line"
                if ("§" in line) {
                    mostRecentColor = line[line.lastIndexOf("§") + 1]
                }
                lastLine
            }
            val whitespace = (0 until indent * 2).joinToString("") { " " }
            return list2.map{"$whitespace$it"}
        }

        fun getArgBool(ics: CommandSourceStack, arg: String): Boolean? {
            val lowerArg = arg.lowercase()
            return if (lowerArg.isEmpty()) {
                cprint(ics, "Error: Empty argument.", indent = 1)
                null
            }else if (lowerArg == "0" || lowerArg == "false" || lowerArg == "no" || lowerArg == "disabled" || lowerArg == "disable") {
                false
            } else if (lowerArg == "1" || lowerArg == "true" || lowerArg == "yes" || lowerArg == "enabled" || lowerArg == "enable") {
                true
            } else {
                cprint(ics, "Error: Expected (true/false), got $arg",  indent = 1)
                null
            }
        }

        fun boolToStr(value: Boolean): String {
            return if (value) "Enabled" else "Disabled"
        }

        fun processCommand(ics: CommandSourceStack, args: List<String>) {
            if (args.isEmpty()) {
                cprint(ics, "${FC.DARK_CYAN}Electrical Age Console, run /eln ls for commands${FC.BRIGHT_GREY}")
                return
            }
            val command = findConsoleCommand(args[0])
            if (command == null) {
                cprint(ics, "${FC.DARK_CYAN}Command not found, run /eln ls for commands${FC.BRIGHT_GREY}")
                return
            }
            cprint(ics, "${FC.DARK_CYAN}${ics.textName} $${FC.DARK_YELLOW} /eln ${args.joinToString(" ")}")
            val permissions = determinePermissionsList(ics)
            if (permissions.any { command.requiredPermission().contains(it) }) {
                command.runCommand(ics, args.drop(1))
            } else {
                cprint(ics, "${FC.DARK_CYAN}You do not have permission to run that command. " +
                    "You need to have one of the following: ${command.requiredPermission()}${FC.BRIGHT_GREY}")
            }
        }

        fun determinePermissionsList(ics: CommandSourceStack): List<UserPermission> {
            val player = ics.player
            val playerPerms = mutableListOf<UserPermission>()
            if (player == null) {
                playerPerms.add(UserPermission.IS_CONSOLE)
                playerPerms.add(UserPermission.IS_OPERATOR)
                return playerPerms
            }
            if (player.isCreative) playerPerms.add(UserPermission.IS_CREATIVE)
            // Singleplayer owners count as operators, as in 1.7.10.
            if (ics.hasPermission(2) || ics.server.isSingleplayerOwner(player.gameProfile)) playerPerms.add(UserPermission.IS_OPERATOR)
            return playerPerms
        }

        fun tabCompletion(args: List<String>): List<String> {
            if (args.isEmpty() || args.size == 1) {
                val prefix = args.firstOrNull() ?: ""
                return ElnConsoleCommandList.filter { it.name.startsWith(prefix, ignoreCase = true) }.map { it.name }
            }
            return findConsoleCommand(args[0])?.getTabCompletion(args.drop(1)) ?: listOf()
        }
    }
}

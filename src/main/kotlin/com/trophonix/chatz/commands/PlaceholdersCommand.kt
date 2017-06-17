package com.trophonix.chatz.commands

import com.trophonix.chatz.ChatZ
import com.trophonix.chatz.util.Messages
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.*
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
* Created by Lucas on 4/25/17.
*/
class PlaceholdersCommand : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isNotEmpty()) {
            val message = args[0].toLowerCase()
            if (FormatCommand.MESSAGES.contains(message)) {
                sender.sendMessage(arrayOf(Messages.DIV,
                        Messages.GREEN + "[" + ChatZ.PREFIX + "] " + Messages.cap(message) + " Message Placeholders:"))
                when(message) {
                    "chat" -> sender.sendMessage("{message} " + Messages.GRAY + "The message")
                    "death-generic", "generic-death", "death" -> sender.sendMessage("{victim} " + Messages.GRAY + "The player who died")
                    "death-pvp", "pvp-death", "pvp" -> sender.sendMessage(arrayOf("{killer} " + Messages.GRAY + "The killer player", "{item} " + Messages.GRAY + "The item held by the killer"))
                    "death-mob", "mob-death", "mob" -> sender.sendMessage("{mob} " + Messages.GRAY + "The killer mob (alternative to {killer})")
                    else -> {
                        if (message.startsWith("filter-")) {
                            sender.sendMessage("{message} " + Messages.GRAY + "The message that was flagged")
                        } else sender.sendMessage(Messages.GRAY + "There are no specific placeholders for " + message + " messages")
                    }
                }
                sender.sendMessage(Messages.DIV)
                return true
            }
        }

        val cmd = "/$label"
        sender.sendMessage(arrayOf(Messages.DIV,
                Messages.GREEN + "[" + ChatZ.PREFIX + "] General Placeholders:",
                "{player} " + Messages.GRAY + "The relevant player's name",
                "{channel:prefix} " + Messages.GRAY + "The prefix of the player's chat channel",
                "{prefix} " + Messages.GRAY + "The player's primary prefix",
                "{suffix} " + Messages.GRAY + "The player's primary suffix",
                "{player:prefix} " + Messages.GRAY + "The player's direct prefix",
                "{player:suffix} " + Messages.GRAY + "The player's direct suffix",
                "{group:prefix} " + Messages.GRAY + "The player's primary group's prefix",
                "{group:suffix} " + Messages.GRAY + "The player's primary group's suffix",
                "",
                Messages.GREEN + "Type " + Messages.DARK_GREEN + "$cmd <event>" + Messages.GREEN + " to view specific placeholders."))
        if (sender is Player) {
            val messages = ArrayList<BaseComponent>()
            var lineSize = 0
            FormatCommand.MESSAGES.forEach {
                val i = it + if (FormatCommand.MESSAGES.indexOf(it) < (FormatCommand.MESSAGES.size - 1)) {
                    lineSize += it.length + 2
                    Messages.GREEN + ", "
                } else {
                    lineSize += it.length
                    ""
                }
                if (lineSize > (52 - it.length - 2)) {
                    lineSize = 0
                    messages.add(TextComponent("\n"))
                }
                messages.add(TextComponent(*ComponentBuilder(i).color(ChatColor.DARK_GREEN)
                        .event(ClickEvent(ClickEvent.Action.RUN_COMMAND, "$cmd $it"))
                        .event(HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder("View ${Messages.cap(it)} Message Placeholders").color(ChatColor.GREEN).create()))
                        .create()))
            }
            sender.sendMessage(Messages.GREEN + "Available Events:")
            sender.spigot().sendMessage(TextComponent(
                    *messages.toTypedArray()
            ))
        }
        sender.sendMessage(Messages.DIV)
        return true
    }

}
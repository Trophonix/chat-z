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
            if (MessagesCommand.MESSAGE_EVENTS.contains(message)) {
                sender.sendMessage(arrayOf(Messages.SEPARATOR,
                        Messages.GREEN + "[" + ChatZ.PREFIX + "] " + Messages.cap(message) + " Message Placeholders:"))
                when(message) {
                    "chat" -> sender.sendMessage("{message} " + Messages.GRAY + "The message")
                    "death-generic", "generic-death", "death" -> sender.sendMessage("{victim} " + Messages.GRAY + "The player who died")
                    "death-pvp", "pvp-death", "pvp" -> sender.sendMessage(arrayOf("{killer} " + Messages.GRAY + "The killer player", "{item} " + Messages.GRAY + "The item held by the killer"))
                    else -> sender.sendMessage(Messages.GRAY + "There are no specific placeholders for " + message + " messages")
                }
                sender.sendMessage(Messages.SEPARATOR)
                return true;
            }
        }

        sender.sendMessage(arrayOf(Messages.SEPARATOR,
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
                Messages.GREEN + "Type " + Messages.DARK_GREEN + "/placeholders <event>" + Messages.GREEN + " to view specific placeholders."))
        if (sender is Player) {
            val messages = ArrayList<BaseComponent>()
            MessagesCommand.MESSAGE_EVENTS.forEach {
                val i = it + if (MessagesCommand.MESSAGE_EVENTS.indexOf(Messages.cap(it)) < MessagesCommand.MESSAGE_EVENTS.size - 1) Messages.GREEN + ", " else ""
                messages.add(TextComponent(*ComponentBuilder(i).color(ChatColor.DARK_GREEN)
                        .event(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/placeholders $it"))
                        .event(HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder("View ${Messages.cap(it)} Message Placeholders").color(ChatColor.GREEN).create()))
                        .create()))
            }
            sender.spigot().sendMessage(TextComponent(
                    *ComponentBuilder("Available Events: ").color(ChatColor.GREEN).create(),
                    *messages.toTypedArray()
            ))
        }
        sender.sendMessage(Messages.SEPARATOR)
        return true;
    }

}
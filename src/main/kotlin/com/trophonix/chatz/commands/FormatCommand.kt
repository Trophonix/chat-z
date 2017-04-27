package com.trophonix.chatz.commands

import com.trophonix.chatz.ChatZ
import com.trophonix.chatz.util.Messages
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.*
import org.apache.commons.lang.StringUtils
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
* Created by Lucas on 4/25/17.
*/
class FormatCommand(val plugin : ChatZ) : CommandExecutor {

    companion object {
        @JvmField val MESSAGES = arrayListOf("chat", "join", "quit", "death-generic", "death-pvp", "death-mob")
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isNotEmpty()) {
            val message = args[0].toLowerCase()
            if (MESSAGES.contains(message)) {
                if (args.size == 1) {
                    val format = plugin.config.getString("messages." + message)
                    sender.sendMessage(arrayOf(Messages.SEPARATOR,
                            Messages.GREEN + "[" + ChatZ.PREFIX + "] Current " + Messages.cap(message) + " Message Format:",
                            format ?: Messages.RED + "Not Yet Set!", ""
                    ))
                    if (sender !is Player ) {
                        sender.sendMessage(Messages.GREEN + "Type " + Messages.DARK_GREEN + "/placeholders" + Messages.GREEN + " to view the available placeholders.")
                    } else {
                        val slashPlaceholders : Array<BaseComponent> = ComponentBuilder("/placeholders").color(ChatColor.DARK_GREEN)
                                .event(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/placeholders"))
                                .event(HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder("View the Available Placeholders").color(ChatColor.GREEN).create()))
                                .create()
                        sender.spigot().sendMessage(TextComponent(
                                TextComponent(Messages.GREEN + "Type "),
                                TextComponent(*slashPlaceholders),
                                TextComponent(Messages.GREEN + " to view the available placeholders.")
                        ))
                        val clickHere : Array<BaseComponent> = ComponentBuilder("Click Here").color(ChatColor.DARK_GREEN)
                                .event(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/format $message ${format?:""}"))
                                .event(HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder("Edit Current Format").color(ChatColor.GREEN).create()))
                                .create()
                        sender.spigot().sendMessage(TextComponent(TextComponent(*clickHere), TextComponent(Messages.GREEN + " to edit the current format.")))
                    }
                    sender.sendMessage(Messages.SEPARATOR)
                } else {
                    val newFormat = StringUtils.join(args.copyOfRange(1, args.size), ' ')
                    plugin.config.set("messages." + message, newFormat)
                    Messages.success(sender, Messages.SEPARATOR + "\\n[" + ChatZ.PREFIX + "] " + Messages.cap(message) + " Message Format set to:\\n{0}\\n" + Messages.SEPARATOR, newFormat)
                }
                return true
            }
        }
        val cmd = "/$label "
        sender.sendMessage(Messages.SEPARATOR)
        sender.sendMessage(Messages.GREEN + "[" + ChatZ.PREFIX + "] Change Message Formats:")
        if (sender !is Player)
            MESSAGES.forEach { sender.sendMessage(cmd + it) }
        else
            MESSAGES.forEach {
                sender.spigot().sendMessage(TextComponent(
                        *ComponentBuilder(cmd + it).event(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/placeholders $it"))
                                .event(HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder("Edit the $it message").create()))
                                .create()
                ))
            }
        sender.sendMessage("")
        if (sender !is Player) {
            sender.sendMessage(Messages.GREEN + "Type " + Messages.DARK_GREEN + "/placeholders" + Messages.GREEN + " to view the available placeholders.")
        } else {
            val slashPlaceholders: Array<BaseComponent> = ComponentBuilder("/placeholders").color(ChatColor.DARK_GREEN)
                    .event(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/placeholders"))
                    .event(HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder("View the Available Placeholders").color(ChatColor.GREEN).create()))
                    .create()
            sender.spigot().sendMessage(TextComponent(
                    TextComponent(Messages.GREEN + "Type "),
                    TextComponent(*slashPlaceholders),
                    TextComponent(Messages.GREEN + " to view the available placeholders.")
            ))
        }
        sender.sendMessage(Messages.SEPARATOR)
        return true
    }

}

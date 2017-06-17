package com.trophonix.chatz.commands

import com.trophonix.chatz.ChatZ
import com.trophonix.chatz.data.PlayerData
import com.trophonix.chatz.util.Messages
import net.md_5.bungee.api.chat.*
import org.apache.commons.lang.StringUtils
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
* Created by Lucas on 4/25/17.
*/
class ColorsCommand (val plugin : ChatZ) : CommandExecutor {
    companion object {
        @JvmField val COLOR_TYPES = arrayListOf("name", "chat")
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val cmd = "/$label"
        if (command.name == "colors") {
            val colors = StringBuilder()
            ChatColor.values().filter { it.isColor }.forEach {
                if (Messages.hasColorPermission(sender, it)) {
                    val split = colors.split("\n")
                    if (split[split.size - 1].length > 45) colors.append("\n")
                    colors.append(it.toString()).append(Messages.cap(it.name.replace('_', ' '))).append(" ")
                }
            }
            if (colors.toString() == "") colors.append(Messages.RED).append("None")
            sender.sendMessage(arrayOf(
                    Messages.DIV,
                    Messages.GREEN + "[" + ChatZ.PREFIX + "] You can use the following colors:",
                    *colors.toString().split("\n").toTypedArray(),
                    Messages.DIV
            ))
            return true
        } else if (command.name == "color") {
            if (sender !is Player) {
                Messages.notPlayer(sender)
                return true
            }
            if (args.size > 1) {
                val data = plugin.playerData[sender.uniqueId]
                val item = ColorItem.getByName(args[0])
                if (item != null) {
                    var color = Messages.getColor(args[1])
                    if (color == null && args.size > 2) {
                        color = Messages.getColor(args[1] + "_" + args[2])
                    }
                    if (color == null) {
                        if (args[1].equals("clear", true) || args[1].equals("null", true) || args[1] == "''" || args[1] == "\"\"") {
                            item.set(data, null)
                            Messages.success(sender, "[" + ChatZ.PREFIX + "] Cleared your ${item.displayName}!")
                            return true
                        }
                        Messages.failure(sender, "Unknown color: {0}", StringUtils.join(args.copyOfRange(1, args.size), ' '))
                        return true
                    }
                    item.set(data, color)
                    Messages.success(sender, "[" + ChatZ.PREFIX + "] Set your ${item.displayName} to: " + color.toString() + Messages.cap(color.name.replace('_', ' ')))
                    return true
                }
            } else if (args.isNotEmpty()) {
                val data = plugin.playerData[sender.uniqueId]
                val item = ColorItem.getByName(args[0])
                if (item != null) {
                    val color = item.get(data)
                    sender.sendMessage(arrayOf(
                            Messages.DIV,
                            Messages.GREEN + "Your ${item.displayName} is: " +
                            if (color != null) color.toString() + Messages.cap(color.name.replace('_', ' '))
                            else Messages.DARK_RED + "Not Set",
                            ""))
                    val clickHere = ComponentBuilder("Click Here").color(net.md_5.bungee.api.ChatColor.DARK_GREEN)
                            .event(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/colors"))
                            .event(HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder("View Your Available Colors").color(net.md_5.bungee.api.ChatColor.GREEN).create()))
                            .create()
                    sender.spigot().sendMessage(TextComponent(
                            *clickHere,
                            TextComponent(Messages.GREEN + " to view your available colors.")
                    ))
                    sender.sendMessage(Messages.DIV)
                    return true
                }
            }
            sender.sendMessage(arrayOf(
                    Messages.DIV,
                    Messages.GREEN + "[" + ChatZ.PREFIX + "] Player Color Options:"
            ))
            ColorItem.values().forEach {
                sender.spigot().sendMessage(TextComponent(*ComponentBuilder("$cmd ${it.simpleName} <color>")
                        .event(ClickEvent(ClickEvent.Action.RUN_COMMAND, "$cmd ${it.simpleName}"))
                        .event(HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                ComponentBuilder("View Your ${it.displayName}").color(net.md_5.bungee.api.ChatColor.GREEN).create()))
                        .create()))
            }
            sender.sendMessage(Messages.DIV)
            return true
        } else if (command.name.contains("color")) {
            Bukkit.dispatchCommand(sender, "color " + command.name.replace("color", "") + " " + StringUtils.join(args, ' '))
            return true
        }
        return true
    }

    enum class ColorItem(val displayName : String, val simpleName : String) {

        NAME_COLOR("Name Color", "name") {
            override fun get(data: PlayerData?) : ChatColor? {
                return data?.nameColor
            }
            override fun set(data: PlayerData?, value: ChatColor?) {
                data?.nameColor = value
            }
        },
        CHAT_COLOR("Chat Color", "chat") {
            override fun get(data: PlayerData?): ChatColor? {
                return data?.chatColor
            }
            override fun set(data: PlayerData?, value: ChatColor?) {
                data?.chatColor = value
            }
        };

        abstract fun get(data : PlayerData?) : ChatColor?
        abstract fun set(data : PlayerData?, value : ChatColor?)

        companion object {
            fun getByName(name : String) : ColorItem? {
                return values().firstOrNull { it.name.equals(name, true) || it.displayName.toLowerCase().contains(name.toLowerCase()) }
            }
        }

    }

}
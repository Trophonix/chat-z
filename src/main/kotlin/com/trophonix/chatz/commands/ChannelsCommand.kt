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
class ChannelsCommand(val plugin : ChatZ) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val cmd = "/$label"
        if (command.name == "channel") {
            if (args.isNotEmpty()) {
                if (sender !is Player) {
                    sender.sendMessage(Messages.RED + "This command is for players only.")
                    return true
                }
                val channel = args[0].toLowerCase()
                if (!plugin.channels!!.contains("channels.$channel")) {
                    Messages.failure(sender, "A channel called {0} does not exist.", channel)
                    return true
                }
                plugin.playerData[sender.uniqueId]?.channel = channel
                Messages.success(sender, "Successfully joined {0}", channel)
                return true
            }
            sender.sendMessage(Messages.GREEN + "Usage: " + Messages.DARK_GREEN + "$cmd <name>")
            return true
        } else if (command.name == "channels") {
            if (args.size == 1) {
                val channel = args[0].toLowerCase()
                if (!plugin.channels!!.contains("channels.$channel")) {
                    if (channel == "default") {
                        sender.sendMessage(arrayOf(
                                Messages.DIV,
                                Messages.GREEN + "[" + ChatZ.PREFIX + "] The default channel is:",
                                Messages.GRAY + plugin.channels!!.getString("default", Messages.DARK_RED + "Not Set!"),
                                ""
                        ))
                        if (sender !is Player) {
                            sender.sendMessage(Messages.GREEN + "Type " + Messages.DARK_GREEN +
                                    "$cmd default <name>" + Messages.GREEN + " to change it.")
                        } else {
                            val clickHere : Array<BaseComponent> = ComponentBuilder("Click Here").color(ChatColor.DARK_GREEN)
                                    .event(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "$cmd default "))
                                    .event(HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder("Change the default channel").color(ChatColor.GREEN).create()))
                                    .create()
                            sender.spigot().sendMessage(TextComponent(
                                    *clickHere,
                                    *ComponentBuilder(" to change it.").color(ChatColor.GREEN).create()
                            ))
                        }
                        sender.sendMessage(Messages.DIV)
                        return true
                    } else if (channel == "list") {
                        sender.sendMessage(arrayOf(Messages.DIV,
                                Messages.GREEN + "[" + ChatZ.PREFIX + "] Channels:"))
                        if (sender !is Player) {
                            plugin.channels!!.getConfigurationSection("channels").getKeys(false).forEach {
                                sender.sendMessage(" - " + Messages.GRAY + it)
                            }
                        } else {
                            plugin.channels!!.getConfigurationSection("channels").getKeys(false).forEach {
                                sender.spigot().sendMessage(TextComponent(
                                        *ComponentBuilder(" - " + Messages.GRAY + it)
                                                .event(ClickEvent(ClickEvent.Action.RUN_COMMAND, "$cmd $it"))
                                                .event(HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                        ComponentBuilder("View info about $it").color(ChatColor.GREEN).create()))
                                                .create()
                                ))
                            }
                        }
                        sender.sendMessage(Messages.DIV)
                        return true
                    }
                    Messages.failure(sender, "A channel called {0} does not exist.", channel)
                    return true
                }
                sender.sendMessage(arrayOf(
                        Messages.DIV,
                        Messages.GREEN + "[" + ChatZ.PREFIX + "] Channel Info ($channel):"
                ))
                ChannelItem.values().forEach {
                    sender.sendMessage("${it.displayName}: " + Messages.GRAY + plugin.channels!!.get("channels.$channel.${it.name}", Messages.DARK_RED + "Not Set")?.toString())
                }
                sender.sendMessage(Messages.DIV)
                return true
            } else if (args.size >= 2) {
                when (args[0].toLowerCase()) {
                    "add", "create", "make" -> {
                        val channel = args[1].toLowerCase()
                        if (plugin.channels!!.contains("channels.$channel")) {
                            Messages.failure(sender, "A channel called {0} already exists!", channel)
                            return true
                        }
                        ChannelItem.values().forEach {
                            plugin.channels!!.set("channels.$channel." + it.name, it.defaultValue)
                        }
                        Messages.success(sender, "Successfully created the {0} channel!", channel)
                        return true
                    }
                    "delete", "remove", "del", "rem" -> {
                        val channel = args[1].toLowerCase()
                        if (!plugin.channels!!.contains("channels.$channel")) {
                            Messages.failure(sender, "A channel called {0} does not exist.", channel)
                            return true
                        }
                        plugin.channels!!.set("channels.$channel", null)
                        Messages.success(sender, "Successfully deleted the {0} channel!", channel)
                        return true
                    }
                    "default" -> {
                        val channel = args[1].toLowerCase()
                        if (!plugin.channels!!.contains("channels.$channel")) {
                            Messages.failure(sender, "A channel called {0} does not exist.", channel)
                            return true
                        }
                        plugin.channels!!.set("default", channel)
                        Messages.success(sender, "Default channel set to {0}", channel)
                        return true
                    }
                    else -> {
                        val channel = args[0].toLowerCase()
                        val item = ChannelItem.getByName(args[1])
                        if (item != null) {
                            if (args.size >= 3) {
                                var value = StringUtils.join(args.copyOfRange(2, args.size), ' ')
                                if (value == "\"\"" || value.equals("null", true) || value.equals("clear", true)) {
                                    plugin.channels!!.set("channels.$channel.${item.name}", null)
                                    Messages.success(sender, "${item.displayName} of $channel cleared!")
                                    return true
                                }
                                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                                    value = value.substring(1..value.length - 2);
                                }
                                try {
                                    plugin.channels!!.set("channels.$channel.${item.name}", item.parse(value))
                                    Messages.success(sender, "${item.displayName} of $channel set to {0}", value)
                                } catch (ex : Exception) {
                                    Messages.failure(sender, ex.message!!, value)
                                }
                            } else {
                                sender.sendMessage(arrayOf(
                                        Messages.DIV,
                                        Messages.GREEN + "[" + ChatZ.PREFIX + "] ${item.displayName} of $channel:",
                                        plugin.channels!!.get("channels.$channel.${item.name}", Messages.DARK_RED + "Not Set!").toString(),
                                        ""
                                ))
                                if (sender !is Player) {
                                    sender.sendMessage(Messages.GREEN + "Type " + Messages.DARK_GREEN +
                                            "$cmd $channel ${item.name.toLowerCase()} <value> " +
                                            Messages.GREEN + "to change this.")
                                } else {
                                    val clickHere : Array<BaseComponent> = ComponentBuilder("Click Here").color(ChatColor.DARK_GREEN)
                                            .event(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "$cmd $channel ${item.name.toLowerCase()} "))
                                            .event(HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder("Edit ${item.displayName} of $channel").color(ChatColor.GREEN).create()))
                                            .create()
                                    sender.spigot().sendMessage(TextComponent(
                                        *clickHere,
                                        *ComponentBuilder(" to edit this value!").color(ChatColor.GREEN).create()
                                    ))
                                }
                                sender.sendMessage(Messages.DIV)
                            }
                            return true
                        }
                    }
                }
            }
            sender.sendMessage(arrayOf(
                    Messages.DIV,
                    Messages.GREEN + "[" + ChatZ.PREFIX + "] " + "Channels:",
                    "$cmd add <name> " + Messages.GRAY + "Make a channel",
                    "$cmd del <name> " + Messages.GRAY + "Delete a channel",
                    "$cmd default <name> " + Messages.GRAY + "Set the default channel",
                    "$cmd list",
                    "$cmd <name> " + Messages.GRAY + "View info about a channel"
            ))
            ChannelItem.values().forEach {
                sender.sendMessage("$cmd <name> ${it.name.toLowerCase()} [value] " + Messages.GRAY + it.description)
            }
            sender.sendMessage(Messages.DIV)
        }
        return true
    }

    enum class ChannelItem(val displayName : String, val description : String, val defaultValue : Any?) {

        PREFIX("Prefix", "Prefix to display in messages", null) {
            override fun parse(string: String): Any {
                return string
            }
        },
        CHAT_COLOR("Chat Color", "Color for chat in this channel", null) {
            override fun parse(string: String): Any {
                val color = Messages.getColor(string) ?: throw Exception("Unknown color: {0}")
                return color.name
            }
        },
        NAME_COLOR("Name Color", "Color for player names", null) {
            override fun parse(string: String): Any {
                val color = Messages.getColor(string) ?: throw Exception("Unknown color: {0}")
                return color.name
            }
        },
        DELAY("Delay", "Delay between messages", null) {
            override fun parse(string: String): Any {
                try {
                    return string.toDouble()
                } catch (ex : NumberFormatException) {
                    throw Exception("Not a number: {0}")
                }
            }
        },
        RANGE("Range", "Range that messages reach", null) {
            override fun parse(string: String): Any {
                try {
                    return string.toDouble()
                } catch (ex : NumberFormatException) {
                    throw Exception("Not a number: {0}")
                }
            }
        },
        GLOBAL("Global", "Other channels see chat in this one", false) {
            override fun parse(string: String): Any {
                return string.toBoolean()
            }
        };

        abstract fun parse(string : String) : Any

        companion object {
            fun getByName(name : String) : ChannelItem? {
                for (item in values()) {
                    if (item.name.equals(name, true) || item.name.replace("_", "").toLowerCase() == name.replace("_", "").toLowerCase()) {
                        return item
                    }
                }
                return null
            }
        }

    }

}
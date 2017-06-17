package com.trophonix.chatz.commands

import com.trophonix.chatz.ChatZ
import com.trophonix.chatz.util.Messages
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.apache.commons.lang.StringUtils
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
* Created by Lucas on 4/24/17.
*/
class ChatZCommand(val plugin: ChatZ) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isNotEmpty()) {
            if (plugin.getCommand(args[0].toLowerCase()) != null) {
                Bukkit.dispatchCommand(sender, StringUtils.join(args, ' '))
                return true
            }
        }
        val cmd = "/$label "
        sender.sendMessage(arrayOf(Messages.DIV, Messages.GREEN + "[" + ChatZ.PREFIX + " by Trophonix]"))
        plugin.description.commands.forEach { c, d ->
            if (c != command.name) {
                if (sender !is Player)
                    sender.sendMessage("$cmd$c ${Messages.GRAY} ${d["description"]}")
                else
                    sender.spigot().sendMessage(TextComponent(*ComponentBuilder(cmd + c + " " + Messages.GRAY + d["description"])
                            .event(ClickEvent(ClickEvent.Action.RUN_COMMAND, "$cmd$c"))
                            .event(HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder("Run $cmd$c").create()))
                            .create()))
            }
        }
        sender.sendMessage(Messages.DIV)
        return true
    }

}

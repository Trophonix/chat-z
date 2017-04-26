package com.trophonix.chatz.commands

import com.trophonix.chatz.ChatZ
import com.trophonix.chatz.util.Messages
import org.apache.commons.lang.StringUtils
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

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
        sender.sendMessage(arrayOf(Messages.SEPARATOR, Messages.GREEN + "[" + ChatZ.PREFIX + " by Trophonix]"))
        plugin.description.commands.forEach { c, d ->
            if (c != command.name) sender.sendMessage(Messages.WHITE + cmd + c + " " + Messages.GRAY + d["description"])
        }
        sender.sendMessage(Messages.SEPARATOR)
        return true
    }

}

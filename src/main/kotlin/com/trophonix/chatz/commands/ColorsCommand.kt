package com.trophonix.chatz.commands

import com.trophonix.chatz.ChatZ
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

/**
* Created by Lucas on 4/25/17.
*/
class ColorsCommand (plugin : ChatZ) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        return true
    }

}
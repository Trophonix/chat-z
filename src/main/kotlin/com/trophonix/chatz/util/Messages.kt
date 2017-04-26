package com.trophonix.chatz.util

import org.bukkit.ChatColor
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
* Created by Lucas on 4/25/17.
*/
class Messages {
    companion object {
        val BLACK = ChatColor.BLACK.toString()
        val DARK_BLUE = ChatColor.DARK_BLUE.toString()
        val DARK_GREEN = ChatColor.DARK_GREEN.toString()
        val DARK_AQUA = ChatColor.DARK_AQUA.toString()
        val DARK_RED = ChatColor.DARK_RED.toString()
        val PURPLE = ChatColor.DARK_PURPLE.toString()
        val GOLD = ChatColor.GOLD.toString()
        val GRAY = ChatColor.GRAY.toString()
        val DARK_GRAY = ChatColor.DARK_GRAY.toString()
        val BLUE = ChatColor.BLUE.toString()
        val GREEN = ChatColor.GREEN.toString()
        val AQUA = ChatColor.AQUA.toString()
        val RED = ChatColor.RED.toString()
        val PINK = ChatColor.LIGHT_PURPLE.toString()
        val YELLOW = ChatColor.YELLOW.toString()
        val WHITE = ChatColor.WHITE.toString()
        val BOLD = ChatColor.BOLD.toString()
        val ITALIC = ChatColor.ITALIC.toString()
        val UNDERLINE = ChatColor.UNDERLINE.toString()
        val STRIKE = ChatColor.STRIKETHROUGH.toString()
        val RESET = ChatColor.RESET.toString()
        val SEPARATOR = RED + "-----------------------------------"

        fun success(sender : CommandSender, m : String, vararg inserts : String) {
            var message = m
            for (i in inserts.indices) {
                message = message.replace("{$i}", WHITE + inserts[i] + GREEN)
            }
            message.split("\\n").forEach({
                sender.sendMessage(GREEN + it)
            })
            if (sender is Player) {
                sender.playSound(sender.eyeLocation, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
            }
        }
        fun failure(sender : CommandSender, m : String, vararg inserts : String) {
            var message = m
            for (i in inserts.indices) {
                message = message.replace("{$i}", GRAY + inserts[i] + RED)
            }
            sender.sendMessage(RED + message)
            if (sender is Player) {
                sender.playSound(sender.eyeLocation, Sound.ENTITY_VILLAGER_HURT, 1f, 1f)
            }
        }
        fun colorize(message : String) : String {
            return ChatColor.translateAlternateColorCodes('&', message)
        }
        fun center(m : String) : String {
            val chatWidth = 52
            if (m.length >= chatWidth) return m
            val sb = StringBuilder(m)
            while (sb.length < chatWidth) {
                sb.insert(0, ' ')
                sb.append(' ')
            }
            return sb.toString()
        }
        fun cap(m : String) : String {
            val message = StringBuilder()
            for (i in m.indices) {
                if (i == 0 || m[i - 1] == '-') {
                    message.append(m[i].toUpperCase())
                } else {
                    message.append(m[i].toLowerCase())
                }
            }
            return message.toString()
        }
    }
}
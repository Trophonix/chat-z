package com.trophonix.chatz.commands

import com.trophonix.chatz.ChatZ
import com.trophonix.chatz.util.Messages
import org.apache.commons.lang.StringUtils
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

/**
* Created by Lucas on 4/25/17.
*/
class FilterCommand(val plugin : ChatZ) : CommandExecutor {

    companion object {
        @JvmField val BANNED = HashMap<String, MutableList<String>>()
        @JvmField val WHITELISTED_URLS = ArrayList<String>()
        @JvmField val COMMON_BYPASSES = linkedMapOf(
                "|(" to "k",
                "(" to "c",
                "$" to "s",
                "#" to "h",
                "!" to "i",
                "1" to "i",
                "fuk" to "fuck",
                "<" to "c",
                "fack" to "fuck",
                "fqck" to "fuck",
                "0" to "o",
                "5" to "s",
                "4" to "a"
        )
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isNotEmpty()) {
            val type = args[0].toLowerCase()
            val cmd = "/$label $type"
            when (type) {
                "profanity", "swear", "curse" -> {
                    if (args.size > 1) {
                        when (args[1].toLowerCase()) {
                            "add" -> {
                                if (args.size > 2) {
                                    var msg = StringUtils.join(args.copyOfRange(2, args.size), ' ').toLowerCase()
                                    var category = args[args.size - 1].toLowerCase()
                                    if (!category.startsWith("c:")) category = "general" else {
                                        msg = msg.substring(0..msg.lastIndexOf(' ') - 1)
                                        category = category.replaceFirst("c:", "")
                                    }
                                    if (category == "urls") {
                                        Messages.failure(sender, "That filter cannot be modified.")
                                        return true
                                    }
                                    val list = (BANNED[category] ?: ArrayList<String>())
                                    val phrases = msg.replace(" , ", ",").replace(", ", ",").split(",")
                                    for (entry in BANNED) {
                                        for (phrase in phrases) {
                                            if (entry.value.contains(phrase)) {
                                                Messages.failure(sender, "The filter already contains {0}.", phrase)
                                                return true
                                            }
                                        }
                                    }
                                    list.addAll(phrases)
                                    BANNED.put(category, list)
                                    if (!FormatCommand.MESSAGES.contains("filter-$category")) {
                                        FormatCommand.MESSAGES.add("filter-$category")
                                    }
                                    Messages.success(sender, Messages.DIV + "\n[" + ChatZ.PREFIX + "] Added phrase${if (phrases.size > 1) "s" else ""} to $category category:\n{0}\n" + Messages.DIV, StringUtils.join(phrases, ", "))
                                    return true
                                }
                            }
                            "remove" -> {
                                if (args.size > 2) {
                                    var msg = StringUtils.join(args.copyOfRange(2, args.size), ' ').toLowerCase()
                                    var category = args[args.size - 1].toLowerCase()
                                    if (!category.startsWith("c:")) category = "general" else {
                                        msg = msg.substring(0..msg.lastIndexOf(' ') - 1)
                                        category = category.replaceFirst("c:", "")
                                    }
                                    if (category == "urls") {
                                        Messages.failure(sender, "That filter cannot be modified.")
                                        return true
                                    }
                                    val list = (BANNED[category] ?: ArrayList<String>())
                                    val phrases = msg.split(",")
                                    for (phrase in phrases) {
                                        if (!list.contains(phrase)) {
                                            Messages.failure(sender, "The filter doesn't contain {0}.", phrase)
                                            return true
                                        }
                                    }
                                    list.removeAll(phrases)
                                    if (list.isNotEmpty()) BANNED.put(category, list) else {
                                        BANNED.remove(category)
                                        FormatCommand.MESSAGES.remove("filter-$category")
                                    }
                                    Messages.success(sender, Messages.DIV + "\n[" + ChatZ.PREFIX + "] Removed phrase${if (phrases.size > 1) "s" else ""} from $category category:\n{0}\n" + Messages.DIV, StringUtils.join(phrases, ", "))
                                    return true
                                }
                            }
                            "list" -> {
                                var category = if (args.size >= 3) args[args.size - 1].toLowerCase() else "general"
                                if (category.startsWith("c:")) category = category.replaceFirst("c:", "")
                                val list = (BANNED[category] ?: ArrayList<String>())
                                if (category == "urls") {
                                    list.clear()
                                    list.add(Messages.DARK_GREEN + "This filter is used for anti-advertisement.")
                                }
                                sender.sendMessage(arrayOf(
                                        Messages.DIV,
                                        Messages.GREEN + "[" + ChatZ.PREFIX + "] Banned Words ($category):",
                                        if (list.isNotEmpty()) StringUtils.join(list, ", ") else Messages.RED + "Add phrases with " + Messages.GRAY + "/filter profanity add <phrase> c:$category",
                                        Messages.DIV
                                ))
                                return true
                            }
                        }
                    }
                    sender.sendMessage(arrayOf(
                            Messages.DIV,
                            Messages.GREEN + "[" + ChatZ.PREFIX + "] Profanity Filter Settings:",
                            "$cmd add <phrase> [c:category] " + Messages.GRAY + "Ban a phrase",
                            "$cmd remove <phrase> " + Messages.GRAY + "Unban a phrase",
                            "$cmd list [category] " + Messages.GRAY + "List banned words",
                            Messages.DIV
                    ))
                    return true
                }
                "urls", "url", "advertisement", "adv" -> {
                    if (args.size > 1) {
                        when (args[1].toLowerCase()) {
                            "whitelist" -> {
                                if (args.size > 2) {
                                    val words = StringUtils.join(args.copyOfRange(2, args.size), ' ').toLowerCase().replace(" , ", ",").replace(", ", ",").split(",")
                                    words.forEach {
                                        if (WHITELISTED_URLS.contains(it)) {
                                            Messages.failure(sender, "The whitelist already contains {0}.", it)
                                            return true
                                        }
                                    }
                                    WHITELISTED_URLS.addAll(words);
                                    Messages.success(sender, Messages.DIV + "\n[" + ChatZ.PREFIX + "] Added word${if (words.size > 1) "s" else ""} to whitelist:\n{0}\n" + Messages.DIV, StringUtils.join(words, ", "))
                                    return true
                                }
                                sender.sendMessage(arrayOf(
                                        Messages.DIV,
                                        Messages.GREEN + "[" + ChatZ.PREFIX + "] URL Filter Whitelist:",
                                        if (WHITELISTED_URLS.isEmpty()) Messages.RED + "Empty!" else StringUtils.join(WHITELISTED_URLS, ", "),
                                        "", Messages.GREEN + "Add words with " + Messages.DARK_GREEN + "/filter urls whitelist <word>",
                                        Messages.DIV
                                ))
                                return true
                            }
                            "unwhitelist" -> {
                                if (args.size > 2) {
                                    val words = StringUtils.join(args.copyOfRange(2, args.size), ' ').toLowerCase().replace(" , ", ",").replace(", ", ",").split(",")
                                    words.forEach {
                                        if (!WHITELISTED_URLS.contains(it)) {
                                            Messages.failure(sender, "The whitelist doesn't contain {0}.", it)
                                            return true
                                        }
                                    }
                                    WHITELISTED_URLS.removeAll(words);
                                    Messages.success(sender, Messages.DIV + "\n[" + ChatZ.PREFIX + "] Removed word${if (words.size > 1) "s" else ""} from whitelist:\n{0}\n" + Messages.DIV, StringUtils.join(words, ", "))
                                    return true
                                }
                            }
                        }
                    }
                    sender.sendMessage(arrayOf(
                            Messages.DIV,
                            Messages.GREEN + "[" + ChatZ.PREFIX + "] URL Filter Settings:",
                            "$cmd whitelist <word> " + Messages.GRAY + "Whitelist a phrase",
                            "$cmd unwhitelist <word> " + Messages.GRAY + "Unwhitelist a phrase",
                            "$cmd whitelist " + Messages.GRAY + "List whitelisted words",
                            Messages.DIV
                    ))
                    return true
                }
            }
        }

        val cmd = "/$label"

        sender.sendMessage(arrayOf(
                Messages.DIV,
                Messages.GREEN + "[" + ChatZ.PREFIX + "] Chat Filter Settings:",
                "$cmd profanity " + Messages.GRAY + "Modify the profanity filter",
                "$cmd urls " + Messages.GRAY + "Modify the anti-advertisement filter",
                Messages.DIV
        ))
        return true
    }

}
package com.trophonix.chatz

import com.trophonix.chatz.commands.*
import com.trophonix.chatz.util.Messages
import com.trophonix.trunk.api.TrunkAPI
import com.trophonix.trunk.api.chat.TrunkChat
import me.clip.placeholderapi.PlaceholderAPI
import net.milkbowl.vault.chat.Chat
import org.apache.commons.lang.StringUtils
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import org.mcstats.Metrics

/**
 * Created by Lucas on 4/24/17.
 */
class ChatZ : JavaPlugin(), Listener {

    companion object {
        @JvmField val PREFIX = Messages.DARK_GREEN + "Chat-Z" + Messages.GREEN
    }

    var pApiEnabled: Boolean = false

    var trunkChat: TrunkChat? = null
    var vaultChat: Chat? = null

    override fun onEnable() {
        pApiEnabled = server.pluginManager.isPluginEnabled("PlaceholderAPI")
        trunkChat = TrunkAPI.getAPI(TrunkChat::class.java)
        vaultChat = server.servicesManager.getRegistration(Chat::class.java)?.provider

        /*  Initialize Metrics  */
        logger.info("Enabling metrics...")
        val metrics = Metrics(this)
        metrics.start()

        /*  Commands  */
        logger.info("Registering commands...")
        getCommand("chatz").executor = ChatZCommand(this)
        getCommand("format").executor = FormatCommand(this)
        getCommand("messages").executor = MessagesCommand(this)
        getCommand("placeholders").executor = PlaceholdersCommand()
        val channelsCommand = ChannelsCommand(this)
        getCommand("channels").executor = channelsCommand
        getCommand("channel").executor = channelsCommand
        val colorsCommand = ColorsCommand(this)
        getCommand("colors").executor = colorsCommand
        getCommand("color").executor = colorsCommand
        getCommand("namecolor").executor = colorsCommand
        getCommand("chatcolor").executor = colorsCommand

        /*  Register Events  */
        logger.info("Registering events...")
        server.pluginManager.registerEvents(this, this)
    }

    override fun onDisable() {
        saveConfig()
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onChat(event: AsyncPlayerChatEvent) {
        logger.info("AsyncPlayerChatEvent!")
        var format = Messages.colorize(config.getString("format", "{player}&r: {message}"))
        format = format.replace("{message}", event.message)
        format = format(format, event.player)
        event.format = format
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onJoin(event: PlayerJoinEvent) {
        if (config.isSet("messages.join")) {
            var format = Messages.colorize(config.getString("messages.join"))
            format = format(format, event.player)
            event.joinMessage = format
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onQuit(event: PlayerQuitEvent) {
        if (config.isSet("messages.quit")) {
            var format = Messages.colorize(config.getString("messages.quit"))
            format = format(format, event.player)
            event.quitMessage = format
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onDeath(event: PlayerDeathEvent) {
        /*  Event is pvp death  */
        if (event.entity.killer != null && config.isSet("messages.death-pvp")) {
            var format = config.getString("messages.death-pvp")
            format = format(format, event.entity)
            format = format(format, event.entity, "victim")
            format = format(format, event.entity.killer, "killer")
            event.deathMessage = format
        }
        /*  Generic death  */
        else if (config.isSet("messages.death-generic")) {
            var format = config.getString("messages.death-generic")
            format = format(format, event.entity)
            format = format(format, event.entity, "victim")
            event.deathMessage = format
        }
    }
    private fun format(f : String, player : Player) : String {
        return format(f, player, "player")
    }

    private fun format(f : String, player : Player, playerTag : String) : String {
        var format = f

        /*  Insert basic placeholders  */
        format = format.replace("{$playerTag}", player.displayName)

        /*  Integrate PlaceholderAPI placeholders into format  */
        if (pApiEnabled) {
            format = PlaceholderAPI.setPlaceholders(player, format)
        }

        /*  Set up prefixes and suffixes  */
        val groupPrefix : String
        val groupSuffix : String
        val playerPrefix : String
        val playerSuffix : String
        val groupPrefixes = ArrayList<String>()
        val groupSuffixes = ArrayList<String>()

        if (trunkChat != null) {
            logger.info("Found Trunk!")
            val group = trunkChat!!.getPrimaryGroup(player)
            groupPrefix = Messages.colorize(trunkChat!!.getGroupPrefix(group))
            groupSuffix = Messages.colorize(trunkChat!!.getGroupSuffix(group))
            playerPrefix = Messages.colorize(trunkChat!!.getPrefix(player))
            playerSuffix = Messages.colorize(trunkChat!!.getSuffix(player))
            trunkChat!!.getGroups(player).mapTo(groupPrefixes) { trunkChat!!.getGroupPrefix(it) }
            trunkChat!!.getGroups(player).mapTo(groupSuffixes) { trunkChat!!.getGroupSuffix(it) }
        } else if (vaultChat != null) {
            logger.info("Found Vault!")
            val group = vaultChat!!.getPrimaryGroup(player)
            groupPrefix = Messages.colorize(vaultChat!!.getGroupPrefix(player.world, group))
            groupSuffix = Messages.colorize(vaultChat!!.getGroupSuffix(player.world, group))
            playerPrefix = Messages.colorize(vaultChat!!.getPlayerPrefix(player))
            playerSuffix = Messages.colorize(vaultChat!!.getPlayerSuffix(player))
            vaultChat!!.getPlayerGroups(player).mapTo(groupPrefixes) { vaultChat!!.getGroupPrefix(player.world, it) }
            vaultChat!!.getPlayerGroups(player).mapTo(groupSuffixes) { vaultChat!!.getGroupSuffix(player.world, it) }
        } else {
            groupPrefix = ""
            groupSuffix = ""
            playerPrefix = ""
            playerSuffix = ""
        }

        /*  Set a group prefix/suffix into format  */
        format = format.replace("{group:prefix}", groupPrefix)
        format = format.replace("{group:suffix}", groupSuffix)

        /*  Set player prefix/suffix into format  */
        format = format.replace("{$playerTag:prefix}", playerPrefix)
        format = format.replace("{$playerTag:suffix}", playerSuffix)

        /*  Set primary prefix into format  */
        if (playerPrefix.isNotEmpty()) {
            format = format.replace("{prefix}", playerPrefix)
        } else if (groupPrefix.isNotEmpty()) {
            format = format.replace("{prefix}", groupPrefix)
        } else {
            format = format.replace("{prefix}", "")
        }

        /*  Set primary suffix into format  */
        if (playerSuffix.isNotEmpty()) {
            format = format.replace("{suffix}", playerSuffix)
        } else if (groupSuffix.isNotEmpty()) {
            format = format.replace("{suffix}", groupSuffix)
        } else {
            format = format.replace("{suffix}", "")
        }

        /*  Replace all prefixes and suffixes into format  */
        format = format.replace("{group:prefixes}", StringUtils.join(groupPrefixes, ' '))
        format = format.replace("group:suffixes}", StringUtils.join(groupSuffixes, ' '))

        return format
    }

}
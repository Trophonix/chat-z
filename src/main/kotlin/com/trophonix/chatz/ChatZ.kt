package com.trophonix.chatz

import com.trophonix.chatz.commands.*
import com.trophonix.chatz.util.ItemStacks
import com.trophonix.chatz.util.Messages
import com.trophonix.trunk.api.chat.TrunkChat
import me.clip.placeholderapi.PlaceholderAPI
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import net.milkbowl.vault.chat.Chat
import net.milkbowl.vault.item.Items
import org.apache.commons.lang.StringUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import org.mcstats.Metrics
import java.io.File
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors

/**
* Created by Lucas on 4/24/17.
*/
class ChatZ : JavaPlugin(), Listener {

    companion object {
        @JvmField val PREFIX = Messages.DARK_GREEN + "Chat-Z" + Messages.GREEN
    }

    val dateFormat = DecimalFormat("0.##")

    var pApiEnabled: Boolean = false

    var trunkChat: TrunkChat? = null
    var vaultChat: Chat? = null

    val playerChannels = ConcurrentHashMap<UUID, String>()
    val playerDelays = ConcurrentHashMap<UUID, Long>()

    var channelsFile: File? = null
    var channels: FileConfiguration? = null

    override fun onEnable() {
        saveDefaultConfig()

        logger.info("Loading channels yml...")
        channelsFile = File(dataFolder, "channels.yml")
        if (channelsFile!!.exists()) {
            logger.info("No channels.yml found! Copying default...")
            try {
                saveResource("channels.yml", false)
            } catch (ex : Exception) {

            }
        }
        channels = YamlConfiguration.loadConfiguration(channelsFile)
        if (channels != null) {
            logger.info("Loaded channels.yml successfully.")
        } else {
            logger.warning("Failed to load channels.yml. Shutting down.")
            server.pluginManager.disablePlugin(this)
            return
        }

        logger.info("Looking for plugins to hook into...")
        pApiEnabled = server.pluginManager.isPluginEnabled("PlaceholderAPI")
        if (pApiEnabled) logger.info("Hooked into PlaceholderAPI")
        trunkChat = server.servicesManager.getRegistration(TrunkChat::class.java)?.provider
        if (trunkChat != null) {
            logger.info("Hooked into Trunk")
        } else {
            vaultChat = server.servicesManager.getRegistration(Chat::class.java)?.provider
            if (vaultChat != null) logger.info("Hooked into Vault")
        }
        if (!pApiEnabled && trunkChat == null && vaultChat == null) {
            logger.info("Found no plugins to hook into.")
        }

        /*  Initialize Metrics  */
        logger.info("Enabling metrics...")
        val metrics = Metrics(this)
        metrics.start()

        /*  Commands  */
        logger.info("Registering commands...")
        getCommand("chatz").executor = ChatZCommand(this)
        getCommand("format").executor = FormatCommand(this)
        getCommand("placeholders").executor = PlaceholdersCommand()
        getCommand("filter").executor = FilterCommand(this)
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

        /*  Load Config  */
        logger.info("Loading configuration options...")
        if (config.isSet("filter.words")) {
            for (key in config.getConfigurationSection("filter.words").getKeys(false)) {
                val list = config.getStringList("filter.words." + key) ?: ArrayList<String>()
                FilterCommand.BANNED.put(key, list)
                FormatCommand.MESSAGES.add("filter-" + key)
            }
        }

        logger.info("Loaded!")
    }

    override fun onDisable() {
        saveConfig()
        channels!!.save(channelsFile)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onChat(event: AsyncPlayerChatEvent) {
        /*  Filter  */
        var message = event.message.toLowerCase()
        FilterCommand.COMMON_BYPASSES.forEach { t, u ->
            message = message.replace(t, u)
        }
        message = message.replace("[^a-zA-Z0-9\\s]", "")
        message = " $message "

        var flagged = false
        var filterFlagged = "general"
        var highlighted = message
        FilterCommand.BANNED.forEach { t, u ->
            u.forEach {
                if (message.contains(" $it ")) {
                    flagged = true
                    highlighted = highlighted.replace(it, Messages.DARK_GRAY + it + Messages.GRAY)
                }
            }
            if (flagged) {
                filterFlagged = t
                return@forEach
            }
        }

        if (flagged) {
            var msg = config.getString("messages.filter-$filterFlagged") ?: config.getString("messages.filter-general", "Profanity was detected in your message!")
            msg = msg.replace("{message}", highlighted.replaceFirst(" ", ""))
            Messages.failure(event.player, Messages.colorize(msg))
            event.isCancelled = true
            return
        }

        /*  Chat Format  */
        if (config.isSet("messages.chat")) {
            var format = Messages.colorize(config.getString("messages.chat"))
            format = format.replace("{message}", event.message)
            format = format(format, event.player)
            event.format = format
        }

        /* ----- Channel Stuff ----- */

        val channel = playerChannels[event.player.uniqueId]

        /*  Channel Delay  */
        val delay = channels!!.getDouble("channels.$channel.DELAY", -1.0)

        if (delay >= 0.0) {
            if (!playerDelays.contains(event.player.uniqueId)) {
                playerDelays[event.player.uniqueId] = System.currentTimeMillis()
            } else {
                val last = playerDelays[event.player.uniqueId]
                val timeUntil = last!! + (delay * 1000.0)
                if (System.currentTimeMillis() < timeUntil) {
                    Messages.failure(event.player, "You can speak again in {0} in {1} second${if (timeUntil == 0.0) "s" else ""}!", channel.toString(), dateFormat.format(timeUntil / 1000.0))
                } else {
                    playerDelays[event.player.uniqueId] = System.currentTimeMillis()
                }
            }
        }

        /*  Channel Range and Global Values  */
        val recipients = getPlayersInMessage(event.player, channel!!)
        if (recipients.isNotEmpty()) {
            event.recipients.clear()
            event.recipients.addAll(recipients)
        }

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
        val cause = event.entity.lastDamageCause
        /*  PvP death  */
        if (event.entity.killer != null && config.isSet("messages.death-pvp")) {
            var format = config.getString("messages.death-pvp")
            format = format(format, event.entity)
            format = formatEvent(format, event.entity, "victim")
            format = formatEvent(format, event.entity.killer, "killer")

            val splitByItem = format.split("{item}")

            val item = event.entity.killer.inventory.itemInMainHand
            val info = Items.itemByType(item.type)

            val itemName = if (item?.type != Material.AIR) item?.itemMeta?.displayName?: info?.name?: Messages.cap(item.type.name.replace("_", "-")).replace("-", " ") else "fists"

            val finalMessage = TextComponent(
                    TextComponent(splitByItem[0]),
                    TextComponent(
                            *ComponentBuilder(itemName).color(ChatColor.AQUA)
                                    .event(HoverEvent(HoverEvent.Action.SHOW_ITEM, ItemStacks.itemDescriptor(item)))
                                    .create()
                    ),
                    TextComponent(splitByItem[1])
            )

            Bukkit.getOnlinePlayers().forEach { it.spigot().sendMessage(finalMessage) }
            event.deathMessage = null
        }
        /*  Mob death  */
        else if (cause is EntityDamageByEntityEvent) {
            var format = config.getString("messages.death-mob")
            format = format(format, event.entity)
            format = format(format, event.entity, "victim")
            val mobName = cause.damager.customName ?: cause.damager.name
            format = format.replace("{killer}", mobName)
            format = format.replace("{mob}", mobName)
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

    private fun formatEvent(f : String, player : Player, playerTag : String) : String {
        var format = f

        /*  Insert basic placeholders  */
        format = format.replace("{$playerTag}", player.displayName)

        /*  Integrate PlaceholderAPI placeholders into format  */
        if (pApiEnabled) {
            format = PlaceholderAPI.setPlaceholders(player, format)
        }

        /* ----- Vault/Trunk Hook Stuff  ----- */

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
        format = format.replace("{$playerTag:group:prefix}", groupPrefix)
        format = format.replace("{$playerTag:group:suffix}", groupSuffix)

        /*  Set primary prefix into format  */
        if (playerPrefix.isNotEmpty()) {
            format = format.replace("{$playerTag:prefix}", playerPrefix)
        } else if (groupPrefix.isNotEmpty()) {
            format = format.replace("{$playerTag:prefix}", groupPrefix)
        } else {
            format = format.replace("{$playerTag:prefix}", "")
        }

        /*  Set primary suffix into format  */
        if (playerSuffix.isNotEmpty()) {
            format = format.replace("{$playerTag:suffix}", playerSuffix)
        } else if (groupSuffix.isNotEmpty()) {
            format = format.replace("{$playerTag:suffix}", groupSuffix)
        } else {
            format = format.replace("{$playerTag:suffix}", "")
        }

        /*  Replace all prefixes and suffixes into format  */
        format = format.replace("{$playerTag:group:prefixes}", StringUtils.join(groupPrefixes, ' '))
        format = format.replace("$playerTag:group:suffixes}", StringUtils.join(groupSuffixes, ' '))

        /* ----- Channel Stuff -----  */
        val channel = playerChannels.get(player.uniqueId)

        /*  Channel Prefix  */
        val channelPrefix = Messages.colorize(channels!!.getString("channels.$channel.PREFIX"));
        format = format.replace("{channel:prefix}", channelPrefix);
        format = format.replace("{$playerTag:channel:prefix}", channelPrefix)

        val channelNameColor = Messages.getColor(channels!!.getString("channels.$channel.NAME_COLOR"))
        if (channelNameColor != null) {
            format = format.replace("{channel:namecolor}", channelNameColor.toString())
            format = format.replace("{channel:name_color}", channelNameColor.toString())
            format = format.replace("{$playerTag:channel:namecolor}", channelNameColor.toString())
            format = format.replace("{$playerTag:channel:name_color}", channelNameColor.toString())
        }

        val channelChatColor = Messages.getColor(channels!!.getString("channels.$channel.CHAT_COLOR"))
        if (channelChatColor != null) {
            format = format.replace("{channel:chatcolor}", channelChatColor.toString())
            format = format.replace("{channel:chat_color}", channelChatColor.toString())
            format = format.replace("{$playerTag:channel:chatcolor}", channelChatColor.toString())
            format = format.replace("{$playerTag:channel:chat_color}", channelChatColor.toString())
        }

        return format
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

        /* ----- Channel Stuff -----  */
        val channel = playerChannels.get(player.uniqueId)

        /*  Channel Prefix  */
        val channelPrefix = Messages.colorize(channels!!.getString("channels.$channel.PREFIX"));
        format = format.replace("{channel:prefix}", channelPrefix);
        format = format.replace("{$playerTag:channel:prefix}", channelPrefix)

        val channelNameColor = Messages.getColor(channels!!.getString("channels.$channel.NAME_COLOR"))
        if (channelNameColor != null) {
            format = format.replace("{channel:namecolor}", channelNameColor.toString())
            format = format.replace("{channel:name_color}", channelNameColor.toString())
            format = format.replace("{$playerTag:channel:namecolor}", channelNameColor.toString())
            format = format.replace("{$playerTag:channel:name_color}", channelNameColor.toString())
        }

        val channelChatColor = Messages.getColor(channels!!.getString("channels.$channel.CHAT_COLOR"))
        if (channelChatColor != null) {
            format = format.replace("{channel:chatcolor}", channelChatColor.toString())
            format = format.replace("{channel:chat_color}", channelChatColor.toString())
            format = format.replace("{$playerTag:channel:chatcolor}", channelChatColor.toString())
            format = format.replace("{$playerTag:channel:chat_color}", channelChatColor.toString())
        }

        return format
    }

    @Suppress("UNUSED_EXPRESSION")
    fun getPlayersInMessage(sender : Player, channel : String) : MutableSet<Player> {
        return server.onlinePlayers.stream().filter({
            if (it.uniqueId == sender.uniqueId) true
            val range = channels!!.getDouble("channels.$channel.RANGE", -1.0)
            if (range >= 0.0 && sender.location.distance(it.location) > range) false
            val itChannel = playerChannels[it.uniqueId]
            channel == itChannel || channels!!.getBoolean("channels.$channel.GLOBAL")
        }).collect(Collectors.toSet())
    }

}
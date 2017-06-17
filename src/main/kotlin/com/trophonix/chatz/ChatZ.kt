package com.trophonix.chatz

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.stream.JsonWriter
import com.trophonix.chatz.commands.*
import com.trophonix.chatz.data.DataMap
import com.trophonix.chatz.data.PlayerData
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
import org.apache.commons.validator.routines.InetAddressValidator
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
import java.io.FileReader
import java.io.FileWriter
import java.text.DecimalFormat
import java.util.regex.Pattern
import java.util.stream.Collectors
import kotlin.collections.ArrayList

/**
* Created by Lucas on 4/24/17.
*/
class ChatZ : JavaPlugin(), Listener {

    companion object {
        @JvmField val PREFIX = Messages.DARK_GREEN + "Chat-Z" + Messages.GREEN
        @JvmField var INSTANCE : ChatZ? = null
        @JvmField val DOMAIN_PATTERN = Pattern.compile("^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}$")
    }

    val dateFormat = DecimalFormat("0.##")

    var pApiEnabled: Boolean = false

    var trunkChat: TrunkChat? = null
    var vaultChat: Chat? = null

    val playerData = DataMap()

    var channelsFile: File? = null
    var channels: FileConfiguration? = null

    override fun onEnable() {
        INSTANCE = this

        saveDefaultConfig()

        logger.info("Looking for plugins to hook into")
        pApiEnabled = server.pluginManager.isPluginEnabled("PlaceholderAPI")
        if (pApiEnabled) logger.info("Hooked into PlaceholderAPI")
        trunkChat = server.servicesManager.getRegistration(TrunkChat::class.java)?.provider
        if (trunkChat != null) logger.info("Hooked into Trunk")
        vaultChat = server.servicesManager.getRegistration(Chat::class.java)?.provider
        if (vaultChat != null) logger.info("Hooked into Vault")
        if (!pApiEnabled && trunkChat == null && vaultChat == null) {
            logger.info("Found no plugins to hook into.")
        }

        /*  Initialize Metrics  */
        logger.info("Enabling metrics")
        val metrics = Metrics(this)
        metrics.start()

        /*  Commands  */
        logger.info("Registering commands")
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
        logger.info("Registering events")
        server.pluginManager.registerEvents(this, this)

        logger.info("Loading channels.yml")
        channelsFile = File(dataFolder, "channels.yml")
        if (!channelsFile!!.exists()) {
            logger.info("No channels.yml found! Copying default")
            try {
                saveResource("channels.yml", false)
            } catch (ex : Exception) {
                logger.warning("Failed to copy default channels.yml:")
                ex.printStackTrace()
                logger.warning("Shutting down.")
                server.pluginManager.disablePlugin(this)
                return
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

        logger.info("Loading data")
        try {
            if (File(dataFolder, "data.json").exists()) {
                val parser = JsonParser()
                val dataFile = File(dataFolder, "data.json")
                val d = parser.parse(FileReader(dataFile))
                d?.asJsonArray?.forEach {
                    val o = it.asJsonObject
                    val data = PlayerData.fromJson(o)
                    playerData[data.uniqueId] = data
                }
            }
        } catch (ex : Exception) {
            ex.printStackTrace()
            logger.warning("Failed to load data! Shutting down.")
            server.pluginManager.disablePlugin(this)
            return
        }

        /*  Load Config  */
        logger.info("Loading configuration options")
        if (config.isSet("filter.words")) {
            for (key in config.getConfigurationSection("filter.words").getKeys(false)) {
                val list = config.getStringList("filter.categories." + key) ?: ArrayList<String>()
                FilterCommand.BANNED.put(key, list)
                FormatCommand.MESSAGES.add("filter-" + key)
            }
        }
        //FilterCommand.BANNED.put("urls", ArrayList<String>())
        //FormatCommand.MESSAGES.add("filter-urls")

        logger.info("Loaded!")
    }

    override fun onDisable() {
        saveData()
        channels!!.save(channelsFile)
        FilterCommand.BANNED.forEach { t, u ->
            if (t != "urls") config["filter.categories." + t] = u
        }
        saveConfig()
    }

    fun saveData() {
        val array = JsonArray()
        playerData.forEach {
            array.add(it.value.toJson())
        }
        val writer = JsonWriter(FileWriter(File(dataFolder, "data.json")))
        val gson = GsonBuilder().create()
        gson.toJson(array, writer)
        writer.close()
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
        val phrasesFlagged = ArrayList<String>()
        var highlighted = message
        FilterCommand.BANNED.forEach { t, u ->
            if (t == "urls") {
                val m = event.message
                (5..m.length).forEach { i ->
                    (0..(m.length - i))
                            .map { m.subSequence(it, it + i).toString() }
                            .forEach {
                                Bukkit.broadcastMessage("Testing > $it")
                                if (DOMAIN_PATTERN.matcher(it).find() || InetAddressValidator.getInstance().isValid(it.replace(",", "."))) {
                                    Bukkit.broadcastMessage("Found > $it")
                                    flagged = true
                                    filterFlagged = t
                                    phrasesFlagged.add(it)
                                } else {
                                    Bukkit.broadcastMessage("Not FOund > $it")
                                }
                            }
                }
            } else {
                u.forEach {
                    if (message.contains(" $it ")) {
                        flagged = true
                        highlighted = highlighted.replace(it, Messages.DARK_GRAY + it + Messages.GRAY)
                        phrasesFlagged.add(it)
                    }
                }
            }
            if (flagged && filterFlagged == "") {
                filterFlagged = t
                return@forEach
            }
        }

        if (flagged) {
            var msg = config.getString("messages.filter-$filterFlagged") ?: config.getString("messages.filter-general", "Profanity was detected in your message!")
            msg = msg.replace("{message}", highlighted.replaceFirst(" ", ""))
            val join = StringUtils.join(phrasesFlagged, ", ")
            msg = msg.replace("{phrase}", join).replace("{words}", join).replace("{phrase}", join).replace("{word}", join)
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
        val data = playerData[event.player.uniqueId]

        val channel = data?.channel

        if (channel == null) {
            Messages.failure(event.player, "You aren't in a channel!\nType {0} to join one.", "/channel <name>")
            event.isCancelled = true
            return
        } else {
            /*  Channel Delay  */
            val delay = channels!!.getDouble("channels.$channel.DELAY", -1.0)

            if (delay >= 0.0) {
                if (!data.lastChat.containsKey(channel)) {
                    data.lastChat[channel] = System.currentTimeMillis()
                } else {
                    val last = data.lastChat[channel]
                    val timeUntil = (last!! + (delay * 1000.0)) - System.currentTimeMillis()
                    if (timeUntil > 0.0) {
                        Messages.failure(event.player, "You can speak again in {0} in {1} second${if (timeUntil == 1.0) "" else "s"}!", channel, dateFormat.format(timeUntil / 1000.0))
                        event.isCancelled = true
                        return
                    } else {
                        data.lastChat[channel] = System.currentTimeMillis()
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
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onJoin(event: PlayerJoinEvent) {
        if (!playerData.containsKey(event.player.uniqueId))
            playerData[event.player.uniqueId] = PlayerData(event.player.uniqueId)
        val data = PlayerData(event.player.uniqueId)
        if (data.channel == null && channels!!.isSet("default")) {
            data.channel = channels!!.getString("default")
        }
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

        /* ----- Player Data Stuff ----- */

        val data = playerData[player.uniqueId]

        /* ----- Channel Stuff -----  */
        val channel = data!!.channel ?: ""

        format = format.replace("{channel}", channel)
        format = format.replace("{channel:name}", channel)
        format = format.replace("{$playerTag:channel}", channel)

        /*  Channel Prefix  */
        val channelPrefix= Messages.colorize(channels!!.getString("channels.$channel.PREFIX")) ?: ""
        format = format.replace("{channel:prefix}", channelPrefix)
        format = format.replace("{$playerTag:channel:prefix}", channelPrefix)

        /*  Channel Name Color  */
        val channelNameColor = Messages.getColor(channels!!.getString("channels.$channel.NAME_COLOR"))?.toString() ?: ""
        format = format.replace("{channel:namecolor}", channelNameColor)
        format = format.replace("{channel:name_color}", channelNameColor)
        format = format.replace("{$playerTag:channel:namecolor}", channelNameColor)
        format = format.replace("{$playerTag:channel:name_color}", channelNameColor)

        /* ----- Player Settings ----- */

        /*  Player Name Color  */
        val playerNameColor = data.nameColor
        if (playerNameColor != null) {
            format = format.replace("{$playerTag:namecolor}", playerNameColor.toString())
            format = format.replace("{$playerTag:name_color}", playerNameColor.toString())
        }

        if (playerNameColor != null) format = format.replace("{player:namecolor}", playerNameColor.toString())

        val nameColor = playerNameColor ?: channelNameColor
        format = format.replace("{namecolor}", nameColor.toString())

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

        /* ----- Player Data Stuff ----- */

        val data = playerData[player.uniqueId]

        /* ----- Channel Stuff -----  */
        val channel = data!!.channel ?: ""

        format = format.replace("{channel}", channel)
        format = format.replace("{channel:name}", channel)
        format = format.replace("{$playerTag:channel}", channel)

        /*  Channel Prefix  */
        val channelPrefix= Messages.colorize(channels!!.getString("channels.$channel.PREFIX", ""))
        format = format.replace("{channel:prefix}", channelPrefix)
        format = format.replace("{$playerTag:channel:prefix}", channelPrefix)

        /*  Channel Name Color  */
        val channelNameColor = Messages.getColor(channels!!.getString("channels.$channel.NAME_COLOR", ""))?.toString() ?: ""
        format = format.replace("{channel:namecolor}", channelNameColor)
        format = format.replace("{channel:name_color}", channelNameColor)
        format = format.replace("{$playerTag:channel:namecolor}", channelNameColor)
        format = format.replace("{$playerTag:channel:name_color}", channelNameColor)

        /*  Channel Chat Color  */
        val channelChatColor = Messages.getColor(channels!!.getString("channels.$channel.CHAT_COLOR", ""))?.toString() ?: ""
        format = format.replace("{channel:chatcolor}", channelChatColor)
        format = format.replace("{channel:chat_color}", channelChatColor)
        format = format.replace("{$playerTag:channel:chatcolor}", channelChatColor)
        format = format.replace("{$playerTag:channel:chat_color}", channelChatColor)

        /* ----- Player Settings ----- */

        /*  Player Name Color  */
        val playerNameColor = data.nameColor
        if (playerNameColor != null) {
            format = format.replace("{$playerTag:namecolor}", playerNameColor.toString())
            format = format.replace("{$playerTag:name_color}", playerNameColor.toString())
        }

        /*  Player Chat Color  */
        val playerChatColor = data.chatColor
        if (playerChatColor != null) format = format.replace("{$playerTag:chatcolor}", playerChatColor.toString())

        /*  Final Name Color  */
        val nameColor = playerNameColor ?: channelNameColor
        format = format.replace("{namecolor}", nameColor.toString())
        format = format.replace("{name_color}", nameColor.toString())

        /*  Final Chat Color  */
        val chatColor = playerChatColor ?: channelChatColor
        format = format.replace("{chatcolor}", chatColor.toString())
        format = format.replace("{chat_color}", chatColor.toString())

        return format
    }

    @Suppress("UNUSED_EXPRESSION")
    fun getPlayersInMessage(sender : Player, channel : String) : MutableSet<Player> {
        return server.onlinePlayers.stream().filter({
            if (it.uniqueId == sender.uniqueId) true
            val range = channels!!.getDouble("channels.$channel.RANGE", -1.0)
            if (range >= 0.0 && sender.location.distance(it.location) > range) false
            val itChannel = playerData[it.uniqueId]!!.channel
            channel == itChannel || channels!!.getBoolean("channels.$channel.GLOBAL")
        }).collect(Collectors.toSet())
    }

}
package com.trophonix.chatz.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.trophonix.chatz.ChatZ
import com.trophonix.chatz.util.Messages
import org.bukkit.ChatColor
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by Lucas on 4/30/17.
 */
class PlayerData(val uniqueId : UUID) {

    companion object {
        fun fromJson(o : JsonObject) : PlayerData {
            val uniqueId = UUID.fromString(o["player"].asString)
            val data = PlayerData(uniqueId)
            val channel = o["channel"]?.asString ?: "null"
            if (channel != "null" && ChatZ.INSTANCE?.channels!!.contains("channels." + channel)) {
                data.channel = channel
            }
            val nameColor = o["name_color"]?.asString ?: "null"
            if (nameColor != "null") data.nameColor = Messages.getColor(nameColor)
            val chatColor = o["chat_color"]?.asString ?: "null"
            if (chatColor != "null") data.chatColor = Messages.getColor(chatColor)
            o["last_chat"]?.asJsonArray?.forEach {
                val o1 = it.asJsonObject
                data.lastChat[o1["channel"].asString] = o1["time"].asLong
            }
            return data
        }
    }

    /*  Channel Stuff  */
    var channel : String? = null
    val lastChat = ConcurrentHashMap<String, Long>()

    /*  Player Data  */
    var nameColor : ChatColor? = null
    var chatColor : ChatColor? = null

    fun toJson() : JsonObject {
        val o = JsonObject()
        o.addProperty("player", uniqueId.toString())
        o.addProperty("channel", channel ?: "null")
        o.addProperty("name_color", nameColor?.name ?: "null")
        o.addProperty("chat_color", chatColor?.name ?: "null")
        val lastChatArray = JsonArray()
        lastChat.forEach {
            val o1 = JsonObject()
            o1.addProperty("channel", it.key)
            o1.addProperty("time", it.value)
            lastChatArray.add(o1)
        }
        o.add("last_chat", lastChatArray)
        return o
    }

}
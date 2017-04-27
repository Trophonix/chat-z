package com.trophonix.chatz.util

import org.bukkit.Bukkit
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Created by Lucas on 4/26/17.
 */
class NMSUtil {

    companion object {
        @JvmField val packageName = Bukkit.getServer()::class.java.`package`.name
        @JvmField val version = packageName.substring(packageName.lastIndexOf('.') + 1) + "."

        @JvmField val loadedNMS = HashMap<String, Class<*>>()
        @JvmField val loadedCB = HashMap<String, Class<*>>()
        @JvmStatic fun getNMSClass(className : String) : Class<*>? {
            if (loadedNMS.containsKey(className)) {
                return loadedNMS[className]
            }

            val clazzName = "net.minecraft.server.$version$className"
            val clazz : Class<*>?

            try {
                clazz = Class.forName(clazzName)
            } catch (ex : Exception) {
                ex.printStackTrace()
                return null
            }

            loadedNMS.put(className, clazz)
            return clazz
        }
        @JvmStatic fun getCBClass(className : String) : Class<*>? {
            if (loadedCB.containsKey(className)) {
                return loadedCB[className]
            }

            val clazzName = "org.bukkit.craftbukkit.$version$className"
            val clazz : Class<*>?

            try {
                clazz = Class.forName(clazzName)
            } catch (ex : Exception) {
                ex.printStackTrace()
                return null
            }

            loadedCB.put(className, clazz)
            return clazz
        }
        @JvmStatic fun getMethod(clazz : Class<*>, method : String, vararg args : Class<*>) : Method {
            return clazz.getMethod(method, *args)
        }
        @JvmStatic fun getField(clazz: Class<*>, field : String) : Field {
            return clazz.getDeclaredField(field)
        }
    }
}
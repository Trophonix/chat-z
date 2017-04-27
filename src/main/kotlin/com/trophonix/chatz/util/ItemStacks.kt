package com.trophonix.chatz.util

import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Material
import org.bukkit.inventory.ItemStack


/**
 * Created by Lucas on 4/26/17.
 */
class ItemStacks {
    companion object {
        @JvmStatic fun itemDescriptor(item : ItemStack?) : Array<BaseComponent> {
            if (item == null || item.type == Material.AIR) return arrayOf(TextComponent(Messages.AQUA + "fists"))
            val asNMSCopy = NMSUtil.getMethod(NMSUtil.getCBClass("inventory.CraftItemStack")!!, "asNMSCopy", ItemStack::class.java)
            val nmsItem = asNMSCopy.invoke(null, item)
            val itemStackClass = NMSUtil.getNMSClass("ItemStack")
            val nbtTagCompoundClazz = NMSUtil.getNMSClass("NBTTagCompound")
            val save = NMSUtil.getMethod(itemStackClass!!, "save", nbtTagCompoundClazz!!)
            val nbtTagCompound = save.invoke(nmsItem, nbtTagCompoundClazz.newInstance())
            return arrayOf(TextComponent(nbtTagCompound.toString()))

        }
    }
}
package com.trophonix.chatz.data;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Created by Lucas on 5/5/17.
 */
public class DataMap implements Map<UUID, PlayerData> {

    private Set<PlayerData> data = new HashSet<PlayerData>();

    public int size() {
        return data.size();
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public boolean containsKey(Object key) {
        for (PlayerData d : data) {
            if (d.getUniqueId().equals(key)) return true;
        }
        return false;
    }

    public boolean containsValue(Object value) {
        for (PlayerData d : data) {
            if (d.equals(value)) return true;
        }
        return false;
    }

    public PlayerData get(Object key) {
        for (PlayerData d : data) {
            if (d.getUniqueId().equals(key)) return d;
        }
        return null;
    }

    public PlayerData put(UUID key, PlayerData value) {
        PlayerData oldData = null;
        for (PlayerData d : data) {
            if (d.getUniqueId().equals(key)) {
                oldData = d;
                break;
            }
        }
        data.add(value);
        if (oldData != null) data.remove(oldData);
        return oldData;
    }

    public PlayerData remove(Object key) {
        PlayerData result = null;
        for (PlayerData d : data) {
            if (d.getUniqueId().equals(key)) {
                result = d;
                break;
            }
        }
        if (result != null) data.remove(result);
        return result;
    }

    public void putAll(@NotNull Map<? extends UUID, ? extends PlayerData> m) {
        for (Entry<? extends UUID, ? extends PlayerData> e : m.entrySet()) {
            data.add(e.getValue());
        }
    }

    public void clear() {
        data.clear();
    }

    @NotNull
    public Set<UUID> keySet() {
        Set<UUID> keys = new HashSet<UUID>();
        for (PlayerData d : data) {
            keys.add(d.getUniqueId());
        }
        return keys;
    }

    @NotNull
    public Collection<PlayerData> values() {
        return data;
    }

    @NotNull
    public Set<Entry<UUID, PlayerData>> entrySet() {
        Set<Entry<UUID, PlayerData>> entrySet = new HashSet<Entry<UUID, PlayerData>>();
        for (PlayerData da : data) {
            final PlayerData d = da;
            entrySet.add(new Entry<UUID, PlayerData>() {
                public UUID getKey() {
                    return d.getUniqueId();
                }
                public PlayerData getValue() {
                    return d;
                }
                public PlayerData setValue(PlayerData value) {
                    PlayerData old = d;
                    data.remove(old);
                    data.add(value);
                    return old;
                }
            });
        }
        return entrySet;
    }
}

package com.denizenscript.depenizen.bukkit.clientizen.events;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.depenizen.bukkit.clientizen.DataDeserializer;
import com.denizenscript.depenizen.bukkit.clientizen.DataSerializer;
import org.bukkit.entity.Player;

public abstract class ClientizenScriptEvent extends ScriptEvent {
    private boolean enabled;
    public Player player;

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void init() {
        enabled = true;
    }

    @Override
    public void destroy() {
        enabled = false;
    }

    public void fireInternal(Player player, DataDeserializer data) {
        this.player = player;
        fire(data);
    }

    public void fire(DataDeserializer data) {}

    public void write(DataSerializer serializer) {}
}

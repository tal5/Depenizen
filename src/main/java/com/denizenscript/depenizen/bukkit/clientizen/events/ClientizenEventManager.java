package com.denizenscript.depenizen.bukkit.clientizen.events;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.depenizen.bukkit.clientizen.Channels;
import com.denizenscript.depenizen.bukkit.clientizen.DataSerializer;
import com.denizenscript.depenizen.bukkit.clientizen.NetworkManager;

import java.util.HashMap;
import java.util.Map;

public class ClientizenEventManager {

    private static final Map<String, ClientizenScriptEvent> clientizenEvents = new HashMap<>();
    public static DataSerializer eventsSerializer;

    public static void init() {
        NetworkManager.registerInChannel(Channels.RECEIVE_EVENT, ((player, message) -> {
            clientizenEvents.get(message.readString()).fireInternal(player, message);
        }));
    }

    public static void registerEvent(Class<? extends ClientizenScriptEvent> event) {
        try {
            ClientizenScriptEvent instance = event.getConstructor().newInstance();
            ScriptEvent.registerScriptEvent(instance);
            clientizenEvents.put(instance.getName(), instance);
        }
        catch (Exception ex) {
            Debug.echoError("Something went wrong while registering clientizen event '" + event.getName() + "':");
            Debug.echoError(ex);
        }
    }

    public static void reload() {
        reloadEvents();
        NetworkManager.broadcast(Channels.EVENT_DATA, eventsSerializer);
    }

    private static void reloadEvents() {
        int size = 0;
        DataSerializer eventData = new DataSerializer();
        for (Map.Entry<String, ClientizenScriptEvent> entry : clientizenEvents.entrySet()) {
            ClientizenScriptEvent event = entry.getValue();
            if (!event.isEnabled()) {
                return;
            }
            eventData.writeString(entry.getKey());
            event.write(eventData);
            size++;
        }
        eventsSerializer = new DataSerializer().writeInt(size).writeBytes(eventData.toByteArray());
    }
}

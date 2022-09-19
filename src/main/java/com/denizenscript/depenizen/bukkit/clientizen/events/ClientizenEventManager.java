package com.denizenscript.depenizen.bukkit.clientizen.events;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.depenizen.bukkit.clientizen.Channels;
import com.denizenscript.depenizen.bukkit.clientizen.DataSerializer;
import com.denizenscript.depenizen.bukkit.clientizen.NetworkManager;

import java.util.HashSet;
import java.util.Set;

public class ClientizenEventManager {

    private static final Set<ClientizenScriptEvent> clientizenEvents = new HashSet<>();
    public static DataSerializer eventsSerializer;

    public static void init() {
        NetworkManager.registerInChannel(Channels.RECEIVE_EVENT, ((player, message) -> {
            ((ClientizenScriptEvent) ScriptEvent.eventLookup.get(message.readString())).fireInternal(player, message);
        }));
    }

    public static void registerEvent(Class<? extends ClientizenScriptEvent> event) {
        try {
            ClientizenScriptEvent instance = event.getConstructor().newInstance();
            ScriptEvent.registerScriptEvent(instance);
            clientizenEvents.add(instance);
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
        for (ClientizenScriptEvent event : clientizenEvents) {
            if (!event.isEnabled()) {
                return;
            }
            eventData.writeString(event.getName());
            event.write(eventData);
            size++;
        }
        eventsSerializer = new DataSerializer().writeInt(size).writeBytes(eventData.toByteArray());
    }
}

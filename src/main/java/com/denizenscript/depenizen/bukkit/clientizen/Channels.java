package com.denizenscript.depenizen.bukkit.clientizen;

public class Channels {
    public static final String CHANNEL_NAMESPACE = "clientizen";
    public static final String SET_SCRIPTS = id("set_scripts");
    public static final String RECEIVE_CONFIRM = id("receive_confirmation");
    public static final String EVENT_DATA = id("event_data");
    public static final String RECEIVE_EVENT = id("fire_event");

    public static String id(String key) {
        return CHANNEL_NAMESPACE + ':' + key;
    }
}

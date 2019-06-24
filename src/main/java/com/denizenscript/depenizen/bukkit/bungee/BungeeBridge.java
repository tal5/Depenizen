package com.denizenscript.depenizen.bukkit.bungee;

import com.denizenscript.depenizen.bukkit.Depenizen;
import com.denizenscript.depenizen.bukkit.bungee.packets.in.*;
import com.denizenscript.depenizen.bukkit.bungee.packets.out.ControlsProxyPingPacketOut;
import com.denizenscript.depenizen.bukkit.bungee.packets.out.KeepAlivePacketOut;
import com.denizenscript.depenizen.bukkit.commands.bungee.BungeeCommand;
import com.denizenscript.depenizen.bukkit.commands.bungee.BungeeExecuteCommand;
import com.denizenscript.depenizen.bukkit.commands.bungee.BungeeRunCommand;
import com.denizenscript.depenizen.bukkit.events.bungee.*;
import com.denizenscript.depenizen.bukkit.properties.bungee.BungeePlayerProperties;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.aufdemrand.denizen.objects.dPlayer;
import net.aufdemrand.denizen.utilities.DenizenAPI;
import net.aufdemrand.denizen.utilities.debugging.dB;
import net.aufdemrand.denizencore.events.ScriptEvent;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.TagRunnable;
import net.aufdemrand.denizencore.objects.dList;
import net.aufdemrand.denizencore.objects.properties.PropertyParser;
import net.aufdemrand.denizencore.tags.Attribute;
import net.aufdemrand.denizencore.tags.ReplaceableTagEvent;
import net.aufdemrand.denizencore.tags.TagManager;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BungeeBridge {

    public static BungeeBridge instance;

    public Channel channel;

    public NioEventLoopGroup workerGroup;

    public BungeeClientHandler handler;

    public String address;

    public int port;

    public String serverName = "<Unknown>";

    public List<String> knownServers = new ArrayList<>();

    public HashMap<Integer, PacketIn> packets = new HashMap<>();

    public boolean connected = false;

    public boolean controlsProxyPing = false;

    public int keepAliveTickRate = 10;

    public int ticksTilKeepalive = 0;

    public long lastPacketReceived = 0;

    public void checkBroadcastProxyPing() {
        if (connected) {
            sendPacket(new ControlsProxyPingPacketOut(controlsProxyPing));
        }
    }

    public void registerPackets() {
        packets.put(0, new KeepAlivePacketIn());
        packets.put(50, new YourInfoPacketIn());
        packets.put(51, new AddServerPacketIn());
        packets.put(52, new RemoveServerPacketIn());
        packets.put(53, new PlayerJoinPacketIn());
        packets.put(54, new PlayerQuitPacketIn());
        packets.put(55, new PlayerSwitchServerPacketIn());
        packets.put(56, new ProxyPingPacketIn());
        packets.put(57, new RunScriptPacketIn());
        packets.put(58, new RunCommandsPacketIn());
    }

    public void sendPacket(PacketOut packet) {
        ByteBuf buf = channel.alloc().buffer();
        packet.writeTo(buf);
        ByteBuf header = channel.alloc().buffer();
        header.writeInt(buf.writerIndex());
        header.writeInt(packet.getPacketId());
        channel.writeAndFlush(header);
        channel.writeAndFlush(buf);
        ticksTilKeepalive = 0;
    }

    public void init(String address, int port) {
        this.address = address;
        this.port = port;
        workerGroup = new NioEventLoopGroup();
        registerPackets();
        connect();
        successInit();
    }

    public boolean reconnectPending = false;

    public void reconnect() {
        if (reconnectPending) {
            return;
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(Depenizen.instance, new Runnable() {
            @Override
            public void run() {
                reconnectPending = false;
                connect();
            }
        }, 20 * 5);
    }

    private boolean showedLastError = false;

    private boolean hasConnectionLoading = false;

    public void connect() {
        try {
            hasConnectionLoading = false;
            knownServers.clear();
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                    hasConnectionLoading = true;
                    handler = new BungeeClientHandler();
                    handler.channel = ch;
                    ch.pipeline().addLast(handler).addLast(new NettyExceptionHandler());
                    channel = ch;
                }
            });
            b.connect(address, port).addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future) {
                    Bukkit.getScheduler().scheduleSyncDelayedTask(Depenizen.instance, new Runnable() {
                        @Override
                        public void run() {
                            if (!connected && !hasConnectionLoading) {
                                reconnect();
                            }
                        }
                    }, 10);
                }
            });
            showedLastError = false;
        }
        catch (Throwable ex) {
            if (!showedLastError) {
                showedLastError = true;
                dB.echoError(ex);
            }
            reconnect();
        }
    }

    public void successInit() {
        instance = this;
        ScriptEvent.registerScriptEvent(new BungeePlayerJoinsScriptEvent());
        ScriptEvent.registerScriptEvent(new BungeePlayerQuitsScriptEvent());
        ScriptEvent.registerScriptEvent(new BungeePlayerServerSwitchScriptEvent());
        ScriptEvent.registerScriptEvent(new BungeeProxyServerListPingScriptEvent());
        ScriptEvent.registerScriptEvent(new BungeeServerConnectScriptEvent());
        ScriptEvent.registerScriptEvent(new BungeeServerDisconnectScriptEvent());
        PropertyParser.registerProperty(BungeePlayerProperties.class, dPlayer.class);
        DenizenAPI.getCurrentInstance().getCommandRegistry().registerCoreMember(BungeeRunCommand.class, "BUNGEERUN",
                "bungeerun [<server>|...] [<script name>] (def:<definition>|...)", 2);
        DenizenAPI.getCurrentInstance().getCommandRegistry().registerCoreMember(BungeeExecuteCommand.class, "BUNGEEEXECUTE",
                "bungeeexecute [<command>]", 1);
        DenizenAPI.getCurrentInstance().getCommandRegistry().registerCoreMember(BungeeCommand.class, "BUNGEE",
                "bungee [<server>|...] [<commands>]", 1);
        TagManager.registerTagHandler(new TagRunnable.RootForm() {
            @Override
            public void run(ReplaceableTagEvent event) {
                tagEvent(event);
            }
        }, "bungee");
        Bukkit.getScheduler().scheduleSyncRepeatingTask(Depenizen.instance, new Runnable() {
            @Override
            public void run() {
                if (!connected) {
                    return;
                }
                if (System.currentTimeMillis() > lastPacketReceived + 20 * 1000) {
                    // 20 seconds without a packet = connection lost!
                    handler.fail("Connection time out.");
                    return;
                }
                ticksTilKeepalive--;
                if (ticksTilKeepalive <= 0) {
                    sendPacket(new KeepAlivePacketOut());
                    ticksTilKeepalive = keepAliveTickRate;
                }
            }
        }, 1, 1);
    }

    public void tagEvent(ReplaceableTagEvent event) {
        Attribute attribute = event.getAttributes().fulfill(1);

        // <--[tag]
        // @attribute <bungee.server>
        // @returns Element
        // @description
        // Returns the name of the current server (according to the Bungee proxy config).
        // @Plugin Depenizen, BungeeCord
        // -->
        if (attribute.startsWith("server")) {
            event.setReplacedObject(new Element(serverName)
                    .getObjectAttribute(attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <bungee.list_servers>
        // @returns dList
        // @description
        // Returns a list of known bungee server names.
        // @Plugin Depenizen, BungeeCord
        // -->
        if (attribute.startsWith("list_servers")) {
            event.setReplacedObject(new dList(knownServers)
                    .getObjectAttribute(attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <bungee.connected>
        // @returns Element(Boolean)
        // @description
        // Returns this server is currently connected to the BungeeCord proxy server.
        // @Plugin Depenizen, BungeeCord
        // -->
        if (attribute.startsWith("connected")) {
            event.setReplacedObject(new Element(connected)
                    .getObjectAttribute(attribute.fulfill(1)));
        }
    }
}
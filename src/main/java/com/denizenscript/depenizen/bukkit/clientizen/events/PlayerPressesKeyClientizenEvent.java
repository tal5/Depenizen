package com.denizenscript.depenizen.bukkit.clientizen.events;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.depenizen.bukkit.clientizen.DataDeserializer;
import com.denizenscript.depenizen.bukkit.clientizen.DataSerializer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerPressesKeyClientizenEvent extends ClientizenScriptEvent {

    public static Set<Integer> listenToKeys = new HashSet<>();

    public KeyboardKeys key;

    public PlayerPressesKeyClientizenEvent() {
        registerCouldMatcher("player presses key");
        registerSwitches("key");
        id = "PlayerPressesKey";
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (!runGenericSwitchCheck(path, "key", key.name())) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public ObjectTag getContext(String name) {
        switch (name) {
            case "key": return new ElementTag(key.name());
        }
        return super.getContext(name);
    }

    @Override
    public void init() {
        super.init();
        for (ScriptPath path : eventPaths) {
            if (path.switches.containsKey("key")) {
                listenToKeys.addAll(KeyboardKeys.getKeysMatching(path.switches.get("key")));
            }
            else {
                listenToKeys.clear();
                break;
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        listenToKeys.clear();
    }

    @Override
    public void fire(DataDeserializer data) {
        key = KeyboardKeys.keysByID.get(data.readInt());
        fire();
    }

    @Override
    public void write(DataSerializer serializer) {
        serializer.writeIntList(listenToKeys);
    }
    
    enum KeyboardKeys {
        UNKNOWN(-1),
        SPACE(32),
        APOSTROPHE(39),
        COMMA(44),
        MINUS(45),
        PERIOD(46),
        SLASH(47),
        KEY_0(48),
        KEY_1(49),
        KEY_2(50),
        KEY_3(51),
        KEY_4(52),
        KEY_5(53),
        KEY_6(54),
        KEY_7(55),
        KEY_8(56),
        KEY_9(57),
        SEMICOLON(59),
        EQUAL(61),
        A(65),
        B(66),
        C(67),
        D(68),
        E(69),
        F(70),
        G(71),
        H(72),
        I(73),
        J(74),
        K(75),
        L(76),
        M(77),
        N(78),
        O(79),
        P(80),
        Q(81),
        R(82),
        S(83),
        T(84),
        U(85),
        V(86),
        W(87),
        X(88),
        Y(89),
        Z(90),
        LEFT_BRACKET(91),
        BACKSLASH(92),
        RIGHT_BRACKET(93),
        GRAVE_ACCENT(96),
        WORLD_1(161),
        WORLD_2(162),
        ESCAPE(256),
        ENTER(257),
        TAB(258),
        BACKSPACE(259),
        INSERT(260),
        DELETE(261),
        RIGHT(262),
        LEFT(263),
        DOWN(264),
        UP(265),
        PAGE_UP(266),
        PAGE_DOWN(267),
        HOME(268),
        END(269),
        CAPS_LOCK(280),
        SCROLL_LOCK(281),
        NUM_LOCK(282),
        PRINT_SCREEN(283),
        PAUSE(284),
        F1(290),
        F2(291),
        F3(292),
        F4(293),
        F5(294),
        F6(295),
        F7(296),
        F8(297),
        F9(298),
        F10(299),
        F11(300),
        F12(301),
        F13(302),
        F14(303),
        F15(304),
        F16(305),
        F17(306),
        F18(307),
        F19(308),
        F20(309),
        F21(310),
        F22(311),
        F23(312),
        F24(313),
        F25(314),
        KP_0(320),
        KP_1(321),
        KP_2(322),
        KP_3(323),
        KP_4(324),
        KP_5(325),
        KP_6(326),
        KP_7(327),
        KP_8(328),
        KP_9(329),
        KP_DECIMAL(330),
        KP_DIVIDE(331),
        KP_MULTIPLY(332),
        KP_SUBTRACT(333),
        KP_ADD(334),
        KP_ENTER(335),
        KP_EQUAL(336),
        LEFT_SHIFT(340),
        LEFT_CONTROL(341),
        LEFT_ALT(342),
        LEFT_SUPER(343),
        RIGHT_SHIFT(344),
        RIGHT_CONTROL(345),
        RIGHT_ALT(346),
        RIGHT_SUPER(347),
        MENU(348);
        
        public final int key;
        
        KeyboardKeys(int key) {
            this.key = key;
        }

        public static KeyboardKeys getByName(String name) {
            if (name == null) {
                return null;
            }
            return !alternateNames.containsKey(name) ? new ElementTag(name).asEnum(KeyboardKeys.class) : alternateNames.get(name);
        }

        public static Set<Integer> getKeysMatching(String matcher) {
            Set<Integer> result = new HashSet<>();
            MatchHelper matchHelper = ScriptEvent.createMatcher(matcher);
            for (Map.Entry<String, KeyboardKeys> entry : alternateNames.entrySet()) {
                if (matchHelper.doesMatch(entry.getKey())) {
                    result.add(entry.getValue().key);
                }
            }
            for (KeyboardKeys key : KeyboardKeys.values()) {
                if (matchHelper.doesMatch(key.name())) {
                    result.add(key.key);
                }
            }
            return result;
        }

        public static final Map<Integer, KeyboardKeys> keysByID = new HashMap<>();
        private static final Map<String, KeyboardKeys> alternateNames = new HashMap<>();

        static {
            alternateNames.put("0", KEY_0);
            alternateNames.put("1", KEY_1);
            alternateNames.put("2", KEY_2);
            alternateNames.put("3", KEY_3);
            alternateNames.put("4", KEY_4);
            alternateNames.put("5", KEY_5);
            alternateNames.put("6", KEY_6);
            alternateNames.put("7", KEY_7);
            alternateNames.put("8", KEY_8);
            alternateNames.put("9", KEY_9);
            for (KeyboardKeys key : KeyboardKeys.values()) {
                keysByID.put(key.key, key);
            }
        }
    }
}

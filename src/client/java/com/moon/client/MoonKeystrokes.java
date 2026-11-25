package com.moon.client;

import java.util.ArrayDeque;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

/** Keystrokes HUD (W A S D, mouse buttons with CPS, space bar) using the Moon Client Canva key art. */
public final class MoonKeystrokes {
    private static final int TEXT = 0xff7a4a62;
    private static final float BLANK_ASPECT = 126f / 141f;
    private static final Identifier BLANK = tex("key_blank");
    private static final Identifier BLANK_DOWN = tex("key_blank_down");
    private static final Identifier[] UP = {tex("key_w"), tex("key_a"), tex("key_s"), tex("key_d")};
    private static final Identifier[] DOWN = {tex("key_w_down"), tex("key_a_down"), tex("key_s_down"), tex("key_d_down")};

    private static final ArrayDeque<Long> leftClicks = new ArrayDeque<>();
    private static final ArrayDeque<Long> rightClicks = new ArrayDeque<>();
    private static final java.util.Map<KeyMapping, Long> lastPress = new java.util.HashMap<>();
    /** A tap is shown as pressed for at least this long, so quick taps are visible. */
    private static final long FLASH_MS = 90;

    private MoonKeystrokes() {}

    private static Identifier tex(String n) {
        return Identifier.fromNamespaceAndPath("moon-client", "textures/gui/keys/" + n + ".png");
    }

    /** Called by the Fabric HUD every frame. */
    static void hud(GuiGraphicsExtractor g, DeltaTracker delta) {
        var mc = Minecraft.getInstance();
        var cfg = MoonConfig.current;
        if (!cfg.keystrokes || mc.player == null || mc.gui.screen() instanceof MoonKeysScreen) {
            return;
        }
        prune();
        int sw = g.guiWidth();
        int sh = g.guiHeight();
        int[] size = size(cfg.keysScale);
        int x = Math.round(cfg.keysX * sw);
        int y = Math.round(cfg.keysY * sh);
        x = Math.max(0, Math.min(sw - size[0], x));
        y = Math.max(0, Math.min(sh - size[1], y));
        draw(g, x, y, cfg.keysScale, false);
    }

    /** Called from the mouse mixin on every button press. */
    public static void onMouse(MouseButtonInfo info) {
        var mc = Minecraft.getInstance();
        if (mc.gui.screen() != null) {
            return;
        }
        var ev = new MouseButtonEvent(0, 0, info);
        long now = Util.getMillis();
        if (mc.options.keyAttack.matchesMouse(ev)) {
            leftClicks.add(now);
            lastPress.put(mc.options.keyAttack, now);
        }
        if (mc.options.keyUse.matchesMouse(ev)) {
            rightClicks.add(now);
            lastPress.put(mc.options.keyUse, now);
        }
    }

    /** Called from the keyboard mixin on every key press. */
    public static void onKey(KeyEvent ev) {
        var mc = Minecraft.getInstance();
        if (mc.gui.screen() != null) {
            return;
        }
        var o = mc.options;
        for (KeyMapping k : new KeyMapping[] {o.keyUp, o.keyLeft, o.keyDown, o.keyRight, o.keyJump, o.keyAttack, o.keyUse}) {
            if (k.matches(ev)) {
                lastPress.put(k, Util.getMillis());
            }
        }
    }

    private static boolean down(KeyMapping k) {
        Long t = lastPress.get(k);
        return k.isDown() || (t != null && Util.getMillis() - t < FLASH_MS);
    }

    private static void prune() {
        long now = Util.getMillis();
        while (!leftClicks.isEmpty() && now - leftClicks.peekFirst() > 1000) leftClicks.pollFirst();
        while (!rightClicks.isEmpty() && now - rightClicks.peekFirst() > 1000) rightClicks.pollFirst();
    }

    static int unit(float scale) {
        return Math.max(12, Math.round(22 * scale));
    }

    static int gap(float scale) {
        return Math.max(1, Math.round(2 * scale));
    }

    /** {width, height} of the whole HUD. */
    static int[] size(float scale) {
        var cfg = MoonConfig.current;
        int u = unit(scale);
        int kh = Math.round(u / BLANK_ASPECT);
        int gp = gap(scale);
        int h = kh * 2 + gp;
        if (cfg.mouseKeys) h += gp + kh;
        if (cfg.spaceBar) h += gp + Math.round(kh * 0.6f);
        return new int[] {u * 3 + gp * 2, h};
    }

    static void draw(GuiGraphicsExtractor g, int x, int y, float scale, boolean preview) {
        var mc = Minecraft.getInstance();
        var o = mc.options;
        var cfg = MoonConfig.current;
        int u = unit(scale);
        int kh = Math.round(u / BLANK_ASPECT);
        int gp = gap(scale);
        int full = u * 3 + gp * 2;

        KeyMapping[] keys = {o.keyUp, o.keyLeft, o.keyDown, o.keyRight};
        int[][] pos = {{x + u + gp, y}, {x, y + kh + gp}, {x + u + gp, y + kh + gp}, {x + 2 * (u + gp), y + kh + gp}};
        for (int i = 0; i < 4; i++) {
            boolean down = !preview && down(keys[i]);
            int px = pos[i][0];
            int py = pos[i][1] + (down ? 1 : 0);
            g.blit(down ? DOWN[i] : UP[i], px, py, px + u, py + kh, 0f, 1f, 0f, 1f);
        }

        int cy = y + kh * 2 + gp * 2;
        if (cfg.mouseKeys) {
            int half = (full - gp) / 2;
            mouseKey(g, x, cy, half, kh, "LMB", leftClicks.size(), !preview && down(o.keyAttack));
            mouseKey(g, x + half + gp, cy, full - half - gp, kh, "RMB", rightClicks.size(), !preview && down(o.keyUse));
            cy += kh + gp;
        }
        if (cfg.spaceBar) {
            int sh = Math.round(kh * 0.6f);
            boolean down = !preview && down(o.keyJump);
            int py = cy + (down ? 1 : 0);
            stretched(g, down ? BLANK_DOWN : BLANK, x, py, full, sh);
            int bw = full / 3;
            int by = py + sh / 2 - 1;
            g.fill(x + (full - bw) / 2, by, x + (full + bw) / 2, by + Math.max(1, Math.round(scale)), TEXT);
        }
    }

    private static void mouseKey(GuiGraphicsExtractor g, int x, int y, int w, int h, String name, int cps, boolean down) {
        var font = Minecraft.getInstance().font;
        int py = y + (down ? 1 : 0);
        stretched(g, down ? BLANK_DOWN : BLANK, x, py, w, h);
        int face = h - Math.max(3, h / 6); // leave the darker bottom lip free
        String c = cps + " CPS";
        if (face >= 19) {
            int top = py + (face - 17) / 2;
            g.text(font, name, x + (w - font.width(name)) / 2, top, TEXT, false);
            g.text(font, c, x + (w - font.width(c)) / 2, top + 9, TEXT, false);
        } else {
            g.text(font, c, x + (w - font.width(c)) / 2, py + (face - 8) / 2, TEXT, false);
        }
    }

    /** Draws a key texture wider than it is, keeping the rounded corners (3-slice). */
    private static void stretched(GuiGraphicsExtractor g, Identifier t, int x, int y, int w, int h) {
        float cut = 0.3f;
        int cap = Math.round(cut * BLANK_ASPECT * h);
        if (w <= cap * 2) {
            g.blit(t, x, y, x + w, y + h, 0f, 1f, 0f, 1f);
            return;
        }
        g.blit(t, x, y, x + cap, y + h, 0f, cut, 0f, 1f);
        g.blit(t, x + cap, y, x + w - cap, y + h, cut, 1f - cut, 0f, 1f);
        g.blit(t, x + w - cap, y, x + w, y + h, 1f - cut, 1f, 0f, 1f);
    }
}

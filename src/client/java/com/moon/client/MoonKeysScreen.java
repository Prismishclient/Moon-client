package com.moon.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Settings for the keystrokes HUD. Drag the keys to move them. */
public class MoonKeysScreen extends Screen {
    private final Screen prev;
    private boolean dragging;
    private double grabX;
    private double grabY;

    public MoonKeysScreen(Screen prev) {
        super(Component.literal("Keystrokes"));
        this.prev = prev;
    }

    private String onOff(String name, boolean v) {
        return name + ": " + (v ? "ON" : "OFF");
    }

    @Override
    protected void init() {
        var cfg = MoonConfig.current;
        int bw = 120;
        int bh = 20;
        int cx = width / 2;
        int y = height / 2 - 40;
        addRenderableWidget(new MoonButton(cx - bw / 2, y - 36, bw, 28, "Keystrokes", MoonScreen.keystrokes, () -> {}))
                .active = false;
        addRenderableWidget(new MoonButton(cx - bw / 2, y, bw, bh, onOff("Keystrokes", cfg.keystrokes), null, () -> {
            cfg.keystrokes = !cfg.keystrokes;
            rebuildWidgets();
        }));
        addRenderableWidget(new MoonButton(cx - bw / 2, y + 24, bw, bh, onOff("Mouse buttons", cfg.mouseKeys), null, () -> {
            cfg.mouseKeys = !cfg.mouseKeys;
            rebuildWidgets();
        }));
        addRenderableWidget(new MoonButton(cx - bw / 2, y + 48, bw, bh, onOff("Space bar", cfg.spaceBar), null, () -> {
            cfg.spaceBar = !cfg.spaceBar;
            rebuildWidgets();
        }));
        int half = (bw - 4) / 2;
        addRenderableWidget(new MoonButton(cx - bw / 2, y + 72, half, bh, "Size -", null, () ->
                cfg.keysScale = Math.max(0.5f, Math.round((cfg.keysScale - 0.1f) * 10) / 10f)));
        addRenderableWidget(new MoonButton(cx - bw / 2 + half + 4, y + 72, bw - half - 4, bh, "Size +", null, () ->
                cfg.keysScale = Math.min(2.5f, Math.round((cfg.keysScale + 0.1f) * 10) / 10f)));
        addRenderableWidget(new MoonButton(cx - bw / 2, y + 96, half, bh, "Reset", null, () -> {
            var d = new MoonConfig();
            cfg.keysX = d.keysX;
            cfg.keysY = d.keysY;
            cfg.keysScale = d.keysScale;
        }));
        addRenderableWidget(new MoonButton(cx - bw / 2 + half + 4, y + 96, bw - half - 4, bh, "Done", null, this::onClose));
    }

    private int[] hudRect() {
        var cfg = MoonConfig.current;
        int[] s = MoonKeystrokes.size(cfg.keysScale);
        int x = Math.max(0, Math.min(width - s[0], Math.round(cfg.keysX * width)));
        int y = Math.max(0, Math.min(height - s[1], Math.round(cfg.keysY * height)));
        return new int[] {x, y, s[0], s[1]};
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent ev, boolean doubleClick) {
        if (super.mouseClicked(ev, doubleClick)) {
            return true;
        }
        int[] r = hudRect();
        if (MoonConfig.current.keystrokes && ev.button() == InputConstants.MOUSE_BUTTON_LEFT
                && ev.x() >= r[0] && ev.x() < r[0] + r[2] && ev.y() >= r[1] && ev.y() < r[1] + r[3]) {
            dragging = true;
            grabX = ev.x() - r[0];
            grabY = ev.y() - r[1];
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent ev, double dx, double dy) {
        if (dragging) {
            int[] r = hudRect();
            double nx = Math.max(0, Math.min(width - r[2], ev.x() - grabX));
            double ny = Math.max(0, Math.min(height - r[3], ev.y() - grabY));
            MoonConfig.current.keysX = (float) (nx / width);
            MoonConfig.current.keysY = (float) (ny / height);
            return true;
        }
        return super.mouseDragged(ev, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent ev) {
        dragging = false;
        return super.mouseReleased(ev);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float a) {
        if (minecraft.level == null) {
            extractPanorama(g, a);
        }
        g.fill(0, 0, width, height, 0x55000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float a) {
        super.extractRenderState(g, mx, my, a);
        var cfg = MoonConfig.current;
        if (cfg.keystrokes) {
            int[] r = hudRect();
            g.fill(r[0] - 2, r[1] - 2, r[0] + r[2] + 2, r[1] + r[3] + 2, dragging ? 0x66ffc0e0 : 0x33ffffff);
            MoonKeystrokes.draw(g, r[0], r[1], cfg.keysScale, true);
        }
        g.centeredText(font, "Drag the keys to move them  •  Size " + Math.round(cfg.keysScale * 100) + "%",
                width / 2, height - 14, 0xfffff2fc);
    }

    @Override
    public void onClose() {
        MoonConfig.save();
        minecraft.gui.setScreen(prev);
    }
}

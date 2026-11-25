package com.moon.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

final class MoonField extends EditBox {
    private final Identifier art;
    private final boolean secret;

    MoonField(Font font, int x, int y, int w, int h, String label, Identifier art, boolean secret) {
        super(font, x + 4, y, w - 8, h, Component.literal(label));
        this.art = art;
        this.secret = secret;
        setBordered(false);
        setCentered(true);
        setTextShadow(false);
        setTextColor(0xff58364d);
        setMaxLength(secret ? 128 : 254);
        if (secret) {
            addFormatter((s, n) -> FormattedCharSequence.forward("•".repeat(s.length()), Style.EMPTY));
        }
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float a) {
        int x = getX() - 4;
        int y = getY();
        int w = getWidth() + 8;
        int h = getHeight();
        var bg = isHoveredOrFocused() ? MoonScreen.pillfocus : MoonScreen.pill;
        g.blit(bg, x, y, x + w, y + h, 0f, 1f, 0f, 1f);
        if (art != null && getValue().isEmpty() && !isFocused()) {
            int aw = Math.round(w * (secret ? 0.82f : 0.88f));
            int ah = h - 4;
            int ax = x + (w - aw) / 2;
            int ay = y + (h - ah) / 2;
            g.blit(art, ax, ay, ax + aw, ay + ah, 0f, 1f, 0f, 1f);
        }
        super.extractWidgetRenderState(g, mx, my, a);
    }

    @Override
    protected MutableComponent createNarrationMessage() {
        return secret ? getMessage().copy() : super.createNarrationMessage();
    }

    @Override
    public boolean keyPressed(KeyEvent ev) {
        return (secret && (ev.isCopy() || ev.isCut())) || super.keyPressed(ev);
    }

    @Override
    public boolean preeditUpdated(PreeditEvent ev) {
        return secret ? isFocused() : super.preeditUpdated(ev);
    }
}

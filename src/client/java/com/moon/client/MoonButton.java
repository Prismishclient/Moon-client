package com.moon.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

final class MoonButton extends AbstractButton {
    private final Runnable run;
    private final Identifier art;

    MoonButton(int x, int y, int w, int h, String text, Identifier art, Runnable run) {
        super(x, y, w, h, Component.literal(text));
        this.art = art;
        this.run = run;
    }

    @Override
    public void onPress(InputWithModifiers in) {
        run.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float a) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        if (art != null && art.equals(MoonScreen.keystrokes)) {
            // Canva "Keystrokes" button already has its own pill.
            int lift = active && isHoveredOrFocused() ? 1 : 0;
            g.blit(art, x, y - lift, x + w, y + h - lift, 0f, 1f, 0f, 1f);
            return;
        }
        var bg = isHoveredOrFocused() ? MoonScreen.pillfocus : MoonScreen.pill;
        g.blit(bg, x, y, x + w, y + h, 0f, 1f, 0f, 1f);
        if (art != null) {
            int aw = Math.round(w * (art.equals(MoonScreen.brand) ? 0.85f : 0.48f));
            int ah = h - 3;
            int ax = x + (w - aw) / 2;
            int ay = y + (h - ah) / 2;
            g.blit(art, ax, ay, ax + aw, ay + ah, 0f, 1f, 0f, 1f);
        } else {
            var font = Minecraft.getInstance().font;
            g.text(font, getMessage(), x + (w - font.width(getMessage())) / 2, y + (h - 8) / 2, 0xff58364d, false);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        defaultButtonNarrationText(out);
    }
}

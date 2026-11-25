package com.moon.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/**
 * Simple text box made for the Moon Client login.
 * Text is always centred (left/right and up/down) inside the pill, and the
 * password dots are measured as dots so they stay centred too.
 * IME/autocorrect "composition" text is ignored; only the final typed
 * characters are inserted, so the OS can't scramble what the player types.
 */
final class MoonField extends AbstractWidget {
    private static final int BACKSPACE = 8;
    private static final int DELETE = 127;
    private static final int HOME = 1073741898;
    private static final int END = 1073741901;
    private static final String DOT = "•";
    private static final int PAD = 8;

    private final Font font;
    private final Identifier art;
    private final boolean secret;
    private final boolean noSpaces;
    private String value = "";
    private int cursor;
    private int anchor;
    private int maxLength;
    private boolean editable = true;
    private Component hint;
    private Runnable onEnter = () -> {};
    private long focusTime;

    // Filled while drawing, used to map mouse clicks to a letter.
    private int drawX;
    private int drawStart;

    MoonField(Font font, int x, int y, int w, int h, String label, Identifier art, boolean secret) {
        super(x, y, w, h, Component.literal(label));
        this.font = font;
        this.art = art;
        this.secret = secret;
        this.noSpaces = !secret;
        this.maxLength = secret ? 128 : 254;
    }

    String getValue() {
        return value;
    }

    void setValue(String v) {
        value = v.length() > maxLength ? v.substring(0, maxLength) : v;
        cursor = anchor = value.length();
    }

    void setMaxLength(int n) {
        maxLength = n;
        if (value.length() > n) {
            setValue(value.substring(0, n));
        }
    }

    void setEditable(boolean e) {
        editable = e;
    }

    void setHint(Component c) {
        hint = c;
    }

    void onEnter(Runnable r) {
        onEnter = r;
    }

    private String shown(String s) {
        return secret ? DOT.repeat(s.length()) : s;
    }

    private int selStart() {
        return Math.min(cursor, anchor);
    }

    private int selEnd() {
        return Math.max(cursor, anchor);
    }

    private void insert(String text) {
        if (!editable) {
            return;
        }
        StringBuilder clean = new StringBuilder();
        text.codePoints().forEach(cp -> {
            if (cp >= 32 && cp != 127 && !(noSpaces && Character.isWhitespace(cp))) {
                clean.appendCodePoint(cp);
            }
        });
        String rest = value.substring(0, selStart()) + value.substring(selEnd());
        int room = maxLength - rest.length();
        String add = clean.length() > room ? clean.substring(0, Math.max(0, room)) : clean.toString();
        value = rest.substring(0, selStart()) + add + rest.substring(selStart());
        cursor = anchor = selStart() + add.length();
    }

    private void moveTo(int pos, boolean select) {
        cursor = Math.max(0, Math.min(value.length(), pos));
        if (!select) {
            anchor = cursor;
        }
        focusTime = Util.getMillis();
    }

    private int wordEdge(int dir) {
        int i = cursor;
        if (dir < 0) {
            while (i > 0 && value.charAt(i - 1) == ' ') i--;
            while (i > 0 && value.charAt(i - 1) != ' ') i--;
        } else {
            while (i < value.length() && value.charAt(i) == ' ') i++;
            while (i < value.length() && value.charAt(i) != ' ') i++;
        }
        return i;
    }

    private void erase(int dir, boolean word) {
        if (!editable) {
            return;
        }
        if (cursor == anchor) {
            int to = word && !secret ? wordEdge(dir) : cursor + dir;
            anchor = Math.max(0, Math.min(value.length(), to));
        }
        insert("");
    }

    @Override
    public boolean keyPressed(KeyEvent ev) {
        if (!isFocused() || !active) {
            return false;
        }
        boolean shift = ev.hasShiftDown();
        boolean ctrl = ev.hasControlDownWithQuirk();
        switch (ev.shortcutKey()) {
            case BACKSPACE -> { erase(-1, ctrl); return true; }
            case DELETE -> { erase(1, ctrl); return true; }
            case HOME -> { moveTo(0, shift); return true; }
            case END -> { moveTo(value.length(), shift); return true; }
            default -> { }
        }
        if (ev.isConfirmation()) {
            onEnter.run();
            return true;
        }
        if (ev.isLeft()) {
            moveTo(ctrl && !secret ? wordEdge(-1) : cursor - 1, shift);
            return true;
        }
        if (ev.isRight()) {
            moveTo(ctrl && !secret ? wordEdge(1) : cursor + 1, shift);
            return true;
        }
        if (ev.isSelectAll()) {
            anchor = 0;
            cursor = value.length();
            return true;
        }
        if (ev.isCopy() || ev.isCut()) {
            if (!secret) {
                Minecraft.getInstance().keyboardHandler.setClipboard(value.substring(selStart(), selEnd()));
                if (ev.isCut()) {
                    insert("");
                }
            }
            return true;
        }
        if (ev.isPaste()) {
            insert(Minecraft.getInstance().keyboardHandler.getClipboard());
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(CharacterEvent ev) {
        if (!isFocused() || !active || !editable) {
            return false;
        }
        insert(ev.codepointAsString());
        return true;
    }

    @Override
    public boolean preeditUpdated(@Nullable PreeditEvent ev) {
        // Swallow IME/autocorrect previews. The final text still arrives through charTyped.
        return isFocused();
    }

    @Override
    public boolean capturesInput() {
        return active && isFocused() && editable;
    }

    @Override
    public void setFocused(boolean focused) {
        boolean was = isFocused();
        super.setFocused(focused);
        if (focused != was) {
            focusTime = Util.getMillis();
            Minecraft.getInstance().onTextInputFocusChange(this, focused && editable);
        }
    }

    @Override
    public void onClick(MouseButtonEvent ev, boolean doubleClick) {
        if (doubleClick) {
            anchor = 0;
            cursor = value.length();
            return;
        }
        moveTo(posAt(ev.x()), ev.hasShiftDown());
    }

    @Override
    protected void onDrag(MouseButtonEvent ev, double dx, double dy) {
        moveTo(posAt(ev.x()), true);
    }

    private int posAt(double mx) {
        String s = shown(value);
        int best = drawStart;
        double bestDist = Double.MAX_VALUE;
        for (int i = drawStart; i <= s.length(); i++) {
            int px = drawX + font.width(s.substring(drawStart, i));
            double d = Math.abs(px - mx);
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float a) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        var bg = isHoveredOrFocused() ? MoonScreen.pillfocus : MoonScreen.pill;
        g.blit(bg, x, y, x + w, y + h, 0f, 1f, 0f, 1f);

        int ty = y + (h - 8) / 2;
        int cx = x + w / 2;
        if (value.isEmpty() && !isFocused()) {
            if (art != null) {
                int aw = Math.round(w * (secret ? 0.82f : 0.88f));
                int ah = h - 4;
                int ax = x + (w - aw) / 2;
                int ay = y + (h - ah) / 2;
                g.blit(art, ax, ay, ax + aw, ay + ah, 0f, 1f, 0f, 1f);
            } else if (hint != null) {
                g.text(font, hint, cx - font.width(hint) / 2, ty, 0xff9a7a8e, false);
            }
            return;
        }

        int color = editable ? 0xff58364d : 0xff9a7a8e;
        int inner = w - PAD * 2;
        String s = shown(value);
        // Scroll so the cursor is always visible.
        int start = Math.min(drawStart, cursor);
        while (start < cursor && font.width(s.substring(start, cursor)) > inner) {
            start++;
        }
        int end = start;
        while (end < s.length() && font.width(s.substring(start, end + 1)) <= inner) {
            end++;
        }
        // Text got shorter (backspace): scroll back so the box stays full.
        while (start > 0 && font.width(s.substring(start - 1, end)) <= inner) {
            start--;
        }
        String vis = s.substring(start, end);
        int tx = cx - font.width(vis) / 2;
        drawStart = start;
        drawX = tx;

        int ss = Math.max(start, Math.min(end, selStart()));
        int se = Math.max(start, Math.min(end, selEnd()));
        if (ss != se) {
            int hx0 = tx + font.width(s.substring(start, ss));
            int hx1 = tx + font.width(s.substring(start, se));
            g.fill(hx0, ty - 1, hx1, ty + 9, 0xffe9b8d3);
        }
        g.text(font, vis, tx, ty, color, false);

        if (isFocused() && editable && ((Util.getMillis() - focusTime) / 500) % 2 == 0) {
            int c = Math.max(start, Math.min(end, cursor));
            int px = tx + font.width(s.substring(start, c));
            g.fill(px, ty - 1, px + 1, ty + 9, color);
        }
        if (isFocused()) {
            int c = Math.max(start, Math.min(end, cursor));
            int px = tx + font.width(s.substring(start, c));
            Minecraft.getInstance().textInputManager().setTextInputArea(px, ty, px + 1, ty + 10);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        out.add(NarratedElementType.TITLE, secret || value.isEmpty()
                ? getMessage()
                : Component.literal(getMessage().getString() + ": " + value));
    }
}

package com.moon.client;

import java.time.Instant;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class MoonScreen extends Screen {
    static final Identifier pill = Identifier.fromNamespaceAndPath("moon-client", "textures/gui/pill.png");
    static final Identifier pillfocus = Identifier.fromNamespaceAndPath("moon-client", "textures/gui/pill_focus.png");
    static final Identifier brand = Identifier.fromNamespaceAndPath("moon-client", "textures/gui/brand.png");
    private final Identifier title = Identifier.fromNamespaceAndPath("moon-client", "textures/gui/heading.png");
    private final Identifier login = Identifier.fromNamespaceAndPath("moon-client", "textures/gui/login.png");
    private final Identifier email = Identifier.fromNamespaceAndPath("moon-client", "textures/gui/email.png");
    private final Identifier pass = Identifier.fromNamespaceAndPath("moon-client", "textures/gui/password.png");
    private final Identifier[] frames = new Identifier[22];
    private final long start = System.nanoTime();
    private final Screen prev;
    private MoonField em;
    private MoonField pw;
    private String msg = "";
    private MoonButton submit;
    private boolean busy;
    private String mail = "";
    private MoonAuth.Action mode = MoonAuth.Action.LOGIN;
    private float sc;
    private int x;
    private int y;

    public MoonScreen(Screen prev) {
        super(Component.literal("Moon Client"));
        this.prev = prev;
        for (int i = 0; i < frames.length; i++) {
            frames[i] = Identifier.fromNamespaceAndPath("moon-client", "textures/gui/room_" + i + ".png");
        }
    }

    @Override
    protected void init() {
        sc = Math.min(width / 596f, height / 335f);
        x = Math.round((width - 596 * sc) / 2);
        y = Math.round((height - 335 * sc) / 2);
        int lx = x + Math.round(222 * sc);
        int w = Math.round(151 * sc);
        int h = Math.max(12, Math.round(26 * sc));
        if (MoonAuth.session != null && !MoonAuth.session.expires().isAfter(Instant.now())) {
            MoonAuth.session = null;
        }
        if (MoonAuth.session == null) {
            if (em != null) {
                mail = em.getValue();
            }
            if (pw != null) {
                pw.setValue("");
            }
            em = addRenderableWidget(new MoonField(font, lx, y + Math.round(122 * sc), w, h, "Email", email, false));
            em.setValue(mail);
            if (mode == MoonAuth.Action.VERIFY) {
                em.setEditable(false);
                pw = addRenderableWidget(new MoonField(font, lx, y + Math.round(168 * sc), w, h, "Email code", null, false));
                pw.setHint(Component.literal("Email code"));
                pw.setMaxLength(8);
            } else {
                pw = addRenderableWidget(new MoonField(font, lx, y + Math.round(168 * sc), w, h, "Password", pass, true));
            }
            boolean log = mode == MoonAuth.Action.LOGIN;
            String label = log ? "Login" : mode == MoonAuth.Action.REGISTER ? "Register" : "Verify code";
            submit = addRenderableWidget(new MoonButton(lx, y + Math.round(219 * sc), w, h,
                    label, log ? login : null, () -> send(mode)));
            submit.active = !busy;
            int gap = 4;
            int half = (w - gap) / 2;
            int by = y + Math.round(255 * sc);
            var left = addRenderableWidget(new MoonButton(lx, by, half, h, log ? "Register" : "Login", null,
                    () -> changeMode(log ? MoonAuth.Action.REGISTER : MoonAuth.Action.LOGIN)));
            left.active = !busy;
            boolean ver = mode == MoonAuth.Action.VERIFY;
            var right = addRenderableWidget(new MoonButton(lx + half + gap, by, w - half - gap, h,
                    ver ? "Resend" : "Use code", null, () -> {
                        if (ver) {
                            send(MoonAuth.Action.RESEND);
                        } else {
                            changeMode(MoonAuth.Action.VERIFY);
                        }
                    }));
            right.active = !busy;
        } else {
            em = null;
            pw = null;
            submit = addRenderableWidget(new MoonButton(lx, y + Math.round(219 * sc), w, h, "Sign out", null, () -> {
                var ses = MoonAuth.session;
                MoonAuth.session = null;
                msg = "Signed out";
                rebuildWidgets();
                MoonAuth.signOut(ses).thenAccept(ok -> minecraft.execute(() -> {
                    if (!ok && minecraft.gui.screen() == this && MoonAuth.session == null && !busy) {
                        msg = "Signed out here. Server sign-out could not be confirmed.";
                    }
                }));
            }));
        }
        addRenderableWidget(new MoonButton(width - 64, height - 24, 58, 19, "Back", null, this::onClose));
    }

    private void changeMode(MoonAuth.Action act) {
        if (act == MoonAuth.Action.VERIFY && !em.getValue().strip().contains("@")) {
            msg = "Enter your Moon Client email first.";
            return;
        }
        mode = act;
        msg = act == MoonAuth.Action.VERIFY ? "Enter the code from your email." : "";
        rebuildWidgets();
    }

    private void send(MoonAuth.Action act) {
        if (busy) {
            return;
        }
        String addr = em.getValue().strip();
        String val = pw.getValue();
        if (!addr.contains("@")) {
            msg = "Enter your Moon Client email.";
            return;
        }
        if (act == MoonAuth.Action.VERIFY && !val.strip().matches("[0-9]{6,8}")) {
            msg = "Enter the numeric code from your email.";
            return;
        }
        if ((act == MoonAuth.Action.LOGIN || act == MoonAuth.Action.REGISTER) && val.isEmpty()) {
            msg = "Enter a password.";
            return;
        }
        pw.setValue("");
        clearFocus();
        busy = true;
        msg = switch (act) {
            case LOGIN -> "Signing in...";
            case REGISTER, RESEND -> "Requesting an email...";
            case VERIFY -> "Checking the code...";
        };
        rebuildWidgets();
        try {
            MoonAuth.submit(act, addr, act == MoonAuth.Action.VERIFY ? val.strip() : val)
                    .thenAccept(res -> minecraft.execute(() -> {
                        busy = false;
                        if (minecraft.gui.screen() != this) {
                            if (res.session() != null) {
                                MoonAuth.signOut(res.session());
                            }
                            return;
                        }
                        msg = res.message();
                        MoonAuth.session = res.session();
                        if (res.ok() && (act == MoonAuth.Action.REGISTER || act == MoonAuth.Action.RESEND)) {
                            mode = MoonAuth.Action.VERIFY;
                        }
                        if (res.session() != null) {
                            mode = MoonAuth.Action.LOGIN;
                        }
                        rebuildWidgets();
                    }));
        } catch (RuntimeException ignored) {
            busy = false;
            msg = "Moon Client sign-in is unavailable.";
            rebuildWidgets();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (MoonAuth.session != null && !MoonAuth.session.expires().isAfter(Instant.now())) {
            MoonAuth.session = null;
            msg = "Session expired. Sign in again.";
            rebuildWidgets();
        }
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(prev);
    }

    @Override
    public void removed() {
        if (pw != null) {
            pw.setValue("");
        }
        super.removed();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float a) {
        extractPanorama(g, a);
        int i = (int) (((System.nanoTime() - start) / 40_000_000) % frames.length);
        g.nextStratum();
        g.blit(frames[i], x + Math.round(102 * sc), y + Math.round(25 * sc),
                x + Math.round(494 * sc), y + Math.round(288 * sc), 0.082f, 0.918f, 0f, 1f);
        g.nextStratum();
        g.blit(title, x + Math.round(248 * sc), y + Math.round(26 * sc),
                x + Math.round(332 * sc), y + Math.round(70 * sc), 0f, 1f, 0f, 1f);
        g.blit(brand, x + Math.round(93 * sc), y + Math.round(267 * sc),
                x + Math.round(210 * sc), y + Math.round(288 * sc), 0f, 1f, 0f, 1f);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float a) {
        super.extractRenderState(g, mx, my, a);
        if (MoonAuth.session != null) {
            g.centeredText(font, "Signed in to Moon Client", width / 2, y + Math.round(153 * sc), 0xff58364d);
        } else if (!msg.isEmpty()) {
            g.centeredText(font, msg, width / 2, height - 13, 0xfffff2fc);
        }
    }
}

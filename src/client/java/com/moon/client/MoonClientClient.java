package com.moon.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.Identifier;

public class MoonClientClient implements ClientModInitializer {
    private static KeyMapping openKeys;

    @Override
    public void onInitializeClient() {
        MoonConfig.load();

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("moon-client", "keystrokes"), MoonKeystrokes::hud);

        openKeys = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.moon-client.keystrokes",
                InputConstants.KEY_RSHIFT, KeyMapping.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKeys.consumeClick()) {
                if (mc.gui.screen() == null) {
                    mc.gui.setScreen(new MoonKeysScreen(null));
                }
            }
        });

        ScreenEvents.AFTER_INIT.register((mc, sc, w, h) -> {
            if (sc instanceof TitleScreen) {
                Screens.getWidgets(sc).add(new MoonButton(w - 104, 4, 100, 20,
                        "Moon Client", MoonScreen.brand, () -> mc.gui.setScreen(new MoonScreen(sc))));
            }
        });
    }
}

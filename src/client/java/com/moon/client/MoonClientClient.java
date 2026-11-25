package com.moon.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screens.TitleScreen;

public class MoonClientClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((mc, sc, w, h) -> {
            if (sc instanceof TitleScreen) {
                Screens.getWidgets(sc).add(new MoonButton(w - 104, 4, 100, 20,
                        "Moon Client", MoonScreen.brand, () -> mc.gui.setScreen(new MoonScreen(sc))));
            }
        });
    }
}

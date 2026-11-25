package com.moon.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Saved settings in config/moon-client.json */
final class MoonConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("moon-client.json");
    static MoonConfig current = new MoonConfig();

    boolean keystrokes = true;
    boolean mouseKeys = true;
    boolean spaceBar = true;
    /** Position of the top-left corner, as a fraction of the screen (0..1). */
    float keysX = 0.01f;
    float keysY = 0.02f;
    float keysScale = 1.0f;

    static void load() {
        try {
            if (Files.exists(FILE)) {
                MoonConfig c = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), MoonConfig.class);
                if (c != null) {
                    current = c;
                }
            }
        } catch (IOException | RuntimeException ignored) {
            current = new MoonConfig();
        }
        current.keysScale = Math.max(0.5f, Math.min(2.5f, current.keysScale));
    }

    static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(current), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }
}

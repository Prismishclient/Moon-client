package com.moon.client.mixin;

import com.moon.client.MoonKeystrokes;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Remembers key taps so a very short tap still lights the key up on the HUD. */
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void moon$keyPress(long handle, int action, KeyEvent event, CallbackInfo ci) {
        if (action == 1) {
            MoonKeystrokes.onKey(event);
        }
    }
}

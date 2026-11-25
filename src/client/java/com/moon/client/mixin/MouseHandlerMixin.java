package com.moon.client.mixin;

import com.moon.client.MoonKeystrokes;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Counts every mouse press, even very fast clicks between two frames (for CPS). */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void moon$onButton(long handle, MouseButtonInfo info, int action, CallbackInfo ci) {
        if (action == 1) {
            MoonKeystrokes.onMouse(info);
        }
    }
}

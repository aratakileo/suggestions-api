package io.github.aratakileo.suggestionsapi.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class RenderUtil {
    public static void renderTexture(
            GuiGraphics guiGraphics,
            ResourceLocation resourceLocation,
            int x,
            int y,
            int width,
            int height
    ) {
        RenderSystem.enableBlend();

        guiGraphics.blit(resourceLocation, x, y, 0f, 0f, width, height, width, height);
        RenderSystem.disableBlend();
    }
}

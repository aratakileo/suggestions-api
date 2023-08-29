package io.github.aratakileo.suggestionsapi.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public final class RenderUtil {
    public static void renderFittedCenterTexture(
            @NotNull GuiGraphics guiGraphics,
            @NotNull ResourceLocation resourceLocation,
            int x,
            int y,
            int sourceWidth,
            int sourceHeight,
            int renderWidth,
            int renderHeight
    ) {
        if (sourceWidth < sourceHeight) {
            final var oldRenderWidth = renderWidth;

            renderWidth *= ((float) sourceWidth / sourceHeight);
            x += (oldRenderWidth - renderWidth) / 2;
        }
        else if (sourceHeight < sourceWidth) {
            final var oldRenderHeight = renderHeight;

            renderHeight *= ((float) sourceHeight / sourceWidth);
            y += (oldRenderHeight - renderHeight) / 2;
        }

        renderTexture(guiGraphics, resourceLocation, x, y, renderWidth, renderHeight);
    }

    public static void renderTexture(
            @NotNull GuiGraphics guiGraphics,
            @NotNull ResourceLocation resourceLocation,
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

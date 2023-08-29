package io.github.aratakileo.suggestionsapi.suggestion;

import io.github.aratakileo.suggestionsapi.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class IconSuggestion extends SimpleSuggestion implements SuggestionRenderer {
    public static final int RENDER_ICON_SIZE = 8;

    private final boolean alwaysShow;
    private final ResourceLocation iconResource;
    private final int iconWidth, iconHeight;

    public IconSuggestion(
            @NotNull String suggestionText,
            @NotNull ResourceLocation iconResource,
            int iconWidth,
            int iconHeight,
            boolean alwaysShow
    ) {
        super(suggestionText);

        this.iconResource = iconResource;
        this.iconWidth = iconWidth;
        this.iconHeight = iconHeight;
        this.alwaysShow = alwaysShow;
    }

    @Override
    public int getWidth() {
        return iconWidth + Minecraft.getInstance().font.width(suggestionText) + 6;
    }

    @Override
    public int renderContent(
            @NotNull GuiGraphics guiGraphics,
            @NotNull Font font,
            int x,
            int y,
            int color
    ) {
        RenderUtil.renderFittedCenterTexture(
                guiGraphics,
                iconResource,
                x + 1,
                y,
                iconWidth,
                iconHeight,
                RENDER_ICON_SIZE,
                RENDER_ICON_SIZE
        );

        return guiGraphics.drawString(
                font,
                suggestionText,
                x + iconWidth + 3,
                y,
                color
        );
    }

    @Override
    public boolean shouldShowFor(String currentExpression) {
        return alwaysShow || super.shouldShowFor(currentExpression);
    }
}

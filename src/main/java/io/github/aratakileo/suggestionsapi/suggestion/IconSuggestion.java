package io.github.aratakileo.suggestionsapi.suggestion;

import io.github.aratakileo.suggestionsapi.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class IconSuggestion extends SimpleSuggestion implements SuggestionRenderer {
    public static final int DEFAULT_ICON_SIZE = 8;

    private final ResourceLocation iconResource;
    private final int iconWidth, iconHeight;

    public IconSuggestion(
            @NotNull String suggestionText,
            @NotNull ResourceLocation iconResource,
            int iconWidth,
            int iconHeight
    ) {
        super(suggestionText);

        this.iconResource = iconResource;
        this.iconWidth = iconWidth;
        this.iconHeight = iconHeight;
    }

    public IconSuggestion(@NotNull String suggestionText, @NotNull ResourceLocation iconResource) {
        this(suggestionText, iconResource, DEFAULT_ICON_SIZE, DEFAULT_ICON_SIZE);
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
        RenderUtil.renderTexture(guiGraphics, iconResource, x + 1, y, iconWidth, iconHeight);

        return guiGraphics.drawString(
                font,
                suggestionText,
                x + iconWidth + 3,
                y,
                color
        );
    }
}

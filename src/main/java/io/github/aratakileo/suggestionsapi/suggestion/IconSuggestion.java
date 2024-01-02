package io.github.aratakileo.suggestionsapi.suggestion;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.aratakileo.suggestionsapi.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class IconSuggestion implements Suggestion, SuggestionRenderer {
    public static final int RENDER_ICON_SIZE = 8;

    protected final String suggestionText;
    private final ResourceLocation iconResource;
    private final int iconWidth, iconHeight;

    private boolean isIconOnLeft;

    public IconSuggestion(
            @NotNull String suggestionText,
            @NotNull ResourceLocation iconResource,
            int iconWidth,
            int iconHeight,
            boolean isIconOnLeft
    ) {
        this.suggestionText = suggestionText;
        this.iconResource = iconResource;
        this.iconWidth = iconWidth;
        this.iconHeight = iconHeight;
        this.isIconOnLeft = isIconOnLeft;
    }

    public IconSuggestion(
            @NotNull String suggestionText,
            @NotNull ResourceLocation iconResource,
            int iconWidth,
            int iconHeight
    ) {
        this(suggestionText, iconResource, iconWidth, iconHeight, true);
    }

    @Override
    public int getWidth() {
        return RENDER_ICON_SIZE + Minecraft.getInstance().font.width(suggestionText) + 6;
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
                x + (isIconOnLeft ? 1 : Minecraft.getInstance().font.width(suggestionText) + 3),
                y,
                iconWidth,
                iconHeight,
                RENDER_ICON_SIZE,
                RENDER_ICON_SIZE
        );

        return guiGraphics.drawString(
                font,
                suggestionText,
                x + (isIconOnLeft ? RENDER_ICON_SIZE + 3 : 1),
                y,
                color
        );
    }

    @Override
    public String getSuggestionText() {
        return suggestionText;
    }

    public boolean isIconOnLeft() {
        return isIconOnLeft;
    }

    public void setIconOnLeft(boolean iconOnLeft) {
        isIconOnLeft = iconOnLeft;
    }

    public static IconSuggestion usingIconSize(
            @NotNull String suggestionText,
            @NotNull ResourceLocation iconResource,
            boolean isIconOnLeft
    ) {
        try {
            final var resource = Minecraft.getInstance().getResourceManager().getResource(iconResource).get();
            final var nativeImage = NativeImage.read(resource.open());

            return new IconSuggestion(
                    suggestionText,
                    iconResource,
                    nativeImage.getWidth(),
                    nativeImage.getHeight(),
                    isIconOnLeft
            );
        } catch (IOException ignore) {}

        return null;
    }

    public static IconSuggestion usingIconSize(
            @NotNull String suggestionText,
            @NotNull ResourceLocation iconResource
    ) {
        return usingIconSize(suggestionText, iconResource, true);
    }
}

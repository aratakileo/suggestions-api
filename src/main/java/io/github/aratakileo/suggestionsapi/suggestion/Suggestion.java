package io.github.aratakileo.suggestionsapi.suggestion;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public interface Suggestion {
    String getSuggestionText();

    default boolean shouldShowFor(@NotNull String currentExpression) {
        return getSuggestionText().toLowerCase().startsWith(currentExpression.toLowerCase());
    }

    @NotNull
    static Suggestion simple(@NotNull String suggestionText) {
        return () -> suggestionText;
    }

    @NotNull
    static Suggestion alwaysShown(@NotNull String suggestionText) {
        return new Suggestion() {
            @Override
            public String getSuggestionText() {
                return suggestionText;
            }

            @Override
            public boolean shouldShowFor(@NotNull String currentExpression) {
                return true;
            }
        };
    }

    @Nullable
    static IconSuggestion withIcon(
            @NotNull String suggestionText,
            @NotNull ResourceLocation icon
    ) {
        try {
            final var resource = Minecraft.getInstance().getResourceManager().getResource(icon).get();
            final var nativeImage = NativeImage.read(resource.open());

            return new IconSuggestion(suggestionText, icon, nativeImage.getWidth(), nativeImage.getHeight());
        } catch (IOException ignore) {}

        return null;
    }

    @Nullable
    static IconSuggestion alwaysShownWithIcon(
            @NotNull String suggestionText,
            @NotNull ResourceLocation icon
    ) {
        try {
            final var resource = Minecraft.getInstance().getResourceManager().getResource(icon).get();
            final var nativeImage = NativeImage.read(resource.open());

            return new IconSuggestion(suggestionText, icon, nativeImage.getWidth(), nativeImage.getHeight()) {
                @Override
                public boolean shouldShowFor(@NotNull String currentExpression) {
                    return true;
                }
            };
        } catch (IOException ignore) {}

        return null;
    }
}

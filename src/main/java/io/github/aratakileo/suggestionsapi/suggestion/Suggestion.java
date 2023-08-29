package io.github.aratakileo.suggestionsapi.suggestion;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public interface Suggestion {
    String getSuggestionText();

    default boolean shouldShowFor(String currentExpression) {
        return getSuggestionText().toLowerCase().startsWith(currentExpression.toLowerCase());
    }

    static @NotNull SimpleSuggestion simple(@NotNull String suggestionText) {
        return new SimpleSuggestion(suggestionText);
    }

    static @NotNull AlwaysShownSuggestion alwaysShown(@NotNull String suggestionText) {
        return new AlwaysShownSuggestion(suggestionText);
    }

    static @Nullable IconSuggestion withIcon(
            @NotNull String suggestionText,
            @NotNull ResourceLocation icon,
            boolean alwaysShow
    ) {
        try {
            final var resource = Minecraft.getInstance().getResourceManager().getResource(icon).get();
            final var nativeImage = NativeImage.read(resource.open());

            return new IconSuggestion(
                    suggestionText,
                    icon, nativeImage.getWidth(),
                    nativeImage.getHeight(),
                    alwaysShow
            );
        } catch (IOException ignore) {}

        return null;
    }
}

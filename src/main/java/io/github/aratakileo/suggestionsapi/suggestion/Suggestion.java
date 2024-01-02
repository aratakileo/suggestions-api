package io.github.aratakileo.suggestionsapi.suggestion;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.BiFunction;

public interface Suggestion {
    String getSuggestionText();

    default boolean shouldShowFor(@NotNull String currentExpression) {
        return DEFAULT_CONDITION.apply(getSuggestionText(), currentExpression);
    }

    @NotNull
    static Suggestion simple(@NotNull String suggestionText) {
        return () -> suggestionText;
    }

    @NotNull
    static Suggestion simple(
            @NotNull String suggestionText,
            @NotNull BiFunction<@NotNull String, @NotNull String, @NotNull Boolean> showCondition
    ) {
        return new Suggestion() {
            @Override
            public String getSuggestionText() {
                return suggestionText;
            }

            @Override
            public boolean shouldShowFor(@NotNull String currentExpression) {
                return showCondition.apply(suggestionText, currentExpression);
            }
        };
    }

    @NotNull
    static Suggestion alwaysShown(@NotNull String suggestionText) {
        return simple(suggestionText, ALWAYS_SHOW_CONDITION);
    }

    @Nullable
    static IconSuggestion withIcon(
            @NotNull String suggestionText,
            @NotNull ResourceLocation icon
    ) {
        return IconSuggestion.usingIconSize(suggestionText, icon);
    }

    @Nullable
    static IconSuggestion withIcon(
            @NotNull String suggestionText,
            @NotNull ResourceLocation icon,
            boolean isIconOnLeft
    ) {
        return IconSuggestion.usingIconSize(suggestionText, icon, isIconOnLeft);
    }

    @Nullable
    static IconSuggestion withIcon(
            @NotNull String suggestionText,
            @NotNull ResourceLocation icon,
            @NotNull BiFunction<@NotNull String, @NotNull String, @NotNull Boolean> showCondition
    ) {
        return withIcon(suggestionText, icon, showCondition, true);
    }

    @Nullable
    static IconSuggestion withIcon(
            @NotNull String suggestionText,
            @NotNull ResourceLocation icon,
            @NotNull BiFunction<@NotNull String, @NotNull String, @NotNull Boolean> showCondition,
            boolean isIconOnLeft
    ) {
        try {
            final var resource = Minecraft.getInstance().getResourceManager().getResource(icon).get();
            final var nativeImage = NativeImage.read(resource.open());

            return new IconSuggestion(
                    suggestionText,
                    icon,
                    nativeImage.getWidth(),
                    nativeImage.getHeight(),
                    isIconOnLeft
            ) {
                @Override
                public boolean shouldShowFor(@NotNull String currentExpression) {
                    return showCondition.apply(getSuggestionText(), currentExpression);
                }
            };
        } catch (IOException ignore) {}

        return null;
    }

    @Nullable
    static IconSuggestion alwaysShownWithIcon(
            @NotNull String suggestionText,
            @NotNull ResourceLocation icon
    ) {
        return withIcon(suggestionText, icon, ALWAYS_SHOW_CONDITION);
    }

    @Nullable
    static IconSuggestion alwaysShownWithIcon(
            @NotNull String suggestionText,
            @NotNull ResourceLocation icon,
            boolean isIconOnLeft
    ) {
        return withIcon(suggestionText, icon, ALWAYS_SHOW_CONDITION, isIconOnLeft);
    }

    BiFunction<
            @NotNull String,
            @NotNull String,
            @NotNull Boolean
            > ALWAYS_SHOW_CONDITION = (suggestionText, currentExpression) -> true,
            DEFAULT_CONDITION = (suggestionText, currentExpression) -> suggestionText.toLowerCase()
                    .startsWith(currentExpression.toLowerCase());
}

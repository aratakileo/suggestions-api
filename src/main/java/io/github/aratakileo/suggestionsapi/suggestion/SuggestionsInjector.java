package io.github.aratakileo.suggestionsapi.suggestion;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface SuggestionsInjector {
    @Nullable <T extends Suggestion> List<T> getSuggestions(@NotNull String currentExpression);

    default int getStartOffset() {
        return 0;
    }

    default boolean isIsolated() {
        return false;
    }
}

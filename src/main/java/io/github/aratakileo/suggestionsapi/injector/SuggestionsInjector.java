package io.github.aratakileo.suggestionsapi.injector;

import io.github.aratakileo.suggestionsapi.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface SuggestionsInjector extends Injector {
    @Nullable List<Suggestion> getSuggestions(@NotNull String currentExpression);
}

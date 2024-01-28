package io.github.aratakileo.suggestionsapi.injector;

import io.github.aratakileo.suggestionsapi.util.StringContainer;
import io.github.aratakileo.suggestionsapi.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface SuggestionsInjector extends InputRelatedInjector {
    @Nullable List<Suggestion> getSuggestions(@NotNull StringContainer stringContainer);
}

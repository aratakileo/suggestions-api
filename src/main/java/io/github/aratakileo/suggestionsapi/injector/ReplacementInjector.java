package io.github.aratakileo.suggestionsapi.injector;

import io.github.aratakileo.suggestionsapi.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ReplacementInjector extends Injector {
    @Nullable Suggestion getReplace(@NotNull String nonApiSuggestion);
}

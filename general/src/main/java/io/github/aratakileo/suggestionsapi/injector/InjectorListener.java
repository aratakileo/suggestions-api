package io.github.aratakileo.suggestionsapi.injector;

import io.github.aratakileo.suggestionsapi.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;

public interface InjectorListener extends Injector {
    default void onSessionInited() {}
    default void onSuggestionSelected(@NotNull Suggestion suggestion) {}
}

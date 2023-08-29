package io.github.aratakileo.suggestionsapi.suggestion;

import org.jetbrains.annotations.NotNull;

public class AlwaysShownSuggestion extends SimpleSuggestion {
    public AlwaysShownSuggestion(@NotNull String suggestionText) {
        super(suggestionText);
    }

    @Override
    public boolean shouldShowFor(String currentExpression) {
        return true;
    }
}

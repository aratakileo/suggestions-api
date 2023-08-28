package io.github.aratakileo.suggestionsapi.suggestion;

import org.jetbrains.annotations.NotNull;

public class SimpleSuggestion implements Suggestion {
    protected final String suggestionText;

    public SimpleSuggestion(@NotNull String suggestionText) {
        this.suggestionText = suggestionText;
    }

    @Override
    public String getSuggestionText() {
        return suggestionText;
    }
}

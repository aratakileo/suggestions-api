package io.github.aratakileo.suggestionsapi.suggestion;

public interface Suggestion {
    String getSuggestionText();

    default boolean shouldShowFor(String currentExpression) {
        return getSuggestionText().toLowerCase().startsWith(currentExpression.toLowerCase());
    }
}

package io.github.aratakileo.suggestionsapi.suggestion;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.regex.Pattern;

public abstract class SimpleSuggestionsInjector implements SuggestionsInjector {
    private final Pattern pattern;
    private int startOffset = 0;

    public SimpleSuggestionsInjector(@NotNull Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public @Nullable <T extends Suggestion> List<T> getSuggestions(@NotNull String currentExpression) {
        final var matcher = pattern.matcher(currentExpression);

        if (!matcher.find()) return null;

        startOffset = matcher.start();
        var end = matcher.end();

        while (matcher.find()) {
            startOffset = matcher.start();
            end = matcher.end();
        }

        return end != currentExpression.length() ? null : getUncheckedSuggestions(currentExpression);
    }

    abstract public @Nullable <T extends Suggestion> List<T> getUncheckedSuggestions(@NotNull String currentExpression);

    @Override
    public int getStartOffset() {
        return startOffset;
    }
}

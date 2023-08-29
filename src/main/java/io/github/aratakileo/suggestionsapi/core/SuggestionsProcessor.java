package io.github.aratakileo.suggestionsapi.core;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestions;
import io.github.aratakileo.suggestionsapi.suggestion.DynamicSuggestionsInjector;
import io.github.aratakileo.suggestionsapi.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class SuggestionsProcessor {
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("(\\s+)");

    private final String textUptoCursor;
    private final int wordStart;
    private final HashMap<String, Suggestion> suggestions;
    private final ArrayList<DynamicSuggestionsInjector> dynamicSuggestionsInjectors;
    private final Consumer<HashMap<String, Suggestion>> dynamicSuggestionsConsumer;

    private SuggestionsProcessor(
            @NotNull HashMap<String, Suggestion> suggestions,
            @NotNull ArrayList<DynamicSuggestionsInjector> dynamicSuggestionsInjectors,
            @NotNull Consumer<HashMap<String, Suggestion>> dynamicSuggestionsConsumer,
            @NotNull String textUptoCursor,
            int wordStart
    ) {
        this.textUptoCursor = textUptoCursor;
        this.suggestions = suggestions;
        this.dynamicSuggestionsInjectors = dynamicSuggestionsInjectors;
        this.dynamicSuggestionsConsumer = dynamicSuggestionsConsumer;
        this.wordStart = wordStart;
    }

    public @Nullable CompletableFuture<Suggestions> getPendingSuggestions() {
        var minOffset = -1;

        final var currentExpression = textUptoCursor.substring(wordStart);
        final var dynamicSuggestionsBuffer = new HashMap<DynamicSuggestionsInjector, Collection<Suggestion>>();

        for (final var dynamicSuggestionsInjector: dynamicSuggestionsInjectors) {
            final var suggestions = dynamicSuggestionsInjector.getSuggestions(currentExpression);

            if (suggestions == null || suggestions.isEmpty()) continue;

            dynamicSuggestionsBuffer.put(dynamicSuggestionsInjector, suggestions);

            if (minOffset != -1) {
                minOffset = Math.min(minOffset, dynamicSuggestionsInjector.getExpressionStartOffset());
                continue;
            }

            if (dynamicSuggestionsInjector.getExpressionStartOffset() == 0) {
                minOffset = 0;
                break;
            }

            minOffset = Integer.MAX_VALUE;
        }

        final var applicableMojangSuggestions = new ArrayList<com.mojang.brigadier.suggestion.Suggestion>();

        var dynamicSuggestions = new HashMap<String, Suggestion>();

        for (final var dynamicSuggestionsEntry: dynamicSuggestionsBuffer.entrySet()) {
            final var dynamicSuggestionsInjector = dynamicSuggestionsEntry.getKey();

            if (
                    minOffset != -1
                            && dynamicSuggestionsInjector.isIsolated()
                            && dynamicSuggestionsInjector.getExpressionStartOffset() > minOffset
            ) continue;

            for (final var suggestion: dynamicSuggestionsEntry.getValue()) {
                final var offset = dynamicSuggestionsInjector.getExpressionStartOffset();

                if (!suggestion.shouldShowFor(currentExpression.substring(offset))) continue;

                applicableMojangSuggestions.add(new com.mojang.brigadier.suggestion.Suggestion(
                        StringRange.between(
                                wordStart + offset,
                                textUptoCursor.length()
                        ),
                        suggestion.getSuggestionText()
                ));

                dynamicSuggestions.put(suggestion.getSuggestionText(), suggestion);
            }
        }

        dynamicSuggestionsConsumer.accept(dynamicSuggestions);

        suggestions.forEach((suggestionText, suggestion) -> {
            if (suggestion.shouldShowFor(currentExpression))
                applicableMojangSuggestions.add(new com.mojang.brigadier.suggestion.Suggestion(
                        StringRange.between(wordStart, textUptoCursor.length()),
                        suggestionText
                ));
        });

        if (suggestions.isEmpty()) return null;

        return CompletableFuture.completedFuture(Suggestions.create(textUptoCursor, applicableMojangSuggestions));
    }

    public static @Nullable SuggestionsProcessor from(
            @NotNull HashMap<String, Suggestion> suggestions,
            @NotNull ArrayList<DynamicSuggestionsInjector> dynamicSuggestionsInjectors,
            @NotNull Consumer<HashMap<String, Suggestion>> dynamicSuggestionsConsumer,
            @NotNull String textUptoCursor
    ) {
        if (textUptoCursor.isEmpty()) return null;

        final var uptoCursorMatcher = WHITESPACE_PATTERN.matcher(textUptoCursor);

        var wordStart = 0;

        while (uptoCursorMatcher.find()) wordStart = uptoCursorMatcher.end();

        if (wordStart == textUptoCursor.length()) return null;

        return new SuggestionsProcessor(
                suggestions,
                dynamicSuggestionsInjectors,
                dynamicSuggestionsConsumer,
                textUptoCursor,
                wordStart
        );
    }

    public static class Builder {
        private final HashMap<String, Suggestion> suggestions;
        private final ArrayList<DynamicSuggestionsInjector> dynamicSuggestionsInjectors;
        private final Consumer<HashMap<String, Suggestion>> dynamicSuggestionsConsumer;

        private String textUptoCursor;

        public Builder(
                @NotNull HashMap<String, Suggestion> suggestions,
                @NotNull ArrayList<DynamicSuggestionsInjector> dynamicSuggestionsInjectors,
                @NotNull Consumer<HashMap<String, Suggestion>> dynamicSuggestionsConsumer
        ) {
            this.suggestions = suggestions;
            this.dynamicSuggestionsInjectors = dynamicSuggestionsInjectors;
            this.dynamicSuggestionsConsumer = dynamicSuggestionsConsumer;
        }

        public Builder setTextUptoCursor(@NotNull String textUptoCursor) {
            this.textUptoCursor = textUptoCursor;
            return this;
        }

        public @Nullable SuggestionsProcessor build() {
            return SuggestionsProcessor.from(
                    suggestions,
                    dynamicSuggestionsInjectors,
                    dynamicSuggestionsConsumer,
                    textUptoCursor
            );
        }
    }
}

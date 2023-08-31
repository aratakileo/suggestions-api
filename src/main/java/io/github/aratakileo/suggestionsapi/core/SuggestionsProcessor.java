package io.github.aratakileo.suggestionsapi.core;

import com.mojang.brigadier.context.StringRange;
import io.github.aratakileo.suggestionsapi.suggestion.AsyncInjector;
import io.github.aratakileo.suggestionsapi.suggestion.Injector;
import io.github.aratakileo.suggestionsapi.suggestion.Suggestion;
import io.github.aratakileo.suggestionsapi.suggestion.SuggestionsInjector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class SuggestionsProcessor {
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("(\\s+)");
    private static final Logger LOGGER = LoggerFactory.getLogger(SuggestionsProcessor.class);

    private final String textUptoCursor;
    private final int wordStart;
    private final HashMap<String, Suggestion> suggestions;
    private final ArrayList<Injector> injectors;
    private final Consumer<HashMap<String, Suggestion>> dynamicSuggestionsConsumer;
    private final BiConsumer<String, List<com.mojang.brigadier.suggestion.Suggestion>> newSuggestionsApplier;

    private final ArrayList<Callable<Void>> tasks = new ArrayList<>();

    private SuggestionsProcessor(
            @NotNull HashMap<String, Suggestion> suggestions,
            @NotNull ArrayList<Injector> injectors,
            @NotNull Consumer<HashMap<String, Suggestion>> dynamicSuggestionsConsumer,
            @NotNull BiConsumer<String, List<com.mojang.brigadier.suggestion.Suggestion>> newSuggestionsApplier,
            @NotNull String textUptoCursor,
            int wordStart
    ) {
        this.textUptoCursor = textUptoCursor;
        this.suggestions = suggestions;
        this.injectors = injectors;
        this.dynamicSuggestionsConsumer = dynamicSuggestionsConsumer;
        this.newSuggestionsApplier = newSuggestionsApplier;
        this.wordStart = wordStart;
    }

    public boolean process() {
        var minOffset = -1;

        final var currentExpression = textUptoCursor.substring(wordStart);
        final var suggestionsInjectorsBuffer = new HashMap<SuggestionsInjector, Collection<Suggestion>>();
        final var asyncInjectorsBuffer = new ArrayList<AsyncInjector>();

        for (final var injector: injectors) {
            if (injector instanceof SuggestionsInjector suggestionsInjector) {
                final var suggestions = suggestionsInjector.getSuggestions(currentExpression);

                if (suggestions == null || suggestions.isEmpty()) continue;

                suggestionsInjectorsBuffer.put(suggestionsInjector, suggestions);
            } else if (injector instanceof AsyncInjector asyncInjector) {
                final var willApplySuggestions = asyncInjector.initAsyncApplier(
                        currentExpression,
                        suggestionList -> {
                            if (suggestionList == null || suggestionList.isEmpty()) return;

                            final var mojangSuggestions = new ArrayList<com.mojang.brigadier.suggestion.Suggestion>();
                            final var offset = injector.getStartOffset();

                            suggestionList.forEach(suggestion -> {
                                if (suggestion.shouldShowFor(currentExpression.substring(offset)))
                                    mojangSuggestions.add(new com.mojang.brigadier.suggestion.Suggestion(
                                            StringRange.between(wordStart + offset, textUptoCursor.length()),
                                            suggestion.getSuggestionText()
                                    ));
                            });

                            if (mojangSuggestions.isEmpty()) return;

                            newSuggestionsApplier.accept(textUptoCursor, mojangSuggestions);
                        }
                );

                if (!willApplySuggestions) continue;

                asyncInjectorsBuffer.add(asyncInjector);
            } else {
                LOGGER.error("Invalid Injector! (" + injector + ")");

                continue;
            }

            if (injector.getStartOffset() == 0) {
                minOffset = 0;
                continue;
            }

            if (minOffset != -1) {
                minOffset = Math.min(minOffset, injector.getStartOffset());
                continue;
            }

            minOffset = injector.getStartOffset();
        }

        final var applicableMojangSuggestions = new ArrayList<com.mojang.brigadier.suggestion.Suggestion>();

        var dynamicSuggestions = new HashMap<String, Suggestion>();

        for (final var injectorEntry: suggestionsInjectorsBuffer.entrySet()) {
            final var injector = injectorEntry.getKey();

            if (minOffset != -1 && injector.isIsolated() && injector.getStartOffset() > minOffset) continue;

            for (final var suggestion: injectorEntry.getValue()) {
                final var offset = injector.getStartOffset();

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

        var hasUsedAsyncInjector = false;

        for (final var injector: asyncInjectorsBuffer) {
            if (minOffset != -1 && injector.isIsolated() && injector.getStartOffset() > minOffset) continue;

            hasUsedAsyncInjector = true;

            injector.runAsyncApplier();
        }

        if (applicableMojangSuggestions.isEmpty() && !hasUsedAsyncInjector) return false;

        newSuggestionsApplier.accept(textUptoCursor, applicableMojangSuggestions);

        return true;
    }

    public static @Nullable SuggestionsProcessor from(
            @NotNull HashMap<String, Suggestion> suggestions,
            @NotNull ArrayList<Injector> injectors,
            @NotNull Consumer<HashMap<String, Suggestion>> dynamicSuggestionsConsumer,
            @NotNull BiConsumer<String, List<com.mojang.brigadier.suggestion.Suggestion>> newSuggestionsApplier,
            @NotNull String textUptoCursor
    ) {
        if (textUptoCursor.isEmpty()) return null;

        final var uptoCursorMatcher = WHITESPACE_PATTERN.matcher(textUptoCursor);

        var wordStart = 0;

        while (uptoCursorMatcher.find()) wordStart = uptoCursorMatcher.end();

        if (wordStart == textUptoCursor.length()) return null;

        return new SuggestionsProcessor(
                suggestions,
                injectors,
                dynamicSuggestionsConsumer,
                newSuggestionsApplier,
                textUptoCursor,
                wordStart
        );
    }

    public static class Builder {
        private final HashMap<String, Suggestion> suggestions;
        private final ArrayList<Injector> injectors;
        private final Consumer<HashMap<String, Suggestion>> dynamicSuggestionsConsumer;

        private String textUptoCursor;
        private BiConsumer<String, List<com.mojang.brigadier.suggestion.Suggestion>> newSuggestionsApplier;

        public Builder(
                @NotNull HashMap<String, Suggestion> suggestions,
                @NotNull ArrayList<Injector> injectors,
                @NotNull Consumer<HashMap<String, Suggestion>> dynamicSuggestionsConsumer
        ) {
            this.suggestions = suggestions;
            this.injectors = injectors;
            this.dynamicSuggestionsConsumer = dynamicSuggestionsConsumer;
        }

        public Builder setOtherValues(
                @NotNull String textUptoCursor,
                @NotNull BiConsumer<String, List<com.mojang.brigadier.suggestion.Suggestion>> newSuggestionsApplier
        ) {
            this.textUptoCursor = textUptoCursor;
            this.newSuggestionsApplier = newSuggestionsApplier;
            return this;
        }

        public @Nullable SuggestionsProcessor build() {
            return SuggestionsProcessor.from(
                    suggestions,
                    injectors,
                    dynamicSuggestionsConsumer,
                    newSuggestionsApplier,
                    textUptoCursor
            );
        }
    }
}

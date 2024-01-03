package io.github.aratakileo.suggestionsapi.core;

import com.mojang.brigadier.context.StringRange;
import io.github.aratakileo.suggestionsapi.SuggestionsAPI;
import io.github.aratakileo.suggestionsapi.injector.AsyncInjector;
import io.github.aratakileo.suggestionsapi.injector.Injector;
import io.github.aratakileo.suggestionsapi.injector.InjectorListener;
import io.github.aratakileo.suggestionsapi.suggestion.Suggestion;
import io.github.aratakileo.suggestionsapi.injector.SuggestionsInjector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class SuggestionsProcessor {
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("(\\s+)");
    private static final Logger LOGGER = LoggerFactory.getLogger(SuggestionsProcessor.class);
    private static final HashMap<AsyncInjector, CompletableFuture<Void>> asyncProcessors = new HashMap<>();

    private static SuggestionsProcessor instance = null;

    private final String textUptoCursor;
    private final int wordStart;
    private final HashMap<String, Suggestion> suggestions;
    private final ArrayList<Injector> injectors;
    private final Consumer<HashMap<String, Suggestion>> tempSuggestionsConsumer;
    private final BiConsumer<String, List<com.mojang.brigadier.suggestion.Suggestion>> newSuggestionsApplier;

    private HashMap<InjectorListener, List<Suggestion>> usedInjectorListeners = new HashMap<>();

    private SuggestionsProcessor(
            @NotNull HashMap<String, Suggestion> suggestions,
            @NotNull ArrayList<Injector> injectors,
            @NotNull Consumer<HashMap<String, Suggestion>> tempSuggestionsConsumer,
            @NotNull BiConsumer<String, List<com.mojang.brigadier.suggestion.Suggestion>> newSuggestionsApplier,
            @NotNull String textUptoCursor,
            int wordStart
    ) {
        this.textUptoCursor = textUptoCursor;
        this.suggestions = suggestions;
        this.injectors = injectors;
        this.tempSuggestionsConsumer = tempSuggestionsConsumer;
        this.newSuggestionsApplier = newSuggestionsApplier;
        this.wordStart = wordStart;

        instance = this;
    }

    public boolean process() {
        var minOffset = -1;

        final var currentExpression = textUptoCursor.substring(wordStart);
        final var suggestionsInjectorsBuffer = new HashMap<SuggestionsInjector, Collection<Suggestion>>();
        final var asyncInjectorsBuffer = new HashMap<AsyncInjector, Runnable>();

        for (final var injector: injectors) {
            var isActiveInjector = false;
            var isValidInjector = false;

            if (injector instanceof SuggestionsInjector suggestionsInjector) {
                isValidInjector = true;

                final var suggestions = suggestionsInjector.getSuggestions(currentExpression);

                if (Objects.nonNull(suggestions) && !suggestions.isEmpty()) {
                    suggestionsInjectorsBuffer.put(suggestionsInjector, suggestions);

                    if (injector instanceof InjectorListener injectorListener)
                        usedInjectorListeners.put(injectorListener, suggestions);

                    isActiveInjector = true;
                }
            }

            if (injector instanceof AsyncInjector asyncInjector) {
                isValidInjector = true;

                final var asyncApplier = asyncInjector.getAsyncApplier(currentExpression);

                if (Objects.nonNull(asyncApplier)) {
                    asyncInjectorsBuffer.put(asyncInjector, () -> {
                        final var suggestionList = asyncApplier.get();

                        if (Objects.isNull(suggestionList) || suggestionList.isEmpty()) {
                            asyncProcessors.remove(asyncInjector);
                            return;
                        }

                        final var mojangSuggestions = new ArrayList<com.mojang.brigadier.suggestion.Suggestion>();
                        final var offset = injector.getStartOffset();

                        suggestionList.forEach(suggestion -> {
                            if (suggestion.shouldShowFor(currentExpression.substring(offset)))
                                mojangSuggestions.add(new com.mojang.brigadier.suggestion.Suggestion(
                                        StringRange.between(wordStart + offset, textUptoCursor.length()),
                                        suggestion.getSuggestionText()
                                ));
                        });

                        if (mojangSuggestions.isEmpty()) {
                            asyncProcessors.remove(asyncInjector);
                            return;
                        }

                        newSuggestionsApplier.accept(textUptoCursor, mojangSuggestions);

                        if (injector instanceof InjectorListener injectorListener) {
                            if (usedInjectorListeners.containsKey(injectorListener))
                                usedInjectorListeners.get(injectorListener).addAll(suggestionList);
                            else usedInjectorListeners.put(injectorListener, suggestionList);
                        }
                    });

                    isActiveInjector = true;
                    asyncProcessors.remove(asyncInjector);
                }
            }

            if (!isValidInjector) {
                LOGGER.error("Invalid Injector! (" + injector + ")");

                continue;
            }

            if (!isActiveInjector) continue;

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

        var tempSuggestions = new HashMap<String, Suggestion>();

        for (final var injectorEntry: suggestionsInjectorsBuffer.entrySet()) {
            final var injector = injectorEntry.getKey();

            if (minOffset != -1 && !injector.isNestable() && injector.getStartOffset() > minOffset) continue;

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

                tempSuggestions.put(suggestion.getSuggestionText(), suggestion);
            }
        }

        tempSuggestionsConsumer.accept(tempSuggestions);

        suggestions.forEach((suggestionText, suggestion) -> {
            if (suggestion.shouldShowFor(currentExpression))
                applicableMojangSuggestions.add(new com.mojang.brigadier.suggestion.Suggestion(
                        StringRange.between(wordStart, textUptoCursor.length()),
                        suggestionText
                ));
        });

        var hasUsedAsyncInjector = false;

        for (final var injectorEntry: asyncInjectorsBuffer.entrySet()) {
            final var injector = injectorEntry.getKey();

            if (minOffset != -1 && !injector.isNestable() && injector.getStartOffset() > minOffset) continue;

            hasUsedAsyncInjector = true;

            CompletableFuture<Void> currentCompletableFuture;

            if (
                    asyncProcessors.containsKey(injector) && !(
                            currentCompletableFuture = asyncProcessors.get(injector)
                    ).isDone()
            )
                currentCompletableFuture.cancel(true);

            asyncProcessors.put(injector, CompletableFuture.runAsync(injectorEntry.getValue()));
        }

        if (applicableMojangSuggestions.isEmpty() && !hasUsedAsyncInjector) return false;

        newSuggestionsApplier.accept(textUptoCursor, applicableMojangSuggestions);

        return true;
    }

    public void selectSuggestion(@NotNull String suggestionText) {
        final var suggestion = SuggestionsAPI.getSuggestion(suggestionText);

        if (Objects.isNull(suggestion)) return;

        for (final var injectorListenerEntry: usedInjectorListeners.entrySet())
            if (injectorListenerEntry.getValue().contains(suggestion)) {
                injectorListenerEntry.getKey().onSuggestionSelected(suggestion);
                break;
            }
    }

    public void initSession() {
        for (final var injector: injectors)
            if (injector instanceof InjectorListener injectorListener) injectorListener.onSessionInited();
    }

    public static SuggestionsProcessor getInstance() {
        return instance;
    }

    public static @Nullable SuggestionsProcessor from(
            @NotNull HashMap<String, Suggestion> suggestions,
            @NotNull ArrayList<Injector> injectors,
            @NotNull Consumer<HashMap<String, Suggestion>> tempSuggestionsConsumer,
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
                tempSuggestionsConsumer,
                newSuggestionsApplier,
                textUptoCursor,
                wordStart
        );
    }

    public static class Builder {
        private final HashMap<String, Suggestion> suggestions;
        private final ArrayList<Injector> injectors;
        private final Consumer<HashMap<String, Suggestion>> tempSuggestionsConsumer;

        private String textUptoCursor;
        private BiConsumer<String, List<com.mojang.brigadier.suggestion.Suggestion>> newSuggestionsApplier;

        public Builder(
                @NotNull HashMap<String, Suggestion> suggestions,
                @NotNull ArrayList<Injector> injectors,
                @NotNull Consumer<HashMap<String, Suggestion>> tempSuggestionsConsumer
        ) {
            this.suggestions = suggestions;
            this.injectors = injectors;
            this.tempSuggestionsConsumer = tempSuggestionsConsumer;
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
                    tempSuggestionsConsumer,
                    newSuggestionsApplier,
                    textUptoCursor
            );
        }
    }
}

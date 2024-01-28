package io.github.aratakileo.suggestionsapi;

import com.mojang.brigadier.context.StringRange;
import io.github.aratakileo.suggestionsapi.injector.*;
import io.github.aratakileo.suggestionsapi.suggestion.Suggestion;
import io.github.aratakileo.suggestionsapi.util.StringContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class SuggestionsAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(SuggestionsAPI.class);
    private final static HashMap<@NotNull AsyncInjector, @NotNull CompletableFuture<@NotNull Void>> asyncProcessors
            = new HashMap<>();
    private final static ArrayList<@NotNull Injector> injectors = new ArrayList<>();
    private final static @NotNull InjectorProcessor injectorProcessor = new InjectorProcessor();

    private static HashMap<@NotNull String, @NotNull Suggestion> cachedSuggestions = null;
    private static HashMap<@NotNull Injector, @NotNull Collection<@NotNull Suggestion>> injectorsCache = null;

    public static boolean hasCachedSuggestion(@NotNull String suggestionText) {
        return Objects.nonNull(cachedSuggestions) && cachedSuggestions.containsKey(suggestionText);
    }

    public static @Nullable Suggestion getCachedSuggestion(@NotNull String suggestionText) {
        return Objects.nonNull(cachedSuggestions) ? cachedSuggestions.get(suggestionText) : null;
    }

    public static void registerInjector(@NotNull Injector injector) {
        injectors.add(injector);
    }

    public static @NotNull InjectorProcessor getInjectorProcessor() {
        return injectorProcessor;
    }

    public static class InjectorProcessor {
        private BiConsumer<@NotNull String, @NotNull List<com.mojang.brigadier.suggestion.Suggestion>>
                newSuggestionsApplier;
        private Supplier<@NotNull List<@NotNull String>> nonApiSuggestionsConsumer;
        private @Nullable StringContainer lastStringContainer = null;

        private InjectorProcessor() {
        }

        public boolean hasItBeenProcessedYetFor(@NotNull StringContainer stringContainer) {
            return Objects.nonNull(lastStringContainer) && lastStringContainer.equals(stringContainer);
        }

        public void setMinecraftSuggestionsCallback(
                @NotNull BiConsumer<@NotNull String, @NotNull List<com.mojang.brigadier.suggestion.Suggestion>>
                        newSuggestionsApplier,
                Supplier<@NotNull List<@NotNull String>> nonApiSuggestionsConsumer
        ) {
            this.newSuggestionsApplier = newSuggestionsApplier;
            this.nonApiSuggestionsConsumer = nonApiSuggestionsConsumer;
        }

        public static void initSession() {
            for (final var injector: injectors)
                if (injector instanceof InjectorListener injectorListener) injectorListener.onSessionInited();
        }

        public static void selectSuggestion(@NotNull String suggestionText) {
            final var suggestion = SuggestionsAPI.getCachedSuggestion(suggestionText);

            if (Objects.isNull(suggestion) || Objects.isNull(injectorsCache)) return;

            for (final var injectorEntry: injectorsCache.entrySet()) {
                if (
                        injectorEntry.getKey() instanceof InjectorListener injectorListener
                                && injectorEntry.getValue().contains(suggestion)
                ) {
                    injectorListener.onSuggestionSelected(suggestion);
                    break;
                }
            }
        }

        public boolean process(@NotNull StringContainer stringContainer) {
            lastStringContainer = stringContainer;
            cachedSuggestions = new HashMap<>();
            injectorsCache = new HashMap<>();

            final var nonApiSuggestions = nonApiSuggestionsConsumer.get();
            final var asyncInjectorsBuffer = new HashMap<@NotNull AsyncInjector, @NotNull Runnable>();
            final var minOffset = processInjectors(stringContainer, asyncInjectorsBuffer, nonApiSuggestions);
            final var applicableMojangSuggestions = new ArrayList<com.mojang.brigadier.suggestion.Suggestion>();
            final var textUpToCursor = stringContainer.getContent();

            for (final var injectorEntry: injectorsCache.entrySet()) {
                if (
                        !(injectorEntry.getKey() instanceof SuggestionsInjector)
                                && !(injectorEntry.getKey() instanceof InputRelatedInjector)
                ) continue;

                final var injector = (InputRelatedInjector) injectorEntry.getKey();

                if (
                        minOffset != -1
                                && !injector.getNestingStatus().isApiNestable()
                                && injector.getStartOffset() > minOffset
                ) continue;

                for (final var suggestion: injectorEntry.getValue()) {
                    final var offset = injector.getStartOffset();

                    if (!suggestion.shouldShowFor(textUpToCursor.substring(offset))) continue;

                    final var suggestionText = suggestion.getText();

                    if (isImplicitSuggestionsReplacement(nonApiSuggestions, suggestionText)) continue;

                    applicableMojangSuggestions.add(new com.mojang.brigadier.suggestion.Suggestion(
                            StringRange.between(
                                    offset,
                                    textUpToCursor.length()
                            ),
                            suggestionText
                    ));

                    cachedSuggestions.put(suggestionText, suggestion);
                }
            }

            var hasUsedAsyncInjector = false;

            for (final var injectorEntry: asyncInjectorsBuffer.entrySet()) {
                final var injector = injectorEntry.getKey();

                if (
                        minOffset != -1
                                && !injector.getNestingStatus().isApiNestable()
                                && injector.getStartOffset() > minOffset
                ) continue;

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

            newSuggestionsApplier.accept(textUpToCursor, applicableMojangSuggestions);

            return true;
        }

        private int processInjectors(
                @NotNull StringContainer stringContainer,
                @NotNull HashMap<@NotNull AsyncInjector, @NotNull Runnable> asyncInjectorBuffer,
                @NotNull List<@NotNull String> nonApiSuggestions
        ) {
            var minOffset = -1;

            for (final var injector: injectors) {
                var isActiveInjector = false;
                var isValidInjector = false;

                if (injector instanceof SuggestionsInjector suggestionsInjector) {
                    isValidInjector = true;

                    if (nonApiSuggestions.isEmpty() || suggestionsInjector.getNestingStatus().isNonApiNestable()) {
                        final var suggestions = suggestionsInjector.getSuggestions(stringContainer);

                        if (Objects.nonNull(suggestions) && !suggestions.isEmpty()) {
                            injectorsCache.put(suggestionsInjector, suggestions);

                            isActiveInjector = true;
                        }
                    }
                }

                if (injector instanceof ReplacementInjector replacementInjector) {
                    isValidInjector = true;

                    nonApiSuggestions.forEach(nonApiSuggestion -> {
                        final var newSuggestion = replacementInjector.getReplace(nonApiSuggestion);

                        if (Objects.isNull(newSuggestion)) return;

                        if (hasCachedSuggestion(nonApiSuggestion)) {
                            LOGGER.error(
                                    "[Suggestions API] Replacement is cancelled (reason: suggestion `"
                                            + nonApiSuggestion
                                            + "` is already replaced)"
                            );
                            return;
                        }

                        if (!newSuggestion.getText().equals(nonApiSuggestion)) {
                            LOGGER.error(
                                    "[Suggestions API] Replacement is cancelled (reason: expected suggestion `"
                                            + nonApiSuggestion
                                            + "` but got suggestion `"
                                            + newSuggestion.getText()
                                            + "`)"
                            );
                            return;
                        }

                        cachedSuggestions.put(nonApiSuggestion, newSuggestion);
                    });
                }

                if (injector instanceof AsyncInjector asyncInjector) {
                    isValidInjector = true;

                    final var asyncApplier = asyncInjector.getAsyncApplier(stringContainer);

                    if (
                            Objects.nonNull(asyncApplier) && (
                                    nonApiSuggestions.isEmpty() || asyncInjector.getNestingStatus().isNonApiNestable()
                            )
                    ) {
                        asyncInjectorBuffer.put(asyncInjector, () -> {
                            final var suggestionList = asyncApplier.get();

                            if (Objects.isNull(suggestionList) || suggestionList.isEmpty()) {
                                asyncProcessors.remove(asyncInjector);
                                return;
                            }

                            final var mojangSuggestions = new ArrayList<com.mojang.brigadier.suggestion.Suggestion>();
                            final var offset = asyncInjector.getStartOffset();
                            final var textUpToCursor = stringContainer.getContent();

                            suggestionList.forEach(suggestion -> {
                                if (!suggestion.shouldShowFor(textUpToCursor.substring(offset))) return;

                                final var suggestionText = suggestion.getText();

                                if (isImplicitSuggestionsReplacement(nonApiSuggestions, suggestionText))
                                    return;

                                mojangSuggestions.add(new com.mojang.brigadier.suggestion.Suggestion(
                                        StringRange.between(offset, textUpToCursor.length()),
                                        suggestionText
                                ));

                                cachedSuggestions.put(suggestionText, suggestion);
                            });

                            if (mojangSuggestions.isEmpty()) {
                                asyncProcessors.remove(asyncInjector);
                                return;
                            }

                            newSuggestionsApplier.accept(textUpToCursor, mojangSuggestions);
                        });

                        isActiveInjector = true;
                        asyncProcessors.remove(asyncInjector);
                    }
                }

                if (!isValidInjector) {
                    LOGGER.error("[Suggestions API] Invalid Injector! (" + injector + ")");

                    continue;
                }

                if (!isActiveInjector) continue;

                if (injector instanceof InputRelatedInjector inputRelatedInjector) {
                    if (inputRelatedInjector.getStartOffset() == 0) {
                        minOffset = 0;
                        continue;
                    }

                    if (minOffset != -1) {
                        minOffset = Math.min(minOffset, inputRelatedInjector.getStartOffset());
                        continue;
                    }

                    minOffset = inputRelatedInjector.getStartOffset();
                }
            }

            return minOffset;
        }

        private boolean isImplicitSuggestionsReplacement(
                @NotNull List<@NotNull String> nonApiSuggestions,
                @NotNull String suggestionText
        ) {
            if (nonApiSuggestions.contains(suggestionText) || hasCachedSuggestion(suggestionText)) {
                LOGGER.error(
                        "[Suggestions API] Implicit replacement of other suggestions is prohibited (prohibited suggestion `"
                                + suggestionText
                                + "`)"
                );

                return true;
            }

            return false;
        }
    }
}

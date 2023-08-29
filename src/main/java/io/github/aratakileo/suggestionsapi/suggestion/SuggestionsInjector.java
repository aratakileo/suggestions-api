package io.github.aratakileo.suggestionsapi.suggestion;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

public interface SuggestionsInjector {
    @Nullable <T extends Suggestion> List<T> getSuggestions(@NotNull String currentExpression);

    default int getStartOffset() {
        return 0;
    }

    default boolean isIsolated() {
        return false;
    }

    static <T extends Suggestion> SimpleSuggestionsInjector simple(
            @NotNull Pattern pattern,
            Function<@NotNull String, @Nullable List<T>> uncheckedSuggestionsGetter
    ) {
        return simple(pattern, uncheckedSuggestionsGetter, false);
    }

    static <T extends Suggestion> SimpleSuggestionsInjector simple(
            @NotNull Pattern pattern,
            Function<@NotNull String, @Nullable List<T>> uncheckedSuggestionsGetter,
            boolean isIsolated
    ) {
        return new SimpleSuggestionsInjector(pattern) {
            @Override
            public @Nullable <V extends Suggestion> List<V> getUncheckedSuggestions(@NotNull String currentExpression) {
                return (List<V>) uncheckedSuggestionsGetter.apply(currentExpression);
            }

            @Override
            public boolean isIsolated() {
                return isIsolated;
            }
        };
    }

    static <T extends Suggestion> SimpleSuggestionsInjector simple(
            @NotNull Pattern pattern,
            BiFunction<@NotNull String, @NotNull String, @Nullable List<T>> uncheckedSuggestionsGetter
    ) {
        return simple(pattern, uncheckedSuggestionsGetter, false);
    }

    static <T extends Suggestion> SimpleSuggestionsInjector simple(
            @NotNull Pattern pattern,
            BiFunction<@NotNull String, @NotNull String, @Nullable List<T>> uncheckedSuggestionsGetter,
            boolean isIsolated
    ) {
        return new SimpleSuggestionsInjector(pattern) {
            @Override
            public @Nullable <V extends Suggestion> List<V> getUncheckedSuggestions(@NotNull String currentExpression) {
                return (List<V>) uncheckedSuggestionsGetter.apply(
                        currentExpression,
                        currentExpression.substring(getStartOffset())
                );
            }

            @Override
            public boolean isIsolated() {
                return isIsolated;
            }
        };
    }
}

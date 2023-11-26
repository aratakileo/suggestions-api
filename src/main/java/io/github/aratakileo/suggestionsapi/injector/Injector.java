package io.github.aratakileo.suggestionsapi.injector;

import io.github.aratakileo.suggestionsapi.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public interface Injector {
    Pattern SIMPLE_WORD_PATTERN = Pattern.compile("[A-Za-z0-9]+");

    default int getStartOffset() {
        return 0;
    }

    default boolean isNestable() {
        return false;
    }

    static @NotNull SuggestionsInjector simple(
            @NotNull Pattern pattern,
            BiFunction<@NotNull String, @NotNull Integer, @Nullable List<Suggestion>> uncheckedSuggestionsGetter
    ) {
        return simple(pattern, uncheckedSuggestionsGetter, false);
    }

    static @NotNull SuggestionsInjector simple(
            @NotNull Pattern pattern,
            BiFunction<@NotNull String, @NotNull Integer, @Nullable List<Suggestion>> uncheckedSuggestionsGetter,
            boolean isNestable
    ) {
        return new SuggestionsInjector() {
            private int startOffset = 0;

            @Override
            public @Nullable List<Suggestion> getSuggestions(@NotNull String currentExpression) {
                final var lastMatchedStart = getLastMatchedStart(pattern, currentExpression);

                if (lastMatchedStart == -1)
                    return null;

                startOffset = lastMatchedStart;

                return uncheckedSuggestionsGetter.apply(currentExpression, lastMatchedStart);
            }

            @Override
            public int getStartOffset() {
                return startOffset;
            }

            @Override
            public boolean isNestable() {
                return isNestable;
            }
        };
    }

    static @NotNull AsyncInjector async(
            @NotNull Pattern pattern,
            BiFunction<
                    @NotNull String,
                    @NotNull Integer,
                    @Nullable List<Suggestion>
                    > uncheckedSupplierGetter
    ) {
        return async(pattern, uncheckedSupplierGetter, false);
    }

    static @NotNull AsyncInjector async(
            @NotNull Pattern pattern,
            BiFunction<
                    @NotNull String,
                    @NotNull Integer,
                    @Nullable List<Suggestion>
                    > uncheckedSupplierGetter,
            boolean isNestable
    ) {
        return new AsyncInjector() {
            private int startOffset = 0;

            @Override
            @Nullable
            public Supplier<@Nullable List<Suggestion>> getAsyncApplier(
                    @NotNull String currentExpression
            ) {
                final var lastMatchedStart = getLastMatchedStart(pattern, currentExpression);

                if (lastMatchedStart == -1)
                    return null;

                startOffset = lastMatchedStart;

                return () -> uncheckedSupplierGetter.apply(currentExpression, lastMatchedStart);
            }

            @Override
            public int getStartOffset() {
                return startOffset;
            }

            @Override
            public boolean isNestable() {
                return isNestable;
            }
        };
    }

    static int getLastMatchedStart(
            @NotNull Pattern pattern,
            @NotNull String currentExpression
    ) {
        final var matcher = pattern.matcher(currentExpression);

        if (!matcher.find()) return -1;

        var start = matcher.start();
        var end = matcher.end();

        while (matcher.find()) {
            start = matcher.start();
            end = matcher.end();
        }

        return end != currentExpression.length() ? -1 : start;
    }
}

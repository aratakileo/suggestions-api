package io.github.aratakileo.suggestionsapi.injector;

import io.github.aratakileo.suggestionsapi.suggestion.Suggestion;
import io.github.aratakileo.suggestionsapi.util.StringContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public interface Injector {
    Pattern
            SIMPLE_WORD_PATTERN = Pattern.compile("[A-Za-z0-9]+$"),
            IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z0-9_]+$"),
            NAMESPACABLE_IDENTIFIER_PATTERN = Pattern.compile("([A-Za-z0-9_]+:)?[A-Za-z0-9_]+$"),
            ANYTHING_WITHOUT_SPACES_PATTERN = Pattern.compile("\\S+$");

    default int getStartOffset() {
        return 0;
    }

    default boolean isNestable() {
        return false;
    }

    static @NotNull SuggestionsInjector simple(
            @NotNull Pattern pattern,
            BiFunction<
                    @NotNull StringContainer,
                    @NotNull Integer,
                    @Nullable List<Suggestion>
                    > uncheckedSuggestionsGetter
    ) {
        return simple(pattern, uncheckedSuggestionsGetter, false);
    }

    static @NotNull SuggestionsInjector simple(
            @NotNull Pattern pattern,
            BiFunction<
                    @NotNull StringContainer,
                    @NotNull Integer,
                    @Nullable List<Suggestion>
                    > uncheckedSuggestionsGetter,
            boolean isNestable
    ) {
        return new SuggestionsInjector() {
            private int startOffset = 0;

            @Override
            public @Nullable List<Suggestion> getSuggestions(@NotNull StringContainer stringContainer) {
                final var matcher = pattern.matcher(stringContainer.getContent());

                if (!matcher.find()) return null;

                startOffset = matcher.start();

                return uncheckedSuggestionsGetter.apply(stringContainer, startOffset);
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
                    @NotNull StringContainer,
                    @NotNull Integer,
                    @Nullable List<Suggestion>
                    > uncheckedSupplierGetter
    ) {
        return async(pattern, uncheckedSupplierGetter, false);
    }

    static @NotNull AsyncInjector async(
            @NotNull Pattern pattern,
            BiFunction<
                    @NotNull StringContainer,
                    @NotNull Integer,
                    @Nullable List<Suggestion>
                    > uncheckedSupplierGetter,
            boolean isNestable
    ) {
        return new AsyncInjector() {
            private int startOffset = 0;

            @Override
            public @Nullable Supplier<@Nullable List<Suggestion>> getAsyncApplier(
                    @NotNull StringContainer stringContainer
            ) {
                final var matcher = pattern.matcher(stringContainer.getContent());

                if (!matcher.find()) return null;

                startOffset = matcher.start();

                return () -> uncheckedSupplierGetter.apply(stringContainer, startOffset);
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
}

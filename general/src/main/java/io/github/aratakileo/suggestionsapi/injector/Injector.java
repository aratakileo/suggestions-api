package io.github.aratakileo.suggestionsapi.injector;

import io.github.aratakileo.suggestionsapi.util.StringContainer;
import io.github.aratakileo.suggestionsapi.suggestion.Suggestion;
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

    static @NotNull SuggestionsInjector simple(
            @NotNull Pattern pattern,
            BiFunction<
                    @NotNull StringContainer,
                    @NotNull Integer,
                    @Nullable List<Suggestion>
                    > uncheckedSuggestionsGetter
    ) {
        return simple(pattern, uncheckedSuggestionsGetter, InputRelatedInjector.NestingStatus.NOT_NESTABLE);
    }

    static @NotNull SuggestionsInjector simple(
            @NotNull Pattern pattern,
            BiFunction<
                    @NotNull StringContainer,
                    @NotNull Integer,
                    @Nullable List<Suggestion>
                    > uncheckedSuggestionsGetter,
            @NotNull InputRelatedInjector.NestingStatus nestingStatus
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
            public @NotNull NestingStatus getNestingStatus() {
                return nestingStatus;
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
        return async(pattern, uncheckedSupplierGetter, InputRelatedInjector.NestingStatus.NOT_NESTABLE);
    }

    static @NotNull AsyncInjector async(
            @NotNull Pattern pattern,
            BiFunction<
                    @NotNull StringContainer,
                    @NotNull Integer,
                    @Nullable List<Suggestion>
                    > uncheckedSupplierGetter,
            @NotNull InputRelatedInjector.NestingStatus nestingStatus
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
            public @NotNull NestingStatus getNestingStatus() {
                return nestingStatus;
            }
        };
    }

    static @NotNull ReplacementInjector replacement(@NotNull ReplacementInjector replacementInjector) {
        return replacementInjector;
    }
}

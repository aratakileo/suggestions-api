package io.github.aratakileo.suggestionsapi.suggestion;

import io.github.aratakileo.suggestionsapi.util.TripleFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public interface Injector {
    default int getStartOffset() {
        return 0;
    }

    default boolean isIsolated() {
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
            boolean isIsolated
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
            public boolean isIsolated() {
                return isIsolated;
            }
        };
    }

    static @NotNull AsyncInjector async(
            @NotNull Pattern pattern,
            TripleFunction<
                    @NotNull String,
                    @NotNull Integer,
                    @NotNull Consumer<@Nullable List<Suggestion>>,
                    @Nullable Runnable
                    > uncheckedSupplierGetter
    ) {
        return async(pattern, uncheckedSupplierGetter, false);
    }

    static @NotNull AsyncInjector async(
            @NotNull Pattern pattern,
            TripleFunction<
                    @NotNull String,
                    @NotNull Integer,
                    @NotNull Consumer<@Nullable List<Suggestion>>,
                    @Nullable Runnable
                    > uncheckedSupplierGetter,
            boolean isIsolated
    ) {
        return new AsyncInjector() {
            private int startOffset = 0;
            private CompletableFuture<Void> currentProcess = null;
            private Runnable applierBody = null;

            @Override
            @Nullable
            public CompletableFuture<Void> getCurrentProcess() {
                return currentProcess;
            }

            @Override
            public void setApplierBody(@Nullable Runnable applierBody) {
                this.applierBody = applierBody;
            }

            @Override
            public void runAsyncApplier() {
                currentProcess = CompletableFuture.runAsync(applierBody);
            }

            @Override
            public boolean initAsyncApplier(
                    @NotNull String currentExpression,
                    @NotNull Consumer<@Nullable List<Suggestion>> applier
            ) {
                final var lastMatchedStart = getLastMatchedStart(pattern, currentExpression);

                if (lastMatchedStart == -1)
                    return false;

                if (currentProcess != null && !currentProcess.isDone()) {
                    currentProcess.cancel(true);
                    currentProcess = null;
                }

                startOffset = lastMatchedStart;

                setApplierBody(uncheckedSupplierGetter.apply(currentExpression, lastMatchedStart, applier));

                return applierBody != null;
            }

            @Override
            public int getStartOffset() {
                return startOffset;
            }

            @Override
            public boolean isIsolated() {
                return isIsolated;
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

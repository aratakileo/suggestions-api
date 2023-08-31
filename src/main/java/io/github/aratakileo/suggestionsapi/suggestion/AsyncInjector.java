package io.github.aratakileo.suggestionsapi.suggestion;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface AsyncInjector extends Injector {
    @Nullable CompletableFuture<Void> getCurrentProcess();

    void setCurrentProcess(@Nullable CompletableFuture<Void> currentProcess);

    @Nullable Runnable getApplier();

    void setApplier(@Nullable Runnable applier);

    default void runAsyncApplier() {
        if (getApplier() == null) return;

        setCurrentProcess(CompletableFuture.runAsync(getApplier()));
    }

    boolean initAsyncApplier(
            @NotNull String currentExpression,
            @NotNull Consumer<@Nullable List<Suggestion>> applier
    );

    static void setApplier(@NotNull AsyncInjector asyncInjector, @Nullable Runnable applier) {
        final var currentProcess = asyncInjector.getCurrentProcess();

        if (currentProcess != null && !currentProcess.isDone())
            currentProcess.cancel(true);

        asyncInjector.setApplier(applier);
    }
}

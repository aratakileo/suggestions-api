package io.github.aratakileo.suggestionsapi.suggestion;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface AsyncInjector extends Injector {
    @Nullable CompletableFuture<Void> getCurrentProcess();

    void setApplierBody(@Nullable Runnable applierBody);

    void runAsyncApplier();

    boolean initAsyncApplier(
            @NotNull String currentExpression,
            @NotNull Consumer<@Nullable List<Suggestion>> applier
    );
}

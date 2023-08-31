package io.github.aratakileo.suggestionsapi.suggestion;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public interface AsyncInjector extends Injector {
    @Nullable Supplier<@Nullable List<Suggestion>> getSupplier(@NotNull String currentExpression);
}

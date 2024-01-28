package io.github.aratakileo.suggestionsapi.injector;

import io.github.aratakileo.suggestionsapi.suggestion.Suggestion;
import io.github.aratakileo.suggestionsapi.util.StringContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public interface AsyncInjector extends InputRelatedInjector {
    @Nullable Supplier<@Nullable List<Suggestion>> getAsyncApplier(@NotNull StringContainer stringContainer);
}

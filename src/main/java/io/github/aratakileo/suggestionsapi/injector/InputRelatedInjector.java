package io.github.aratakileo.suggestionsapi.injector;

import org.jetbrains.annotations.NotNull;

public interface InputRelatedInjector extends Injector {
    default int getStartOffset() {
        return 0;
    }

    default @NotNull NestingStatus getNestingStatus() {
        return NestingStatus.NOT_NESTABLE;
    }

    enum NestingStatus {
        ALL_NESTABLE,
        ONLY_VANILLA_NESTABLE,
        ONLY_SUGGESTIONS_API_NESTABLE,
        NOT_NESTABLE;

        public boolean isVanillaNestable() {
            return this == ALL_NESTABLE || this == ONLY_VANILLA_NESTABLE;
        }

        public boolean isSuggestionsApiNestable() {
            return this == ALL_NESTABLE || this == ONLY_SUGGESTIONS_API_NESTABLE;
        }
    }
}

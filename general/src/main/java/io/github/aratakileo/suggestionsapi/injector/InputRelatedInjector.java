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
        @Deprecated
        ONLY_NON_API_NESTABLE,
        ONLY_API_NESTABLE,
        NOT_NESTABLE;

        @Deprecated
        public boolean isNonApiNestable() {
            return this == ALL_NESTABLE || this == ONLY_NON_API_NESTABLE;
        }

        public boolean isApiNestable() {
            return this == ALL_NESTABLE || this == ONLY_API_NESTABLE;
        }
    }
}

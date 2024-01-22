package io.github.aratakileo.suggestionsapi.injector;

public interface InputRelatedInjector extends Injector {
    default int getStartOffset() {
        return 0;
    }

    default boolean isNestable() {
        return false;
    }
}

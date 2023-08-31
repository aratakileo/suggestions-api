package io.github.aratakileo.suggestionsapi.util;

import io.github.aratakileo.suggestionsapi.suggestion.Suggestion;

import java.util.List;

public final class Cast {
    @SuppressWarnings("unchecked")
    public static <T extends Suggestion> List<Suggestion> of(List<T> suggestions) {
        return (List<Suggestion>) suggestions;
    }

    @SuppressWarnings("unchecked")
    public static <T, R> R unsafeOf(T value) throws ClassCastException {
        return (R) value;
    }
}

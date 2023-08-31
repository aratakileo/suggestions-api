package io.github.aratakileo.suggestionsapi.util;

public interface TripleFunction <T, U, V, R> {
    R apply (T t, U u, V v);
}

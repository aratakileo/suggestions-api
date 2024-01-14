package io.github.aratakileo.suggestionsapi.injector;

import net.minecraft.client.gui.components.EditBox;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public class StringContainer {
    private final String fullContent, contentUpToCursor;
    private final boolean isCommandContext;
    private final int cursorPosition;

    public StringContainer(@NotNull EditBox input, boolean isCommandsOnly) {
        this(input.getValue(), input.getCursorPosition(), isCommandsOnly);
    }

    public StringContainer(@NotNull String inputValue, int cursorPosition, boolean isCommandsOnly) {
        this.cursorPosition = cursorPosition;

        final var hasSlash = inputValue.charAt(0) == '/';

        isCommandContext = isCommandsOnly || hasSlash;
        fullContent = hasSlash ? inputValue.substring(1) : inputValue;
        contentUpToCursor = inputValue.substring(hasSlash ? 1 : 0, cursorPosition);
    }

    public boolean isCommandContext() {
        return isCommandContext;
    }

    public int getCursorPosition() {
        return cursorPosition;
    }

    public @NotNull String getContent() {
        return getContent(false);
    }

    public @NotNull String getContent(boolean ignoreCursor) {
        return ignoreCursor ? fullContent : contentUpToCursor;
    }

    public boolean isEmpty() {
        return !isCommandContext && fullContent.isEmpty();
    }
}

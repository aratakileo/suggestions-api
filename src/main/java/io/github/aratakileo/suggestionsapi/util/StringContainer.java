package io.github.aratakileo.suggestionsapi.util;

import net.minecraft.client.gui.components.EditBox;
import org.jetbrains.annotations.NotNull;

public class StringContainer {
    private final String fullContent, contentUpToCursor;
    private final Context context;
    private final int cursorPosition;

    public StringContainer(@NotNull EditBox input, boolean isCommandsOnly) {
        this(input.getValue(), input.getCursorPosition(), isCommandsOnly);
    }

    public StringContainer(@NotNull String inputValue, int cursorPosition, boolean isCommandsOnly) {
        this.cursorPosition = cursorPosition;

        final var hasSlash = !inputValue.isEmpty() && inputValue.charAt(0) == '/';

        context = isCommandsOnly ? Context.COMMAND_BLOCK : (hasSlash ? Context.CHAT_COMMAND : Context.NOT_COMMAND);

        if (inputValue.isEmpty()) {
            fullContent = "";
            contentUpToCursor = "";
            return;
        }

        fullContent = hasSlash ? inputValue.substring(1) : inputValue;
        contentUpToCursor = inputValue.substring(hasSlash ? 1 : 0, cursorPosition);
    }

    public Context getContext() {
        return context;
    }

    public int getCursorPosition() {
        return cursorPosition;
    }

    public @NotNull String getContent() {
        return getContent(false);
    }

    public @NotNull String getContent(boolean ignoreCursorPosition) {
        return getContent(ignoreCursorPosition, true);
    }

    public @NotNull String getContent(boolean ignoreCursorPosition, boolean applyContext) {
        return (applyContext && context.isChatCommand() ? "/" : "")
                + (ignoreCursorPosition ? fullContent : contentUpToCursor);
    }

    public boolean isEmpty() {
        return !context.isChatCommand() && fullContent.isEmpty();
    }

    public boolean equals(@NotNull StringContainer other) {
        return this == other || (
                fullContent.equals(other.fullContent)
                        && cursorPosition == other.cursorPosition
                        && context == other.context
        );
    }

    public enum Context {
        NOT_COMMAND,
        COMMAND_BLOCK,
        CHAT_COMMAND;

        public boolean isNotCommand() {
            return this == NOT_COMMAND;
        }

        public boolean isCommandBlock() {
            return this == COMMAND_BLOCK;
        }

        public boolean isChatCommand() {
            return this == CHAT_COMMAND;
        }
    }
}

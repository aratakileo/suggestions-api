package io.github.aratakileo.suggestionsapi.util;

import net.minecraft.client.gui.components.EditBox;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

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

        context = isCommandsOnly ? Context.COMMAND_BLOCK : (hasSlash ? Context.CHAT_COMMAND : Context.OTHER);

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

    public enum Context {
        OTHER,
        COMMAND_BLOCK,
        CHAT_COMMAND;

        public boolean isOther() {
            return this == OTHER;
        }

        public boolean isCommandBlock() {
            return this == COMMAND_BLOCK;
        }

        public boolean isChatCommand() {
            return this == CHAT_COMMAND;
        }
    }
}

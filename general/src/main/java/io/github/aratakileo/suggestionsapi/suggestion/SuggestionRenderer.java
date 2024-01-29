package io.github.aratakileo.suggestionsapi.suggestion;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

public interface SuggestionRenderer {
    int getWidth();

    int renderContent(
            @NotNull GuiGraphics guiGraphics,
            @NotNull Font font,
            int x,
            int y,
            int color
    );
}

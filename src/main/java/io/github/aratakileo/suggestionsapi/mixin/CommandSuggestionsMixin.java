package io.github.aratakileo.suggestionsapi.mixin;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import io.github.aratakileo.suggestionsapi.SuggestionsAPI;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {
    @Shadow @Final
    EditBox input;

    @Shadow @Nullable
    private CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow @Final
    private boolean commandsOnly;

    @Inject(method = "updateCommandInfo", at = @At("TAIL"), cancellable = true)
    private void updateCommandInfo(CallbackInfo ci){
        final var contentText = input.getValue();
        final var stringReader = new StringReader(contentText);
        final var hasSlash = stringReader.canRead() && stringReader.peek() == '/';
        final var cursorPosition = input.getCursorPosition();

        if (hasSlash)
            stringReader.skip();

        if (commandsOnly || hasSlash) return;

        final var suggestionProcessor = SuggestionsAPI.getSuggestionProcessorBuilder()
                .setTextUptoCursor(contentText.substring(0, cursorPosition))
                .build();

        if (suggestionProcessor == null) return;

        pendingSuggestions = suggestionProcessor.getPendingSuggestions();

        if (pendingSuggestions == null) return;

        pendingSuggestions.thenRun(() -> {
            if (!pendingSuggestions.isDone()) return;
            ((CommandSuggestions)(Object)this).showSuggestions(false);
        });

        ci.cancel();
    }
}

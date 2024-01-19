package io.github.aratakileo.suggestionsapi.mixin;

import com.mojang.brigadier.suggestion.Suggestions;
import io.github.aratakileo.suggestionsapi.SuggestionsAPI;
import io.github.aratakileo.suggestionsapi.util.StringContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {
    @Shadow @Final
    EditBox input;

    @Shadow @Nullable
    private CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow @Final
    private boolean commandsOnly;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(
            Minecraft minecraft,
            Screen screen,
            EditBox editBox,
            Font font,
            boolean bl,
            boolean bl2,
            int i,
            int j,
            boolean bl3,
            int k,
            CallbackInfo ci
    ) {
        SuggestionsAPI.InjectorProcessor.initSession();
    }

    @Inject(method = "updateCommandInfo", at = @At("TAIL"), cancellable = true)
    private void updateCommandInfo(CallbackInfo ci){
        final var injectorProcessor = SuggestionsAPI.getInjectorProcessor();
        injectorProcessor.setNewSuggestionsApplier((textUpToCursor, suggestionList) -> {
            if (Objects.nonNull(pendingSuggestions)) {
                suggestionList = Stream.concat(
                        pendingSuggestions.join().getList().stream(),
                        suggestionList.stream()
                ).toList();
            }

            pendingSuggestions = CompletableFuture.completedFuture(Suggestions.create(
                    textUpToCursor,
                    suggestionList
            ));

            pendingSuggestions.thenRun(() -> {
                if (pendingSuggestions.isDone())
                    ((CommandSuggestions) (Object) this).showSuggestions(false);
            });
        });

        final var stringContainer = new StringContainer(input, commandsOnly);

        if (injectorProcessor.process(stringContainer))
            return;

        ci.cancel();
    }
}

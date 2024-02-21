package io.github.aratakileo.suggestionsapi.mixin;

import com.google.common.collect.Lists;
import com.mojang.brigadier.suggestion.Suggestion;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
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

    @Unique
    private List<String> suggestionsApi$getNonApiSuggestions() {
//        if (Objects.isNull(pendingSuggestions)) return new ArrayList<>();
//
//        final var returnable = Lists.newArrayList(
//                pendingSuggestions.join()
//                        .getList()
//                        .stream()
//                        .map(Suggestion::getText)
//                        .toList()
//        );
//
//        returnable.removeIf(SuggestionsAPI::hasCachedSuggestion);
//
//        return returnable;

        return Lists.newArrayList();  // stub
    }

    @Inject(method = "updateCommandInfo", at = @At("TAIL"), cancellable = true)
    private void updateCommandInfo(CallbackInfo ci){
        final var injectorProcessor = SuggestionsAPI.getInjectorProcessor();
        final var stringContainer = new StringContainer(input, commandsOnly);

        if (injectorProcessor.hasItBeenProcessedYetFor(stringContainer))
            return;

        injectorProcessor.setMinecraftSuggestionsCallback(
                (textUpToCursor, suggestionList) -> {
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
                },
                this::suggestionsApi$getNonApiSuggestions
        );

        if (injectorProcessor.process(stringContainer))
            return;

        ci.cancel();
    }
}

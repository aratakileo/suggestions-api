package io.github.aratakileo.suggestionsapi.mixin;

import com.mojang.brigadier.suggestion.Suggestion;
import io.github.aratakileo.suggestionsapi.SuggestionsAPI;
import io.github.aratakileo.suggestionsapi.core.SuggestionsProcessor;
import io.github.aratakileo.suggestionsapi.suggestion.SuggestionRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.renderer.Rect2i;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;

@Mixin(CommandSuggestions.SuggestionsList.class)
public class SuggestionsListMixin {
    @Shadow @Final
    private Rect2i rect;

    @Shadow @Final
    private List<Suggestion> suggestionList;

    @Shadow
    private int current;

    @Inject(method = "useSuggestion", at = @At("HEAD"))
    private void useSuggestion(CallbackInfo ci) {
        if (Objects.isNull(SuggestionsProcessor.getInstance())) return;

        SuggestionsProcessor.getInstance().selectSuggestion(suggestionList.get(current).getText());
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(
            CommandSuggestions commandSuggestions,
            int x,
            int y,
            int width,
            List<Suggestion> suggestions,
            boolean bl,
            CallbackInfo ci
    ){
        suggestions.forEach(mojangSuggestion -> {
            io.github.aratakileo.suggestionsapi.suggestion.Suggestion suggestion;

            if (
                    Objects.nonNull(suggestion = SuggestionsAPI.getSuggestion(mojangSuggestion.getText()))
                            && suggestion instanceof SuggestionRenderer
            ) {
                rect.setWidth(Math.max(
                        rect.getWidth(),
                        ((SuggestionRenderer) suggestion).getWidth()
                ));
            }
        });
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I"))
    private int updateCommandInfo(GuiGraphics instance, Font font, String suggestionText, int x, int y, int color){
        io.github.aratakileo.suggestionsapi.suggestion.Suggestion suggestion;
        if (
                Objects.isNull(suggestion = SuggestionsAPI.getSuggestion(suggestionText))
                        || !(suggestion instanceof SuggestionRenderer)
        ) return instance.drawString(font, suggestionText, x, y, color);

        return ((SuggestionRenderer) suggestion).renderContent(instance, font, x, y, color);
    }
}

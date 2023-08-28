package io.github.aratakileo.suggestionsapi;

import io.github.aratakileo.suggestionsapi.suggestion.DynamicSuggestionsInjector;
import io.github.aratakileo.suggestionsapi.suggestion.IconSuggestion;
import io.github.aratakileo.suggestionsapi.suggestion.SimpleSuggestion;
import io.github.aratakileo.suggestionsapi.suggestion.Suggestion;
import io.github.aratakileo.suggestionsapi.core.SuggestionsProcessor;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;

public class SuggestionsAPI implements ClientModInitializer {
    public static Logger LOGGER = LoggerFactory.getLogger(SuggestionsAPI.class);

    private final static HashMap<String, Suggestion> suggestions = new HashMap<>();
    private final static ArrayList<DynamicSuggestionsInjector> dynamicSuggestionsInjectors = new ArrayList<>();

    private static HashMap<String, Suggestion> dynamicSuggestions;

    @Override
    public void onInitializeClient() {
        addSuggestion("bonjour");
        addSuggestion(new IconSuggestion(
                "barrier",
                new ResourceLocation("minecraft", "textures/item/barrier.png")
        ));
    }

    public static <T extends Suggestion>  void addSuggestion(@NotNull T suggestion) {
        suggestions.put(suggestion.getSuggestionText(), suggestion);
    }

    public static void addSuggestion(@NotNull String suggestionText) {
        suggestions.put(suggestionText, new SimpleSuggestion(suggestionText));
    }

    public static @Nullable Suggestion getSuggestion(@NotNull String suggestionText) {
        return suggestions.containsKey(suggestionText)
                ? suggestions.get(suggestionText) : dynamicSuggestions.get(suggestionText);
    }

    public static <T extends DynamicSuggestionsInjector>  void registerDynamicSuggestionsInjector(
            @NotNull T dymanicSuggestionsInjector
    ) {
        dynamicSuggestionsInjectors.add(dymanicSuggestionsInjector);
    }

    public static @NotNull SuggestionsProcessor.Builder getSuggestionProcessorBuilder() {
        return new SuggestionsProcessor.Builder(
                suggestions,
                dynamicSuggestionsInjectors,
                newDynamicSuggestions -> dynamicSuggestions = newDynamicSuggestions
        );
    }
}

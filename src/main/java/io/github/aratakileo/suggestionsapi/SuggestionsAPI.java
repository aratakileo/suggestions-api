package io.github.aratakileo.suggestionsapi;

import io.github.aratakileo.suggestionsapi.suggestion.*;
import io.github.aratakileo.suggestionsapi.core.SuggestionsProcessor;
import net.fabricmc.api.ClientModInitializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;

public class SuggestionsAPI implements ClientModInitializer {
    private final static HashMap<String, Suggestion> suggestions = new HashMap<>();
    private final static ArrayList<Injector> injectors = new ArrayList<>();

    private static HashMap<String, Suggestion> dynamicSuggestions;

    @Override
    public void onInitializeClient() {

    }

    public static void addSuggestion(@NotNull Suggestion suggestion) {
        suggestions.put(suggestion.getSuggestionText(), suggestion);
    }

    public static void removeSuggestion(@NotNull Suggestion suggestion) {
        if (!suggestions.containsValue(suggestion)) return;

        suggestions.values().remove((Suggestion) suggestions);
    }

    public static void removeSuggestion(@NotNull String suggestionText) {
        suggestions.remove(suggestionText);
    }

    public static @Nullable Suggestion getSuggestion(@NotNull String suggestionText) {
        return suggestions.containsKey(suggestionText)
                ? suggestions.get(suggestionText) : dynamicSuggestions.get(suggestionText);
    }

    public static void registerSuggestionsInjector(@NotNull Injector injector) {
        injectors.add(injector);
    }

    public static @NotNull SuggestionsProcessor.Builder getSuggestionProcessorBuilder() {
        return new SuggestionsProcessor.Builder(
                suggestions,
                injectors,
                newDynamicSuggestions -> dynamicSuggestions = newDynamicSuggestions
        );
    }
}

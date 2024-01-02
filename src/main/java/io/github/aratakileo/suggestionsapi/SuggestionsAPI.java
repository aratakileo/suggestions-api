package io.github.aratakileo.suggestionsapi;

import io.github.aratakileo.suggestionsapi.injector.Injector;
import io.github.aratakileo.suggestionsapi.suggestion.*;
import io.github.aratakileo.suggestionsapi.core.SuggestionsProcessor;
import io.github.aratakileo.suggestionsapi.util.Cast;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class SuggestionsAPI implements ClientModInitializer {
    private final static HashMap<String, Suggestion> suggestions = new HashMap<>();
    private final static ArrayList<Injector> injectors = new ArrayList<>();
    private final static ArrayList<Supplier<@NotNull List<Suggestion>>> resourceDependedSuggestionContainers = new ArrayList<>();

    private static HashMap<String, Suggestion> tempSuggestions;
    private static boolean areResourcesLoaded = false;

    @Override
    public void onInitializeClient() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public ResourceLocation getFabricId() {
                        return new ResourceLocation("suggestionsapi", "");
                    }

                    @Override
                    public void onResourceManagerReload(ResourceManager resourceManager) {
                        if (areResourcesLoaded) return;

                        areResourcesLoaded = true;

                        resourceDependedSuggestionContainers.forEach(
                                container -> container.get().forEach(SuggestionsAPI::addSuggestion)
                        );
                        resourceDependedSuggestionContainers.clear();
                    }
                }
        );
    }

    public static void addSuggestion(@NotNull Suggestion suggestion) {
        suggestions.put(suggestion.getSuggestionText(), suggestion);
    }

    public static void addResourceDependedSuggestionsContainer(@NotNull Supplier<@NotNull List<Suggestion>> container) {
        if (areResourcesLoaded) {
            container.get().forEach(SuggestionsAPI::addSuggestion);
            return;
        }

        resourceDependedSuggestionContainers.add(container);
    }

    public static void removeSuggestion(@NotNull Suggestion suggestion) {
        if (!suggestions.containsValue(suggestion)) return;

        suggestions.values().remove(suggestion);
    }

    public static void removeSuggestion(@NotNull String suggestionText) {
        suggestions.remove(suggestionText);
    }

    public static @Nullable Suggestion getSuggestion(@NotNull String suggestionText) {
        return suggestions.containsKey(suggestionText)
                ? suggestions.get(suggestionText)
                : Objects.nonNull(tempSuggestions)
                        ? tempSuggestions.get(suggestionText)
                        : null;
    }

    public static void registerSuggestionsInjector(@NotNull Injector injector) {
        injectors.add(injector);
    }


    public static ArrayList<Injector> getInjectors() {
        return Cast.unsafeOf(injectors.clone());
    }

    public static @NotNull SuggestionsProcessor.Builder getSuggestionProcessorBuilder() {
        return new SuggestionsProcessor.Builder(
                suggestions,
                injectors,
                newTempSuggestions -> tempSuggestions = newTempSuggestions
        );
    }
}

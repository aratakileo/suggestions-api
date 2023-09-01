package io.github.aratakileo.suggestionsapi;

import io.github.aratakileo.suggestionsapi.suggestion.*;
import io.github.aratakileo.suggestionsapi.core.SuggestionsProcessor;
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
import java.util.function.Supplier;

public class SuggestionsAPI implements ClientModInitializer {
    private final static HashMap<String, Suggestion> suggestions = new HashMap<>();
    private final static ArrayList<Injector> injectors = new ArrayList<>();
    private final static ArrayList<Supplier<@NotNull List<Suggestion>>> resourceDependedInjectors = new ArrayList<>();

    private static HashMap<String, Suggestion> dynamicSuggestions;
    private static boolean isResourcesLoaded = false;

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
                        if (isResourcesLoaded) return;

                        isResourcesLoaded = true;

                        resourceDependedInjectors.forEach(
                                injector -> injector.get().forEach(SuggestionsAPI::addSuggestion)
                        );
                        resourceDependedInjectors.clear();
                    }
                }
        );
    }

    public static void addSuggestion(@NotNull Suggestion suggestion) {
        suggestions.put(suggestion.getSuggestionText(), suggestion);
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
                ? suggestions.get(suggestionText) : dynamicSuggestions.get(suggestionText);
    }

    public static void registerSuggestionsInjector(@NotNull Injector injector) {
        injectors.add(injector);
    }

    public static void registerResourceDependedInjector(@NotNull Supplier<@NotNull List<Suggestion>> injector) {
        resourceDependedInjectors.add(injector);
    }

    public static @NotNull SuggestionsProcessor.Builder getSuggestionProcessorBuilder() {
        return new SuggestionsProcessor.Builder(
                suggestions,
                injectors,
                newDynamicSuggestions -> dynamicSuggestions = newDynamicSuggestions
        );
    }
}

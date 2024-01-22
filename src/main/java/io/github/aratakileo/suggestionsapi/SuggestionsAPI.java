package io.github.aratakileo.suggestionsapi;

import com.mojang.brigadier.context.StringRange;
import io.github.aratakileo.suggestionsapi.injector.*;
import io.github.aratakileo.suggestionsapi.suggestion.*;
import io.github.aratakileo.suggestionsapi.util.StringContainer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class SuggestionsAPI implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SuggestionsAPI.class);
    private final static HashMap<@NotNull AsyncInjector, @NotNull CompletableFuture<@NotNull Void>> asyncProcessors
            = new HashMap<>();
    private final static @NotNull ConcurrentHashMap<@NotNull String, @NotNull Suggestion> suggestions
            = new ConcurrentHashMap<>();
    private final static ArrayList<@NotNull Injector> injectors = new ArrayList<>();
    private final static ArrayList<@NotNull Supplier<@NotNull List<Suggestion>>> resourceDependedSuggestionContainers
            = new ArrayList<>();
    private final static @NotNull InjectorProcessor injectorProcessor = new InjectorProcessor();

    private static HashMap<@NotNull String, @NotNull Suggestion> cachedSuggestions = null;
    private static HashMap<@NotNull Injector, @NotNull Collection<@NotNull Suggestion>> injectorsCache = null;
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

        registerInjector(Injector.simple(Injector.SIMPLE_WORD_PATTERN, ((stringContainer, startingOffset) -> List.of(Suggestion.alwaysShown("often")))));
        registerInjector(Injector.async(Injector.SIMPLE_WORD_PATTERN, ((stringContainer, startingOffset) -> List.of(Suggestion.alwaysShown("often")))));
    }

    public static void addSuggestion(@NotNull Suggestion suggestion) {
        suggestions.put(suggestion.getSuggestionText(), suggestion);
    }

    public static void addResourceDependedContainer(@NotNull Supplier<@NotNull List<Suggestion>> container) {
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

    public static boolean hasSuggestion(@NotNull String suggestionText) {
        return suggestions.containsKey(suggestionText) || hasCachedSuggestion(suggestionText);
    }

    public static boolean hasCachedSuggestion(@NotNull String suggestionText) {
        return Objects.nonNull(cachedSuggestions) && cachedSuggestions.containsKey(suggestionText);
    }

    public static @Nullable Suggestion getSuggestion(@NotNull String suggestionText) {
        return suggestions.containsKey(suggestionText)
                ? suggestions.get(suggestionText)
                : Objects.nonNull(cachedSuggestions)
                        ? cachedSuggestions.get(suggestionText)
                        : null;
    }

    public static void registerInjector(@NotNull Injector injector) {
        injectors.add(injector);
    }

    public static @NotNull InjectorProcessor getInjectorProcessor() {
        return injectorProcessor;
    }

    public static class InjectorProcessor {
        private BiConsumer<@NotNull String, @NotNull List<com.mojang.brigadier.suggestion.Suggestion>>
                newSuggestionsApplier;
        private Supplier<@NotNull List<@NotNull String>> nonApiSuggestionsConsumer;

        private InjectorProcessor() {}

        public void setMinecraftSuggestionsCallback(
                @NotNull BiConsumer<@NotNull String, @NotNull List<com.mojang.brigadier.suggestion.Suggestion>>
                        newSuggestionsApplier,
                Supplier<@NotNull List<@NotNull String>> nonApiSuggestionsConsumer
        ) {
            this.newSuggestionsApplier = newSuggestionsApplier;
            this.nonApiSuggestionsConsumer = nonApiSuggestionsConsumer;
        }

        public static void initSession() {
            for (final var injector: injectors)
                if (injector instanceof InjectorListener injectorListener) injectorListener.onSessionInited();
        }

        public static void selectSuggestion(@NotNull String suggestionText) {
            final var suggestion = SuggestionsAPI.getSuggestion(suggestionText);

            if (Objects.isNull(suggestion) || Objects.isNull(injectorsCache)) return;

            for (final var injectorEntry: injectorsCache.entrySet()) {
                if (
                        injectorEntry.getKey() instanceof InjectorListener injectorListener
                                && injectorEntry.getValue().contains(suggestion)
                ) {
                    injectorListener.onSuggestionSelected(suggestion);
                    break;
                }
            }
        }

        public boolean process(@NotNull StringContainer stringContainer) {
            cachedSuggestions = new HashMap<>();
            injectorsCache = new HashMap<>();

            final var nonApiSuggestions = nonApiSuggestionsConsumer.get();
            final var asyncInjectorsBuffer = new HashMap<@NotNull AsyncInjector, @NotNull Runnable>();
            final var minOffset = processInjectors(stringContainer, asyncInjectorsBuffer, nonApiSuggestions);
            final var applicableMojangSuggestions = new ArrayList<com.mojang.brigadier.suggestion.Suggestion>();
            final var textUpToCursor = stringContainer.getContent();

            for (final var injectorEntry: injectorsCache.entrySet()) {
                if (!(injectorEntry.getKey() instanceof SuggestionsInjector)) continue;

                final var injector = injectorEntry.getKey();

                if (minOffset != -1 && !injector.isNestable() && injector.getStartOffset() > minOffset) continue;

                for (final var suggestion: injectorEntry.getValue()) {
                    final var offset = injector.getStartOffset();

                    if (!suggestion.shouldShowFor(textUpToCursor.substring(offset))) continue;

                    final var suggestionText = suggestion.getSuggestionText();

                    if (isImplicitSuggestionsReplacement(nonApiSuggestions, suggestionText)) continue;

                    applicableMojangSuggestions.add(new com.mojang.brigadier.suggestion.Suggestion(
                            StringRange.between(
                                    offset,
                                    textUpToCursor.length()
                            ),
                            suggestionText
                    ));

                    cachedSuggestions.put(suggestionText, suggestion);
                }
            }

            processSuggestions(textUpToCursor, applicableMojangSuggestions, nonApiSuggestions);

            var hasUsedAsyncInjector = false;

            for (final var injectorEntry: asyncInjectorsBuffer.entrySet()) {
                final var injector = injectorEntry.getKey();

                if (minOffset != -1 && !injector.isNestable() && injector.getStartOffset() > minOffset) continue;

                hasUsedAsyncInjector = true;

                CompletableFuture<Void> currentCompletableFuture;

                if (
                        asyncProcessors.containsKey(injector) && !(
                                currentCompletableFuture = asyncProcessors.get(injector)
                        ).isDone()
                )
                    currentCompletableFuture.cancel(true);

                asyncProcessors.put(injector, CompletableFuture.runAsync(injectorEntry.getValue()));
            }

            if (applicableMojangSuggestions.isEmpty() && !hasUsedAsyncInjector) return false;

            newSuggestionsApplier.accept(textUpToCursor, applicableMojangSuggestions);

            return true;
        }

        private void processSuggestions(
                @NotNull String textUpToCursor,
                @NotNull ArrayList<com.mojang.brigadier.suggestion.Suggestion> applicableMojangSuggestions,
                @NotNull List<@NotNull String> nonApiSuggestions
        ) {
            final var upToCursorMatcher = Injector.ANYTHING_WITHOUT_SPACES_PATTERN.matcher(textUpToCursor);

            if (!upToCursorMatcher.find()) return;

            suggestions.forEach((suggestionText, suggestion) -> {
                if (!suggestion.shouldShowFor(textUpToCursor.substring(upToCursorMatcher.start()))) return;

                if (isImplicitSuggestionsReplacement(nonApiSuggestions, suggestionText)) {
                    suggestions.remove(suggestionText);
                    LOGGER.warn("[Suggestions API] Static suggestion `" + suggestionText + "` has been removed (reason: implicit replacement other suggestion)!");
                    return;
                }

                applicableMojangSuggestions.add(new com.mojang.brigadier.suggestion.Suggestion(
                        StringRange.between(upToCursorMatcher.start(), textUpToCursor.length()),
                        suggestionText
                ));
            });
        }

        private int processInjectors(
                @NotNull StringContainer stringContainer,
                @NotNull HashMap<@NotNull AsyncInjector, @NotNull Runnable> asyncInjectorBuffer,
                @NotNull List<@NotNull String> nonApiSuggestions
        ) {
            var minOffset = -1;

            for (final var injector: injectors) {
                var isActiveInjector = false;
                var isValidInjector = false;

                if (injector instanceof SuggestionsInjector suggestionsInjector) {
                    isValidInjector = true;

                    final var suggestions = suggestionsInjector.getSuggestions(stringContainer);

                    if (Objects.nonNull(suggestions) && !suggestions.isEmpty()) {
                        injectorsCache.put(suggestionsInjector, suggestions);

                        isActiveInjector = true;
                    }
                }

                if (injector instanceof AsyncInjector asyncInjector) {
                    isValidInjector = true;

                    final var asyncApplier = asyncInjector.getAsyncApplier(stringContainer);

                    if (Objects.nonNull(asyncApplier)) {
                        asyncInjectorBuffer.put(asyncInjector, () -> {
                            final var suggestionList = asyncApplier.get();

                            if (Objects.isNull(suggestionList) || suggestionList.isEmpty()) {
                                asyncProcessors.remove(asyncInjector);
                                return;
                            }

                            final var mojangSuggestions = new ArrayList<com.mojang.brigadier.suggestion.Suggestion>();
                            final var offset = injector.getStartOffset();
                            final var textUpToCursor = stringContainer.getContent();

                            suggestionList.forEach(suggestion -> {
                                if (!suggestion.shouldShowFor(textUpToCursor.substring(offset))) return;

                                final var suggestionText = suggestion.getSuggestionText();

                                if (isImplicitSuggestionsReplacement(nonApiSuggestions, suggestionText)) return;

                                mojangSuggestions.add(new com.mojang.brigadier.suggestion.Suggestion(
                                        StringRange.between(offset, textUpToCursor.length()),
                                        suggestionText
                                ));

                                cachedSuggestions.put(suggestionText, suggestion);
                            });

                            if (mojangSuggestions.isEmpty()) {
                                asyncProcessors.remove(asyncInjector);
                                return;
                            }

                            newSuggestionsApplier.accept(textUpToCursor, mojangSuggestions);
                        });

                        isActiveInjector = true;
                        asyncProcessors.remove(asyncInjector);
                    }
                }

                if (!isValidInjector) {
                    LOGGER.error("[Suggestions API] Invalid Injector! (" + injector + ")");

                    continue;
                }

                if (!isActiveInjector) continue;

                if (injector.getStartOffset() == 0) {
                    minOffset = 0;
                    continue;
                }

                if (minOffset != -1) {
                    minOffset = Math.min(minOffset, injector.getStartOffset());
                    continue;
                }

                minOffset = injector.getStartOffset();
            }

            return minOffset;
        }

        private boolean isImplicitSuggestionsReplacement(
                @NotNull List<@NotNull String> nonApiSuggestions,
                @NotNull String suggestionText
        ) {
            if (nonApiSuggestions.contains(suggestionText) || hasSuggestion(suggestionText)) {
                LOGGER.error(
                        "[Suggestions API] Implicit replacement of other suggestions is prohibited! (prohibition for `"
                                + suggestionText
                                + "`)"
                );

                return true;
            }

            return false;
        }
    }
}

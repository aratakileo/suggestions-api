# Suggestions API
API for working with Minecraft suggestions. It allows you to add suggestions with the possibility of changing their rendering. Using this library, you can add them both statically and dynamically.

### Getting started
Merge the following with your

<details><summary><code>build.gradle</code></summary>

```groovy
repositories {
    maven {
        url = "https://api.modrinth.com/maven"
    }
}

dependencies {
    modImplementation "maven.modrinth:suggestions-api:1.0.0"
}
```
</details>

<details><summary><code>build.gradle.kts</code></summary>

```groovy
repositories {
    maven("https://api.modrinth.com/maven")
}

dependencies {
    modImplementation("maven.modrinth", "suggestions-api", "1.0.0")
}
```
</details>

# Quick DOCS
Quick documentation that includes the basics of the library.

### What types of embedded suggestions are there?
The library has two built-in types of suggestions. Below are the functions for their initialization:
- `Suggestion.simple(...)` - simple (just text)
- `Suggestion.withIcon(...)` - with an icon (will be rendered on the left)

The main argument for the initialization function of a simple suggestion will be the text of this suggestion. For example:

```java
final var simpleSuggestion = Suggestion.simple("bonjour!");
```

To initialize a suggestion with an icon, you will need to specify two main arguments: the text of the suggestion and the resource of the icon. This function must be called strictly after loading all the textures of the game, otherwise the game will crash. The barrier texture is taken as an example:

```java
final var suggestionWithIcon = Suggestion.withIcon("barrier", new ResourceLocation("minecraft", "textures/item/barrier.png"));
```

By default, suggestions begin to be shown after the start of entering the text of the suggestion. You can specify your own mapping condition as a lambda function as the last argument. The first argument of the lambda is the text of the suggestion, the second argument is the current expression (the text in the input field between the nearest left space and the cursor). In the example below, the suggestion will be shown if the user fully enters the text of the suggestion:

```java
final var anotherSimpleSuggetion = Suggestion.simple(
        "bonjour!",
        (suggestionText, currentExpression) -> suggestionText.toLowerCase().equals(currentExpression.toLowerCase())
);
```

or

```java
final var anotherSimpleSuggetion = Suggestion.simple("bonjour!", String::equalsIgnoreCase);
```

If you want the suggestions to be displayed always, regardless of the entered text, you can specify condition `(suggestionText, currentExpression) -> true` that is used by default when initializing suggestions in functions `Suggestion.alwaysShown(...)` (as an alternative to the function `Suggestion.simple(...)`) and `Suggestion.alwaysShownWithIcon(...)` (as an alternative to the function `Suggestion.withIcon(...)`):

```java
final var alwaysShownSuggestion = Suggestion.alwaysShown("bonjour!");
```

### How to add new suggestions to the game?
To add your own suggestion use method `SuggestionsAPI.addSuggsetion(...)`:
```java
SuggestionsAPI.addSuggestion(simpleSuggestion);
```

If you are not sure whether the resources will be loaded by the time the resource-dependent suggestions (such as a suggestion with an icon) are initialized, then you can use the method `SuggestionsAPI.registerResourceDependedInjector(...)` to avoid game crash:
```java
SuggestionsAPI.registerResourceDependedInjector(
        () -> List.of(Suggestion.withIcon("barrier", new ResourceLocation("minecraft", "textures/item/barrier.png")))
);
```

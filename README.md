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
    modImplementation "maven.modrinth:suggestions-api:1.0.1"
}
```
</details>

<details><summary><code>build.gradle.kts</code></summary>

```groovy
repositories {
    maven("https://api.modrinth.com/maven")
}

dependencies {
    modImplementation("maven.modrinth", "suggestions-api", "1.0.1")
}
```
</details>

# Quick DOCS
Quick documentation that includes the basics of the library.

### What types of embedded suggestions are there?
The `Suggestion` interface is located in the directory `io.github.aratakileo.suggestionsapi.suggestion`.

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
The `SuggestionsAPI` interface is located in the directory `io.github.aratakileo.suggestionsapi`.

To add your own suggestion use method `SuggestionsAPI.addSuggsetion(...)`:
```java
SuggestionsAPI.addSuggestion(simpleSuggestion);
```

If you are not sure whether the resources will be loaded by the time the resource-dependent suggestions (such as a suggestion with an icon) are initialized, then you can use the method `SuggestionsAPI.addResourceDependedContainer(...)` to avoid game crash:
```java
SuggestionsAPI.addResourceDependedContainer(
        () -> List.of(Suggestion.withIcon("barrier", new ResourceLocation("minecraft", "textures/item/barrier.png")))
);
```

### How to dynamically inject suggestions?
The `Injector` interface is located in the directory `io.github.aratakileo.suggestionsapi.injector`.

There are two types of injectors: simple and asynchronous. To initialize them, the library also provides functions. The first argument of which will be a regex pattern.

To register an injector, it is necessary to pass it to function `SuggestionsAPI.registerInjector(...)` as a single argument.

To create a simple injector, there is a function `Injector.simple(...)`. As the second argument, the function takes a lambda that describes the process of generating a list of suggestions and returns it. At the same time, the lambda has its own two arguments, the first of which contains a string with the current expression (the text in the input field that touches the cursor with the right edge and is found according to the specified pattern), and the second contains a number that is an offset between the beginning of the current expression and the original expression (the text in the input field between the nearest left space and the cursor). As an example, the addition of suggestions of numbers when trying to enter any of them is presented:

```java
SuggestionsAPI.registerInjector(Injector.simple(
        Pattern.compile(":[A-Za-z0-9]+:$"),
        (currentExpression, startOffset) -> Stream.of(
            "67487",
            "nothing",
            "bedrock",
            "bedrock_2"
        ).map(value -> Suggestion.simple(':' + value + ':')).toList()
));
```

or

```java
SuggestionsAPI.registerInjector(Injector.simple(
        Pattern.compile("[A-Za-z0-9]+$"),
        (currentExpression, startOffset) -> Stream.of(
            "Hi, " + currentExpression.substring(startOffset) + '!',
            '"' + currentExpression.substring(startOffset) + '"'
        ).map(Suggestion::alwaysShown).toList()
));
```

or

```java
SuggestionsAPI.registerInjector(Injector.simple(
        Pattern.compile(":[0-9]+:$"),
        (currentExpression, startOffset) -> IntStream.rangeClosed(1000, 1010)
            .boxed()
            .map(Objects::toString)
            .map(Suggestion::alwaysShown)
            .toList()
));

SuggestionsAPI.registerInjector(Injector.simple(
        Pattern.compile("[0-9]$"),
        (currentExpression, startOffset) -> IntStream.rangeClosed(0, 9)
            .boxed()
            .map(Objects::toString)
            .map(Suggestion::alwaysShown)
            .toList()
));
```

By default, if detected string according to the regex pattern of the injector is part of another detected string according to the regex pattern of another injector, then the suggestions of the injector whose string is nested are ignored. This mechanism can be disabled for a specific injector by specifying `true` as the third (last) argument. For example:

```java
SuggestionsAPI.registerInjector(Injector.simple(
        Pattern.compile(":[0-9]+:$"),
        (currentExpression, startOffset) -> IntStream.rangeClosed(1000, 1010)
            .boxed()
            .map(Objects::toString)
            .map(Suggestion::alwaysShown)
            .toList(),
        true
));


// The suggestions of this injector will not appear if the suggestions from the injector above appear, 
// because the interaction string of this injector is included in the interaction string of the injector above

SuggestionsAPI.registerInjector(Injector.simple(
        Pattern.compile("[0-9]$"),
        (currentExpression, startOffset) -> IntStream.rangeClosed(0, 9)
            .boxed()
            .map(Objects::toString)
            .map(Suggestion::alwaysShown)
            .toList(),
        true
));
```

If you need the suggestions to appear synchronously, you can use the function `Injector.async(...)` to initialize the asynchronous injector. The injector initialized with this function provides a mechanism for canceling the current asynchronous process if a request for a new one has been received, and the current process has not had time to complete by this time. This function is similar to the previous one, but this time the second argument, namely lambda, returns a lambda without arguments, which will be launched as an asynchronous process, and has three arguments, the last of which is a lambda that accepts a list of new suggestions and should be used inside the lambda of an asynchronous process. For example:

```java
SuggestionsAPI.registerInjector(Injector.async(
        /* insert your pattern here */,
        (currentExpression, startOffset) -> {
            /* insert your async processing code here */
            
            return /* insert list of suggestion here */;
        }
));
```

Just as in the case of the previous function, a third argument can be specified in this function.

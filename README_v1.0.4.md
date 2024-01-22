Version: [[v1.0.3]](/README.md) | **[v1.0.4]**

# Suggestions API v1.0.4
![](/preview/preview.png)

This library is injected into the Minecraft source code, which is responsible for the logic of Minecraft suggestions in chat, in order to provide a more convenient wrapper for adding or changing suggestions. Currently, the library contains interfaces for:
- adding suggestions synchronous/asynchronous based on the text entered by the user in the text input field
- changing suggestions render
- processing events related to suggestions:
    - on session inited
    - on suggestion selected

for which the library provides ready-made implementations in the form of:
- always shown suggestion (texted suggestion with condition to always show it)
- simple suggestion (texted suggestion with default or custom showing condition)
- icon suggestion (texted suggestion with icon on the left or on the right with default or custom showing condition)
- synchronous/asynchronous suggestions injector (dynamically adding suggestions when entering text into the input field based on matching the specified pattern of regex)

### Which mods use the Suggestions API?
These mods use the Suggestions API:
- [emogg](https://modrinth.com/mod/emogg)
- [JIME](https://modrinth.com/mod/jime)

### Getting started
Not available

# Quick DOCS
Quick documentation that includes the basics of the library.

### What types of embedded suggestions are there?
The `Suggestion` interface is located in `io.github.aratakileo.suggestionsapi.suggestion`.

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
You can add new suggestions to the game using the `Injector` interface that is located in `io.github.aratakileo.suggestionsapi.injector`.

There are two basic types of injectors: simple and asynchronous. To initialize them, the library also provides functions. The first argument of which will be a regex pattern.

To register an injector, it is necessary to pass it to function `SuggestionsAPI.registerInjector(...)` as a single argument.

To create a simple injector, there is a function `Injector.simple(...)` that returns `SuggestionsInjector`. Let's add two new simple suggestions (`Injector.ANYTHING_WITHOUT_SPACES_PATTERN` is `Pattern.compile("\\S+$")`):

```java
SuggestionsAPI.registerInjector(Injector.simple(
        Injector.ANYTHING_WITHOUT_SPACES_PATTERN,
        (stringContainer, startOffset) -> List.of(simpleSuggestion, suggestionWithIcon)  // variables from the example above
));
```

If you want your suggestions not to be displayed when entering a command, you can change the code as follows:

```java
SuggestionsAPI.registerInjector(Injector.simple(
        Injector.ANYTHING_WITHOUT_SPACES_PATTERN,
        (stringContainer, startOffset) -> stringContainer.getContext().isNotCommand() ? List.of(simpleSuggestion, suggestionWithIcon) : null
));
```

As the second argument, function `Injector.simple(...)` takes a lambda that describes the process of generating a list of suggestions and returns it. At the same time, the lambda has its own two arguments, the first of which contains a string with the current expression (the text in the input field that touches the cursor with the right edge and is found according to the specified pattern), and the second contains a number that is an offset between the beginning of the current expression and the original expression (the text in the input field between the nearest left space and the cursor). As an example, the addition of suggestions of numbers when trying to enter any of them is presented:

```java
SuggestionsAPI.registerInjector(Injector.simple(
        Pattern.compile(":[A-Za-z0-9]*(:)?$"),
        (stringContainer, startOffset) -> Stream.of(
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
        (stringContainer, startOffset) -> Stream.of(
            "Hi, " + stringContainer.getContent().substring(startOffset) + '!',
            '"' + stringContainer.getContent().substring(startOffset) + '"'
        ).map(Suggestion::alwaysShown).toList()
));
```

or

```java
SuggestionsAPI.registerInjector(Injector.simple(
        Pattern.compile(":[0-9]*(:)?$"),
        (stringContainer, startOffset) -> IntStream.rangeClosed(1000, 1010)
            .boxed()
            .map(Objects::toString)
            .map(Suggestion::alwaysShown)
            .toList()
));


// The suggestions of this injector will not appear if the suggestions from the injector above appear, 
// because the interaction string of this injector is included in the interaction string of the injector above

SuggestionsAPI.registerInjector(Injector.simple(
        Pattern.compile("[0-9]$"),
        (stringContainer, startOffset) -> IntStream.rangeClosed(0, 9)
            .boxed()
            .map(Objects::toString)
            .map(Suggestion::alwaysShown)
            .toList()
));
```

By default, if detected string according to the regex pattern of the injector is part of another detected string according to the regex pattern of another injector, then the suggestions of the injector whose string is nested are ignored. This mechanism can be disabled for a specific injector by specifying `true` as the third (last) argument. For example:

```java
SuggestionsAPI.registerInjector(Injector.simple(
        Pattern.compile(":[0-9]*(:)?$"),
        (stringContainer, startOffset) -> IntStream.rangeClosed(1000, 1010)
            .boxed()
            .map(Objects::toString)
            .map(Suggestion::alwaysShown)
            .toList(),
        true
));


// The suggestions of this injector will appear if the suggestions from the injector above appear

SuggestionsAPI.registerInjector(Injector.simple(
        Pattern.compile("[0-9]$"),
        (stringContainer, startOffset) -> IntStream.rangeClosed(0, 9)
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
        (stringContainer, startOffset) -> {
            /* insert your async processing code here */
            
            return /* insert list of suggestion here */;
        }
));
```

Function `Injector.async(...)` returns `AsyncInjector`. Just as in the case of the previous function, a third argument can be specified in this function.

### Can I replace Suggestions with new instances?
The Suggestions API prohibits implicit replacement of suggestions, but it allows you to replace suggestions that were not added using it (that is, suggestions that were added by Minecraft or other mods that do not use the Suggestions API). If the suggestion you need has not yet been replaced by another mod, you can replace the suggestion using the `ReplacementInjector` that is located in `io.github.aratakileo.suggestionsapi.injector`. For example:

```java
// To check this, start entering the command `give @s minecraft:barrier` in the chat or in the command block
SuggestionsAPI.registerInjector(Injector.replacement(
        nonApiSuggestion -> nonApiSuggestion.equals("minecraft:barrier") ? Suggestion.withIcon(
                nonApiSuggestion,
                new ResourceLocation("minecraft", "textures/item/barrier.png")
        ) : null
));
```

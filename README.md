# Suggestions API
API for working with Minecraft suggestions

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

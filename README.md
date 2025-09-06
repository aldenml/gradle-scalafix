# gradle-scalafix

[![ci](https://github.com/aldenml/gradle-scalafix/workflows/ci/badge.svg)](https://github.com/aldenml/gradle-scalafix/actions?query=workflow%3ci)

A Gradle plugin that integrates [Scalafix](https://scalacenter.github.io/scalafix/),
a refactoring and linting tool for Scala, into your Gradle build process. This
plugin enables automatic code refactoring, linting, and migration assistance
for Scala 3 projects using Gradle as the build system.

Key Features:

- Automatically integrates with existing Gradle Scala projects.
- Specifically designed for Scala 3 projects.
- Customizable settings through the plugin extension.
- Scalafix tasks are properly integrated into your build lifecycle.
- Automatically processes all configured Scala source sets.
- Requires Gradle 9.0.0 or later.

## Usage

To use this plugin, add the following to your `build.gradle` file:
```groovy
plugins {
    id 'scala'
    id 'com.maindx.gradle.scalafix' version '<version>'
}

repositories {
    mavenCentral()
}

scala {
    scalaVersion = "3.3.6" // or later
}
```
if you are using Kotlin DSL:
```kotlin
plugins {
    scala
    application
    id("com.maindx.gradle.scalafix") version "<version>"
}

repositories {
    mavenCentral()
}

scala {
    scalaVersion = "3.3.6" // or later
}
```

### Scalafix configuration

Scalafix won't run any rules unless they are configured, either via configuration
file or command line argument. Check out Scalafix's [built-in rules](https://scalacenter.github.io/scalafix/docs/rules/overview.html).

To configure this plugin via a configuration file, create a file named `.scalafix.conf`
in the root directory of your project with the rules you want to have enabled:
```hocon
rules = [
  DisableSyntax
]

DisableSyntax.noNulls = true
DisableSyntax.noVars = true
```

You can specify files per projects with different rules.

### Tasks

The following Gradle tasks are created when the plugin is applied:

| Name                       | Description                                                                       |
|:---------------------------|-----------------------------------------------------------------------------------|
| `scalafix`                 | Run scalafix on all sources.                                                      |
| `scalafix<SourceSet>`      | Run scalafix only for the specific source set.                                    |
| `scalafixCheck`            | Checks that all sources are compliant with the rules.                             |
| `scalafixCheck<SourceSet>` | Checks that all sources for the specific source set are compliant with the rules. |

### Extension
The plugin defines the `scalafix` extension which enables some customizations:

| Property name | Type                  | Description                                                                                                                         |
|:--------------|-----------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| `configFile`  | `RegularFileProperty` | Specify the Scalafix configuration file, defaults to a `.scalafix.conf` in the current project directory or root project directory. |
| `includes`    | `SetProperty<String>` | Filter what Scala source files should be included by Scalafix.                                                                      |
| `excludes`    | `SetProperty<String>` | Filter what Scala source files should be excluded by Scalafix.                                                                      |
| `semanticdb`  | `Property<Boolean>`   | Used to specify if semanticdb generation is enabled.                                                                                |

Example:
```groovy
scalafix {
    configFile = file("other/customcalafix.conf")
    includes = ["**/*Foo.scala"]
    excludes = ["**/*Bar.scala"]
    semanticdb = false
}
```

## Development requirements

`pre-commit`

Use `pip install pre-commit` or equivalent for your environment.
Then run `pre-commit install`. You can manually run it with `pre-commit run --all-files`.

`Java 21`

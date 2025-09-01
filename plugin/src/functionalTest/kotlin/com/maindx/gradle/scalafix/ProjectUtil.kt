/*
 * Copyright (C) 2025, Alden Torres
 */

package com.maindx.gradle.scalafix

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText

fun createScalaProject(additionalConfig: String = ""): Path {
    val projectDir = createTempDirectory()

    val settingsFile = projectDir.resolve("settings.gradle")
    settingsFile.writeText("""rootProject.name = "test-scalafix"""")

    val buildFile = projectDir.resolve("build.gradle")
    buildFile.writeText(
        """
        plugins {
            id('scala')
            id('com.maindx.gradle.scalafix')
        }

        repositories {
            mavenCentral()
        }

        scala {
            scalaVersion = "3.3.6"
        }

        $additionalConfig
        """.trimIndent(),
    )

    return projectDir
}

fun createSourceFile(
    projectDir: Path,
    sourceSet: String,
    sourceFile: String,
    content: String,
): Path {
    val relativePath = Path.of("src", sourceSet, "scala", sourceFile)
    val file = projectDir.resolve(relativePath)
    file.toFile().parentFile.mkdirs()
    file.writeText(content)
    return file
}

fun createScalafixConfig(
    projectDir: Path,
    content: String,
): Path {
    val file = projectDir.resolve(".scalafix.conf")
    file.writeText(content)
    return file
}

fun runGradle(
    projectDir: Path,
    vararg arguments: String,
): BuildResult {
    val runner = GradleRunner.create()
    runner.forwardOutput()
    runner.withPluginClasspath()
    runner.withArguments(arguments.toList() + "--configuration-cache")
    runner.withProjectDir(projectDir.toFile())
    val result = runner.build()
    return result
}

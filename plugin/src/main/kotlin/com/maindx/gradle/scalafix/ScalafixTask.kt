/*
 * Copyright (C) 2025, Alden Torres
 */

package com.maindx.gradle.scalafix

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.work.ChangeType
import org.gradle.work.InputChanges
import scalafix.interfaces.Scalafix
import scalafix.interfaces.ScalafixMainMode
import java.nio.file.Path
import kotlin.io.path.Path

@CacheableTask
abstract class ScalafixTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val cliJars: ConfigurableFileCollection

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val configFile: RegularFileProperty

    @get:Input
    abstract val sourceRoot: Property<String>

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    enum class Mode {
        FIX,
        CHECK,
    }

    @get:Input
    abstract val mode: Property<Mode>

    @get:Input
    abstract val scalaVersion: Property<String>

    @get:Input
    @get:Optional
    abstract val compileOptions: ListProperty<String>

    @get:OutputFiles
    abstract val outputFiles: ConfigurableFileCollection

    @TaskAction
    fun run(inputChanges: InputChanges) {
        val scalafixCliArtifact = ScalafixInterfaces.getScalafixCliArtifact()
        val scalafixMode = getScalafixMode()
        val configFilePath = java.util.Optional.ofNullable(configFile.orNull?.asFile?.toPath())

        val sourcePaths = getSourcePaths(inputChanges)
        if (sourcePaths.isEmpty()) {
            logger.debug("No changed Scala source files found - skipping Scalafix")
        }

        logger.debug(
            """
            Running ScalafixTask with:
             - Scalafix cli jars: ${cliJars.files}
             - Mode: $scalafixMode
             - Config file: ${configFile.orNull}
             - Scala version: ${scalaVersion.orNull}
             - Scalac options: ${compileOptions.orNull}
             - Source root: ${sourceRoot.orNull}
             - Sources: ${sourceFiles.files}
             - Classpath: ${classpath.files}
            """.trimIndent(),
        )

        val scalafixClassloader = ScalafixInterfaces.getScalafixCliClassloader(scalafixCliArtifact, cliJars.files)

        val scalafixArgs =
            Scalafix
                .classloadInstance(scalafixClassloader)
                .newArguments()
                .withMode(scalafixMode)
                .withConfig(configFilePath)
                .withSourceroot(Path(sourceRoot.get()))
                .withPaths(sourcePaths)
                .withClasspath(classpath.files.map { it.toPath() })
                .withScalaVersion(scalaVersion.get())
                .withScalacOptions(compileOptions.getOrElse(emptyList()))

        logger.debug(
            """
            Scalafix rules:
             - Available: ${scalafixArgs.availableRules().map { it.name() }}
             - That will run: ${scalafixArgs.rulesThatWillRun().map { it.name() }}
            """.trimIndent(),
        )

        if (scalafixArgs.rulesThatWillRun().isNotEmpty()) {
            logger.debug("Running Scalafix on ${sourcePaths.size} Scala source file(s)")
            val errors = scalafixArgs.run()
            if (errors.size > 0) {
                throw ScalafixException(errors)
            }
        } else {
            logger.warn("No Scalafix rules to run")
        }
    }

    private fun getScalafixMode(): ScalafixMainMode =
        when (mode.get()) {
            Mode.FIX -> ScalafixMainMode.IN_PLACE
            Mode.CHECK -> ScalafixMainMode.CHECK
        }

    private fun getSourcePaths(inputChanges: InputChanges): List<Path?> =
        if (inputChanges.isIncremental) {
            // Only process changed/added files
            val changedFiles =
                inputChanges
                    .getFileChanges(sourceFiles)
                    .filter { it.changeType != ChangeType.REMOVED }
                    .map { it.file.toPath() }

            if (changedFiles.isNotEmpty()) {
                logger.debug("Processing ${changedFiles.size} changed Scala source file(s)")
            }

            changedFiles
        } else {
            // Full rebuild - process all files
            logger.debug("Full rebuild - processing all Scala source files")
            sourceFiles.files.map { it.toPath() }
        }
}

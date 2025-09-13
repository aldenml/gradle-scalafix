/*
 * Copyright (C) 2025, Alden Torres
 */

package com.maindx.gradle.scalafix

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.plugins.scala.ScalaPluginExtension
import org.gradle.api.tasks.ScalaSourceDirectorySet
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.scala.ScalaCompile
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.pathString

class ScalafixPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (isScalaPluginPresent(project)) {
            val extension = project.extensions.create(EXTENSION, ScalafixExtension::class.java, project)

            project.afterEvaluate {
                if (isScala3(project)) {
                    configureTasks(project, extension)
                } else {
                    throw GradleException("The 'com.maindx.gradle.scalafix' plugin only supports Scala 3 projects.")
                }
            }
        } else {
            throw GradleException("The 'com.maindx.gradle.scalafix' plugin requires the 'scala' plugin to be applied.")
        }
    }

    private fun configureTasks(
        project: Project,
        extension: ScalafixExtension,
    ) {
        val fixTask =
            project.tasks.register(FIX_TASK) { task ->
                task.group = TASK_GROUP
                task.description = "Run scalafix on all sources."
            }

        val checkTask =
            project.tasks.register(CHECK_TASK) { task ->
                task.group = TASK_GROUP
                task.description = "Run scalafix and fail if produces a diff or a linter error."
            }

        project.tasks.named("check").configure {
            it.dependsOn(checkTask)
        }

        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        sourceSets.forEach { sourceSet ->
            val scalaCompileTaskName = sourceSet.getCompileTaskName("scala")
            val scalaCompileTask = project.tasks.findByName(scalaCompileTaskName) as? ScalaCompile

            if (scalaCompileTask != null) {
                configureScalaCompileTask(extension, project, scalaCompileTask)

                val cliJars = findCliJars(project)

                configureScalafixTaskForSourceSet(
                    project,
                    sourceSet,
                    ScalafixTask.Mode.FIX,
                    fixTask.get(),
                    scalaCompileTask,
                    cliJars,
                    extension,
                )
                configureScalafixTaskForSourceSet(
                    project,
                    sourceSet,
                    ScalafixTask.Mode.CHECK,
                    checkTask.get(),
                    scalaCompileTask,
                    cliJars,
                    extension,
                )
            } else {
                throw GradleException("No scala compile task found for source set '${sourceSet.name}'")
            }
        }
    }

    private fun configureScalaCompileTask(
        extension: ScalafixExtension,
        project: Project,
        scalaCompileTask: ScalaCompile,
    ) {
        if (extension.semanticdb.get()) {
            val semanticdbOptions =
                listOf(
                    "-Xsemanticdb",
                    "-sourceroot",
                    getSourceRoot(project),
                )

            val currentOptions = scalaCompileTask.scalaCompileOptions.additionalParameters ?: emptyList()

            // only add if not already present
            val newOptions =
                semanticdbOptions.filterNot { option ->
                    currentOptions.contains(option)
                }

            if (newOptions.isNotEmpty()) {
                scalaCompileTask.scalaCompileOptions.additionalParameters = currentOptions + newOptions
            }
        }
    }

    @Suppress("LongParameterList")
    private fun configureScalafixTaskForSourceSet(
        project: Project,
        sourceSet: SourceSet,
        mode: ScalafixTask.Mode,
        parentTask: Task,
        scalaCompileTask: ScalaCompile,
        cliJars: Set<File>,
        extension: ScalafixExtension,
    ) {
        val scalafixTaskName = parentTask.name + sourceSet.name.capitalize()
        val scalafixTask =
            project.tasks.register(scalafixTaskName, ScalafixTask::class.java) { task ->
                task.group = parentTask.group
                task.description = "${parentTask.description} in '${sourceSet.name}'"

                task.cliJars.from(cliJars)
                task.configFile.set(extension.configFile)

                val scalaSourceSet = sourceSet.extensions.findByType(ScalaSourceDirectorySet::class.java)
                if (scalaSourceSet != null) {
                    val scalaSources =
                        scalaSourceSet.matching { patternFilterable ->
                            extension.includes.orNull?.let { includes ->
                                patternFilterable.include(includes)
                            }
                            extension.excludes.orNull?.let { excludes ->
                                patternFilterable.exclude(excludes)
                            }
                        }

                    task.sourceFiles.from(scalaSources)
                    task.outputFile.set(
                        project.layout.buildDirectory.file(
                            Path("scalafix", "$scalafixTaskName-state.txt").pathString,
                        ),
                    )
                } else {
                    throw GradleException("No scala source directory set found for source set '${sourceSet.name}'")
                }

                task.sourceRoot.set(getSourceRoot(project))

                task.classpath.from(getFullClasspath(sourceSet))

                task.mode.set(mode)
                task.scalaVersion.set(getScalaVersion(project))
                task.compileOptions.set(scalaCompileTask.scalaCompileOptions.additionalParameters)

                if (extension.semanticdb.get()) {
                    task.dependsOn(scalaCompileTask)
                }
            }

        parentTask.dependsOn(scalafixTask)
    }

    private fun isScalaPluginPresent(project: Project) = project.plugins.hasPlugin(ScalaPlugin::class.java)

    private fun getScalaVersion(project: Project): String? {
        val scalaExtension = project.extensions.findByType(ScalaPluginExtension::class.java)
        return scalaExtension?.let { extension ->
            try {
                // Try reflection to find available scala version properties
                val methods =
                    extension.javaClass.methods
                        .filter {
                            it.name.contains("scala", ignoreCase = true) &&
                                it.name.contains("version", ignoreCase = true)
                        }

                // Try common property names
                when {
                    methods.any { it.name == "getScalaCompilerVersion" } -> {
                        val method = extension.javaClass.getMethod("getScalaCompilerVersion")
                        @Suppress("UNCHECKED_CAST")
                        (method.invoke(extension) as? org.gradle.api.provider.Property<String>)?.orNull
                    }
                    methods.any { it.name == "getScalaVersion" } -> {
                        val method = extension.javaClass.getMethod("getScalaVersion")
                        @Suppress("UNCHECKED_CAST")
                        (method.invoke(extension) as? org.gradle.api.provider.Property<String>)?.orNull
                    }
                    else -> {
                        // If we can't find a scala version property, return null for now
                        // This will make isScala3 return false, which should be safe
                        null
                    }
                }
            } catch (e: NoSuchMethodException) {
                // Method not found, return null
                project.logger.debug("Scala version method not found: ${e.message}")
                null
            } catch (e: IllegalAccessException) {
                // Cannot access method, return null
                project.logger.debug("Cannot access scala version method: ${e.message}")
                null
            } catch (e: java.lang.reflect.InvocationTargetException) {
                // Method invocation failed, return null
                project.logger.debug("Scala version method invocation failed: ${e.message}")
                null
            }
        }
    }

    private fun isScala3(project: Project): Boolean {
        val scalaVersion = getScalaVersion(project)

        return scalaVersion?.startsWith("3.") ?: false
    }

    private fun findCliJars(project: Project): Set<File> {
        val cliDependency = project.dependencies.create(ScalafixInterfaces.getScalafixCliArtifact())
        val cliConfiguration = project.configurations.detachedConfiguration(cliDependency)

        return cliConfiguration.files
    }

    private fun getFullClasspath(sourceSet: SourceSet): FileCollection {
        val classesDirs = sourceSet.output.classesDirs
        val compileClasspath = sourceSet.compileClasspath
        return classesDirs + compileClasspath
    }

    private fun getSourceRoot(project: Project): String = project.projectDir.absolutePath

    companion object {
        const val EXTENSION = "scalafix"
        const val TASK_GROUP = "scalafix"
        const val FIX_TASK = "scalafix"
        const val CHECK_TASK = "scalafixCheck"

        private fun String.capitalize() = replaceFirstChar { it.titlecase() }
    }
}

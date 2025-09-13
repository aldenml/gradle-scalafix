/*
 * Copyright (C) 2025, Alden Torres
 */

package com.maindx.gradle.scalafix

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.scala.ScalaPluginExtension
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.testfixtures.ProjectBuilder

fun buildScalaProject(
    withScalaPlugin: Boolean = true,
    withScalafixPlugin: Boolean = true,
    withScalaVersion: String = "3.3.6",
    parent: Project? = null,
    evaluate: Boolean = true,
): ProjectInternal {
    val project =
        ProjectBuilder
            .builder()
            .withParent(parent)
            .build()

    with(project) {
        if (withScalaPlugin) plugins.apply("scala")

        if (withScalafixPlugin) plugins.apply("com.maindx.gradle.scalafix")

        repositories.mavenCentral()

        extensions.configure(ScalaPluginExtension::class.java) { extension ->
            extension.scalaVersion.set(withScalaVersion)
        }

        tasks.withType(ScalaCompile::class.java) {
            it.scalaCompileOptions.additionalParameters.add("-Ywarn-unused")
        }
    }

    createSrcFile(project, "src/main/scala/Foo.scala", "object Foo")
    createSrcFile(project, "src/test/scala/FooTest.scala", "object FooTest")

    val internalProject = project as ProjectInternal

    if (evaluate) internalProject.evaluate()

    return internalProject
}

fun createSrcFile(
    project: Project,
    path: String,
    content: String,
) {
    val file = project.file(path)
    file.parentFile.mkdirs()
    file.writeText(content)
}

fun Task.dependsOnTask(taskName: String): Boolean =
    dependsOn.any {
        ((it is TaskProvider<*>) && it.name == taskName) || (it is Task && it.name == taskName)
    }

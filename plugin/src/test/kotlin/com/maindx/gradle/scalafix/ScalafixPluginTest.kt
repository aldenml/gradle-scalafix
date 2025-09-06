/*
 * Copyright (C) 2025, Alden Torres
 */

package com.maindx.gradle.scalafix

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertFalse
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ScalafixPluginTest {
    @Test
    fun `plugin registers task`() {
        val scalaProject = buildScalaProject()

        val tasksToVerify =
            listOf(
                "scalafix",
                "scalafixMain",
                "scalafixTest",
                "scalafixCheck",
                "scalafixCheckMain",
                "scalafixCheckTest",
            )

        tasksToVerify.forEach { taskName ->
            assertNotNull(scalaProject.tasks.findByName(taskName))
        }
    }

    @Test
    fun `plugin registers extension`() {
        val scalaProject = buildScalaProject()

        assertNotNull(scalaProject.extensions.findByName(ScalafixPlugin.EXTENSION))
    }

    @Test
    fun `requires scala plugin`() {
        assertFailsWith<GradleException> {
            buildScalaProject(withScalaPlugin = false)
        }
    }

    @Test
    fun `requires scala 3`() {
        assertFailsWith<GradleException> {
            buildScalaProject(withScalaVersion = "2.13.12")
        }
    }

    @Test
    fun `verify scalafixCheck task configuration`() {
        val scalaProject = buildScalaProject()

        val scalafixCheck = scalaProject.tasks.getByName("scalafixCheck")

        assertTrue(scalafixCheck.dependsOnTask("scalafixCheckMain"))
        assertTrue(scalafixCheck.dependsOnTask("scalafixCheckTest"))

        val check = scalaProject.tasks.getByName("check")

        assertTrue(check.dependsOnTask("scalafixCheck"))
    }

    @Test
    fun `verify scalafixCheckMain task configuration`() {
        val scalaProject = buildScalaProject(evaluate = false)

        val ext = scalaProject.extensions.getByType(ScalafixExtension::class.java)
        ext.configFile.set(scalaProject.file(".custom.conf"))

        scalaProject.evaluate()

        val task = scalaProject.tasks.getByName("scalafixCheckMain")
        assertTrue(task.dependsOnTask("compileScala"))
        assertFalse(task.dependsOnTask("scalafix"))

        assertTrue(task is ScalafixTask)
        assertEquals(ScalafixTask.Mode.CHECK, task.mode.get())
        assertEquals(scalaProject.file(".custom.conf"), task.configFile.get().asFile)
        assertEquals(scalaProject.projectDir.absolutePath, task.sourceRoot.get())
        assertEquals(
            setOf(File(scalaProject.projectDir, "/src/main/scala/Foo.scala")),
            task.sourceFiles.files,
        )
        task.classpath.contains(File(scalaProject.projectDir, "/build/classes/scala/main"))
        task.classpath.contains(File(scalaProject.projectDir, "/build/classes/java/main"))
    }

    @Test
    fun `verify scalafixCheckTest task configuration`() {
        val scalaProject = buildScalaProject(evaluate = false)

        val ext = scalaProject.extensions.getByType(ScalafixExtension::class.java)
        ext.configFile.set(scalaProject.file(".custom.conf"))

        scalaProject.evaluate()

        val task = scalaProject.tasks.getByName("scalafixCheckTest")
        assertTrue(task.dependsOnTask("compileTestScala"))

        assertTrue(task is ScalafixTask)
        assertEquals(ScalafixTask.Mode.CHECK, task.mode.get())
        assertEquals(scalaProject.file(".custom.conf"), task.configFile.get().asFile)
        assertEquals(scalaProject.projectDir.absolutePath, task.sourceRoot.get())
        assertEquals(
            setOf(File(scalaProject.projectDir, "/src/test/scala/FooTest.scala")),
            task.sourceFiles.files,
        )
        task.classpath.contains(File(scalaProject.projectDir, "/build/classes/scala/test"))
        task.classpath.contains(File(scalaProject.projectDir, "/build/classes/java/test"))
    }

    @Test
    fun `verify scalafix task configuration`() {
        val scalaProject = buildScalaProject()

        val scalafix = scalaProject.tasks.getByName("scalafix")

        assertTrue(scalafix.dependsOnTask("scalafixMain"))
        assertTrue(scalafix.dependsOnTask("scalafixTest"))

        val check = scalaProject.tasks.getByName("check")

        assertFalse(check.dependsOnTask("scalafix"))
    }

    @Test
    fun `verify scalafixMain task configuration`() {
        val scalaProject = buildScalaProject(evaluate = false)

        val ext = scalaProject.extensions.getByType(ScalafixExtension::class.java)
        ext.configFile.set(scalaProject.file(".custom.conf"))

        scalaProject.evaluate()

        val task = scalaProject.tasks.getByName("scalafixMain")
        assertTrue(task.dependsOnTask("compileScala"))

        assertTrue(task is ScalafixTask)
        assertEquals(ScalafixTask.Mode.FIX, task.mode.get())
        assertEquals(scalaProject.file(".custom.conf"), task.configFile.get().asFile)
        assertEquals(scalaProject.projectDir.absolutePath, task.sourceRoot.get())
        assertEquals(
            setOf(File(scalaProject.projectDir, "/src/main/scala/Foo.scala")),
            task.sourceFiles.files,
        )
        task.classpath.contains(File(scalaProject.projectDir, "/build/classes/scala/main"))
        task.classpath.contains(File(scalaProject.projectDir, "/build/classes/java/main"))
    }

    @Test
    fun `verify scalafixTest task configuration`() {
        val scalaProject = buildScalaProject(evaluate = false)

        val ext = scalaProject.extensions.getByType(ScalafixExtension::class.java)
        ext.configFile.set(scalaProject.file(".custom.conf"))

        scalaProject.evaluate()

        val task = scalaProject.tasks.getByName("scalafixTest")
        assertTrue(task.dependsOnTask("compileTestScala"))

        assertTrue(task is ScalafixTask)
        assertEquals(ScalafixTask.Mode.FIX, task.mode.get())
        assertEquals(scalaProject.file(".custom.conf"), task.configFile.get().asFile)
        assertEquals(scalaProject.projectDir.absolutePath, task.sourceRoot.get())
        assertEquals(
            setOf(File(scalaProject.projectDir, "/src/test/scala/FooTest.scala")),
            task.sourceFiles.files,
        )
        task.classpath.contains(File(scalaProject.projectDir, "/build/classes/scala/test"))
        task.classpath.contains(File(scalaProject.projectDir, "/build/classes/java/test"))
    }

    @Test
    fun `scalafix uses the provided config file`() {
        val rootProject = ProjectBuilder.builder().withName("root").build()
        val rootProjectConfig = rootProject.file(".scalafix.conf")
        rootProjectConfig.writeText("rules = [A, B]")

        val scalaProject = buildScalaProject(parent = rootProject, evaluate = false)
        val scalaProjectConfig = scalaProject.file(".custom.conf")
        scalaProjectConfig.writeText("rules = [C, D]")

        val ext = scalaProject.extensions.getByType(ScalafixExtension::class.java)
        ext.configFile.set(scalaProjectConfig)

        scalaProject.evaluate()

        val task = scalaProject.tasks.getByName("scalafixCheckMain")

        assertTrue(task is ScalafixTask)
        assertEquals(
            scalaProjectConfig.absolutePath,
            task.configFile
                .get()
                .asFile.absolutePath,
        )
    }

    @Test
    fun `scalafix uses the root config file if not provided`() {
        val rootProject = ProjectBuilder.builder().withName("root").build()
        val rootProjectConfig = rootProject.file(".scalafix.conf")
        rootProjectConfig.writeText("rules = [A, B]")

        val scalaProject = buildScalaProject(parent = rootProject)

        val task = scalaProject.tasks.getByName("scalafixCheckMain")

        assertTrue(task is ScalafixTask)
        assertEquals(
            rootProjectConfig.absolutePath,
            task.configFile
                .get()
                .asFile.absolutePath,
        )
    }

    @Test
    fun `scalafix uses no config file if not provided`() {
        val rootProject = ProjectBuilder.builder().withName("root").build()

        val scalaProject = buildScalaProject(parent = rootProject)

        val task = scalaProject.tasks.getByName("scalafixCheckMain")

        assertTrue(task is ScalafixTask)
        assertTrue(!task.configFile.isPresent)
    }

    @Test
    fun `scalafix include sources matching includes filter`() {
        val scalaProject = buildScalaProject(evaluate = false)
        createSrcFile(scalaProject, "src/main/scala/A.scala", "object A")
        createSrcFile(scalaProject, "src/main/scala/B.scala", "object B")

        val ext = scalaProject.extensions.getByType(ScalafixExtension::class.java)
        ext.includes.set(listOf("**/A.scala"))

        scalaProject.evaluate()

        val task = scalaProject.tasks.getByName("scalafixMain")

        assertTrue(task is ScalafixTask)
        assertEquals(
            setOf(File(scalaProject.projectDir, "/src/main/scala/A.scala")),
            task.sourceFiles.files,
        )
    }

    @Test
    fun `scalafix exclude sources matching excludes filter`() {
        val scalaProject = buildScalaProject(evaluate = false)
        createSrcFile(scalaProject, "src/main/scala/A.scala", "object A")
        createSrcFile(scalaProject, "src/main/scala/B.scala", "object B")

        val ext = scalaProject.extensions.getByType(ScalafixExtension::class.java)
        ext.excludes.set(listOf("**/A.scala", "**/B.scala"))

        scalaProject.evaluate()

        val task = scalaProject.tasks.getByName("scalafixMain")

        assertTrue(task is ScalafixTask)
        assertEquals(
            setOf(File(scalaProject.projectDir, "/src/main/scala/Foo.scala")),
            task.sourceFiles.files,
        )
    }

    @Test
    fun `scalafix select sources matching includes and excludes filter`() {
        val scalaProject = buildScalaProject(evaluate = false)
        createSrcFile(scalaProject, "src/main/scala/A.scala", "object A")
        createSrcFile(scalaProject, "src/main/scala/B.scala", "object B")

        val ext = scalaProject.extensions.getByType(ScalafixExtension::class.java)
        ext.includes.set(listOf("**/*.scala"))
        ext.excludes.set(listOf("**/B.scala"))

        scalaProject.evaluate()

        val task = scalaProject.tasks.getByName("scalafixMain")

        assertTrue(task is ScalafixTask)
        assertEquals(
            setOf(
                File(scalaProject.projectDir, "/src/main/scala/Foo.scala"),
                File(scalaProject.projectDir, "/src/main/scala/A.scala"),
            ),
            task.sourceFiles.files,
        )
    }
}

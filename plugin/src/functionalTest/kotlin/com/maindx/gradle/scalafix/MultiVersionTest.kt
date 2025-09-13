/*
 * Copyright (C) 2025, Alden Torres
 */

package com.maindx.gradle.scalafix

import kotlin.test.Test
import kotlin.test.assertTrue

@Suppress("FunctionName")
class MultiVersionTest {
    private val gradleVersions = listOf("9.0.0", "8.14.3")

    @Test
    fun `scalafix works with multiple gradle versions`() {
        val projectDir = createScalaProject()
        createSourceFile(projectDir, "main", "Foo.scala", "object Foo")

        val results = runGradleWithVersions(projectDir, arguments = listOf("scalafix"), gradleVersions)

        results.forEach { (_, result) ->
            assertTrue(
                result.output.contains("No Scalafix rules to run"),
            )
        }
    }

    @Test
    fun `scalafixCheck works with multiple gradle versions`() {
        val projectDir = createScalaProject()
        createSourceFile(projectDir, "main", "Foo.scala", "object Foo")

        val results = runGradleWithVersions(projectDir, arguments = listOf("scalafixCheck"), gradleVersions)

        results.forEach { (_, result) ->
            assertTrue(
                result.output.contains("No Scalafix rules to run"),
            )
        }
    }
}

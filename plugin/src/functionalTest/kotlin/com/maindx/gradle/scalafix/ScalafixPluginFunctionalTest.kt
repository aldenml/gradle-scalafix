/*
 * Copyright (C) 2025, Alden Torres
 */

package com.maindx.gradle.scalafix

import java.io.File
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@Suppress("FunctionName")
class ScalafixPluginFunctionalTest {
    @Test
    fun `can run scalafix task`() {
        val projectDir = createScalaProject()
        createSourceFile(projectDir, "main", "Foo.scala", "object Foo")
        val result = runGradle(projectDir, "scalafix")

        assertTrue(result.output.contains("No Scalafix rules to run"))
    }

    @Test
    fun `scalafixMain runs compileScala`() {
        val projectDir = createScalaProject()
        createSourceFile(projectDir, "main", "Foo.scala", "object Foo")
        val result = runGradle(projectDir, "scalafix", "-m")

        assertTrue(
            result.output.contains(
                """
                :compileScala SKIPPED
                :scalafixMain SKIPPED
                """.trimIndent(),
            ),
        )

        assertTrue(
            result.output.contains(
                """
                :compileTestScala SKIPPED
                :scalafixTest SKIPPED
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `scalafixCheck runs compileScala`() {
        val projectDir = createScalaProject()
        createSourceFile(projectDir, "main", "Foo.scala", "object Foo")
        val result = runGradle(projectDir, "scalafixCheck", "-m")

        assertTrue(
            result.output.contains(
                """
                :compileScala SKIPPED
                :scalafixCheckMain SKIPPED
                """.trimIndent(),
            ),
        )

        assertTrue(
            result.output.contains(
                """
                :compileTestScala SKIPPED
                :scalafixCheckTest SKIPPED
                :scalafixCheck SKIPPED
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `scalafixSourceSet runs compileSourceSetScala`() {
        val projectDir =
            createScalaProject(
                """
                sourceSets {
                    otherTest {
                        compileClasspath += sourceSets.test.compileClasspath
                    }
                }
                """.trimIndent(),
            )
        createSourceFile(projectDir, "main", "Foo.scala", "object Foo")
        val result = runGradle(projectDir, "scalafix", "-m")

        assertTrue(
            result.output.contains(
                """
                :compileOtherTestScala SKIPPED
                :scalafixOtherTest SKIPPED
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `check runs scalafixCheck and does not run scalafix`() {
        val projectDir = createScalaProject()
        createSourceFile(projectDir, "main", "Foo.scala", "object Foo")
        val result = runGradle(projectDir, "check", "-m")

        assertTrue(result.output.contains(":scalafixCheckMain SKIPPED"))
        assertTrue(result.output.contains(":scalafixCheckTest SKIPPED"))
        assertTrue(result.output.contains(":scalafixCheck SKIPPED"))
        assertTrue(result.output.contains(":check SKIPPED"))

        assertFalse(result.output.contains(":scalafixMain SKIPPED"))
        assertFalse(result.output.contains(":scalafixTest SKIPPED"))
        assertFalse(result.output.contains(":scalafix SKIPPED"))
    }

    @Test
    fun `scalafix tasks are grouped`() {
        val projectDir =
            createScalaProject(
                """
                sourceSets {
                    other {
                        compileClasspath += sourceSets.main.compileClasspath
                    }
                }
                """.trimIndent(),
            )
        val result = runGradle(projectDir, "tasks")

        assertTrue(
            result.output.contains(
                """
                Scalafix tasks
                --------------
                scalafix - Run scalafix on all sources.
                scalafixCheck - Run scalafix and fail if produces a diff or a linter error.
                scalafixCheckMain - Run scalafix and fail if produces a diff or a linter error. in 'main'
                scalafixCheckOther - Run scalafix and fail if produces a diff or a linter error. in 'other'
                scalafixCheckTest - Run scalafix and fail if produces a diff or a linter error. in 'test'
                scalafixMain - Run scalafix on all sources. in 'main'
                scalafixOther - Run scalafix on all sources. in 'other'
                scalafixTest - Run scalafix on all sources. in 'test'
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `semanticdb files are generated with scalafix (semanticdb = true)`() {
        val projectDir =
            createScalaProject(
                """
                scalafix {
                    semanticdb = true
                }
                """.trimIndent(),
            )
        createSourceFile(projectDir, "main", "Foo.scala", "object Foo")
        createSourceFile(projectDir, "test", "FooTest.scala", "object FooTest")
        runGradle(projectDir, "scalafix")

        assertTrue(
            File(
                projectDir.toFile(),
                "build/classes/scala/main/META-INF/semanticdb/src/main/scala/Foo.scala.semanticdb",
            ).exists(),
        )
        assertTrue(
            File(
                projectDir.toFile(),
                "build/classes/scala/test/META-INF/semanticdb/src/test/scala/FooTest.scala.semanticdb",
            ).exists(),
        )
    }

    @Test
    fun `semanticdb files are not generated with scalafix (semanticdb = false)`() {
        val projectDir =
            createScalaProject(
                """
                scalafix {
                    semanticdb = false
                }
                """.trimIndent(),
            )
        createSourceFile(projectDir, "main", "Foo.scala", "object Foo")
        createSourceFile(projectDir, "test", "FooTest.scala", "object FooTest")
        runGradle(projectDir, "scalafix")

        assertFalse(
            File(
                projectDir.toFile(),
                "build/classes/scala/main/META-INF/semanticdb/src/main/scala/Foo.scala.semanticdb",
            ).exists(),
        )
        assertFalse(
            File(
                projectDir.toFile(),
                "build/classes/scala/test/META-INF/semanticdb/src/test/scala/FooTest.scala.semanticdb",
            ).exists(),
        )
    }

    @Test
    fun `scalafix and scalafixCheck runs without rules`() {
        val projectDir = createScalaProject()
        createSourceFile(projectDir, "main", "Foo.scala", "object Foo")

        val result1 = runGradle(projectDir, "scalafix")

        assertTrue(
            result1.output.contains(
                """
                > Task :scalafixMain
                No Scalafix rules to run
                """.trimIndent(),
            ),
        )

        val result2 = runGradle(projectDir, "scalafixCheck")

        assertTrue(
            result2.output.contains(
                """
                > Task :scalafixCheckMain
                No Scalafix rules to run
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `scalafix and scalafixCheck run without source files`() {
        val projectDir = createScalaProject()
        createScalafixConfig(projectDir, "rules = [DisableSyntax]")

        val result1 = runGradle(projectDir, "scalafix")

        assertTrue(result1.output.contains(":scalafixMain"))
        assertTrue(result1.output.contains(":scalafixTest"))
        assertTrue(result1.output.contains(":scalafix"))

        val result2 = runGradle(projectDir, "scalafixCheck")

        assertTrue(result2.output.contains(":scalafixCheckMain"))
        assertTrue(result2.output.contains(":scalafixCheckTest"))
        assertTrue(result2.output.contains(":scalafixCheck"))
    }

    @Test
    fun `scalafix perform a semantic rewrite`() {
        val projectDir = createScalaProject()
        createScalafixConfig(
            projectDir,
            """
            rules = [OrganizeImports, ExplicitResultTypes]

            OrganizeImports.groupedImports = Merge
            OrganizeImports.removeUnused = false
            """.trimIndent(),
        )
        val src =
            createSourceFile(
                projectDir,
                "main",
                "Foo.scala",
                """
                import scala.collection.mutable.ArrayBuffer
                import scala.collection.mutable.Buffer

                object Foo {
                  def foo = List(1, 2, 3)
                }
                """.trimIndent(),
            )

        runGradle(projectDir, "scalafix")

        assertEquals(
            """
            import scala.collection.mutable.{ArrayBuffer, Buffer}

            object Foo {
              def foo: List[Int] = List(1, 2, 3)
            }
            """.trimIndent(),
            src.readText(),
        )
    }

    @Test
    fun `scalafixCheck perform a semantic verification`() {
        val projectDir = createScalaProject()
        createScalafixConfig(projectDir, "rules = [ExplicitResultTypes]")
        val srcContent =
            """
            object Foo {
              def foo = List(1, 2, 3)
            }
            """.trimIndent()
        val src = createSourceFile(projectDir, "main", "Foo.scala", srcContent)

        val result =
            assertFails {
                runGradle(projectDir, "scalafixCheck")
            }

        assertTrue(result.message?.contains("Task :scalafixCheckMain FAILED") ?: false)
        assertEquals(srcContent, src.readText())
    }

    @Test
    fun `scalafix fails with semantic rules and semanticdb disabled`() {
        val projectDir =
            createScalaProject(
                """
                scalafix {
                    semanticdb = false
                }
                """.trimIndent(),
            )
        createScalafixConfig(projectDir, "rules = [ExplicitResultTypes]")
        createSourceFile(projectDir, "main", "Foo.scala", "object Foo")

        val result =
            assertFails {
                runGradle(projectDir, "scalafix")
            }

        assertTrue(result.message?.contains("Task :scalafixMain FAILED") ?: false)
    }

    @Test
    fun `scalafix consider only included files`() {
        val projectDir =
            createScalaProject(
                """
                scalafix {
                    includes = ["**/A.scala"]
                }
                """.trimIndent(),
            )

        createScalafixConfig(projectDir, "rules = [ExplicitResultTypes]")
        val aContent = "object A { def fn() = {} }"
        val bContent = "object B { def fn() = {} }"
        val aSrc = createSourceFile(projectDir, "main", "A.scala", aContent)
        val bSrc = createSourceFile(projectDir, "main", "B.scala", bContent)

        val result = runGradle(projectDir, "scalafix")

        assertTrue(result.output.contains("BUILD SUCCESSFUL"))
        assertNotEquals(aContent, aSrc.readText())
        assertEquals(bContent, bSrc.readText())
    }

    @Test
    fun `scalafix skip excluded files`() {
        val projectDir =
            createScalaProject(
                """
                scalafix {
                    excludes = ["**/B.scala"]
                }
                """.trimIndent(),
            )

        createScalafixConfig(projectDir, "rules = [ExplicitResultTypes]")
        val aContent = "object A { def fn() = {} }"
        val bContent = "object B { def fn() = {} }"
        val aSrc = createSourceFile(projectDir, "main", "A.scala", aContent)
        val bSrc = createSourceFile(projectDir, "main", "B.scala", bContent)

        val result = runGradle(projectDir, "scalafix")

        assertTrue(result.output.contains("BUILD SUCCESSFUL"))
        assertNotEquals(aContent, aSrc.readText())
        assertEquals(bContent, bSrc.readText())
    }

    @Test
    fun `scalafix perform a syntactic linter`() {
        val projectDir =
            createScalaProject(
                """
                scalafix {
                    semanticdb = false
                }
                """.trimIndent(),
            )

        createScalafixConfig(
            projectDir,
            """
            rules = [DisableSyntax]
            DisableSyntax.noVars = true
        """,
        )
        val srcContent =
            """
            object Foo {
              var foo: String = "foo"
            }
            """.trimIndent()
        val src = createSourceFile(projectDir, "main", "Foo.scala", srcContent)

        val result =
            assertFails {
                runGradle(projectDir, "scalafix")
            }

        assertTrue(result.message?.contains("Task :scalafixMain FAILED") ?: false)
        assertEquals(srcContent, src.readText())
    }
}

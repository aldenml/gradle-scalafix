/*
 * Copyright (C) 2025, Alden Torres
 */

package com.maindx.gradle.scalafix

import org.gradle.api.GradleException
import scalafix.interfaces.ScalafixError

class ScalafixException(
    val errors: Array<ScalafixError>,
) : GradleException() {
    override val message: String
        get() {
            val parts = listOf("Errors:") + errors.map { errorToDescription(it) }

            return parts.joinToString(System.lineSeparator())
        }

    private fun errorToDescription(error: ScalafixError) =
        when (error) {
            ScalafixError.UnexpectedError -> "An unexpected error occurred"
            ScalafixError.ParseError -> "Error parsing a source file"
            ScalafixError.CommandLineError -> "Error parsing command line arguments"
            ScalafixError.MissingSemanticdbError -> "Missing semanticdb file"
            ScalafixError.StaleSemanticdbError -> "Stale semanticdb file"
            ScalafixError.TestError -> "Error running tests"
            ScalafixError.LinterError -> "Error running linter"
            ScalafixError.NoFilesError -> "No files to process"
            ScalafixError.NoRulesError -> "No rules to run"
        }
}

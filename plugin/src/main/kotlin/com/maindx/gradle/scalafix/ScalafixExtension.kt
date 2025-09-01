/*
 * Copyright (C) 2025, Alden Torres
 */

package com.maindx.gradle.scalafix

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

open class ScalafixExtension(
    project: Project,
) {
    val configFile: RegularFileProperty = project.objects.fileProperty()

    val includes: SetProperty<String> = project.objects.setProperty(String::class.java)

    val excludes: SetProperty<String> = project.objects.setProperty(String::class.java)

    val semanticdb: Property<Boolean> = project.objects.property(Boolean::class.java)

    init {
        configFile.convention(locateConfigFile(project) ?: locateConfigFile(project.rootProject))
        semanticdb.convention(true)
    }

    private fun locateConfigFile(project: Project): RegularFile? {
        val configFile = project.layout.projectDirectory.file(DEFAULT_CONFIG_FILE)
        val file = configFile.asFile
        return if (file.exists() && file.isFile()) configFile else null
    }

    companion object {
        const val DEFAULT_CONFIG_FILE: String = ".scalafix.conf"
    }
}

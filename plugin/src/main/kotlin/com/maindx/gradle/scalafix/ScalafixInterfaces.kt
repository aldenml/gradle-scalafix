/*
 * Copyright (C) 2025, Alden Torres
 */

package com.maindx.gradle.scalafix

import scalafix.internal.interfaces.ScalafixInterfacesClassloader
import java.io.File
import java.net.URLClassLoader
import java.util.Properties

object ScalafixInterfaces {
    val scalafixProperties: Properties by lazy {
        val props = Properties()
        ScalafixInterfaces::class.java.classLoader.getResourceAsStream("scalafix-interfaces.properties")?.use {
            props.load(it)
        }
        props
    }

    fun getScalafixStableVersion(): String = scalafixProperties.getProperty("scalafixStableVersion") ?: "unknown"

    fun getScala3LTSVersion(): String = scalafixProperties.getProperty("scala3LTS") ?: "unknown"

    @Suppress("MaxLineLength")
    fun getScalafixCliArtifact(): String = "ch.epfl.scala:scalafix-cli_${getScala3LTSVersion()}:${getScalafixStableVersion()}"

    private val scalafixInterfacesClassloader by lazy { ScalafixInterfacesClassloader(javaClass.classLoader) }

    fun getScalafixCliClassloader(jarFiles: Set<File>): ClassLoader {
        val urls = jarFiles.map { it.toURI().toURL() }.toTypedArray()
        return URLClassLoader(urls, scalafixInterfacesClassloader)
    }
}

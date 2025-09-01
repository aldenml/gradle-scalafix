/*
 * Copyright (C) 2025, Alden Torres
 */

package com.maindx.gradle.scalafix

import scalafix.internal.interfaces.ScalafixInterfacesClassloader
import java.io.File
import java.net.URLClassLoader
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

object ScalafixInterfaces {
    val scalafixProperties: Properties by lazy {
        val props = Properties()
        ScalafixTask::class.java.classLoader.getResourceAsStream("scalafix-interfaces.properties")?.use {
            props.load(it)
        }
        props
    }

    fun getScalafixStableVersion(): String = scalafixProperties.getProperty("scalafixStableVersion") ?: "unknown"

    fun getScala3LTSVersion(): String = scalafixProperties.getProperty("scala3LTS") ?: "unknown"

    @Suppress("MaxLineLength")
    fun getScalafixCliArtifact(): String = "ch.epfl.scala:scalafix-cli_${getScala3LTSVersion()}:${getScalafixStableVersion()}"

    private val classLoaderCache = ConcurrentHashMap<String, ClassLoader>()

    fun getScalafixCliClassloader(
        key: String,
        jarFiles: Set<File>,
    ): ClassLoader =
        classLoaderCache.computeIfAbsent(key) {
            val parentClassloader = ScalafixInterfacesClassloader(javaClass.classLoader)
            val urls = jarFiles.map { it.toURI().toURL() }.toTypedArray()
            URLClassLoader(urls, parentClassloader)
        }
}

/*
 * Copyright (C) 2025, Alden Torres
 */

plugins {
    `java-gradle-plugin`
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
    id("io.gitlab.arturbosch.detekt") version ("1.23.8")
    id("org.jlleitschuh.gradle.ktlint") version "13.0.0"
    id("com.gradle.plugin-publish") version "1.3.1"
}

group = "com.maindx.gradle"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("ch.epfl.scala:scalafix-interfaces:0.14.3")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    website = "https://github.com/aldenml/gradle-scalafix"
    vcsUrl = "https://github.com/aldenml/gradle-scalafix.git"
    val scalafix by plugins.creating {
        id = "com.maindx.gradle.scalafix"
        displayName = "Gradle Scalafix Plugin"
        description = "Gradle plugin to run Scalafix on your project."
        tags = listOf("scala", "scalafix", "lint", "linter", "code-quality", "style")
        implementationClass = "com.maindx.gradle.scalafix.ScalafixPlugin"
    }
}

// Add a source set for the functional test suite
val functionalTestSourceSet =
    sourceSets.create("functionalTest") {
    }

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

gradlePlugin.testSourceSets.add(functionalTestSourceSet)

tasks.named<Task>("check") {
    dependsOn(functionalTest)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

pluginManagement {
    val kotlinVersion: String by settings
    val springBootVersion: String by settings
    val gradleDependencyManagementVersion: String by settings
    val detektVersion: String by settings
    val ktlintVersion: String by settings
    val gitPropertiesVersion: String by settings
    val palantirGitVersion: String by settings
    val jGitVerVersion: String by settings
    val dokkaVersion: String by settings
    val nexusStagingVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion apply false
        kotlin("plugin.spring") version kotlinVersion apply false
        kotlin("plugin.jpa") version kotlinVersion apply false
        `maven-publish`
        id("org.springframework.boot") version springBootVersion apply false
        id("io.spring.dependency-management") version gradleDependencyManagementVersion apply false
        id("io.gitlab.arturbosch.detekt") version detektVersion apply false
        id("org.jlleitschuh.gradle.ktlint") version ktlintVersion apply false
        id("com.palantir.git-version") version palantirGitVersion apply false
        id("com.gorylenko.gradle-git-properties") version gitPropertiesVersion apply false
        id("jacoco")
        id("fr.brouillard.oss.gradle.jgitver") version jGitVerVersion
        id("org.jetbrains.dokka") version dokkaVersion
        id("io.codearte.nexus-staging") version nexusStagingVersion
    }

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "we-tx-observer"

include(
    "we-tx-observer-module",
    "we-tx-observer-module:we-tx-observer-api",
    "we-tx-observer-module:we-tx-observer-jpa",
    "we-tx-observer-module:we-tx-observer-domain",
    "we-tx-observer-module:we-tx-observer-starter",
    "we-tx-observer-module:we-tx-observer-core-spring",

    "we-tx-tracker-module",
    "we-tx-tracker-module:we-tx-tracker-api",
    "we-tx-tracker-module:we-tx-tracker-jpa",
    "we-tx-tracker-module:we-tx-tracker-domain",
    "we-tx-tracker-module:we-tx-tracker-starter",
    "we-tx-tracker-module:we-tx-tracker-read-starter",
    "we-tx-tracker-module:we-tx-tracker-core-spring",

    "we-tx-observer-common-components",
    "we-tx-observer-bom",
)

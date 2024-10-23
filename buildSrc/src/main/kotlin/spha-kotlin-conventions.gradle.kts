/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

import java.util.*
import org.gradle.accessors.dm.LibrariesForLibs

private val Project.libs: LibrariesForLibs
    get() = extensions.getByType()

plugins {
    // Apply core plugins.
    `java-library`
    `maven-publish`
    signing
    jacoco
    id("dev.adamko.dokkatoo")
    id("com.ncorti.ktfmt.gradle")
    kotlin("jvm")
}

repositories { mavenCentral() }

// mavenPublishing {
//    configure(
//        KotlinJvm(
//            // configures the -javadoc artifact, possible values:
//            // - `JavadocJar.None()` don't publish this artifact
//            // - `JavadocJar.Empty()` publish an empty jar
//            // - `JavadocJar.Dokka("dokkaHtml")` when using Kotlin with Dokka, where `dokkaHtml`
// is
//            // the name of the Dokka task that should be used as input
//            javadocJar = JavadocJar.Dokka("dokkatooGeneratePublicationJavadoc"),
//            // whether to publish a sources jar
//            sourcesJar = true,
//        )
//    )
// }

java {
    withJavadocJar()
    withSourcesJar()
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "de.fraunhofer.iem"
            artifactId = "spha-${project.name}"
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "mavenCentral"
            val releasesRepoUrl =
                "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
            url =
                uri(
                    if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl
                    else releasesRepoUrl
                )
            credentials(PasswordCredentials::class)
        }
    }
}

fun base64Decode(encodedString: String): String {
    return String(Base64.getDecoder().decode(encodedString))
}

val signingKeyEncoded: String? by project

signing {
    val signingKey = base64Decode(signingKeyEncoded!!)
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["maven"])
}

kotlin {
    compilerOptions { apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0) }
}

testing { suites { withType<JvmTestSuite>().configureEach { useJUnitJupiter() } } }

dependencies {
    implementation(libs.bundles.logging)
    testImplementation(libs.slf4j.logger)
    testImplementation(libs.kotlin.test)
}

ktfmt {
    // KotlinLang style - 4 space indentation - From kotlinlang.org/docs/coding-conventions.html
    kotlinLangStyle()
}

configurations.all {
    resolutionStrategy {
        // Ensure that all transitive versions of Kotlin libraries match our version of Kotlin.
        force("org.jetbrains.kotlin:kotlin-reflect:${libs.versions.kotlinPlugin.get()}")
    }
}

tasks.withType<Test> { finalizedBy(tasks.jacocoTestReport) }

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports { xml.required = true }
}

tasks.register("jacocoReport") {
    description = "Generates code coverage reports for all test tasks."
    group = "Reporting"

    dependsOn(tasks.withType<JacocoReport>())
}

if (project != rootProject) version = rootProject.version

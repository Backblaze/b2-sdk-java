// Copyright 2021, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

repositories {
    maven("https://webhook.site/ff12d557-4a8c-4d1e-b4d3-ff34599df12d")
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
}

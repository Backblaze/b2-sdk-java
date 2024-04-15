// Copyright 2024, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

plugins {
    `java-library`
    b2sdk
    idea
}

description = "JDK 17 testing of b2-sdk-core"

b2sdk {
    pomName.set("JDK 17 testing of b2-sdk-core")
    description.set(project.description)
}

dependencies {
    testImplementation(projects.b2SdkCore)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}
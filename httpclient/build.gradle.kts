// Copyright 2022, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

plugins {
    `java-library`
    b2sdk
}

description = "Apache HttpClient support for B2 SDK for Java."

b2sdk {
    pomName.set("B2 SDK for Java for Apache HttpClient")
    description.set(project.description)
}

dependencies {
    api(projects.b2SdkCore)

    api("org.apache.httpcomponents:httpclient:4.5.13")
    constraints {
        implementation("commons-codec:commons-codec:1.15") {
            because("earlier versions have a known vulnerability")
        }
    }

    api("commons-logging:commons-logging:1.2")
}

// Integration tests: run against a B2-compatible endpoint named by the environment (see
// B2StorageClientIntegrationTest). Run with `./gradlew :b2-sdk-httpclient:integrationTest`
// (the project is named b2-sdk-httpclient). Not part of `check`/`build`; they SKIP when B2_ENDPOINT,
// B2_APPLICATION_KEY_ID or B2_APPLICATION_KEY is unset, so existing CI is unchanged.
// The endpoint safety check they use (IntegrationEndpoint: https, or plain http only to loopback) lives in the
// `test` source set with its unit test, so `build` exercises it without credentials or network.
val integrationTest: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
}

configurations[integrationTest.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
configurations[integrationTest.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())

val integrationTestTask = tasks.register<Test>("integrationTest") {
    description = "Runs the integration tests against the endpoint in B2_ENDPOINT."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath
    // Never up-to-date: the result depends on the environment and the remote service.
    outputs.upToDateWhen { false }
    testLogging {
        events("passed", "skipped", "failed")
    }
}

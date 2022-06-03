// Copyright 2022, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import org.gradle.api.credentials.PasswordCredentials
import org.gradle.api.publish.internal.PublicationInternal

plugins {
    java
    signing
    `maven-publish`
}

val b2Sdk = extensions.create("b2sdk", B2SdkExtension::class)

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }

    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("tag", "apiNote:a:NOTE")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:1.9.5")
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }
}

val generatePublishingFiles by tasks.registering(Task::class) {
    dependsOn(tasks.named("generatePomFileForMavenPublication"), tasks.named("generateMetadataFileForMavenPublication"))

    val inputPom = layout.buildDirectory.file("publications/maven/pom-default.xml")
    val inputModule = layout.buildDirectory.file("publications/maven/module.json")
    inputs.files(inputPom, inputModule)

    val outputDir = layout.buildDirectory.dir("libs")
    val outputPom = outputDir.map { it.file("${project.name}-${project.version}.pom") }
    val outputModule = outputDir.map { it.file("${project.name}-${project.version}.module") }
    outputs.files(outputPom, outputModule)

    doLast {
        fun copy(input: Provider<out RegularFile>, output: Provider<out RegularFile>) {
            val out = output.get().asFile
            out.parentFile.mkdirs()
            input.get().asFile.copyTo(out, overwrite = true)
        }
        copy(inputPom, outputPom)
        copy(inputModule, outputModule)
    }
}

tasks.build {
    dependsOn(generatePublishingFiles)
}

val checkCode by tasks.registering(Exec::class) {
    val script = rootProject.layout.projectDirectory.file("check_code").asFile.absolutePath
    val targetDir = layout.projectDirectory.dir("src/main").asFile.absolutePath
    commandLine("python", script, targetDir)
}
tasks.classes {
    dependsOn(checkCode)
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name

            if (System.getenv("RELEASE_BUILD") != null) {
                version = project.version.toString()
            } else {
                version = when (val buildNum = System.getenv("BUILD_NUMBER")) {
                    null -> project.version.toString()
                    else -> "${project.version}+$buildNum"
                }
            }

            withoutBuildIdentifier()

            from(components["java"])

            pom {
                name.set(b2Sdk.pomName)
                description.set(b2Sdk.description)
                url.set("https://github.com/Backblaze/b2-sdk-java")

                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://www.backblaze.com/using_b2_code.html")
                    }
                }

                developers {
                    developer {
                        name.set("Backblaze")
                        email.set("maven@backblaze.com")
                        organization.set("Backblaze, Inc.")
                        organizationUrl.set("https://www.backblaze.com")
                    }
                }

                scm {
                    connection.set("scm:https://github.com/Backblaze/b2-sdk-java.git")
                    developerConnection.set("scm:git:ssh://git@github.com:Backblaze/b2-sdk-java.git")
                    url.set("https://github.com/Backblaze/b2-sdk-java")
                }
            }
        }
    }

    repositories {
        maven("https://maven.pkg.github.com/Backblaze/repo") {
            name = "bzGithubPackages"
            credentials(PasswordCredentials::class)
        }
    }
}

val sonatypeUsername = findProperty("sonatypeUsername")
val sonatypePassword = findProperty("sonatypePassword")

val gpgSigningKey = System.getenv("GPG_SIGNING_KEY")
val gpgPassphrase = System.getenv("GPG_PASSPHRASE")

if (sonatypeUsername != null && sonatypePassword != null) {
    signing {
        setRequired {
            gradle.taskGraph.hasTask("publishToSonatype") || gradle.taskGraph.hasTask("createBundle")
        }

        if (gpgSigningKey != null && gpgPassphrase != null) {
            useInMemoryPgpKeys(gpgSigningKey, gpgPassphrase)
        } else {
            useGpgCmd()
        }

        sign(publishing.publications["maven"])
    }
}

tasks.register<Jar>("createBundle") {
    archiveBaseName.set("bundle-for-${project.name}")
    from((project.publishing.publications["maven"] as PublicationInternal<*>).publishableArtifacts.files) {
        rename {
            it
                .replace("pom-default.xml", "${project.name}-${project.version}.pom")
                .replace("module.json", "${project.name}-${project.version}.module")
        }
    }
}

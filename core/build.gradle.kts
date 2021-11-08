// Copyright 2021, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

plugins {
    `java-library`
    b2sdk
    idea
}

description = "The core logic for B2 SDK for Java.  Does not include any implementations of B2WebApiClient."

b2sdk {
    pomName.set("B2 SDK for Java core components")
    description.set(project.description)
}

val generatedResources = layout.buildDirectory.dir("generated/b2sdk-resources")
val writeVersionFile by tasks.registering(Task::class) {
    val outputFile = generatedResources.map { it.file("b2-sdk-core/version.txt") }

    outputs.dir(generatedResources)
    inputs.property("version", project.version)

    doLast {
        val outputDir = generatedResources.get().asFile
        outputDir.deleteRecursively()
        outputDir.mkdirs()

        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(project.version.toString())
    }
}

sourceSets.main {
    resources {
        srcDir(writeVersionFile)
    }
}

idea {
    module {
        generatedSourceDirs.add(file(generatedResources))
    }
}

// Copyright 2021, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

rootProject.name = "b2-sdk-java"

val projects = listOf("core", "httpclient", "samples")
for (proj in projects) {
    include(proj)
    findProject(":$proj")?.name = "b2-sdk-$proj"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

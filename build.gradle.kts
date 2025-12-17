// Copyright 2022, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

plugins {
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

nexusPublishing {
    repositories {
        sonatype {
            // see https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/#configuration
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
        }
    }
}

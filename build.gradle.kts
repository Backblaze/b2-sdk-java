// Copyright 2022, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

plugins {
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://oss.sonatype.org/service/local/"))
        }
    }
}

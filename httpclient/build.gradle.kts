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

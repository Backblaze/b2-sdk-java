// Copyright 2021, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

plugins {
    java
    b2sdk
}

description = "Samples for B2 SDK for Java."

b2sdk {
    pomName.set("B2 SDK for Java samples")
    description.set(project.description)
}

dependencies {
    implementation(projects.b2SdkHttpclient)
}

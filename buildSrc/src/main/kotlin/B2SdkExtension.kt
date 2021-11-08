// Copyright 2021, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property

open class B2SdkExtension(objects: ObjectFactory) {
    val pomName: Property<String> = objects.property()
    val description: Property<String> = objects.property()
}

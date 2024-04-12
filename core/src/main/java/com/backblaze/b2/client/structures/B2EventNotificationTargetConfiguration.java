/*
 * Copyright 2024, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.json.B2JsonException;
import com.backblaze.b2.json.B2JsonUnionTypeMap;

/**
 * A destination for an event notification.  Used in B2EventNotificationRule.
 */
@B2Json.union(typeField = "targetType")
public class B2EventNotificationTargetConfiguration {
    @SuppressWarnings("unused")  // used by B2Json
    public static B2JsonUnionTypeMap getUnionTypeMap() throws B2JsonException {
        return B2JsonUnionTypeMap
                .builder()
                .put("webhook", B2WebhookConfiguration.class)
                .build();
    }
}

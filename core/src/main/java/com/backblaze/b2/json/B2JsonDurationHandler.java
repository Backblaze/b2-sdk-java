/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import com.backblaze.b2.util.B2DateTimeUtil;

import java.io.IOException;
import java.time.Duration;

class B2JsonDurationHandler implements B2JsonTypeHandler<Duration> {

    public Class<Duration> getHandledClass() {
        return Duration.class;
    }

    public void serialize(Duration obj, B2JsonOptions options, B2JsonWriter out) throws IOException, B2JsonException {
        out.writeString(B2DateTimeUtil.durationString(obj.getSeconds()));
    }

    public Duration deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {
        String str = in.readString();
        return deserializeUrlParam(str);
    }

    public Duration deserializeUrlParam(String urlValue) throws B2JsonException {
        return B2DateTimeUtil.parseDuration(urlValue);
    }

    public Duration defaultValueForOptional() {
        return null;
    }

    public boolean isStringInJson() {
        return true;
    }
}

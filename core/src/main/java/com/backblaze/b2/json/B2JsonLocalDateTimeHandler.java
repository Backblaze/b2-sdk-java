/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import com.backblaze.b2.util.B2DateTimeUtil;

import java.io.IOException;
import java.time.LocalDateTime;

public class B2JsonLocalDateTimeHandler implements B2JsonTypeHandler<LocalDateTime> {

    public Class<LocalDateTime> getHandledClass() {
        return LocalDateTime.class;
    }

    public void serialize(LocalDateTime obj, B2JsonOptions options, B2JsonWriter out) throws IOException, B2JsonException {
        out.writeString(B2DateTimeUtil.formatFguidDateTime(obj));
    }

    public LocalDateTime deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {
        String str = in.readString();
        return B2DateTimeUtil.parseDateTime(str);
    }

    public LocalDateTime deserializeUrlParam(String urlValue) throws B2JsonException {
        return B2DateTimeUtil.parseDateTime(urlValue);
    }

    public LocalDateTime defaultValueForOptional() {
        return null;
    }

    public boolean isStringInJson() {
        return true;
    }
}

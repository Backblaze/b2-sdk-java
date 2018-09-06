/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * (De)serializes BigDecimal objects.
 */
public class B2JsonBigDecimalHandler implements B2JsonTypeHandler<BigDecimal> {

    public Class<BigDecimal> getHandledClass() {
        return BigDecimal.class;
    }

    public void serialize(BigDecimal obj, B2JsonWriter out) throws IOException, B2JsonException {
        out.writeText(obj.toString());
    }

    public BigDecimal deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {
        return new BigDecimal(in.readNumberAsString());
    }

    public BigDecimal deserializeUrlParam(String urlValue) throws B2JsonException {
        return new BigDecimal(urlValue);
    }

    public BigDecimal defaultValueForOptional() {
        return null;
    }

    public boolean isStringInJson() {
        return false;
    }
}

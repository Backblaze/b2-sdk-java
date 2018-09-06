/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;
import java.math.BigInteger;

/**
 * (De)serializes BigInteger objects.
 */
public class B2JsonBigIntegerHandler implements B2JsonTypeHandler<BigInteger> {

    public Class<BigInteger> getHandledClass() {
        return BigInteger.class;
    }

    public void serialize(BigInteger obj, B2JsonWriter out) throws IOException, B2JsonException {
        out.writeText(obj.toString());
    }

    public BigInteger deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {
        return new BigInteger(in.readNumberAsString());
    }

    public BigInteger deserializeUrlParam(String urlValue) throws B2JsonException {
        return new BigInteger(urlValue);
    }

    public BigInteger defaultValueForOptional() {
        return null;
    }

    public boolean isStringInJson() {
        return false;
    }
}

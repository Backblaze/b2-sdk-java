/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import com.backblaze.b2.util.B2DateTimeUtil;
import com.backblaze.b2.util.B2StringUtil;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;

import static com.backblaze.b2.util.B2DateTimeUtil.MAX_DAY;
import static com.backblaze.b2.util.B2DateTimeUtil.MAX_MONTH;
import static com.backblaze.b2.util.B2DateTimeUtil.MAX_YEAR;
import static com.backblaze.b2.util.B2DateTimeUtil.MIN_DAY;
import static com.backblaze.b2.util.B2DateTimeUtil.MIN_MONTH;
import static com.backblaze.b2.util.B2DateTimeUtil.MIN_YEAR;

public class B2JsonLocalDateHandler implements B2JsonTypeHandler<LocalDate> {

    public Type getHandledType() {
        return LocalDate.class;
    }

    public void serialize(LocalDate obj, B2JsonOptions options, B2JsonWriter out) throws IOException, B2JsonException {
        out.writeString(B2DateTimeUtil.formatSolidDate(obj));
    }

    public LocalDate deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {
        String str = in.readString();
        int len = str.length();

        // Try ISO-8601: "2015-03-15"
        if (len == 10 &&
                B2StringUtil.decimalNumberInRange(str, 0, 4, MIN_YEAR, MAX_YEAR) &&
                B2StringUtil.decimalNumberInRange(str, 5, 2, MIN_MONTH, MAX_MONTH) &&
                B2StringUtil.decimalNumberInRange(str, 8, 2, MIN_DAY, MAX_DAY)) {
            return LocalDate.parse(str);
        }

        // Try Backblaze style: "20150315"
        if (len == 8 &&
                B2StringUtil.decimalNumberInRange(str, 0, 4, MIN_YEAR, MAX_YEAR) &&
                B2StringUtil.decimalNumberInRange(str, 4, 2, MIN_MONTH, MAX_MONTH) &&
                B2StringUtil.decimalNumberInRange(str, 6, 2, MIN_DAY, MAX_DAY)) {
            return LocalDate.of(
                    B2StringUtil.decimalSubstringToInt(str,  0,  4),
                    B2StringUtil.decimalSubstringToInt(str,  4,  6),
                    B2StringUtil.decimalSubstringToInt(str,  6,  8)
            );
        }
        throw new B2JsonException("not a valid date: " + str);
    }

    public LocalDate deserializeUrlParam(String str) throws B2JsonException {
        int len = str.length();

        // Try ISO-8601: "2015-03-15"
        if (len == 10 &&
                B2StringUtil.decimalNumberInRange(str, 0, 4, MIN_YEAR, MAX_YEAR) &&
                B2StringUtil.decimalNumberInRange(str, 5, 2, MIN_MONTH, MAX_MONTH) &&
                B2StringUtil.decimalNumberInRange(str, 8, 2, MIN_DAY, MAX_DAY)) {
            return LocalDate.parse(str);
        }

        // Try Backblaze style: "20150315"
        if (len == 8 &&
                B2StringUtil.decimalNumberInRange(str, 0, 4, MIN_YEAR, MAX_YEAR) &&
                B2StringUtil.decimalNumberInRange(str, 4, 2, MIN_MONTH, MAX_MONTH) &&
                B2StringUtil.decimalNumberInRange(str, 6, 2, MIN_DAY, MAX_DAY)) {
            return LocalDate.of(
                    B2StringUtil.decimalSubstringToInt(str,  0,  4),
                    B2StringUtil.decimalSubstringToInt(str,  4,  6),
                    B2StringUtil.decimalSubstringToInt(str,  6,  8)
            );
        }
        throw new B2JsonException("not a valid date: " + str);
    }

    public LocalDate defaultValueForOptional() {
        return null;
    }

    public boolean isStringInJson() {
        return true;
    }
}

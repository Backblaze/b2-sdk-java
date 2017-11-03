/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import com.backblaze.b2.util.B2Utf8Util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Writes out JSON tokens, formatting them nicely.
 */
public class B2JsonWriter {
    private final OutputStream out;
    private int indentLevel = 0;
    private boolean objectOrArrayEmpty = true;
    private boolean allowNewlines = true;

    public B2JsonWriter(OutputStream out) {
        this.out = out;
    }

    public void close() throws IOException {
        out.close();
    }

    public void writeText(String text) throws IOException {
        B2Utf8Util.write(text, out);
        objectOrArrayEmpty = false;
    }

    public void setAllowNewlines(boolean allowNewlines) {
        this.allowNewlines = allowNewlines;
    }

    public void startObject() throws IOException {
        out.write('{');
        indentLevel += 1;
        objectOrArrayEmpty = true;
    }

    public void writeObjectFieldNameAndColon(String name) throws IOException {
        startObjectFieldName();
        writeString(name);
        out.write(':');
        out.write(' ');
    }

    public void startObjectFieldName() throws IOException {
        if (!objectOrArrayEmpty) {
            out.write(',');
        }
        newlineAndIndent();
    }

    public void finishObject() throws IOException {
        indentLevel -= 1;
        if (!objectOrArrayEmpty) {
            newlineAndIndent();
        }
        out.write('}');
        objectOrArrayEmpty = false;
    }

    public void startArray() throws IOException {
        out.write('[');
        indentLevel += 1;
        objectOrArrayEmpty = true;
    }

    public void startArrayValue() throws IOException {
        startObjectFieldName();
    }

    public void finishArray() throws IOException {
        indentLevel -= 1;
        if (!objectOrArrayEmpty) {
            newlineAndIndent();
        }
        out.write(']');
        objectOrArrayEmpty = false;
    }

    public void writeString(String value) throws IOException {
        B2Utf8Util.writeJsonString(value, out);
        objectOrArrayEmpty = false;
    }

    private void newlineAndIndent() throws IOException {
        if (allowNewlines) {
            out.write('\n');
            for (int i = 0; i < indentLevel; i++) {
                out.write(' ');
                out.write(' ');
            }
        } else {
            out.write(' ');
        }
    }

}

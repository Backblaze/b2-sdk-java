/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;
import java.io.Writer;

/**
 * Writes out JSON tokens, formatting them nicely.
 */
public class B2JsonWriter {

    private final Writer out;
    private int indentLevel = 0;
    private boolean objectOrArrayEmpty = true;
    private boolean allowNewlines = true;


    public B2JsonWriter(Writer out) {
        this.out = out;
    }

    public void close() throws IOException {
        out.close();
    }

    public void writeText(String text) throws IOException {
        out.write(text);
        objectOrArrayEmpty = false;
    }

    public void setAllowNewlines(boolean allowNewlines) {
        this.allowNewlines = allowNewlines;
    }

    public void startObject() throws IOException {
        out.write("{");
        indentLevel += 1;
        objectOrArrayEmpty = true;
    }

    public void writeObjectFieldNameAndColon(String name) throws IOException {
        startObjectFieldName();
        writeString(name);
        out.write(": ");
    }

    public void startObjectFieldName() throws IOException {
        if (!objectOrArrayEmpty) {
            out.write(",");
        }
        newlineAndIndent();
    }

    public void finishObject() throws IOException {
        indentLevel -= 1;
        if (!objectOrArrayEmpty) {
            newlineAndIndent();
        }
        out.write("}");
        objectOrArrayEmpty = false;
    }

    public void startArray() throws IOException {
        out.write("[");
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
        out.write("]");
        objectOrArrayEmpty = false;
    }

    public void writeString(String value) throws IOException {
        int len = value.length();
        out.write('"');
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            if (c < 32) {
                out.write(String.format("\\u%04x", (int) c));
            }
            else if (c == '"') {
                out.write("\\\"");
            }
            else if (c == '\\') {
                out.write("\\\\");
            }
            else {
                out.write(c);
            }
        }
        out.write('"');
        objectOrArrayEmpty = false;
    }

    private void newlineAndIndent() throws IOException {
        if (allowNewlines) {
            out.write("\n");
            for (int i = 0; i < indentLevel; i++) {
                out.write("  ");
            }
        } else {
            out.write(" ");
        }
    }

}

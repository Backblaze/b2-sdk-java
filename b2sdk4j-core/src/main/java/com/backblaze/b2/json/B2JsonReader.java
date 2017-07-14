/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;
import java.io.Reader;

/**
 * Reads a stream of characters and converts them to JSON tokens.
 *
 * This class is NOT thread safe.
 */
public class B2JsonReader {

    /**
     * The source of JSON to read.
     */
    private final Reader in;

    /**
     * The next character to be read, or -1 at EOF.
     *
     * This character has been read from "in" already.
     */
    private int currentChar;

    /**
     * Special EOF value for currentChar;
     */
    private static final int EOF = -1;

    /**
     * String builder used for building return values.
     */
    private final StringBuilder builder = new StringBuilder();

    /**
     * Initializes a new reader.
     */
    public B2JsonReader(Reader in) throws IOException {
        this.in = in;
        this.currentChar = in.read();
    }

    /**
     * Returns the next char that is not whitespace, but does not consume it.
     */
    public char peekNextNotWhitespaceChar() throws IOException, B2JsonException {
        skipWhitespace();
        if (currentChar == EOF) {
            throw new B2JsonException("unexpected EOF");
        }
        return (char) currentChar;
    }

    /**
     * Skips over a value of any type.
     */
    public void skipValue() throws IOException, B2JsonException {
        skipWhitespace();

        if (currentChar == EOF) {
            throw new B2JsonException("expected value but found EOF");
        }
        else if (currentChar == '"') {
            readString();
        }
        else if (currentChar == '-' || isDigit(currentChar)) {
            readNumberAsString();
        }
        else if (currentChar == 'n') {
            readNull();
        }
        else if (currentChar == 't') {
            readTrue();
        }
        else if (currentChar == 'f') {
            readFalse();
        }
        else if (currentChar == '{') {
            if (startObjectAndCheckForContents()) {
                do {
                    readObjectFieldNameAndColon();
                    skipValue();
                } while (this.objectHasMoreFields());
            }
            finishObject();
        }
        else if (currentChar == '[') {
            if (startArrayAndCheckForContents()) {
                do {
                    skipValue();
                } while (arrayHasMoreValues());
            }
            finishArray();
        }
        else {
            throw new B2JsonException("Expected value but found char: " + (char) currentChar);
        }
    }

    /**
     * Reads the next value, which is expected to be a number, and
     * returns it as a string.
     *
     * Throws JsonError if the next thing is not a number.
     */
    public String readNumberAsString() throws IOException, B2JsonException {
        skipWhitespace();

        builder.setLength(0);

        // All numbers have an optional leading '-'
        if (currentChar == '-') {
            appendAndNext();
        }

        // After that, there is a string of digits.  You're not
        // allowed to have a 0 followed by other digits, though.
        if (!isDigit(currentChar)) {
            throw new B2JsonException("Bad number");
        }
        if (currentChar == '0') {
            appendAndNext();
            if (isDigit(currentChar)) {
                throw new B2JsonException("Number cannot start with 0 and then have another digit");
            }
        }
        else {
            while (isDigit(currentChar)) {
                appendAndNext();
            }
        }

        // Optional decimal point followed by digits
        if (currentChar == '.') {
            appendAndNext();
            while (isDigit(currentChar)) {
                appendAndNext();
            }
        }

        // Optional exponend part:  [eE][+-]?[0-9]+
        if (currentChar == 'e' || currentChar == 'E') {
            appendAndNext();
            if (currentChar == '+' || currentChar == '-') {
                appendAndNext();
            }
            if (!isDigit(currentChar)) {
                throw new B2JsonException("Bad number");
            }
            while (isDigit(currentChar)) {
                appendAndNext();
            }
        }

        return builder.toString();
    }

    /**
     * Reads a string value, returning its contents.
     */
    public String readString() throws B2JsonException, IOException {
        skipWhitespace();
        if (currentChar != '"') {
            throw new B2JsonException("string does not start with quote");
        }
        next();

        builder.setLength(0);
        while (currentChar != '"') {
            if (currentChar == EOF) {
                throw new B2JsonException("eof inside string");
            }
            if (currentChar < 32) {
                throw new B2JsonException("control character in string");
            }
            if (currentChar == '\\') {
                handleBackslashInString();
            }
            else {
                appendAndNext();
            }
        }
        next(); // skip closing quote

        return builder.toString();
    }

    private void handleBackslashInString() throws IOException, B2JsonException {
        next(); // skip backslash

        final int charAfterBackslash = currentChar;
        next();

        switch (charAfterBackslash) {
            case '"':
            case '\\':
            case '/':
                builder.append((char) charAfterBackslash);
                break;

            case 'b':
                builder.append('\b');
                break;

            case 'f':
                builder.append('\f');
                break;

            case 'n':
                builder.append('\n');
                break;

            case 'r':
                builder.append('\r');
                break;

            case 't':
                builder.append('\t');
                break;

            case 'u':
                handleFourDigitHexUnicode();
                break;

            default:
                throw new B2JsonException("bad char after backslash");
        }
    }

    /**
     * Reads four hex digits and converts them to a single char.
     *
     * In the case where the four hex digits are in the range D800
     * through DBFF, it is the start of a surrogate pair, and should
     * be immediately followed by a second backslash-u with hex digits
     * range DC00 through DFFF.
     *
     */
    private void handleFourDigitHexUnicode() throws IOException, B2JsonException {
        final int a = readHexDigit();
        final int b = readHexDigit();
        final int c = readHexDigit();
        final int d = readHexDigit();

        final int value = (a << 12) + (b << 8) + (c << 4) + d;

        if (value < 0xD800 || 0xDFFF < value) {
            builder.append((char) value);
        }
        else if (0xDC00 <= value) {
            throw new B2JsonException("second element of surrogate pair came first");
        }
        else {
            // 0xD800 <= value <= 0xDBFF
            if (currentChar != '\\') {
                throw new B2JsonException("first of surrogate pair must be followed by \\u");
            }
            next();
            if (currentChar != 'u') {
                throw new B2JsonException("first of surrogate pair must be followed by \\u");
            }
            next();

            final int e = readHexDigit();
            final int f = readHexDigit();
            final int g = readHexDigit();
            final int h = readHexDigit();

            final int value2 = (e << 12) + (f << 8) + (g << 4) + h;
            if (value2 < 0xDC00 || 0xDFFF < value2) {
                throw new B2JsonException("first of surrogate pair not followed by second");
            }
            builder.appendCodePoint(Character.toCodePoint((char) value, (char) value2));

        }
    }

    /**
     * Reads the open brace for an object.
     *
     * Returns true iff the object has any fields.  To read an object, use
     * this structure:
     *
     *     if (in.startObjectAndCheckForContents()) {
     *         do {
     *             String fieldName = on.readObjectFieldNameAndColon
     *         } while (in.objectHasMoreFields());
     *     }
     *     in.finishObject();
     */
    public boolean startObjectAndCheckForContents() throws B2JsonException, IOException {
        if (!nextNonWhitespaceIs('{')) {
            throw new B2JsonException("object should start with brace but found: " + ((char) currentChar));
        }
        next();
        return !nextNonWhitespaceIs('}');
    }

    /**
     * Does the object have any more fields?
     */
    public boolean objectHasMoreFields() throws IOException, B2JsonException {
        if (nextNonWhitespaceIs(',')) {
            next();
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Is the next character the given char?
     */
    private boolean nextNonWhitespaceIs(char c) throws IOException, B2JsonException {
        skipWhitespace();
        return currentChar == c;
    }

    /**
     * Is the next character the given char?
     */
    public boolean nextNonWhitespaceIsEof() throws IOException, B2JsonException {
        skipWhitespace();
        return currentChar == EOF;
    }

    /**
     * Reads the name of the next object field.  After calling this, you
     * can read the value using whichever method is right for the value
     * type.
     */
    public String readObjectFieldNameAndColon() throws B2JsonException, IOException {
        String result = readString();
        skipObjectColon();
        return result;
    }

    public void skipObjectColon() throws IOException, B2JsonException {
        skipWhitespace();
        if (currentChar != ':') {
            throw new B2JsonException("no ':' after field name in object");
        }
        next();
    }

    /**
     * Reads the close brace for an object.
     */
    public void finishObject() throws B2JsonException, IOException {
        skipWhitespace();
        if (currentChar != '}') {
            throw new B2JsonException("object should end with brace but found: " + ((char) currentChar));
        }
        next();
    }

    /**
     * Starts reading an array by reading the "[".
     *
     * Returns true iff there is anything in the array.
     *
     * Use this code to read an array:
     *
     *    if (in.startArrayAndCheckForContents()) {
     *        do {
     *            // read value
     *        } while (in.arrayHasMoreValues());
     *    }
     *    finishArray();
     */
    public boolean startArrayAndCheckForContents() throws IOException, B2JsonException {
        if (!nextNonWhitespaceIs('[')) {
            throw new B2JsonException("array should start with bracket but found: " + ((char) currentChar));
        }
        next();
        return !nextNonWhitespaceIs(']');
    }

    /**
     * Are there more values in an array?
     */
    public boolean arrayHasMoreValues() throws IOException, B2JsonException {
        if (nextNonWhitespaceIs(',')) {
            next();
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Reads the close bracket for an array.
     */
    public void finishArray() throws B2JsonException, IOException {
        skipWhitespace();
        if (currentChar != ']') {
            throw new B2JsonException("array should end with bracket but found: " + ((char) currentChar));
        }
        next();
    }

    /**
     * Skips over "null".
     */
    public void readNull() throws IOException, B2JsonException {
        if (currentChar != 'n') {
            throw new B2JsonException("expected 'null'");
        }
        next();
        if (currentChar != 'u') {
            throw new B2JsonException("expected 'null'");
        }
        next();
        if (currentChar != 'l') {
            throw new B2JsonException("expected 'null'");
        }
        next();
        if (currentChar != 'l') {
            throw new B2JsonException("expected 'null'");
        }
        next();
    }

    /**
     * Skips over "false".
     */
    public void readFalse() throws IOException, B2JsonException {
        if (currentChar != 'f') {
            throw new B2JsonException("expected 'false'");
        }
        next();
        if (currentChar != 'a') {
            throw new B2JsonException("expected 'false'");
        }
        next();
        if (currentChar != 'l') {
            throw new B2JsonException("expected 'false'");
        }
        next();
        if (currentChar != 's') {
            throw new B2JsonException("expected 'false'");
        }
        next();
        if (currentChar != 'e') {
            throw new B2JsonException("expected 'false'");
        }
        next();
    }

    /**
     * Skips over "true".
     */
    public void readTrue() throws IOException, B2JsonException {
        if (currentChar != 't') {
            throw new B2JsonException("expected 'true'");
        }
        next();
        if (currentChar != 'r') {
            throw new B2JsonException("expected 'true'");
        }
        next();
        if (currentChar != 'u') {
            throw new B2JsonException("expected 'true'");
        }
        next();
        if (currentChar != 'e') {
            throw new B2JsonException("expected 'true'");
        }
        next();
    }

    /**
     * Reads the next char, which must be a hex digit.
     *
     * Returns a number from 0 to 15;
     */
    private int readHexDigit() throws IOException, B2JsonException {
        final int c = currentChar;
        if ('0' <= c && c <= '9') {
            next();
            return c - '0';
        }
        if ('a' <= c && c <= 'f') {
            next();
            return 10 + c - 'a';
        }
        if ('A' <= c && c <= 'F') {
            next();
            return 10 + c - 'A';
        }
        throw new B2JsonException("bad hex digit: " + ((char)c));
    }

    /**
     * Skip over whitespace until reaching a non-whitespace character
     * or EOF.
     */
    private void skipWhitespace() throws IOException, B2JsonException {
        while (true) {
            // These are the whitespace chars defined in the JSON spec.
            if (currentChar == '\t' || currentChar == '\n' || currentChar == '\r' || currentChar == ' ') {
                next();
            }
            // Comments are non-standard.  They're not allowed at all in
            // the JSON spec.  So ignoring comments won't cause any problems
            // reading valid JSON.
            else if (currentChar == '/') {
                next();
                if (currentChar == '/') {
                    next();
                    while (currentChar != '\n' && currentChar != EOF) {
                        next();
                    }
                }
                else {
                    throw new B2JsonException("invalid comment: single slash");
                }
            }
            // That wasn't whitespace.  We're done
            else {
                return;
            }
        }
    }

    /**
     * Appends the current character to the string builder, and then
     * reads the next character.
     */
    private void appendAndNext() throws IOException {
        if (currentChar == EOF) {
            throw new IllegalStateException();
        }
        builder.append((char) currentChar);
        currentChar = in.read();
    }

    /**
     * Advances to the next character.
     */
    private void next() throws IOException {
        currentChar = in.read();
    }

    /**
     * Is this character a digit?
     */
    private boolean isDigit(int c) {
        return '0' <= c && c <= '9';
    }

}

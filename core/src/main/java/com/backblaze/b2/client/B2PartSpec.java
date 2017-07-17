/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import java.util.Comparator;
import java.util.Objects;

/**
 * A B2PartSpec represents part of a large file.
 * It has the partNumber and the offset & length of the part in the file.
 */
class B2PartSpec implements Comparable<B2PartSpec> {
    private static Comparator<B2PartSpec> comparator = Comparator
            .comparingInt(B2PartSpec::getPartNumber)
            .thenComparing(Comparator.comparingLong(B2PartSpec::getStart))
            .thenComparing(Comparator.comparingLong(B2PartSpec::getLength));

    final int partNumber; // one-based part number.
    final long start;     // byte offset in the file (zero-based)
    final long length;    // length in bytes.

    B2PartSpec(int partNumber,
               long start,
               long length) {

        this.partNumber = partNumber;
        this.start = start;
        this.length = length;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public long getStart() {
        return start;
    }

    public long getLength() {
        return length;
    }

    @Override
    public String toString() {
        return "B2PartSpec{" +
                "#" + partNumber +
                ", start=" + start +
                ", pastEnd=" + (start+length) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2PartSpec partSpec = (B2PartSpec) o;
        return partNumber == partSpec.partNumber &&
                start == partSpec.start &&
                length == partSpec.length;
    }

    @Override
    public int hashCode() {
        return Objects.hash(partNumber, start, length);
    }


    @Override
    public int compareTo(B2PartSpec o) {
        return comparator.compare(this, o);
    }
}

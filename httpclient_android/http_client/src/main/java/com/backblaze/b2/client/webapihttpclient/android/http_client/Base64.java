//package com.backblaze.b2.client.webapihttpclient.android.http_client;
//
///*
//implementation for Android < API 26
//See these links for reference:
//https://docs.oracle.com/javase/8/docs/api/java/util/Base64.html
//https://tools.ietf.org/html/rfc4648
//https://tools.ietf.org/html/rfc2045
//
// temp implementation:
// */
//public class Base64 {
//    private Base64() {}
//
//    public static Encoder getEncoder() {
//        return new Encoder(false, null, -1, true);
//    }
//
//    // encoder class
//    public static class Encoder {
//        // private singleton constructor
//        private Encoder(boolean isURL, byte[] newline, int linemax, boolean doPadding) {
//            this.isURL = isURL;
//            this.newline = newline;
//            this.linemax = linemax;
//            this.doPadding = doPadding;
//        }
//
//        @SuppressWarnings("deprecation")
//        public String encodeToString(byte[] src) {
//            byte[] encoded = encode(src);
//            return new String(encoded, 0, 0, encoded.length);
//        }
//
//        public byte[] encode(byte[] src) {
//            int len =  outLength(src.length);          // dst array size
//            byte[] dst = new byte[len];
//            int ret = encode0(src, 0, src.length, dst);
//            if (ret != dst.length)
//                return Arrays.copyOf(dst, ret);
//            return dst;
//        }
//
//        private int encode0(byte[] src, int off, int end, byte[] dst) {
//            char[] base64 = isURL ? toBase64URL : toBase64;
//            int sp = off;
//            int slen = (end - off) / 3 * 3;
//            int sl = off + slen;
//            if (linemax > 0 && slen  > linemax / 4 * 3)
//                slen = linemax / 4 * 3;
//            int dp = 0;
//            while (sp < sl) {
//                int sl0 = Math.min(sp + slen, sl);
//                for (int sp0 = sp, dp0 = dp ; sp0 < sl0; ) {
//                    int bits = (src[sp0++] & 0xff) << 16 |
//                            (src[sp0++] & 0xff) <<  8 |
//                            (src[sp0++] & 0xff);
//                    dst[dp0++] = (byte)base64[(bits >>> 18) & 0x3f];
//                    dst[dp0++] = (byte)base64[(bits >>> 12) & 0x3f];
//                    dst[dp0++] = (byte)base64[(bits >>> 6)  & 0x3f];
//                    dst[dp0++] = (byte)base64[bits & 0x3f];
//                }
//                int dlen = (sl0 - sp) / 3 * 4;
//                dp += dlen;
//                sp = sl0;
//                if (dlen == linemax && sp < end) {
//                    for (byte b : newline){
//                        dst[dp++] = b;
//                    }
//                }
//            }
//            if (sp < end) {               // 1 or 2 leftover bytes
//                int b0 = src[sp++] & 0xff;
//                dst[dp++] = (byte)base64[b0 >> 2];
//                if (sp == end) {
//                    dst[dp++] = (byte)base64[(b0 << 4) & 0x3f];
//                    if (doPadding) {
//                        dst[dp++] = '=';
//                        dst[dp++] = '=';
//                    }
//                } else {
//                    int b1 = src[sp++] & 0xff;
//                    dst[dp++] = (byte)base64[(b0 << 4) & 0x3f | (b1 >> 4)];
//                    dst[dp++] = (byte)base64[(b1 << 2) & 0x3f];
//                    if (doPadding) {
//                        dst[dp++] = '=';
//                    }
//                }
//            }
//            return dp;
//        }
//
//        private int outLength(byte[] src, int sp, int sl) {
//            int[] base64 = isURL ? fromBase64URL : fromBase64;
//            int paddings = 0;
//            int len = sl - sp;
//            if (len == 0)
//                return 0;
//            if (len < 2) {
//                if (isMIME && base64[0] == -1)
//                    return 0;
//                throw new IllegalArgumentException(
//                        "Input byte[] should at least have 2 bytes for base64 bytes");
//            }
//            if (isMIME) {
//                // scan all bytes to fill out all non-alphabet. a performance
//                // trade-off of pre-scan or Arrays.copyOf
//                int n = 0;
//                while (sp < sl) {
//                    int b = src[sp++] & 0xff;
//                    if (b == '=') {
//                        len -= (sl - sp + 1);
//                        break;
//                    }
//                    if ((b = base64[b]) == -1)
//                        n++;
//                }
//                len -= n;
//            } else {
//                if (src[sl - 1] == '=') {
//                    paddings++;
//                    if (src[sl - 2] == '=')
//                        paddings++;
//                }
//            }
//            if (paddings == 0 && (len & 0x3) !=  0)
//                paddings = 4 - (len & 0x3);
//            return 3 * ((len + 3) / 4) - paddings;
//        }
//
//    }
//
//}
//
//
//
//
//
//
//// https://en.wikipedia.org/wiki/Base64
////////
//
//    @SuppressWarnings("deprecation")
//    public String encodeToString(byte[] src) {
//        byte[] encoded = encode(src);
//        return new String(encoded, 0, 0, encoded.length);
//    }
//
//    public byte[] encode(byte[] src) {
//        int len = outLength(src.length);          // dst array size
//        byte[] dst = new byte[len];
//        int ret = encode0(src, 0, src.length, dst);
//        if (ret != dst.length)
//            return Arrays.copyOf(dst, ret);
//        return dst;
//    }
//
//    /**
//     * This array is a lookup table that translates 6-bit positive integer
//     * index values into their "Base64 Alphabet" equivalents as specified
//     * in "Table 1: The Base64 Alphabet" of RFC 2045 (and RFC 4648).
//     */
//    private static final char[] toBase64 = {
//            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
//            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
//            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
//            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
//            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
//    };
//
//    /**
//     * It's the lookup table for "URL and Filename safe Base64" as specified
//     * in Table 2 of the RFC 4648, with the '+' and '/' changed to '-' and
//     * '_'. This table is used when BASE64_URL is specified.
//     */
//    private static final char[] toBase64URL = {
//            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
//            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
//            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
//            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
//            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '_'
//    };
//
//    private int encode0(byte[] src, int off, int end, byte[] dst) {
//        char[] base64 = isURL ? toBase64URL : toBase64;
//        int sp = off;
//        int slen = (end - off) / 3 * 3;
//        int sl = off + slen;
//        if (linemax > 0 && slen  > linemax / 4 * 3)
//            slen = linemax / 4 * 3;
//        int dp = 0;
//        while (sp < sl) {
//            int sl0 = Math.min(sp + slen, sl);
//            for (int sp0 = sp, dp0 = dp ; sp0 < sl0; ) {
//                int bits = (src[sp0++] & 0xff) << 16 |
//                        (src[sp0++] & 0xff) <<  8 |
//                        (src[sp0++] & 0xff);
//                dst[dp0++] = (byte)base64[(bits >>> 18) & 0x3f];
//                dst[dp0++] = (byte)base64[(bits >>> 12) & 0x3f];
//                dst[dp0++] = (byte)base64[(bits >>> 6)  & 0x3f];
//                dst[dp0++] = (byte)base64[bits & 0x3f];
//            }
//            int dlen = (sl0 - sp) / 3 * 4;
//            dp += dlen;
//            sp = sl0;
//            if (dlen == linemax && sp < end) {
//                for (byte b : newline){
//                    dst[dp++] = b;
//                }
//            }
//        }
//        if (sp < end) {               // 1 or 2 leftover bytes
//            int b0 = src[sp++] & 0xff;
//            dst[dp++] = (byte)base64[b0 >> 2];
//            if (sp == end) {
//                dst[dp++] = (byte)base64[(b0 << 4) & 0x3f];
//                if (doPadding) {
//                    dst[dp++] = '=';
//                    dst[dp++] = '=';
//                }
//            } else {
//                int b1 = src[sp++] & 0xff;
//                dst[dp++] = (byte)base64[(b0 << 4) & 0x3f | (b1 >> 4)];
//                dst[dp++] = (byte)base64[(b1 << 2) & 0x3f];
//                if (doPadding) {
//                    dst[dp++] = '=';
//                }
//            }
//        }
//        return dp;
//    }
//
//    private int outLength(byte[] src, int sp, int sl) {
//        int[] base64 = isURL ? fromBase64URL : fromBase64;
//        int paddings = 0;
//        int len = sl - sp;
//        if (len == 0)
//            return 0;
//        if (len < 2) {
//            if (isMIME && base64[0] == -1)
//                return 0;
//            throw new IllegalArgumentException(
//                    "Input byte[] should at least have 2 bytes for base64 bytes");
//        }
//        if (isMIME) {
//            // scan all bytes to fill out all non-alphabet. a performance
//            // trade-off of pre-scan or Arrays.copyOf
//            int n = 0;
//            while (sp < sl) {
//                int b = src[sp++] & 0xff;
//                if (b == '=') {
//                    len -= (sl - sp + 1);
//                    break;
//                }
//                if ((b = base64[b]) == -1)
//                    n++;
//            }
//            len -= n;
//        } else {
//            if (src[sl - 1] == '=') {
//                paddings++;
//                if (src[sl - 2] == '=')
//                    paddings++;
//            }
//        }
//        if (paddings == 0 && (len & 0x3) !=  0)
//            paddings = 4 - (len & 0x3);
//        return 3 * ((len + 3) / 4) - paddings;
//    }
//}
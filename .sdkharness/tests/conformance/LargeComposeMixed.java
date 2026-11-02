// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.B2CopyingPartStorer;
import com.backblaze.b2.client.B2PartStorer;
import com.backblaze.b2.client.B2UploadingPartStorer;
import com.backblaze.b2.client.contentSources.B2ByteArrayContentSource;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2StartLargeFileRequest;
import com.backblaze.b2.util.B2ByteRange;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * large.compose_mixed. Run by ../run-conformance (see ../../README.md).
 *
 * <p>"a large file can be assembled from a mix of uploaded and copied parts"
 * (https://www.backblaze.com/docs/cloud-storage-large-files). b2_copy_part "Copies from an existing B2 file,
 * storing it as a part of a large file which has already been started with b2_start_large_file"; its range is
 * "The range of bytes to copy. If not provided, the whole source file will be copied."
 * (https://www.backblaze.com/apidocs/b2-copy-part). Every part but the last is at least the realm's absolute
 * minimum part size ("the minimum part size is 5 MB except for the last part in a file which has a minimum size of
 * 1 byte", https://www.backblaze.com/docs/cloud-storage-create-large-files-with-the-native-api; the size is read
 * from the realm, never hard-coded).
 *
 * <p>One large file is composed with {@code storeLargeFile} from three pieces: bytes held locally (an uploading
 * part), a byte range of an existing file (a copying part with a range) and a whole existing file (a copying part
 * without one). Asserted: the SDK sent one upload part and two copy parts and no other part, and the finished
 * file's bytes equal the ordered concatenation of the three.
 */
public class LargeComposeMixed {
    public static void main(String[] args) {
        Support.run("large.compose_mixed", true, () -> {
            Support.step("bucket");
            final B2Bucket bucket = Support.bucket();
            final int piece = (int) Support.partSize();

            Support.step("sources");
            final byte[] local = Support.payload(piece, 51);
            final byte[] rangedSource = Support.payload(3 * piece, 52);
            final byte[] wholeSource = Support.payload(piece + 7, 53);
            final B2FileVersion ranged = Support.upload(bucket, "st/compose/ranged-source.bin", rangedSource, "application/octet-stream", null);
            final B2FileVersion whole = Support.upload(bucket, "st/compose/whole-source.bin", wholeSource, "application/octet-stream", null);
            final byte[] rangePiece = Arrays.copyOfRange(rangedSource, piece, 2 * piece);

            Support.step("compose");
            final B2FileVersion started = Support.client.startLargeFile(
                    B2StartLargeFileRequest.builder(bucket.getBucketId(), "st/compose/composed.bin", "application/octet-stream").build());
            final List<B2PartStorer> parts = new ArrayList<>();
            parts.add(new B2UploadingPartStorer(1, B2ByteArrayContentSource.build(local)));
            parts.add(new B2CopyingPartStorer(2, ranged.getFileId(), B2ByteRange.between(piece, 2L * piece - 1)));
            parts.add(new B2CopyingPartStorer(3, whole.getFileId()));
            Support.wire.clear();
            final B2FileVersion composed = Support.client.storeLargeFile(started, parts, null, Support.executor(2));

            Support.step("the SDK used a mix of parts");
            Support.eq("b2_upload_part requests", 1, Support.wireCount("POST", "b2_upload_part"));
            Support.eq("b2_copy_part requests", 2, Support.wireCount("POST", "b2_copy_part"));
            Support.eq("b2_finish_large_file requests", 1, Support.wireCount("POST", "b2_finish_large_file"));
            Support.eq("composed fileId", started.getFileId(), composed.getFileId());

            Support.step("bytes");
            final ByteArrayOutputStream expected = new ByteArrayOutputStream();
            expected.write(local);
            expected.write(rangePiece);
            expected.write(wholeSource);
            Support.eq("composed contentLength", (long) expected.size(), Support.client.getFileInfo(composed.getFileId()).getContentLength());
            Support.check(Arrays.equals(expected.toByteArray(), Support.downloadById(composed.getFileId()).bytes),
                    "the composed file is not the ordered concatenation of the local bytes, the copied range and the whole copied file");
        });
    }
}

// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2ListFileNamesRequest;
import com.backblaze.b2.client.structures.B2ListFileVersionsRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * files.list. Run by ../run-conformance (see ../../README.md).
 *
 * <p>b2_list_file_names returns the newest version of each file "in alphabetical order by file name",
 * honours prefix, delimiter (a folder entry per common prefix) and startFileName, and continues across
 * pages; b2_list_file_versions returns every version, newest first within a name. The SDK's iterable
 * fetches page after page, so a page size of 2 over six files proves it follows the continuation.
 * https://www.backblaze.com/apidocs/b2-list-file-names
 * https://www.backblaze.com/apidocs/b2-list-file-versions
 */
public class FilesList {
    public static void main(String[] args) {
        Support.run("files.list", () -> {
            Support.step("bucket");
            final B2Bucket bucket = Support.bucket();
            final String id = bucket.getBucketId();

            Support.step("upload");
            final List<String> listed = Arrays.asList("st/list/a.txt", "st/list/b.txt", "st/list/c.txt",
                    "st/list/d.txt", "st/list/e.txt", "st/list/sub/f.txt");
            for (String name : listed) {
                Support.upload(bucket, name, Support.utf8(name), "text/plain", null);
            }
            Support.upload(bucket, "st/other/z.txt", Support.utf8("other"), "text/plain", null);

            Support.step("paged listing by prefix");
            final List<String> paged = Support.names(Support.client.fileNames(
                    B2ListFileNamesRequest.builder(id).setPrefix("st/list/").setMaxFileCount(2).build()));
            Support.eq("names under st/list/ with a page size of 2", listed, paged);

            Support.step("whole bucket");
            Support.eq("name count", 7, Support.names(Support.client.fileNames(id)).size());

            Support.step("start name");
            Support.eq("names from c.txt", Arrays.asList("st/list/c.txt", "st/list/d.txt", "st/list/e.txt", "st/list/sub/f.txt"),
                    Support.names(Support.client.fileNames(B2ListFileNamesRequest.builder(id)
                            .setPrefix("st/list/").setStartFileName("st/list/c.txt").build())));

            Support.step("delimiter");
            final List<String> folders = new ArrayList<>();
            for (B2FileVersion entry : Support.client.fileNames(B2ListFileNamesRequest.builder(id).setPrefix("st/").setDelimiter("/").build())) {
                Support.check(entry.isFolder(), "an entry directly under st/ is not a folder: " + entry.getFileName());
                folders.add(entry.getFileName());
            }
            Support.eq("folder entries under st/", Arrays.asList("st/list/", "st/other/"), folders);

            Support.step("versions");
            final B2FileVersion newer = Support.upload(bucket, "st/list/a.txt", Support.utf8("newer a"), "text/plain", null);
            final List<B2FileVersion> versions = new ArrayList<>();
            for (B2FileVersion v : Support.client.fileVersions(B2ListFileVersionsRequest.builder(id).setPrefix("st/list/a.txt").build())) {
                versions.add(v);
            }
            Support.eq("versions of st/list/a.txt", 2, versions.size());
            Support.eq("newest version comes first", newer.getFileId(), versions.get(0).getFileId());
            final List<B2FileVersion> current = new ArrayList<>();
            for (B2FileVersion v : Support.client.fileNames(B2ListFileNamesRequest.builder(id).setPrefix("st/list/a.txt").build())) {
                current.add(v);
            }
            Support.eq("names listing shows one entry for st/list/a.txt", 1, current.size());
            Support.eq("names listing shows the newest version", newer.getFileId(), current.get(0).getFileId());
        });
    }
}

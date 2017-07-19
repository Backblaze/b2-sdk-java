/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.sample;

import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.contentSources.B2ContentTypes;
import com.backblaze.b2.client.contentSources.B2FileContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2ListFileNamesRequest;
import com.backblaze.b2.client.structures.B2ListFileVersionsRequest;
import com.backblaze.b2.client.structures.B2Part;
import com.backblaze.b2.client.structures.B2UpdateBucketRequest;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.client.webApiHttpClient.B2StorageHttpClientBuilder;
import com.backblaze.b2.util.B2ExecutorUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class B2 implements AutoCloseable {
    private static final String APP_NAME = "b2_4j";
    private static final String VERSION = "0.0.1";
    private static final String USER_AGENT = APP_NAME + "/" + VERSION;

    // these just don't make sense (yet), since i'm getting credentials from the environment:
    //  "    b2 authorize_account [<accountId>] [<applicationKey>]\n" +
    //  "    b2 clear_account\n" +

    // these are more work than i want to do right now:
    //  "    b2 ls [--long] [--versions] <bucketName> [<folderName>]\n" +
    //  "    b2 sync [--delete] [--keepDays N] [--skipNewer] [--replaceNewer] \\\n" +
    //  "        [--threads N] [--noProgress] <source> <destination>\n" +


    private static final String USAGE =
            "USAGE:\n" +
                    //"    b2 cancel_all_unfinished_large_files <bucketName>\n" +
                    //"    b2 cancel_large_file <fileId>\n" +
                    "    b2 create_bucket <bucketName> [allPublic | allPrivate]\n" +
                    "    b2 delete_bucket <bucketName>\n" +
                    "    b2 delete_file_version <fileName> <fileId>\n" +
                    //"    b2 download_file_by_id [--noProgress] <fileId> <localFileName>\n" +
                    //"    b2 download_file_by_name [--noProgress] <bucketName> <fileName> <localFileName>\n" +
                    "    b2 get_file_info <fileId>\n" +
                    //"    b2 help [commandName]\n" +
                    "    b2 hide_file <bucketName> <fileName>\n" +
                    "    b2 list_buckets\n" +
                    "    b2 list_file_names <bucketName> [<startFileName>] [<maxPerFetch>]  // unlike the python b2 cmd, this will continue fetching, until done.\n" +
                    "    b2 list_file_versions <bucketName> [<startFileName>] [<startFileId>] [<maxPerFetch>]  // unlike the python b2 cmd, this will continue fetching, until done.\n" +
                    "    b2 list_parts <largeFileId>\n" +
                    "    b2 list_unfinished_large_files <bucketName>\n" +
                    //"    b2 make_url <fileId>\n" +
                    "    b2 update_bucket <bucketName> [allPublic | allPrivate]\n" +
                    "    b2 upload_file [--sha1 <sha1sum>] [--contentType <contentType>] [--info <key>=<value>]* \\\n" +
                    "        [--noProgress] [--threads N] <bucketName> <localFilePath> <b2FileName>\n" +
                    "    b2 version\n";

    private static final long KILOBYTES = 1000;
    private static final long MEGABYTES = 1000 * KILOBYTES;
    //private static final long GIGABYTES = 1000 * MEGABYTES;

    // where we should write normal output to.
    private final PrintStream out;

    // our client.
    private final B2StorageClient client;

    // overridden by some command line args.
    private boolean showProgress = true;
    private int numThreads = 6;

    // this is our executor.  use getExecutor() to access it.
    // it's null until the first time getExecutor() is called.
    private ExecutorService executor;

    private B2() throws B2Exception {
        out = System.out;
        client = B2StorageHttpClientBuilder.builder(USER_AGENT).build();
    }

    private ExecutorService getExecutor() {
        if (executor == null) {
            executor = Executors.newFixedThreadPool(numThreads, B2ExecutorUtils.createThreadFactory(APP_NAME + "-%d"));
        }
        return executor;
    }

    @Override
    public void close() {
        if (executor != null) {
            B2ExecutorUtils.shutdownAndAwaitTermination(executor, 10, 10);
            executor = null;
        }
    }

    private static void usageAndExit(String errMsg) {
        System.err.println("ERROR: " + errMsg);
        System.err.println();
        System.err.println(USAGE);
        System.exit(1);
    }

    public static void main(String[] args) throws B2Exception, IOException {
        //out.println("args = [" + String.join(",", args) + "]");

        if (args.length == 0) {
            usageAndExit("you must specify which command you want to run.");
        }

        final String command = args[0];
        final String[] remainingArgs = Arrays.copyOfRange(args, 1, args.length);
        try (B2 b2 = new B2()) {
            if ("create_bucket".equals(command)) {
                b2.create_bucket(remainingArgs);
            } else if ("delete_bucket".equals(command)) {
                b2.delete_bucket(remainingArgs);
            } else if ("delete_file_version".equals(command)) {
                b2.delete_file_version(remainingArgs);
            } else if ("get_file_info".equals(command)) {
                b2.get_file_info(remainingArgs);
            } else if ("hide_file".equals(command)) {
                b2.hide_file(remainingArgs);
            } else if ("list_buckets".equals(command)) {
                b2.list_buckets(remainingArgs);
            } else if ("list_file_names".equals(command)) {
                b2.list_file_names(remainingArgs);
            } else if ("list_file_versions".equals(command)) {
                b2.list_file_versions(remainingArgs);
            } else if ("list_parts".equals(command)) {
                b2.list_parts(remainingArgs);
            } else if ("list_unfinished_large_files".equals(command)) {
                b2.list_unfinished_large_files(remainingArgs);
            } else if ("update_bucket".equals(command)) {
                b2.update_bucket(remainingArgs);
            } else if ("upload_file".equals(command)) {
                b2.upload_file(remainingArgs);
            } else if ("version".equals(command)) {
                b2.version(remainingArgs);
            } else {
                usageAndExit("unsupported command '" + command + "'");
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////
    //
    // arg-related helpers
    //
    ////////////////////////////////////////////////////////////////////////

    private void checkArgs(boolean isOk,
                           String errMsg) {
        if (!isOk) {
            usageAndExit(errMsg);
        }
    }

    private void checkArgCount(String[] args,
                               int minCount,
                               int maxCount) {
        checkArgs(args.length >= minCount, "too few arguments");
        checkArgs(args.length <= maxCount, "too many arguments");
    }

    private void checkArgCount(String[] args,
                               int exactCount) {
        checkArgCount(args, exactCount, exactCount);
    }

    private void checkArgCountIsAtLeast(String[] args,
                                        int minCount) {
        checkArgCount(args, minCount, Integer.MAX_VALUE);
    }

    private String getArgOrDie(String[] args,
                               String arg,
                               int iArg,
                               int iLastArg) {
        if (iArg > iLastArg) {
            usageAndExit("missing argument for '" + arg + "'");
        }
        return args[iArg];
    }
    private String getArgOrNull(String[] args,
                               int iArg) {
        if (iArg >= args.length) {
            return null;
        }
        return args[iArg];
    }
    private Integer getPositiveIntOrNull(String[] args,
                                         String arg,
                                         int iArg) {
        final String asString = getArgOrNull(args, iArg);
        if (asString == null) {
            return null;
        }

        try {
            return Integer.parseInt(asString);
        } catch (NumberFormatException e) {
            usageAndExit("argument for '" + arg  + "' must be an integer");
            return 666;  // we never get here.
        }
    }

    private int getIntArgOrDie(String[] args,
                               String arg,
                               int iArg,
                               int iLastArg) {
        final String asString = getArgOrDie(args, arg, iArg, iLastArg);
        try {
            return Integer.parseInt(asString);
        } catch (NumberFormatException e) {
            usageAndExit("argument for '" + arg  + "' must be an integer");
            return 666;  // we never get here.
        }
    }
    private int getPositiveIntArgOrDie(String[] args,
                                       String arg,
                                       int iArg,
                                       int iLastArg) {
        int value = getIntArgOrDie(args, arg, iArg, iLastArg);
        if (value <= 0) {
            usageAndExit("argument for '" + arg + "' must be a POSITIVE integer");
        }
        return value;
    }

    ////////////////////////////////////////////////////////////////////////
    //
    // helpers for the commands
    //
    ////////////////////////////////////////////////////////////////////////

    private B2Bucket getBucketByNameOrDie(String bucketName) throws B2Exception {
        for (B2Bucket bucket : client.buckets()) {
            if (bucket.getBucketName().equals(bucketName)) {
                return bucket;
            }
        }

        usageAndExit("can't find bucket named '" + bucketName + "'");
        throw new RuntimeException("usageAndExit never returns!");
    }

    private B2UploadFileRequest makeUploadRequestFromArgs(String[] args) throws B2Exception {
        // [--sha1 <sha1sum>]
        // [--contentType <contentType>]
        // [--info <key>=<value>]*
        // [--noProgress]
        // [--threads N]
        // <bucketName> <localFilePath> <b2FileName>

        checkArgCountIsAtLeast(args, 3);
        int iLastArg = args.length - 1;

        final String b2Path = args[iLastArg];
        iLastArg--;

        final String localPath = args[iLastArg];
        iLastArg--;

        final String bucketName = args[iLastArg];
        iLastArg--;

        String sha1 = null;
        String contentType = B2ContentTypes.B2_AUTO;
        final Map<String,String> infos = new TreeMap<>();

        for (int iArg=0; iArg <= iLastArg; iArg++) {
            final String arg = args[iArg];
            if ("--sha1".equals(arg)) {
                iArg++;
                sha1 = getArgOrDie(args, arg, iArg, iLastArg);
            } else if ("--noProgress".equals(arg)) {
                showProgress = false;
            } else if ("--contentType".equals(arg)) {
                iArg++;
                contentType = getArgOrDie(args, arg, iArg, iLastArg);
            } else if ("--threads".equals(arg)) {
                iArg++;
                numThreads = getPositiveIntArgOrDie(args, arg, iArg, iLastArg);
            } else if ("--info".equals(arg)) {
                iArg++;
                final String pair = getArgOrDie(args, arg, iArg, iLastArg);
                final String[] vParts = pair.split("=");
                if (vParts.length != 2 || vParts[0].isEmpty()) {
                    usageAndExit("bad format for argument to '" + arg + "' (" + pair + ")");
                }
                infos.put(vParts[0], vParts[1]);
            } else {
                usageAndExit("unexpected argument '" + arg + "'");
            }
        }

        final B2Bucket bucket = getBucketByNameOrDie(bucketName);
        final B2ContentSource source = B2FileContentSource
                .builder(new File(localPath))
                .setSha1(sha1)
                .build();

        return B2UploadFileRequest
                .builder(bucket.getBucketId(),
                        b2Path,
                        contentType,
                        source)
                .setCustomFields(infos)
                .build();
    }

    ////////////////////////////////////////////////////////////////////////
    //
    // methods for each command
    //
    ////////////////////////////////////////////////////////////////////////


    private void create_bucket(String[] args) throws B2Exception {
        checkArgCount(args, 2);
        client.createBucket(args[0], args[1]);
    }

    private void update_bucket(String[] args) throws B2Exception {
        checkArgCount(args, 2);
        final B2Bucket bucket = getBucketByNameOrDie(args[0]);
        final B2UpdateBucketRequest request = B2UpdateBucketRequest
                .builder(bucket)
                .setBucketType(args[1])
                .build();
        client.updateBucket(request);
    }

    private void delete_bucket(String[] args) throws B2Exception {
        checkArgCount(args, 1);
        final B2Bucket bucket = getBucketByNameOrDie(args[0]);
        client.deleteBucket(bucket.getBucketId());
    }

    private void delete_file_version(String[] args) throws B2Exception {
        // <fileName> <fileId>
        checkArgCount(args, 2);
        final String b2Path = args[0];
        final String fileId = args[1];
        client.deleteFileVersion(b2Path, fileId);
    }

    private void get_file_info(String[] args) throws B2Exception {
        // <fileId>
        checkArgCount(args, 1);
        final String fileId = args[0];
        final B2FileVersion version = client.getFileInfo(fileId);
        out.println(version);
        out.println("fileInfo:  " + version.getFileInfo());
    }

    private void hide_file(String[] args) throws B2Exception {
        // <bucketName> <fileName>
        checkArgCount(args, 2);
        final String bucketName = args[0];
        final String b2Path = args[1];
        final B2Bucket bucket = getBucketByNameOrDie(bucketName);
        client.hideFile(bucket.getBucketId(), b2Path);
    }

    private void list_buckets(String[] args) throws B2Exception {
        checkArgCount(args, 0);
        for (B2Bucket bucket : client.buckets()) {
            out.println(bucket);
        }
    }

    private void list_file_names(String[] args) throws B2Exception {
        // <bucketName> [<startFileName>] [<maxPerFetch >]
        checkArgCount(args, 1, 3);

        final String bucketName = args[0];
        final String startFileName = getArgOrNull(args, 1);
        final Integer maxPerFetch = getPositiveIntOrNull(args, "maxPerFetch", 2);

        final B2Bucket bucket = getBucketByNameOrDie(bucketName);

        final B2ListFileNamesRequest.Builder builder = B2ListFileNamesRequest
                .builder(bucket.getBucketId())
                .setMaxFileCount(maxPerFetch);
        if (startFileName != null) {
            builder.setStartFileName(startFileName);
        }

        final B2ListFileNamesRequest request = builder.build();

        for (B2FileVersion version : client.fileNames(request)) {
            out.println(version);
        }
    }


    private void list_file_versions(String[] args) throws B2Exception {
        // list_file_versions <bucketName> [<startFileName> <startFileId>] [<maxPerFetch>]
        checkArgCount(args, 1, 4);

        final String bucketName = args[0];
        final String startFileName = getArgOrNull(args, 1);
        final String startFileId = getArgOrNull(args, 2);
        final Integer maxPerFetch = getPositiveIntOrNull(args, "maxPerFetch", 3);

        if (startFileName != null) {
            checkArgs(startFileId != null, "if you specify startFileName, you must specify startFileId too");
        }

        final B2Bucket bucket = getBucketByNameOrDie(bucketName);

        final B2ListFileVersionsRequest.Builder builder = B2ListFileVersionsRequest
                .builder(bucket.getBucketId())
                .setMaxFileCount(maxPerFetch);
        if (startFileName != null) {
            builder.setStart(startFileName, startFileId);
        }

        final B2ListFileVersionsRequest request = builder.build();

        for (B2FileVersion version : client.fileVersions(request)) {
            out.println(version);
        }
    }

    private void list_unfinished_large_files(String[] args) throws B2Exception {
        // <bucketName>
        checkArgCount(args, 1);
        final String bucketName = args[0];
        final B2Bucket bucket = getBucketByNameOrDie(bucketName);

        for (B2FileVersion version : client.unfinishedLargeFiles(bucket.getBucketId())) {
            out.println(version);
        }
    }

    private void list_parts(String[] args) throws B2Exception {
        // <largeFileId>
        checkArgCount(args, 1);
        final String largeFileId = args[0];

        for (B2Part part : client.parts(largeFileId)) {
            out.println(part);
        }
    }

    private void upload_file(String[] args) throws B2Exception, IOException {
        B2UploadFileRequest request = makeUploadRequestFromArgs(args);
        if (shouldBeSmallFile(request)) {
            client.uploadSmallFile(request);
        } else {
            client.uploadLargeFile(request, getExecutor());
        }
    }

    private boolean shouldBeSmallFile(B2UploadFileRequest request) throws IOException {
        // XXX: improve this.  right now it's just a convenient size for my playing with it.
        //      it should be based on the server's recommendedPartSize.
        return request.getContentSource().getContentLength() < (10 * MEGABYTES);
    }

    private void version(String[] args) {
        checkArgCount(args, 0);
        out.println("b2 command line tool in java, version " + VERSION);
    }

}

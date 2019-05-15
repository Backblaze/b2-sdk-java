/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.sample;

import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.B2StorageClientFactory;
import com.backblaze.b2.client.contentHandlers.B2ContentFileWriter;
import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.contentSources.B2ContentTypes;
import com.backblaze.b2.client.contentSources.B2FileContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2CorsRule;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2GetDownloadAuthorizationRequest;
import com.backblaze.b2.client.structures.B2ListFileNamesRequest;
import com.backblaze.b2.client.structures.B2ListFileVersionsRequest;
import com.backblaze.b2.client.structures.B2Part;
import com.backblaze.b2.client.structures.B2UpdateBucketRequest;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.json.B2JsonException;
import com.backblaze.b2.util.B2ExecutorUtils;
import com.backblaze.b2.util.B2IoUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class B2 implements AutoCloseable {
    private static final String APP_NAME = "b2_4j";
    private static final String VERSION = "0.0.1";
    private static final String USER_AGENT = APP_NAME + "/" + VERSION;

    // these just don't make sense (yet), since i'm getting credentials from the environment:
    //  "    b2 authorize_account [<applicationKeyId>] [<applicationKey>]\n" +
    //  "    b2 clear_account\n" +

    // these are more work than i want to do right now:
    //  "    b2 ls [--long] [--versions] <bucketName> [<folderName>]\n" +
    //  "    b2 sync [--delete] [--keepDays N] [--skipNewer] [--replaceNewer] \\\n" +
    //  "        [--threads N] [--noProgress] <source> <destination>\n" +


    private static final String USAGE =
            "USAGE:\n" +
                    "    b2 cancel_all_unfinished_large_files <bucketName>\n" +
                    "    b2 cancel_large_file <fileId>\n" +
                    "    b2 create_bucket <bucketName> [allPublic | allPrivate]\n" +
                    "    b2 delete_bucket <bucketName>\n" +
                    "    b2 delete_file_version <fileName> <fileId>\n" +
                    "    b2 download_file_by_id [--noProgress] <fileId> <localFileName>\n" +
                    "    b2 download_file_by_name [--noProgress] <bucketName> <fileName> <localFileName>\n" +
                    "    b2 finish_uploading_large_file [--noProgress] [--threads N] <bucketName> <largeFileId> <localFileName>\n" +
                    "    b2 get_download_authorization [--noProgress] <bucketName> <fileName>\n" +
                    "    b2 get_download_file_by_id_url [--noProgress] <fileId>\n" +
                    "    b2 get_download_file_by_name_url [--noProgress] <bucketName> <fileName>\n" +
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
                    "    b2 update_bucket_cors_rules <bucketName> [rules | @rules.json]\n" +
                    "    b2 upload_file [--sha1 <sha1sum>] [--contentType <contentType>] [--info <key>=<value>]* \\\n" +
                    "        [--noProgress] [--threads N] <bucketName> <localFilePath> <b2FileName>\n" +
                    "    b2 upload_large_file [--sha1 <sha1sum>] [--contentType <contentType>] [--info <key>=<value>]* \\\n" +
                    "        [--noProgress] [--threads N] <bucketName> <localFilePath> <b2FileName>\n" +
                    "    b2 version\n";

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

    private B2() {
        out = System.out;
        client = B2StorageClientFactory.createDefaultFactory().create(USER_AGENT);
    }

    private ExecutorService getExecutor() {
        if (executor == null) {
            executor = Executors.newFixedThreadPool(numThreads, B2ExecutorUtils.createThreadFactory(APP_NAME + "-%d"));
        }
        return executor;
    }

    @Override
    public void close() {
        B2IoUtils.closeQuietly(client);

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

    public static void main(String[] args) throws B2Exception, IOException, B2JsonException {
        //out.println("args = [" + String.join(",", args) + "]");

        if (args.length == 0) {
            usageAndExit("you must specify which command you want to run.");
        }

        final String command = args[0];
        final String[] remainingArgs = Arrays.copyOfRange(args, 1, args.length);
        try (B2 b2 = new B2()) {
            if ("cancel_all_unfinished_large_files".equals(command)) {
                b2.cancel_all_unfinished_large_files(remainingArgs);
            } else if ("cancel_large_file".equals(command)) {
                b2.cancel_large_file(remainingArgs);
            } else if ("create_bucket".equals(command)) {
                b2.create_bucket(remainingArgs);
            } else if ("delete_bucket".equals(command)) {
                b2.delete_bucket(remainingArgs);
            } else if ("delete_file_version".equals(command)) {
                b2.delete_file_version(remainingArgs);
            } else if ("download_file_by_id".equals(command)) {
                b2.download_file_by_id(remainingArgs);
            } else if ("download_file_by_name".equals(command)) {
                b2.download_file_by_name(remainingArgs);
            } else if ("finish_uploading_large_file".equals(command)) {
                b2.finish_uploading_large_file(remainingArgs);
            } else if ("get_download_authorization".equals(command)) {
                b2.get_download_authorization(remainingArgs);
            } else if ("get_download_file_by_id_url".equals(command)) {
                b2.get_download_file_by_id_url(remainingArgs);
            } else if ("get_download_file_by_name_url".equals(command)) {
                b2.get_download_file_by_name_url(remainingArgs);
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
            } else if ("update_bucket_cors_rules".equals(command)) {
                b2.update_bucket_cors_rules(remainingArgs);
            } else if ("upload_file".equals(command)) {
                b2.upload_file(remainingArgs, false);
            } else if ("upload_large_file".equals(command)) {
                b2.upload_file(remainingArgs, true);
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

    @SuppressWarnings("SameParameterValue")
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
    @SuppressWarnings("SameParameterValue")
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

    /**
     * processes arguments from args[iFirstArg], through args[iLastArg], inclusive,
     * setting member variables as needed or does usageAndExit if the argument is unexpected.
     */
    @SuppressWarnings("SameParameterValue")
    private void handleCommonArgsOrDie(String[] args, int iFirstArg, int iLastArg) {
        for (int iArg = iFirstArg; iArg <= iLastArg; iArg++) {
            final String arg = args[iArg];
            if ("--noProgress".equals(arg)) {
                showProgress = false;
            } else if ("--threads".equals(arg)) {
                iArg++;
                numThreads = getPositiveIntArgOrDie(args, arg, iArg, iLastArg);
            } else {
                usageAndExit("unexpected argument '" + arg + "'");
            }
        }
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

    private B2FileVersion getUnfinishedLargeFileOrDie(String bucketId,
                                                      String largeFileId) throws B2Exception {
        for (B2FileVersion version : client.unfinishedLargeFiles(bucketId)) {
            if (version.getFileId().equals(largeFileId)) {
                return version;
            }
        }

        usageAndExit("can't find unfinished large file " + largeFileId);
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


    private void cancel_all_unfinished_large_files(String[] args) throws B2Exception {
        // <bucketName>
        checkArgCount(args, 1);
        final String bucketName = args[0];
        final B2Bucket bucket = getBucketByNameOrDie(bucketName);
        for (B2FileVersion version : client.unfinishedLargeFiles(bucket.getBucketId())) {
            out.println("  about to cancel unfinished large file: " + version);
            client.cancelLargeFile(version.getFileId());
        }
    }

    private void cancel_large_file(String[] args) throws B2Exception {
        // <largeFileId>
        checkArgCount(args, 1);
        final String largeFileId = args[0];
        client.cancelLargeFile(largeFileId);
    }

    private void create_bucket(String[] args) throws B2Exception {
        // <bucketName> <bucketType>
        checkArgCount(args, 2);
        final String bucketName = args[0];
        final String bucketType = args[1];
        client.createBucket(bucketName, bucketType);
    }

    private void update_bucket(String[] args) throws B2Exception {
        // <bucketName> <bucketType>
        checkArgCount(args, 2);
        final String bucketName = args[0];
        final String bucketType = args[1];
        final B2Bucket bucket = getBucketByNameOrDie(bucketName);
        final B2UpdateBucketRequest request = B2UpdateBucketRequest
                .builder(bucket)
                .setBucketType(bucketType)
                .build();
        client.updateBucket(request);
    }

    private void update_bucket_cors_rules(String[] args) throws B2Exception, B2JsonException {
        // <bucketName> [rules | @rules.json]
        checkArgCount(args, 2);
        final String bucketName = args[0];
        final String rulesString = args[1];
        final List<B2CorsRule> corsRules = parseRules(rulesString);
        final B2Bucket bucket = getBucketByNameOrDie(bucketName);
        final B2UpdateBucketRequest request = B2UpdateBucketRequest
                .builder(bucket)
                .setCorsRules(corsRules)
                .build();
        client.updateBucket(request);
    }

    private List<B2CorsRule> parseRules(String rulesString) throws B2JsonException {
        final B2Json b2Json = B2Json.get();
        final String rulesJson;

        if (rulesString.startsWith("@")) {
            rulesJson = readFile(rulesString.substring(1));
        } else {
            rulesJson = rulesString;
        }

        return b2Json.listFromJson(rulesJson, B2CorsRule.class);
    }

    private String readFile(String fileName) {
        try (FileInputStream in = new FileInputStream(fileName);
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            B2IoUtils.copy(in, out);
            return out.toString();
        } catch (IOException e) {
            throw new RuntimeException("trouble reading from " + fileName + ": " + e.getMessage(), e);
        }
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

    private void download_file_by_id(String[] args) throws B2Exception {
        // [--noProgress] <fileId> <localFileName>
        checkArgCount(args, 2, 3);
        final int iLastArg = args.length - 1;
        final String fileId = args[iLastArg-1];
        final String localFileName = args[iLastArg];

        handleCommonArgsOrDie(args, 0, iLastArg-2);

        final B2ContentFileWriter sink = B2ContentFileWriter
                .builder(new File(localFileName))
                .setVerifySha1ByRereadingFromDestination(true)
                .build();
        client.downloadById(fileId, sink);
    }

    private void get_download_authorization(String[] args) throws B2Exception, B2JsonException {
        // [--noProgress] <bucketId> <fileNamePrefix> <validDurationInSecs>
        checkArgCount(args, 3, 4);
        final int iLastArg = args.length - 1;
        final String bucketId = args[iLastArg-2];
        final String fileNamePrefix = args[iLastArg-1];
        final int validDurationInSecs = getPositiveIntArgOrDie(args, "validDurationInSecs", iLastArg, iLastArg);


        //handleCommonArgsOrDie(args, 0, iLastArg-1);

        B2GetDownloadAuthorizationRequest request = B2GetDownloadAuthorizationRequest
                .builder(bucketId, fileNamePrefix, validDurationInSecs)
                .setB2ContentDisposition("attachment; filename=\"helloHagi.txt\"")
                .build();
        out.println("  downloadAuth:  " + B2Json.get().toJson(client.getDownloadAuthorization(request)));
    }

    private void get_download_file_by_id_url(String[] args) throws B2Exception {
        // [--noProgress] <fileId>
        checkArgCount(args, 1, 2);
        final int iLastArg = args.length - 1;
        final String fileId = args[iLastArg];

        handleCommonArgsOrDie(args, 0, iLastArg-1);

        out.println("  url:  " + client.getDownloadByIdUrl(fileId));
    }

    private void download_file_by_name(String[] args) throws B2Exception {
        // [--noProgress] <bucketName> <fileName> <localFileName>
        checkArgCount(args, 3, 4);

        final int iLastArg = args.length - 1;
        final String bucketName = args[iLastArg-2];
        final String b2Path = args[iLastArg-1];
        final String localFileName = args[iLastArg];

        handleCommonArgsOrDie(args, 0, iLastArg-3);

        final B2ContentFileWriter sink = B2ContentFileWriter
                .builder(new File(localFileName))
                .setVerifySha1ByRereadingFromDestination(true)
                .build();
        client.downloadByName(bucketName, b2Path, sink);
    }

    private void get_download_file_by_name_url(String[] args) throws B2Exception {
        // [--noProgress] <bucketName> <fileName>
        checkArgCount(args, 2, 3);

        final int iLastArg = args.length - 1;
        final String bucketName = args[iLastArg-1];
        final String b2Path = args[iLastArg];

        handleCommonArgsOrDie(args, 0, iLastArg-2);

        out.println("  url:  " + client.getDownloadByNameUrl(bucketName, b2Path));
    }

    private void finish_uploading_large_file(String[] args) throws B2Exception {
        // [--noProgress] [--threads N] <bucketName> <largeFileId> <localPath>
        checkArgCount(args, 3, 5);
        final int iLastArg = args.length - 1;
        final String bucketName = args[iLastArg-2];
        final String largeFileId = args[iLastArg-1];
        final String localPath = args[iLastArg];

        handleCommonArgsOrDie(args, 0, iLastArg-3);

        final B2Bucket bucket = getBucketByNameOrDie(bucketName);
        final B2FileVersion largeFileVersion = getUnfinishedLargeFileOrDie(bucket.getBucketId(), largeFileId);

        final B2ContentSource source = B2FileContentSource
                .builder(new File(localPath))
                .setSha1(largeFileVersion.getContentSha1())
                .build();

        B2UploadFileRequest request = B2UploadFileRequest
                .builder(bucket.getBucketId(),
                        largeFileVersion.getFileName(),
                        largeFileVersion.getContentType(),
                        source)
                .setCustomFields(largeFileVersion.getFileInfo())
                .build();

        client.finishUploadingLargeFile(largeFileVersion, request, getExecutor());
    }


    private void get_file_info(String[] args) throws B2Exception {
        // <fileId>
        checkArgCount(args, 1);
        final String fileId = args[0];
        final B2FileVersion version = client.getFileInfo(fileId);
        out.println(version);
        out.println("  fileInfo:  " + version.getFileInfo());
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
            out.println("  fileInfo:  " + version.getFileInfo());
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

    private void upload_file(String[] args,
                             boolean forceLarge) throws B2Exception, IOException {
        B2UploadFileRequest request = makeUploadRequestFromArgs(args);

        final long contentLength = request.getContentSource().getContentLength();
        if (forceLarge || client.getFilePolicy().shouldBeLargeFile(contentLength)) {
            client.uploadLargeFile(request, getExecutor());
        } else {
            client.uploadSmallFile(request);
        }
    }

    private void version(String[] args) {
        checkArgCount(args, 0);
        out.println("b2 command line tool in java, version " + VERSION);
    }

}

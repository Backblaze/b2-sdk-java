
| Status |
| :------------ |
| [![Build Status](https://travis-ci.org/Backblaze/b2-sdk-java.svg?branch=master)](https://travis-ci.org/Backblaze/b2-sdk-java) |
| [![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://www.backblaze.com/using_b2_code.html)


INTRO
=====

b2-sdk-java is a Java sdk for B2 clients.  See the [B2 API docs][] to get some
context.

STATUS
======

The SDK is being used in production at Backblaze.  All new internal b2 client
code is being written with it.  We are in the process of slowly transitioning
our existing code to the library.  (None of our production code uses the large
file functionality yet, as those features haven't been rewritten, yet.)

We don't expect to make many incompatible changes to the API or
behavior in the near future.

FEATURES
========

* The SDK exposes B2 functionality to Java clients.

* The SDK makes it easy to follow the primary recommendations from the [integration checklist][], including:
  * Multithreading uploads:
    * Parts of large files are automatically uploaded in parallel.
    * The B2StorageClient is safe to use in multiple threads, so you can
      upload multiple files at a time.
  * automatically retries properly in response to errors.
  * adds these metadata fields on uploads:
    * X-Bz-Info-src_last_modified_millis
    * X-Bz-Info-large_file_sha1
  * provides a simple testMode setting to enable various tests.

* The SDK requires Java 8.

* The SDK provides three jars:
* **b2-sdk-core** provides almost all of the SDK.  it does not contain the code for making HTTP requests (B2WebApiClient).
* **b2-sdk-httpclient** provides an implementation of B2WebApiClient built on Apache Commons HttpClient.  It is separate so that if you provide your own B2WebApiClient, you won't need to pull in HttpClient or its dependencies.**
* **b2-sdk-samples** has some samples.

SAMPLE
======

  * If you're in a hurry, and you understand the B2 API already, you
    can probably learn everything you need to know about by looking at
    B2Sample.java.

  * To run B2Sample, you will need to add your credentials to your environment
    in these environment variables:

    * B2_ACCOUNT_ID
    * B2_APPLICATION_KEY

  * Be sure to add the jars to your class path along with their dependencies.  
    If you put all the jar files into one directory and change to the directory,
    here's a sample command line to run (after replacing 'N.N.N' with the version
    of the sdk you're using):

    java -cp b2-sdk-samples-N.N.N.jar:b2-sdk-httpclient-N.N.N.jar:b2-sdk-core-0.0.1.jar:httpclient-4.5.3.jar:httpcore-4.4.4.jar:commons-logging-1.2.jar com.backblaze.b2.sample.B2Sample



HOW TO USE
==========

  * Add the jars to your build.  In the following examples, replace N.N.N
    with the version of the sdk you're using:
    * If you're using gradle, here are the dependency lines to use the sdk:
    ```gradle
    compile 'com.backblaze.b2:b2-sdk-core:N.N.N'
    compile 'com.backblaze.b2:b2-sdk-httpclient:N.N.N'
    ```
    * If you're using maven, here are the dependency tags to use the sdk:
    ```xml
      <dependency>
        <groupId>com.backblaze.b2</groupId>
        <artifactId>b2-sdk-core</artifactId>
        <version>N.N.N</version>
        <scope>compile</scope>
      </dependency>
      <dependency>
        <groupId>com.backblaze.b2</groupId>
        <artifactId>b2-sdk-httpclient</artifactId>
        <version>N.N.N</version>
        <scope>compile</scope>
      </dependency>
    ```

  * create a B2StorageClient.

    * if your code has access to the accountId and applicationKey,
      here's the simplest way to do it:

      ```java
      B2StorageClient client = B2StorageHttpClientBuilder.builder(accountId,
                                           applicationKey,
                                           userAgent).build();
      ```

    * if you want to get the credentials from the environment,
      as B2Sample does, here's how to create your client:
      ```
      B2StorageClient client = B2StorageHttpClientBuilder.builder(userAgent).build();
      ```

  * There's a very straight-forward mapping from B2 API calls to
    methods on the B2StorageClient.

  * Each API call takes a similarly-named "request" structure which
    specifies all of the arguments to the call.  To provide
    flexibility and long-term source-code compatibility, we use the
    Builder pattern.  Builders take all required parameters
    when they are constructed and then you may set optional values.
    See the [javadocs][] for the builder classes to learn about their
    defaults.

  * For some of the simplest calls, we provide methods that just take
    the required parameters and perform the call.  These are 'default'
    methods in the B2StorageClient interface, so you can see exactly what
    they're doing.

  * Each API call returns a "response" structure with the results,
    returns an Iterable, or throws a B2Exception.

    See [Calling the API][] to learn what exceptions may happen.
    The SDK will retry retryable exceptions, so if you see them, it means
    that a reasonable number of retry attempts have already failed.

    When the SDK returns an Iterable, the SDK cannot throw B2Exception
    from iterator(), because Iterable does not allow it.  Similarly,
    Iterator does not allow hasNext() or next() to throw a B2Exception.
    In these cases, instead of throwing a B2Exception, the SDK throws
    a B2RuntimeException.

    Like any Java code, it may also throw any RuntimeException.  For instance,
    you might get an error caused by an IOException.  Similarly, you might get
    an IllegalArgumentException or IllegalStateException if you call the API
    with inappropriate arguments or at inappropriate times.  That said, we
    try to funnel 'normal' b2-related exceptions into B2Exceptions.

  * Some B2 APIs allow you to list items from an unbounded list.
    Examples include b2_list_file_versions, b2_list_file_names, and
    b2_list_unfinished_files.  B2StorageClient provides iterators which
    handle fetching batches of answers as they're needed.  This relieves your
    code from having to do the iteration "manually".

    For instance, you can use the fileVersions() iterable:

    ```java
    final B2ListFileVersionsRequest request = B2ListFileVersionsRequest
            .builder(bucketId)
            .build();
    for (B2FileVersion version : client.fileVersions(request)) {
        writer.println("fileVersion: " + version);
    }
    ```

    Similar to other methods, there is a convenience version of the
    fileVersions() which just takes a bucketId:

    ```java
    for (B2FileVersion version : client.fileVersions(bucketId)) {
      writer.println("fileVersion: " + version);
    }
    ```

  * To get upload progress notifications, implement the [B2UploadListener][]
    interface and call setListener() with an instance of your listener class
    while building your B2UploadFileRequest.

    Uploads are broken into "parts" which may be uploaded in parallel for greater
    throughput.  When uploading a "small file" (using uploadSmallFile()), there is
    only one part.  When uploading a "large file" (using uploadLargeFile() or
    finishUploadingLargeFile()) there will be more than one part and they may be
    uploaded in parallel and they may be started and completed in any order.

    B2UploadListener has one required method, progress(), which is passed
    an immutable [B2UploadProgress][] object periodically.  Each B2UploadProgress
    object says the overall state of the upload (state), how many parts the
    upload consists of (partCount), which of those parts it represents
    (partIndex, a zero-based index), the range of the overall upload that the
    part represents (startByte and length), and how many bytes have been uploaded
    so far (bytesSoFar).

    For small file uploads, many of the B2UploadProgress values are constant.
    For instance, for small files, partCount is always 1, partIndex is always 0
    and startByte is always 0.  For large file uploads, all B2UploadProgress
    objects will have the same partCount, but the other values will vary.

    The [B2UploadState][] of each part upload is one of these: WAITING_TO_START,
    STARTING, UPLOADING, FAILED, SUCCEEDED.  See the javadocs for more details.

    The B2UploadListener just receives progress() calls for parts.  It does not
    receive notifications about the overall success of the upload.  The upload
    methods on B2StorageClient are synchronous.  As with other methods in the
    API, overall success is indicated by a normal return from the method (with the
    B2FileVersion of the uploaded file) and overall failure is indicated by
    throwing an exception.

    Take a look at the example progress listener in B2Sample.java and run it to
    see an example of the notifications you'll get.

FAQ
===

  * Can I add metadata to the files I upload?  How?

    You can add some immutable name-value pairs to each file at the time you upload it.
    See the "File Info" section of the [B2 Files][] documentation for details.
    In the SDK, you can add pairs by calling setCustomField(), as seen in this
    example from B2Sample.java:

    ```java
    B2UploadFileRequest request = B2UploadFileRequest
        .builder(bucketId, fileName, B2ContentTypes.APPLICATION_OCTET, source)
        .setCustomField("color", "green")
        .build();
    ```

  * Do I have to iterate over all of the buckets in an account to find the one I'm looking for?

    For buckets, the b2_list_buckets web call always gives a list.  Since there are at
    most 100 buckets and once we've found one of them, we've got all of them, the servers
    always return the whole list to the caller, so the SDK always provides all of them to
    the Java caller.

    As Java programmers, that was driving us nuts, so we provided a simple helper.
    B2StorageClient.getBucketOrNullByName() grabs the list and does the loop for you,
    if you're looking up a bucket by name.  It really depends on context whether that's
    useful or good for you.  If you're going to be doing lots of the lookups, it'd be
    much better to get them once and cache the answer, but we didn't want the SDK to
    make assumptions about how long the cache would be valid.

  * Do I have to iterate over all of the files in a bucket to find the one I'm looking for?

    Nope.  You can add filters to the B2ListFileVersionsRequest or
    B2ListFileNamesRequest.  See the builders for those classes for details.
    Here's an example:

    ```java
    B2ListFileVersionsRequest request = B2ListFileVersionsRequest
        .builder(bucketId)
        .setStartFileName(fileName)
        .build();
    ```


  * Can I control how many answers are fetched at once for the iterables?

    Yes.  If the request structure you provide specifies a maxCount (or similar
    field), the SDK will use that in its requests to B2.  If you do not specify
    a maxCount, the SDK will default to requesting the maximum number that counts
    as "one transaction" for billing purposes.

  * Is there a way to access the payload of reads as a memory-friendly InputStream?

    Yep.  Take a look at the B2ContentSink interface.  It is passed the responseHeaders
    and an InputStream to read from.  B2ContentMemoryWriter and B2ContentFileWriter are
    just two possible implementations of B2ContentSink.  The main consideration with
    writing a B2ContentSink is handling exceptions, but that's normal right?  :)
    If there's an exception, you'll want to clean up anything you've done before
    returning, and you might get called again if the SDK retries.

    The two existing sink classes have a few nice features related to checking the
    SHA-1 after the download which you may want to mimic in your implementation.

  * What are those @B2Json annotations?

    The B2 SDK uses our B2Json library to read and write JSON.  The @B2Json
    annotations on classes provide information B2Json to tell it how to
    construct instances of classes and how to handle members.

  * Why B2Json instead of [insert your favorite Java JSON library]?

    There are many, many Java packages available for working with JSON.  See
    [[json.org]] for a list.  We've looked at many and we liked and used
    Google's GSON.  One of the main things that we struggled with in GSON
    was that it was hard to make members 'final'.  Similarly, it was hard
    to do overall checking of the values in a structure after they'd all
    been set.  Since we really like immutability and validation, we were
    sad.  We chose to make B2Json.

    B2Json requires that we make a constructor for each class that takes all
    of the parameters, so that we can assign values for final members and validate
    the values as needed.  Since Java reflection doesn't let us access the names
    of the parameters to the constructors, we have to annotate constructors to
    provide the names.  It's a price we're happy to pay for the features it
    provides.


STRUCTURE
=========

This section is mostly for developers who work on the SDK.

To simplify implementation and testing, the B2StorageClient has three main layers and a few helpers.

The top-most layer consists of the B2StorageClientImpl and the various Request and Response classes.  This layer provides the main interface for developers.  The B2StorageClientImpl is responsible for acquiring account authorizations, upload urls and upload authorizations, as needed.  It is also responsible for retrying operations that fail for retryable reasons.  The B2StorageClientImpl uses a B2Retryer to do the retrying.  An implementation of the B2RetryPolicy controls the number of retries that are attempted and the amount of waiting between attempts; the B2DefaultRetryPolicy follows our recommendations and should be suitable for almost all users.  A few operations are complicated enough that they are handled by a separate class; the most prominent example is the B2LargeFileUploader.

The middle layer, consists of the B2StorageClientWebifier.  The webifier's job is to translate logical B2 API calls (such as "list file names", or "upload a file") into the appropriate HTTP requests and to interpret the responses.  The webifier isolates the B2StorageClientImpl from having to do this mapping.  We stub the webifier layer to test B2StorageClientImpl.

The bottom layer is the B2WebApiClient.  It provides a few simple methods, such as postJsonReturnJson(), postDataReturnJson(), and getContent().  We stub B2WebApiClient to test the B2StorageClientImpl.  This layer isolates the rest of the SDK from the HTTPS implementation so that developers can provide their own web client if they want.  The b2-sdk-httpclient jar provides an implementation that uses the [Apache HttpClient][].

One of the main helpers is our B2Json class.  It uses annotations on class members
and constructors to convert between Java classes and JSON.

TESTING
=======

We have lots of unit tests to verify that each of the classes performs
as expected.  As mentioned in the "Structure" section, we stub lower
layers to test the layer above it.  The B2WebApiClient doesn't have
unit tests yet (volunteers?). We exercise the whole client, including
the B2WebApiClient during development and during the official builds
by running B2Sample against the B2 servers.

I'd also like to test more with InterruptedException.

I'd like to verify that it's possible to replace the B2WebApiClient
implementation in an environment that doesn't have the Apache
HttpClient we use.  I want to be sure we're not inadvertently pulling
in classes that won't exist in such an environment.  The first step of
this was to remove the HttpClient implementation from the core jar.
(We would be interested in an implementation that uses built-in java
classes instead of HttpClient to reduce external dependencies.)

For developers who are building on the SDK, we have a provided an
initial implementation of B2StorageClient which simulates the service.
So far, it has a minimal feature set.  Let us know if you'd like to
work on it.  (Actually, it's not in the repo yet.)


Eventual Development TO DOs
===========================

Here are some things we could do someday, in no particular order:

* implement a WebApiClient that uses java.net instead of HttpComponents and make sure
  the SDK can be used without HttpComponents.

* any good way to exercise all the exception handling in B2WebApiClient implementations?

* check public/default/private protection levels! (reduce access where possible)

* eliminate javadocs warnings from the build.  eliminate classes (and
  methods?) from the javadocs which are only public to be visible to
  other parts of the SDK.

* make notes for developers about JVM's TTL for DNS?

* provide easy support for resuming iterables.

* maybe make B2Json's method for finding JsonTypeHandlers more flexible.  (probably mostly needed for compatibility with earlier Java 6 and/or 7 which don't have java.time classes and we're not supporting those initially.)

* from [integration checklist][]
  * Parallelizing downloads of large files. (how should we handle
    the ContentHandler interface?  should we download all before
    processing any?  i'd like to discuss what's really desired.)
    (BrianB notes that the b2 CLI hasn't done this either.  I'm gonna
    note that with Range-based downloads, someone could add it on top
    of B2StorageClient.)

  * An upload queue.  This first version of the SDK is thread-safe
    and you can build whatever orchestration you want above it.

  * deleting all versions of one filename.
    that's pretty easy with the SDK, so i don't think it needs
    a built-in method.  See B2Sample.java.  

    XXX: actually it's slightly more annoying than i thought because we don't have
    a way to get only versions of a given file name.  here's my
    implementation for discussion.  an alternative would be to make
    another optional filter parameter 'fileName' on the list file apis
    to simplify the request and the loop:

    ```java
    B2ListFileVersionsRequest request = B2ListFileVersionsRequest
      .builder(bucketId)
      .setStartFileName(fileNameToDelete)
      .setPrefix(fileNameToDelete)
      .build();

    for (B2FileVersion version : client.fileVersions(request)) {
        if (version.getFileName().equals(fileNameToDelete)) {
            client.deleteFileVersion(version);
        } else {
            break;
        }
    }
    ```

  * teach B2ContentFileWriter to use a temporary file name during download
    and to not move the file until the verification is complete.  meanwhile,
    write to a .tmp file yourself and move to the real name only after success.

  * perhaps take advantage of the "Java Concurrency In Practice"-type annotations.
    So far, we haven't done this because it would add another dependency or
    require importing the code for those annotations.

Potential future features
=========================
  * Should we have some other higher-level operations in the SDK?
    My initial feeling is that we can always add those later.
    candidates so far:
      * delete all file versions with given name in a given bucket.
      * upload files from a list or queue.
      * download files from a list or queue.

  * Seriously-non-goal: detecting that an interrupted small file upload actually
      finished (upload idempotency).  (this is something that ab wants for
      one of his exercisers, but he hasn't heard anyone else even ask for it.)

Contributors
============

In addition to the team at Backblaze, the following people have contributed
questions and feedback to improve the SDK:

* Kyle Hughart

[integration checklist]: https://www.backblaze.com/b2/docs/integration_checklist.html
[B2 API Docs]: https://www.backblaze.com/b2/docs/
[Calling the API]: https://www.backblaze.com/b2/docs/calling.html
[Apache HttpClient]: https://hc.apache.org/httpcomponents-client-ga/
[B2 Files]: https://www.backblaze.com/b2/docs/files.html
[javadocs]: https://backblaze.github.io/b2-sdk-java/
[B2UploadListener]: https://backblaze.github.io/b2-sdk-java/com/backblaze/b2/client/structures/B2UploadListener.html
[B2UploadProgress]: https://backblaze.github.io/b2-sdk-java/com/backblaze/b2/client/structures/B2UploadProgress.html
[B2UploadState]: https://backblaze.github.io/b2-sdk-java/com/backblaze/b2/client/structures/B2UploadState.html
[json.org]: http://www.json.org/

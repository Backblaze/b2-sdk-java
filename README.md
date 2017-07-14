
| Status |
| :------------ |
| [![Build Status](https://travis-ci.com/Backblaze/b2sdk4j.svg?token=ahms42Sqt2n6jisLX78s&branch=master)](https://travis-ci.com/Backblaze/b2sdk4j) |
| [![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://www.backblaze.com/using_b2_code.html)


INTRO
=====

b2sdk4j is a Java sdk for B2 clients.  See the [B2 API docs][] to get some
context.

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

SAMPLE
======

  * If you're in a hurry, and you understand the B2 API already, you
    can probably learn everything you need to know about by looking at
    B2Sample.java.

  * Be sure to add b2sdk4j-N.N.N.jar to your class path.

HOW TO USE
==========

  * Add the b2sdk4j-N.N.N.jar to your class path (XXX: provide snippets for
    maven and other build systems)

  * create a B2StorageClient. here's the simplest way to do it:

        B2StorageClient client = B2StorageClient.builder(accountId,
                                           applicationKey,
                                           userAgent).build();

  * There's a very straight-forward mapping from B2 API calls to
    methods on the B2StorageClient.

  * Each API call takes a similarly-named "request" structure which
    specifies all of the arguments to the call.  To provide
    flexibility and long-term source-code compatibility, we use the
    Builder pattern.  Builders take all required parameters
    when they are constructed and then you may set optional values.
    See the javadocs for the builder classes to learn about their
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
    from iterator(), becaues Iterable does not allow it.  Similarly,
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
    b2_list_unfinshed_files.  B2StorageClient provides iterators which
    handle fetching batches of answers as they're needed.  This relieves your
    code from having to do the iteration "manually".

    For instance, you can use the fileVersions() iterable:

        final B2ListFileVersionsRequest request = B2ListFileVersionsRequest
            .builder(bucketId)
            .build();
        for (B2FileVersion version : client.fileVersions(request)) {
            writer.println("fileVersion: " + version);
        }

    Similar to other methods, there is a convenience version of the
    fileVersions() which just takes a bucketId:

        for (B2FileVersion version : client.fileVersions(bucketId)) {
          writer.println("fileVersion: " + version);
        }

FAQ
===

XXX: what to put here?  let's wait and see what's actually asked.  :)
XXX: probably some common errors people see.  
XXX: probably yet another reminder to add the jar to the path?
XXX: maybe "what are those @B2Json annotations?" and/or "why B2Json instead of <my favorite JSON mechanism>?"

  * Can I control how many answers are fetched at once for the iterables?
    Yes.  If the request structure you provide specifies a maxCount (or similar
    field), the SDK will use that in its requests to B2.  If you do not specify
    a maxCount, the SDK will default to requesting the maximum number that counts
    as "one transaction" for billing purposes.


STRUCTURE
=========

This section is mostly for developers who work on the SDK.

To simplify implementation and testing, the B2StorageClient has three main layers and a few helpers.

The top-most layer consists of the B2StorageClientImpl and the various Request and Response classes.  This layer provides the main interface for developers.  The B2StorageClientImpl is responsible for acquiring account authorizations, upload urls and upload authorizations, as needed.  It is also responsible for retrying operations that fail for retryable reasons.  The B2StorageClientImpl uses a BackoffRetrier to do the retrying.  A few operations are complicated enough that they are handled by a separate class; the most prominent example is the LargeFileUploader.

The middle layer, consists of the B2StorageClientWebifier.  The webifier's job is to translate logical B2 API calls (such as list file names, or upload a file) into the appropriate HTTP requests and to interpret the responses.  The webifier isolates the B2StorageClientImpl from doing this mapping.  We stub the webifier layer to test B2StorageClientImpl.

The bottom layer is the WebApiClient.  It provides a few simple methods, such as postJsonReturnJson(), postDataReturnJson(), and getContent().  We stub WebApiClient to test the B2StorageClientImpl.  This layer isolates the rest of the SDK from the HTTPS implementation so that developers can provide their own web client if they want.  b2sdk4j's default implementation uses the [Apache HttpClient][].

One of the main helpers is our B2Json class.  It uses annotations on class members
and constructors to convert between Java classes and JSON.  (We can discuss why we
use it instead of Gson or other alternatives later, if we want.)

TESTING
=======

We have lots of unit tests to verify that each of the classes performs as expected.  As mentioned in the "Structure" section, we stub lower layers to test the layer above it.  (XXX: I'm not yet sure how to fully unit test the WebApiClientImpl.  Perhaps I'll make a standalone program that either exercises that alone, or perhaps the whole API, against the production server. BrianB says that the CLI looks for a config file with the accountId/appKey and, if it's present, does tests against the live service.)

I'd also like to test with InterruptedException.

I'd like to verify that it's possible to replace the WebApiClient implementation in an environment that doesn't have the Apache HttpClient we use.  I want to be sure we're not inadvertently pulling in classes that won't exist in such an environment.

For developers who are building on the SDK, we have a provided an initial implementation of B2StorageClient which simulates the service.  So far, it has a minimal feature set.  Let us know if you'd like to work on it.  (Actually, it's not in the repo yet.)



Development TO DOs
==================
* remove B2StorageClient.uploadFile().
  to do that, i need to provide info or a decision procedure for determining whether to use
  uploadSmallFile or uploadLargeFile.  there's a max size for small files and minimum size
  for large files.
* update javadocs
* any good way to exercise all the exception handling, esp in the WebApiClientImpl?
* implement a WebApiClient that uses java.net instead of HttpComponents and make sure
  the SDK can be used without HttpComponents.  to do this, split the SDK into
  'b2sdk4j-core', 'b2sdk4j-httpclient'.  move the existing builder to 'b2sdk4j-httpclient'.
  see if there's something common and useful that can be shared in b2sdk4j-core.
* add parameters for sockets?
* check public/default/private protection levels! (maybe put all files in one pkg to tighten protections?)
* continue converting B2 service code to use new sdk.
* make notes for developers about JVM's TTL for DNS?
* etc, etc.

Packaging TO DOs
================
* improve javadocs
* when we're ready, make the b2sdk4j repository public & switch the build to travis-ci.org

Eventual Development TO DOs
===========================

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


[integration checklist]: https://www.backblaze.com/b2/docs/integration_checklist.html
[B2 API Docs]: https://www.backblaze.com/b2/docs/
[Calling the API]: https://www.backblaze.com/b2/docs/calling.html
[Apache HttpClient]: https://hc.apache.org/httpcomponents-client-ga/

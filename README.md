
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

As a whole, b2-sdk-java is currently **ALPHA** software.

The core library is being used internally at Backblaze for new Java B2
client code.  For production code, we use a different implementation
of B2WebApiClient to match the rest of our system.  We are in the
process of slowly transitioning our existing code to the library.

We don't currently expect to make very many incompatible changes to the API or behavior.

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

    java -cp b2-sdk-samples-N.N.N.jar:b2-sdk-httpclient-N.N.N.jar:b2-sdk-core-0.0.1.jar:httpclient-4.5.3.jar:httpcore-4.4.4.jar:commons-logging-1.2.jar  com.backblaze.b2.sample.B2Sample



HOW TO USE
==========

  * Add the jars to your class path (XXX: provide snippets for
    maven and other build systems)

  * create a B2StorageClient.

    * if your code has access to the accountId and appliationKey,
      here's the simplest way to do it:

        B2StorageClient client = B2StorageHttpClientBuilder.builder(accountId,
                                           applicationKey,
                                           userAgent).build();

    * if you want to get the credentials from the environment,
      as B2Sample does, here's how to create your client:

      B2StorageClient client = B2StorageHttpClientBuilder.builder(userAgent).build();


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

The bottom layer is the B2WebApiClient.  It provides a few simple methods, such as postJsonReturnJson(), postDataReturnJson(), and getContent().  We stub B2WebApiClient to test the B2StorageClientImpl.  This layer isolates the rest of the SDK from the HTTPS implementation so that developers can provide their own web client if they want.  The b2-sdk-httpclient jar provides an implementation that uses the [Apache HttpClient][].

One of the main helpers is our B2Json class.  It uses annotations on class members
and constructors to convert between Java classes and JSON.  (We can discuss why we
use it instead of Gson or other alternatives later, if we want.)

TESTING
=======

We have lots of unit tests to verify that each of the classes performs as expected.  As mentioned in the "Structure" section, we stub lower layers to test the layer above it.  (XXX: I'm not yet sure how to fully unit test the B2WebApiHttpClientImpl.  Perhaps I'll make a standalone program that either exercises that alone, or perhaps the whole API, against the production server. BrianB says that the CLI looks for a config file with the accountId/appKey and, if it's present, does tests against the live service.)

I'd also like to test with InterruptedException.

I'd like to verify that it's possible to replace the B2WebApiClient implementation in an environment that doesn't have the Apache HttpClient we use.  I want to be sure we're not inadvertently pulling in classes that won't exist in such an environment.  The first step of this was to remove the HttpClient implementation from the core jar.

For developers who are building on the SDK, we have a provided an initial implementation of B2StorageClient which simulates the service.  So far, it has a minimal feature set.  Let us know if you'd like to work on it.  (Actually, it's not in the repo yet.)


Eventual Development TO DOs
===========================

Here are some things we could do someday, in no particular order:

* implement a WebApiClient that uses java.net instead of HttpComponents and make sure
  the SDK can be used without HttpComponents.

* add a progress listener to upload calls so clients can track progress. (maybe downloads too.)
  possibly as a wrapper on the contentSource, to track the bytes being pulled.
  that's not 100% accurate, but it should be a lot simpler than hooking into the B2WebApiClient.

* any good way to exercise all the exception handling in B2WebApiClient implementations?

* add parameters for sockets?

* check public/default/private protection levels!

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

  * teach B2ContentFileWriter to use a temporary file name during download
    and to not move the file until the verification is complete.  meanwhile,
    write to a .tmp file yourself and move to the real name only after success.

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

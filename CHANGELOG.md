# Changelog

## [Unreleased]
### Changed `[Incompatible]`
* B2Bucket and B2ApplicationKey both have an `options` parameter in the constructor.  These are optional.
* `B2StorageClient.DownloadByName` supports downloading files with looser naming requirements, e.g. names containing double slashes (`//`).
* Updated to version `4.5.9` of `org.apache.httpcomponents:httpclient`. 
## [3.1.0] - 2019-05-16
### Added
* Added `@B2Json.sensitive` annotation to redact fields when B2Json is
  used with the new redactSensitive option set to true.
* Added `B2ContentSource.createContentSourceWithRangeOrNull()` which
  is used by the large file uploader to give the content source the
  opportunity to make a more efficient content source for a range of
  the file. this is really useful if the content isn't coming from
  memory or a local file because the default implementation reads
  and discards data to get to the desired range. Added sample
  UploadLargeFileFromUrl and UrlContentSource.
* Added support for using okhttp3 as the HTTP client instead of
  Apache's HttpClient. there are limitations with this
  implementation for uploading files that are about 2GB or
  larger. See README.md for details.
* Added B2StorageClientFactory interface for creating
  B2StorageClients without having to select a particular
  implementation at build time. The static `createDefaultFactory()`
  method can be used to create a B2StorageClient using either of the
  two official web client implementations, whichever is in the
  classpath at runtime. The sample programs use this now so they
  can be used with either web client implementation.

### Fixed
* Don't use default character set in B2Json; explicitly use UTF8.

## [3.0.0] - 2019-05-01
### Added 
* Renaming the application key terminology to the new convention
* Being able to call `b2FromJsonOrThrow()` to check if you can parse
  a JSON String into a given object

## [2.0.0] - 2018-10-17
### Changed `[Incompatible]`
* The SDK now uses version 2 of the B2 APIs, which deals with restricted application keys differently
* B2Json type handler interface takes B2JsonOptions as a parameter, and not an int bitmask

### Added
* B2Json support for versioned structures

### Fixed
* Remembering account ID after authorizing with application key
* B2Json now serialized unions correctly

## [1.4.0] - 2018-05-23
### Added
* Add BigInteger support to B2Json.

### Fixed
* Percent decoding of file name returned from `getFileInfoByName()`

## [1.3.0] - 2018-05-04
### Added
* Add `deleteAllFilesInBucket()` and `getFileInfoByName()`. (thanks, valenpo!)
* Add support for upcoming ApplicationKeys apis. (thanks, valenpo!)
* Expose `startLargeFile()` and `finishLargeFile()`
* Add support for "union types" in B2Json.
* Add default codes for exception classes so `B2Exception.create()` can be used for HEAD.

### Fixed
* Explicitly set user-agent on HttpClient to keep HttpClient from doing a bunch of unneeded work.
* Improve use of clocks in unit tests to make the tests less order dependent.
* Make search for "file info" headers in B2Headers check header names case-insensitively!

## [1.2.0] - 2017-11-30
### Added
* Adds support setting CorsRules on buckets.
* B2StorageClient now exposes the following methods.
  You should read the comments about why you probably
  want to let the SDK handle things for you instead
  of calling them. ;)
  - `getAccountAuthorization()`
  - `getUploadUrl()`
  - `getUploadPartUrl()`

### Fixed
* Speeds up writing json (by doing custom UTF8 encoding instead of using the generic java versions).
  (and a bug fix which changed the behavior of B2Json.toJson(...outputStream) to not close the stream.)
* While uploading a large file, the caller provides a ExecutorService.
  if that service rejects a task we submit, we throw a
  B2LocalException instead of letting the RejectedExecutionException
  (a RuntimeException) percolate up.

## [1.1.1] - 2017-11-01
### Added
* B2FileVersion now has string constants for "actions"
* Download request objects now have an optional 'b2ContentDisposition' field.
* B2StorageClient now has `getDownloadByIdUrl()` and `getDownloadByNameUrl()`
* B2 command-line program now has an option to get_download_authorization
* List buckets now has an optional 'bucketTypes' field.

### Fixed
* Percent encode file names when downloading by name
* Large file uploader now uses RetryPolicySupplier in all cases, instead of just part of the code.

## [1.0.0] - 2017-09-08
### Added
* We have been using the SDK in production for a while at Backblaze and we're feature complete (for now), so we're going 1.0!
  
  We hope you enjoy it.
  
  Please let us know how it works for you. :)
  
  thanks,
  ab

## [0.0.6] - 2017-08-17
### Added
* These changes were driven by switching our internal b2-sdk uses to use the http client from the sdk instead of a 
different, custom interface.

[Unreleased]: https://github.com/Backblaze/b2-sdk-java/compare/v3.1.0...HEAD
[3.1.0]: https://github.com/Backblaze/b2-sdk-java/releases/tag/v3.1.0
[3.0.0]: https://github.com/Backblaze/b2-sdk-java/releases/tag/v3.0.0
[2.0.0]: https://github.com/Backblaze/b2-sdk-java/releases/tag/v2.0.0
[1.4.0]: https://github.com/Backblaze/b2-sdk-java/releases/tag/v1.4.0 
[1.3.0]: https://github.com/Backblaze/b2-sdk-java/releases/tag/v1.3.0 
[1.2.0]: https://github.com/Backblaze/b2-sdk-java/releases/tag/v1.2.0
[1.1.1]: https://github.com/Backblaze/b2-sdk-java/releases/tag/v1.1.1
[1.0.0]: https://github.com/Backblaze/b2-sdk-java/releases/tag/v1.0.0
[0.0.6]: https://github.com/Backblaze/b2-sdk-java/releases/tag/v-0.0.6-alpha

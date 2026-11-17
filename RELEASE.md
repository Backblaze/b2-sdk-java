# Releasing the B2 SDK for Java

A release is a reviewed version commit on `master`, an annotated, signed `vX.Y.Z` tag on that
commit, and the same version published to Maven Central. This process follows the release-prep
approach in [Blazer's release guide](https://github.com/Backblaze/blazer/blob/main/RELEASE.md).

## Prerequisites

- JDK 11 and the checked-in Gradle wrapper; the build also runs the JDK 17 test module.
- Python 3, Git, GitHub CLI (`gh auth status`), GnuPG or OpenSSH for tag signing.
- Changie v1.24.0, a standalone Go binary. Install with
  `go install github.com/miniscruff/changie@v1.24.0` using Go 1.24 or newer, then put
  `$(go env GOPATH)/bin` on `PATH`. This does not change the Java toolchain.
- Maven Central publishing credentials and artifact-signing secrets remain configured for
  `.github/workflows/deploy.yml`: `SONATYPE_TOKEN_USERNAME`, `SONATYPE_TOKEN_PASSWORD`,
  `GPG_SIGNING_KEY`, and `GPG_PASSPHRASE`. Tag signing and Maven artifact signing are separate.

Do not run a publishing task locally as a verification step. A tag push starts the production
deployment, including closing and releasing the Sonatype staging repository.

## Repository setup and signed-tag rollout

The intended release maintainers are `goanpeca`, `ffumero2003`, and `sophiecarreras`, listed in
`.github/signing-maintainers`. The verifier resolves their public keys from
`https://github.com/<username>.gpg`, `https://github.com/<username>.keys`, and the paginated public
`users/<username>/ssh_signing_keys` API at check time and runs
`git verify-tag` with an isolated keyring and SSH allowed-signers file. GitHub profile key changes
therefore affect the next verification; review the username list through a PR.

Tag verification is **warn-only by default**. Before enabling it, each intended signer must create a
GPG or SSH signing key and register the public key on their GitHub account. Confirm that their signed
tag passes verification, then run `gh variable set ENFORCE_SIGNED_TAGS --body true` for this repository.
Once enabled, an
unsigned, lightweight, unrecognized, or unverifiable tag blocks publication. In warn-only mode those
conditions produce a workflow warning; the security, milestone, and metadata gates still enforce.

An administrator must complete the following before the workflow changes are used for a release:

- Supply `RELEASE_READ_TOKEN`, a read-only token authorized for this repository's code-scanning,
  Dependabot, secret-scanning alerts, repository security advisories (including drafts), and issues.
  Enable these sources and grant access: a disabled or unreadable source holds publication.
  The ordinary workflow token is used only for milestone reads and release creation, with explicit
  job permissions; Maven signing/publishing secrets are exposed only to the publish step.
- Change Settings → Pages → Build and deployment → Source from **Deploy from a branch** to
  **GitHub Actions**. Configure the `github-pages` environment to allow the documentation branches
  `master`, `api-v2`, and `7.x`. The official Pages deployment uses GitHub OIDC with `pages: write`
  and `id-token: write`; this does not change Maven Central authentication.
- Make `changelog-check` a required PR check and protect `.github/signing-maintainers` and the
  release workflows through maintainer review. Keep Dependabot's GitHub Actions updates enabled.

## Changelog and versioning

Use semantic versioning: fixes use a patch release, compatible features a minor release, and
incompatible changes a major release. Choose the version explicitly; a `Changed` entry alone
does not determine whether the release is compatible.

Each PR adds a fragment rather than editing `CHANGELOG.md`:

```bash
changie new --kind Fixed --body 'Describe the observable change.'
python3 scripts/release.py changelog-check
python3 -m unittest discover -s scripts/tests -v
```

Kinds are Added, Changed, Deprecated, Removed, Fixed, and Security. Keep unpublished security
details out of public fragments. Existing release history is preserved in `.changes/v*.md`;
unreleased changes live in `.changes/unreleased/*.yaml`. `CHANGELOG.md` contains the generated
released history. `changie merge` also sets `version=` in `gradle.properties` from the latest
version notes. Do not edit the generated changelog or bump the Gradle version independently.
The `changelog-check` CI job verifies generated output, version consistency, and a fragment for
each PR (or a complete batch and version update for a release-prep PR).
Dependabot PRs that only modify existing workflow YAML files do not need a release fragment;
generated-output checks and the release-tooling tests still run. Source/dependency changes outside
workflow files and human-authored changes still need a fragment.

## Milestones and release preparation

1. Select the version and create its milestone before preparing the release. Assign every
   intended issue and PR to it. Publicly track public work only; unpublished advisories remain
   in GitHub Security Advisories. Deferred work moves to the next milestone with a written reason.

   ```bash
   export VERSION=6.5.1              # choose the actual next version
   export TAG="v$VERSION"
   gh api --method POST repos/Backblaze/b2-sdk-java/milestones -f title="$TAG"
   gh issue edit <issue-number> --milestone "$TAG"
   gh pr edit <pr-number> --milestone "$TAG"
   git fetch origin master --tags
   git switch -c "release/$TAG" origin/master
   ```

2. Confirm the release scope has merged and the relevant CI checks are green. Check the
   repository Security tab: an open medium, high, or critical finding holds the release,
   including findings without a patched dependency. Failures to read findings are unverified,
   not clean. A maintainer must resolve or dismiss findings with a written reason before release.

   ```bash
   GH_TOKEN="$RELEASE_READ_TOKEN" python3 scripts/release.py security
   # After the release-prep PR has merged, the milestone must contain no open issues or PRs:
   python3 scripts/release.py milestone "$TAG"
   ```

   The gate reads all pages of GitHub code-scanning alerts on `master`, Dependabot alerts,
   draft/triage repository advisories, secret-scanning alerts, and severity-labeled open issues.
   Medium/high/critical findings block even without a patched version; all open secret alerts
   and unscored active advisories block. Low findings do not block. The gate prints counts,
   never unpublished advisory text or secret values. Do not put private advisory details in
   a public issue. API failures are not a waiver; restore access and rerun the gate.

3. Batch the changelog and update the version in one operation:

   ```bash
   changie batch "$TAG" --dry-run --allow-no-changes=false
   changie batch "$TAG" --allow-no-changes=false
   changie merge
   python3 scripts/release.py changelog-check
   python3 scripts/release.py notes "$TAG" > /tmp/b2-java-release-notes.md
   git diff -- gradle.properties CHANGELOG.md .changes/
   ```

   Review the generated `.changes/$TAG.md` and regenerate with `changie merge` after any edits.
   A release-prep PR changes only `.changes/`, `CHANGELOG.md`, and `gradle.properties`. Keep
   workflow or SDK behavior changes in separate PRs.

4. Verify locally without publishing:

   ```bash
   python3 -m unittest discover -s scripts/tests -v
   ./gradlew build --no-daemon
   RELEASE_BUILD=true ./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository \
     createBundle --dry-run --no-daemon \
     -PsonatypeUsername=dry-run -PsonatypePassword=dry-run
   ```

   The placeholder credentials are only for task-graph validation with `--dry-run`; this does
   not contact Sonatype, sign artifacts, or prove that production secrets are usable.
   Run the local health check below against a fresh loopback simulator as well.

5. Commit and open a release-prep PR linked to its issue, assign its milestone, and request a
   maintainer review. Include the release notes, build, health-check, and dry-run results.
   A human merges it after required CI and review pass. Do not tag an unmerged PR.

## Tag and deploy

Tag the merged release-prep commit, which may differ from the latest tip of `master`:

```bash
(
  set -euo pipefail
  : "${TAG:?export the release tag, e.g. v6.5.1}"
  VERSION="${TAG#v}"
  git fetch origin master --tags
  SHA=$(gh pr view <release-pr-number> --json mergeCommit --jq .mergeCommit.oid)
  git merge-base --is-ancestor "$SHA" origin/master
  git show "$SHA:gradle.properties" | grep -Fx "version=$VERSION"
  git log -1 --oneline "$SHA"

  # GPG: use your full signing-key fingerprint.
  git -c gpg.format=openpgp tag -s -u <fingerprint> "$TAG" "$SHA" -m "$TAG"
  if ! git verify-tag "$TAG"; then
    git tag --delete "$TAG"
    exit 1
  fi

  # Alternatively, with a registered SSH signing key:
  # git -c gpg.format=ssh -c user.signingkey="$HOME/.ssh/id_ed25519.pub" \
  #   tag -s "$TAG" "$SHA" -m "$TAG"
  # if ! git -c gpg.format=ssh -c gpg.ssh.allowedSignersFile=<allowed-signers-file> verify-tag "$TAG"; then
  #   git tag --delete "$TAG"
  #   exit 1
  # fi

  git push origin "$TAG"
)
```

The tag-triggered deploy workflow publishes using the existing Gradle command:

```bash
./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository createBundle --no-daemon --stacktrace
```

Maven Central, GPG artifact signing, the Sonatype staging endpoint, and the release/close tasks
are unchanged by the changelog tooling. Never run this production command as a dry-run substitute.

Before publishing, `release-checks` reads its gate implementation and maintainer list from `master`,
checks tag ancestry and version/changelog agreement, verifies the tag in the configured mode, and
requires security clearance and a complete `vX.Y.Z` milestone. `deploy` depends on those checks and
runs the Gradle build before publishing. After successful publication it runs `gh release create`
with the exact `.changes/vX.Y.Z.md` section as `--notes-file` and the three existing module bundles.
No default or automatically generated release body is used.

## Verify the published release

1. Inspect the deployment run for the tag and confirm the Sonatype close/release step succeeded:
   `gh run list --workflow deploy.yml --branch "$TAG"` and `gh run view <run-id> --log-failed`.
2. Confirm the GitHub Release contains this version's changelog section and all three module bundles:
   `gh release view "$TAG" --json tagName,body,assets`.

   Release standardization rolls out in stages: changelog tooling and this guide first,
   then automated release notes. For releases made before notes automation is enabled,
   the deployment workflow attaches the three bundles without setting a changelog-derived
   body. From a checkout of the tagged commit, update those releases manually before
   verifying them. Once enabled, automation supplies the notes body.

   ```bash
   (
     set -euo pipefail
     : "${TAG:?export the release tag, e.g. v6.5.1}"
     NOTES_FILE=$(mktemp)
     trap 'rm -f "$NOTES_FILE"' EXIT
     python3 scripts/release.py notes "$TAG" > "$NOTES_FILE"
     gh release edit "$TAG" --notes-file "$NOTES_FILE"
     gh release view "$TAG" --json tagName,body,assets
   )
   ```

3. After Maven Central indexing completes, check each module and its signature:

   TODO (release owner): approve and record the public-key retrieval URL or file, the full
   uppercase primary-key fingerprint, and the out-of-band fingerprint-confirmation channel
   and procedure here. No Maven artifact-signing trust source is approved in this repository
   yet. A key found in a historical Maven signature or on a keyserver is only a candidate.

   Artifact verification remains blocked until the release owner completes that decision.
   Once confirmed, obtain and import the key from the approved location, follow the recorded
   independent confirmation procedure, and export the approved primary-key fingerprint as
   `MAVEN_SIGNING_FINGERPRINT`. There is no default; do not derive the configured value from
   the artifacts or signatures being verified.

   ```bash
   (
     set -euo pipefail
     : "${VERSION:?export the released version, e.g. 6.5.1}"
     : "${MAVEN_SIGNING_FINGERPRINT:?export the independently confirmed full signing fingerprint}"
     for module in core httpclient samples core-test-jdk17; do
       for suffix in .pom .jar -sources.jar -javadoc.jar; do
         artifact="b2-sdk-$module-$VERSION$suffix"
         url="https://repo.maven.apache.org/maven2/com/backblaze/b2/b2-sdk-$module/$VERSION/$artifact"
         curl --fail --show-error --location "$url" -o "/tmp/$artifact"
         curl --fail --show-error --location "$url.asc" -o "/tmp/$artifact.asc"
         signature_status=$(gpg --batch --status-fd=1 --verify "/tmp/$artifact.asc" "/tmp/$artifact")
         printf '%s\n' "$signature_status" | awk -v expected="$MAVEN_SIGNING_FINGERPRINT" '
           $1 == "[GNUPG:]" && $2 == "VALIDSIG" && $12 == expected { found = 1 }
           END { exit !found }
         ' || { printf '%s\n' 'Maven signature does not match the expected fingerprint' >&2; exit 1; }
       done
     done
   )
   ```

   The shared Gradle publication plugin also publishes `b2-sdk-core-test-jdk17`, so this
   loop verifies the POM, main JAR, sources JAR, and Javadoc JAR for all four published
   modules, including that test module, against the configured signing fingerprint.
   Verify using the established Maven artifact-signing public key, independently of tag keys.
4. Confirm there are no open issues/PRs left in the version milestone, then close it. Publish any
   advisories coordinated with the release only after the fixed artifacts are available.

## Local health check

Use a local B2 simulator with the fixed test credentials and an empty `sdkharness-healthcheck`
bucket. This check authorizes, uploads, downloads, compares bytes, lists, and deletes; it refuses
non-loopback endpoints. It must print `SDKHARNESS_RESULT` with `health`, `golden-path`, and `PASS`.
Set `SDKHARNESS_SIMULATOR_URL` to the running simulator's IPv4 loopback URL, then run:

```bash
: "${SDKHARNESS_SIMULATOR_URL:?set the local simulator URL, e.g. http://127.0.0.1:8180}"
./gradlew build --no-daemon
CP=$(./gradlew -q -I scripts/health-classpath.gradle :b2-sdk-httpclient:healthClasspath)
SDKHARNESS_SIMULATOR_URL="$SDKHARNESS_SIMULATOR_URL" \
  SDKHARNESS_TEST_LEVEL=health SDKHARNESS_SCENARIO=golden-path \
  B2_BUCKET_NAME=sdkharness-healthcheck SDKHARNESS_JAVA_CLASSPATH="$CP" \
  .sdkharness/tests/run-health
```

## Recovery

Never move a published tag or reuse a Maven Central version. If a release fails, inspect staging
and Central before retrying: the existing deploy command closes and releases staging repositories.
Do not blindly rerun a partially completed publish. If artifacts are already published, ship a new
patch version. Reverting SDK code does not remove artifacts already used by consumers.

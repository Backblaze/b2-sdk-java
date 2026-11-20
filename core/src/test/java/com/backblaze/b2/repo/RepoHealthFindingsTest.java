/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 *
 * Repository-health checks for known defects (C42, C51-C56).
 * License https://www.backblaze.com/using_b2_code.html
 *
 * These tests read files of the repository (CHANGELOG.md, README.md, SECURITY.md
 * and the sample source), not Java behaviour.  Gradle runs tests with the module directory
 * as working directory, so the repository root is the parent directory.
 *
 * Outside a full checkout (no settings.gradle.kts in the parent directory) the tests are skipped.
 *
 * A test that fails today carries @Category(KnownDefect.class) and runs only in the knownDefectTests task.
 * C50 (a claim about missing tests) has no natural check and is covered by the C4, C6 and C7 tests.
 */
package com.backblaze.b2.repo;

import com.backblaze.b2.testing.KnownDefect;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class RepoHealthFindingsTest {
    private static File root() {
        final File root = new File("..").getAbsoluteFile();
        // skip (not fail) when the module is not built from a full checkout, for example from a source archive
        assumeTrue("not run from a full repository checkout (no settings.gradle.kts at " + root + ")",
                new File(root, "settings.gradle.kts").isFile());
        return root;
    }

    private static String read(String relativePath) throws IOException {
        final File f = new File(root(), relativePath);
        assumeTrue("file not present in this checkout: " + relativePath, f.isFile());
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    private static List<String> lines(String relativePath) throws IOException {
        final List<String> result = new ArrayList<>();
        for (String line : read(relativePath).split("\n", -1)) {
            result.add(line);
        }
        return result;
    }

    private static String[] workflows() {
        final File dir = new File(root(), ".github/workflows");
        final String[] names = dir.list((d, n) -> n.endsWith(".yml") || n.endsWith(".yaml"));
        assumeTrue("no workflow files in this checkout", names != null && names.length > 0);
        return names;
    }

    /** Finding C42: the --noProgress option sets showProgress, but nothing ever reads it. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC42_noProgressOptionIsUsed() throws IOException {
        final String source = read("samples/src/main/java/com/backblaze/b2/sample/B2.java");
        final Matcher uses = Pattern.compile("showProgress(?!\\s*=[^=])").matcher(source);
        int reads = 0;
        while (uses.find()) {
            // the declaration "private boolean showProgress = true;" is also an assignment, so it is skipped above
            reads++;
        }
        assertTrue("showProgress is assigned but never read, so --noProgress has no effect", reads > 0);
    }

    /** Finding C51: every released version heading in the changelog has a link target, and Unreleased compares from the latest release. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC51_changelogHeadingsHaveLinkTargets() throws IOException {
        final String text = read("CHANGELOG.md");
        final Matcher heading = Pattern.compile("(?m)^## \\[([^\\]]+)\\]").matcher(text);
        final List<String> missing = new ArrayList<>();
        String latestRelease = null;
        while (heading.find()) {
            final String name = heading.group(1);
            if (!Pattern.compile("(?m)^\\[" + Pattern.quote(name) + "\\]:\\s*\\S+").matcher(text).find()) {
                missing.add(name);
            }
            if (latestRelease == null && !name.equalsIgnoreCase("Unreleased")) {
                latestRelease = name;
            }
        }
        assertTrue("changelog headings without a link target: " + missing, missing.isEmpty());
        assertTrue("the [Unreleased] link should compare from v" + latestRelease,
                Pattern.compile("(?m)^\\[Unreleased\\]:.*v" + Pattern.quote(String.valueOf(latestRelease)) + "\\.\\.\\.").matcher(text).find());
    }

    /** Finding C52: the release workflow must not use the deprecated ::set-output command. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC52_releaseWorkflowAvoidsDeprecatedSetOutput() throws IOException {
        for (String name : workflows()) {
            assertFalse(name + " uses the deprecated ::set-output command; write to $GITHUB_OUTPUT",
                    read(".github/workflows/" + name).contains("::set-output"));
        }
    }

    /** Finding C53: a fix must not be listed under an "Added" heading (6.3.0 lists "Fixed ..." under Added). */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC53_fixesAreNotListedUnderAdded() throws IOException {
        String heading = "";
        final List<String> misplaced = new ArrayList<>();
        for (String line : lines("CHANGELOG.md")) {
            if (line.startsWith("###")) {
                heading = line.trim();
            } else if (line.startsWith("## ")) {
                heading = "";
            } else if (heading.equals("### Added") && line.matches("^[*-]\\s+Fixed\\b.*")) {
                misplaced.add(line.trim());
            }
        }
        assertTrue("entries starting with 'Fixed' under '### Added': " + misplaced, misplaced.isEmpty());
    }

    /** Finding C54: the README must not point at a Travis badge, the removed 'compile' Gradle keyword, or the placeholder core jar version. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC54_readmeIsNotStale() throws IOException {
        final String readme = read("README.md");
        assertFalse("README still shows a Travis build badge although CI is GitHub Actions", readme.contains("travis-ci"));
        assertFalse("README still uses the removed 'compile' Gradle configuration", readme.contains("compile 'com.backblaze.b2"));
        assertFalse("README classpath example names b2-sdk-core-0.0.1.jar", readme.contains("b2-sdk-core-0.0.1.jar"));
    }

    /** Finding C55: the library is Apache HttpComponents HttpClient, not "Apache Commons HttpClient". */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC55_readmeNamesTheRightHttpLibrary() throws IOException {
        assertFalse("README says 'Apache Commons HttpClient'", read("README.md").contains("Apache Commons HttpClient"));
    }

    /** Finding C56: the repository has a security policy (SECURITY.md at the root, in .github or in docs). */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC56_securityPolicyExists() {
        final File root = root();
        assertTrue("no SECURITY.md at the root, in .github/ or in docs/ (an organisation-wide default may exist outside this repository)",
                new File(root, "SECURITY.md").isFile()
                        || new File(root, ".github/SECURITY.md").isFile()
                        || new File(root, "docs/SECURITY.md").isFile());
    }
}

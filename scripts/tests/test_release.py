"""Local contract tests for release preparation; no network or credentials required."""

import pathlib
import os
import shutil
import subprocess
import sys
import tempfile
import unittest


SCRIPT = pathlib.Path(__file__).resolve().parents[1] / "release.py"


class ReleaseNotesTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = pathlib.Path(self.temporary.name)
        (self.root / ".changes").mkdir()
        self.notes = "## [1.2.3] - 2026-01-02\n\n### Fixed\n\n- Keep this release only.\n"
        (self.root / ".changes/v1.2.3.md").write_text(self.notes)
        (self.root / "CHANGELOG.md").write_text(
            "# Changelog\n\n" + self.notes + "\n## [1.2.2] - 2026-01-01\n\n- Older.\n"
        )
        (self.root / "gradle.properties").write_text("version=1.2.3\ngroup=com.backblaze.b2\n")

    def run_notes(self, tag="v1.2.3"):
        return subprocess.run(
            [sys.executable, str(SCRIPT), "--root", str(self.root), "notes", tag],
            capture_output=True, text=True,
        )

    def test_extracts_exact_release_section(self):
        result = self.run_notes()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(self.notes, result.stdout)
        self.assertNotIn("Older", result.stdout)

    def test_rejects_tag_version_mismatch(self):
        result = self.run_notes("v1.2.4")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("gradle.properties", result.stderr)

    def test_rejects_missing_notes(self):
        (self.root / ".changes/v1.2.3.md").unlink()
        result = self.run_notes()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("release notes", result.stderr)

    def test_rejects_edited_generated_changelog(self):
        (self.root / "CHANGELOG.md").write_text("# Changelog\n\n" + self.notes.replace("Keep", "Changed"))
        result = self.run_notes()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("disagree", result.stderr)

    def test_rejects_path_as_tag(self):
        result = self.run_notes("../v1.2.3")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("version tag", result.stderr)

    def test_rejects_duplicate_version_property(self):
        (self.root / "gradle.properties").write_text("version=1.2.3\nversion=1.2.4\n")
        result = self.run_notes()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("exactly one", result.stderr)


class ChangelogTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = pathlib.Path(self.temporary.name)
        repository = SCRIPT.parents[1]
        for name in (".changie.yaml", "gradle.properties", "CHANGELOG.md"):
            shutil.copy(repository / name, self.root)
        shutil.copytree(repository / ".changes", self.root / ".changes")
        self.changie = os.environ.get("CHANGIE", "changie")

    def run_check(self):
        return subprocess.run(
            [sys.executable, str(SCRIPT), "--root", str(self.root), "changelog-check"],
            capture_output=True, text=True,
        )

    def test_current_generated_changelog_matches(self):
        result = self.run_check()
        self.assertEqual(0, result.returncode, result.stderr)

    def test_hand_edited_changelog_is_rejected_without_modifying_it(self):
        path = self.root / "CHANGELOG.md"
        edited = path.read_text() + "\nHand edited.\n"
        path.write_text(edited)
        result = self.run_check()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("generated", result.stderr)
        self.assertEqual(edited, path.read_text())

    def test_batch_and_merge_update_gradle_version_and_release_notes(self):
        for arguments in (("batch", "v6.5.1", "--allow-no-changes=false"), ("merge",)):
            subprocess.run([self.changie, *arguments], cwd=self.root, check=True, capture_output=True)
        self.assertIn("version=6.5.1\n", (self.root / "gradle.properties").read_text())
        self.assertTrue((self.root / ".changes/v6.5.1.md").is_file())
        result = self.run_check()
        self.assertEqual(0, result.returncode, result.stderr)


if __name__ == "__main__":
    unittest.main()

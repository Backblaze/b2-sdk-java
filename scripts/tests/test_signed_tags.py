"""Verify real disposable Git/GPG/SSH signatures; mock only the key-download boundary."""

import os
import pathlib
import subprocess
import tempfile
import unittest


SCRIPT = pathlib.Path(__file__).resolve().parents[1] / "verify-release-tag.sh"


class SignedTagTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temporary = tempfile.TemporaryDirectory(prefix="b2-tag-", dir="/tmp")
        cls.root = pathlib.Path(cls.temporary.name)
        cls.home = cls.root / "gnupg"
        cls.home.mkdir(mode=0o700)
        cls.environment = dict(os.environ, GNUPGHOME=str(cls.home), GIT_CONFIG_GLOBAL=os.devnull,
                               GIT_CONFIG_NOSYSTEM="1")
        cls.repo = cls.root / "repo"
        cls.repo.mkdir()
        cls.call(["git", "init", "-q"])
        cls.call(["git", "config", "user.name", "Release Fixture"])
        cls.call(["git", "config", "user.email", "fixture@example.invalid"])
        cls.call(["git", "-c", "commit.gpgSign=false", "commit", "--allow-empty", "-qm", "Fixture"])
        cls.reviewed = cls.call(["git", "rev-parse", "HEAD"]).stdout.strip()
        cls.call(["git", "update-ref", "refs/remotes/origin/master", cls.reviewed])
        cls.call(["gpg", "--batch", "--pinentry-mode", "loopback", "--passphrase", "",
                  "--quick-generate-key", "Release Fixture <fixture@example.invalid>", "ed25519", "sign", "0"])
        keys = cls.call(["gpg", "--with-colons", "--list-secret-keys"]).stdout
        fingerprint = next(line.split(":")[9] for line in keys.splitlines() if line.startswith("fpr:"))
        cls.public_gpg = cls.root / "fixture.gpg"
        cls.public_gpg.write_text(cls.call(["gpg", "--armor", "--export", fingerprint]).stdout)
        cls.call(["git", "-c", "gpg.format=openpgp", "-c", "user.signingkey=" + fingerprint,
                  "tag", "-s", "v1.2.3", "-m", "v1.2.3"])
        cls.call(["git", "tag", "v1.2.4"])
        cls.ssh_key = cls.root / "ssh-key"
        cls.call(["ssh-keygen", "-q", "-t", "ed25519", "-N", "", "-f", str(cls.ssh_key)])
        cls.call(["git", "-c", "gpg.format=ssh", "-c", "user.signingkey=" + str(cls.ssh_key),
                  "tag", "-s", "v1.2.5", "-m", "v1.2.5"])
        cls.call(["git", "-c", "commit.gpgSign=false", "commit", "--allow-empty", "-qm", "Unreviewed fixture"])
        cls.unreviewed = cls.call(["git", "rev-parse", "HEAD"]).stdout.strip()
        cls.call(["git", "tag", "v1.2.8"])
        cls.maintainers = cls.root / "maintainers"
        cls.maintainers.write_text("fixture-maintainer\n")
        cls.bin = cls.root / "bin"
        cls.bin.mkdir()
        curl = cls.bin / "curl"
        curl.write_text("""#!/usr/bin/env python3
import os, pathlib, sys
path = os.environ.get('FIXTURE_GPG' if sys.argv[-1].endswith('.gpg') else 'FIXTURE_SSH')
if path:
    print(pathlib.Path(path).read_text(), end='')
""")
        curl.chmod(0o755)
        gh = cls.bin / "gh"
        gh.write_text("""#!/usr/bin/env python3
import os, pathlib
path = os.environ.get('FIXTURE_SSH_SIGNING')
if path:
    print(pathlib.Path(path).read_text(), end='')
""")
        gh.chmod(0o755)

    @classmethod
    def call(cls, command):
        result = subprocess.run(command, cwd=cls.repo, env=cls.environment, text=True, capture_output=True)
        if result.returncode:
            raise RuntimeError(str(command) + ": " + result.stderr)
        return result

    @classmethod
    def tearDownClass(cls):
        subprocess.run(["gpgconf", "--kill", "all"], env=cls.environment, capture_output=True)
        cls.temporary.cleanup()

    def verify(self, tag="v1.2.3", enforce="true", registered=True, signing_only=False):
        environment = dict(self.environment, PATH=str(self.bin) + os.pathsep + self.environment["PATH"],
                           ENFORCE_SIGNED_TAGS=enforce,
                           FIXTURE_GPG=str(self.public_gpg) if registered else "",
                           FIXTURE_SSH=str(self.ssh_key) + ".pub" if registered else "",
                           FIXTURE_SSH_SIGNING=str(self.ssh_key) + ".pub" if signing_only else "")
        return subprocess.run(["bash", str(SCRIPT), tag, str(self.maintainers)],
                              cwd=self.repo, env=environment, text=True, capture_output=True)

    def test_registered_gpg_signature_passes_enforcing_gate(self):
        result = self.verify()
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertIn("verified", result.stdout)

    def test_registered_ssh_signature_passes_enforcing_gate(self):
        tag = self.call(["git", "cat-file", "tag", "v1.2.5"]).stdout
        self.assertIn("tagger Release Fixture <fixture@example.invalid>", tag)
        result = self.verify("v1.2.5")
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertIn("verified", result.stdout)

    def test_ssh_key_registered_only_for_signing_passes(self):
        result = self.verify("v1.2.5", registered=False, signing_only=True)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertIn("verified", result.stdout)

    def test_unsigned_tag_blocks_when_enforced(self):
        result = self.verify("v1.2.4")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("annotated", result.stdout + result.stderr)

    def test_unregistered_signature_blocks_when_enforced(self):
        result = self.verify(registered=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("not verified", result.stdout + result.stderr)

    def test_default_mode_warns_without_blocking(self):
        result = self.verify("v1.2.4", enforce="")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("::warning::", result.stdout)

    def test_missing_registered_keys_warns_without_blocking(self):
        result = self.verify(enforce="false", registered=False)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("::warning::", result.stdout)

    def check_commit(self, tag, commit):
        return subprocess.run(["python3", str(SCRIPT.with_name("release.py")), "--root", str(self.repo),
                               "verify-commit", tag, commit], cwd=self.repo, env=self.environment,
                              text=True, capture_output=True)

    def test_tag_moved_away_from_event_commit_is_rejected(self):
        result = self.check_commit("v1.2.3", self.unreviewed)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("event commit", result.stderr)

    def test_matching_event_commit_on_master_passes(self):
        result = self.check_commit("v1.2.3", self.reviewed)
        self.assertEqual(0, result.returncode, result.stderr)

    def test_matching_tag_outside_master_is_rejected(self):
        result = self.check_commit("v1.2.8", self.unreviewed)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("origin/master", result.stderr)


if __name__ == "__main__":
    unittest.main()

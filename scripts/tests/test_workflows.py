"""Release workflow policy and GitHub Pages status contract checks."""

import json
import os
import pathlib
import re
import shutil
import subprocess
import sys
import tempfile
import unittest


ROOT = pathlib.Path(__file__).resolve().parents[2]


def step_script(workflow, name):
    step = workflow.split("      - name: " + name + "\n", 1)[1]
    step = step.split("\n      - ", 1)[0]
    body = step.split("        run: |\n", 1)[1]
    return "\n".join(line[10:] for line in body.splitlines()) + "\n"


class WorkflowTest(unittest.TestCase):
    def test_changelog_check_runs_for_pull_requests_without_branch_filters(self):
        workflow = (ROOT / ".github/workflows/changelog.yml").read_text()
        triggers = re.search(r"^on:[ \t]*\n(.*?)(?=^[^\s#]|\Z)", workflow, re.M | re.S)
        self.assertIsNotNone(triggers, "changelog workflow must declare its triggers")
        pull_request = re.search(r"^  pull_request:[ \t]*\n(.*?)(?=^  \S|\Z)",
                                 triggers.group(1), re.M | re.S)
        self.assertIsNotNone(pull_request, "changelog-check must run on pull requests")
        self.assertNotRegex(pull_request.group(1), r"(?m)^    branches(?:-ignore)?:")
        self.assertIn("\n  changelog-check:\n", workflow)

    def test_every_external_action_is_pinned_to_commit(self):
        for workflow in (ROOT / ".github/workflows").glob("*.yml"):
            for action in re.findall(r"uses:\s*([^\s#]+)", workflow.read_text()):
                with self.subTest(workflow=workflow.name, action=action):
                    self.assertRegex(action, r"^[A-Za-z0-9_./-]+@[0-9a-f]{40}$")

    def test_replaced_community_actions_are_absent(self):
        workflows = "\n".join(path.read_text() for path in (ROOT / ".github/workflows").glob("*.yml"))
        for action in ("crazy-max/ghaction-github-pages", "crazy-max/ghaction-github-status", "softprops/action-gh-release"):
            self.assertNotIn(action, workflows)

    def test_publish_command_and_signing_secrets_are_preserved(self):
        workflow = (ROOT / ".github/workflows/deploy.yml").read_text()
        self.assertIn("./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository createBundle --no-daemon --stacktrace", workflow)
        for secret in ("GPG_SIGNING_KEY", "GPG_PASSPHRASE", "SONATYPE_TOKEN_USERNAME", "SONATYPE_TOKEN_PASSWORD"):
            self.assertIn("secrets." + secret, workflow)

    def test_release_checks_and_deploy_use_the_same_event_commit(self):
        workflow = (ROOT / ".github/workflows/deploy.yml").read_text()
        self.assertIn('verify-commit "$RELEASE_TAG" "$GITHUB_SHA"', workflow)
        deploy = workflow.split("\n  deploy:\n", 1)[1]
        self.assertIn("needs: release-checks", deploy)
        self.assertIn("ref: ${{ github.sha }}", deploy)

    def test_release_gate_commands_are_wired_before_deployment(self):
        workflow = (ROOT / ".github/workflows/deploy.yml").read_text()
        checks, deploy = workflow.split("\n  release-checks:\n", 1)[1].split("\n  deploy:\n", 1)
        verification = step_script(checks, "Verify release tag")
        self.assertIsNotNone(re.search(
            r'^bash scripts/verify-release-tag\.sh "\$RELEASE_TAG"\s*$', verification, re.M),
            "release-checks must invoke the signed-tag verifier")
        for command in ('milestone "$RELEASE_TAG"', "security"):
            with self.subTest(command=command):
                self.assertIsNotNone(re.search(
                    r"^        run: python3 scripts/release\.py " + re.escape(command) + r"\s*$", checks, re.M),
                    "release-checks must invoke " + command)
        metadata = step_script(checks, "Check tag, version, and changelog agreement")
        self.assertIn(
            'python3 scripts/release.py --root "$RUNNER_TEMP/release-metadata" notes "$RELEASE_TAG" > /dev/null',
            metadata, "release-checks must validate metadata from the event commit")
        self.assertIn("needs: release-checks", deploy)

    def test_metadata_gate_runs_against_the_event_commit_and_rejects_mismatches(self):
        workflow = (ROOT / ".github/workflows/deploy.yml").read_text()
        script = step_script(workflow, "Check tag, version, and changelog agreement")
        notes = "## [1.2.3] - 2026-01-01\n\n### Fixed\n\n* Release fixture.\n"
        cases = (("v1.2.3", notes, ""),
                 ("v1.2.4", notes, "tag does not match gradle.properties"),
                 ("v1.2.3", notes + "\n* Unreviewed notes.\n", "release notes and CHANGELOG.md disagree"))
        for tag, changelog, error in cases:
            with self.subTest(tag=tag, error=error), tempfile.TemporaryDirectory(prefix="b2-metadata-gate-") as temporary:
                root = pathlib.Path(temporary)
                (root / ".changes").mkdir()
                (root / ".changes/v1.2.3.md").write_text(notes)
                (root / "CHANGELOG.md").write_text("# Changelog\n\n" + changelog)
                (root / "gradle.properties").write_text("version=1.2.3\n")
                (root / "scripts").mkdir()
                shutil.copy(ROOT / "scripts/release.py", root / "scripts/release.py")
                environment = dict(os.environ, GIT_CONFIG_GLOBAL=os.devnull, GIT_CONFIG_NOSYSTEM="1")
                for args in (("init", "-q"), ("config", "user.name", "Release Fixture"),
                             ("config", "user.email", "fixture@example.invalid"), ("add", "."),
                             ("-c", "commit.gpgSign=false", "commit", "-qm", "Release metadata fixture")):
                    subprocess.run(["git", *args], cwd=root, env=environment, check=True, capture_output=True)
                commit = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=root, env=environment,
                                                 text=True).strip()
                runner = root / "runner"
                runner.mkdir()
                environment.update(RELEASE_TAG=tag, GITHUB_SHA=commit, RUNNER_TEMP=str(runner))
                # An uncommitted local version must not replace the immutable event's metadata.
                (root / "gradle.properties").write_text("version=9.9.9\n")
                result = subprocess.run(["bash", "-e", "-o", "pipefail", "-c", script], cwd=root,
                                        env=environment, text=True, capture_output=True)
                if error:
                    self.assertNotEqual(0, result.returncode, result.stdout + result.stderr)
                    self.assertIn(error, result.stderr)
                else:
                    self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_dependabot_updates_action_pins_weekly_from_repository_root(self):
        configuration = (ROOT / ".github/dependabot.yml").read_text()
        updates = re.findall(r"^  - package-ecosystem:.*?(?=^  - |\Z)", configuration, re.M | re.S)
        action_updates = [entry for entry in updates if re.match(
            r'''^  - package-ecosystem:\s*["']?github-actions["']?\s*(?:#.*)?$''',
            entry.splitlines()[0])]
        self.assertEqual(1, len(action_updates), "configure exactly one GitHub Actions update entry")
        self.assertRegex(action_updates[0], re.compile(r'''^    directory:\s*["']?/["']?\s*(?:#.*)?$''', re.M))
        self.assertRegex(action_updates[0], re.compile(
            r'''^    schedule:\s*\n      interval:\s*["']?weekly["']?\s*(?:#.*)?$''', re.M))

    def test_official_pages_actions_and_status_gate_are_connected(self):
        workflow = (ROOT / ".github/workflows/ci_cd.yml").read_text()
        build, deploy = workflow.split("\n  deploy-pages:\n", 1)
        self.assertRegex(build, r"uses: actions/upload-pages-artifact@[0-9a-f]{40}\b")
        self.assertIn("path: core/build/docs/javadoc", build)
        self.assertIn("needs: build", deploy)
        self.assertRegex(deploy, r"uses: actions/deploy-pages@[0-9a-f]{40}\b")
        self.assertIn("pages: write", deploy)
        self.assertIn("id-token: write", deploy)
        status = step_script(deploy, "Check GitHub Pages status")
        self.assertRegex(status, re.compile(
            r"^curl\b.*?https://www\.githubstatus\.com/api/v2/components\.json"
            r"\s*\|\s*python3 scripts/release\.py pages-status\b", re.M | re.S))

    def test_release_command_uses_notes_and_each_module_bundle(self):
        workflow = (ROOT / ".github/workflows/deploy.yml").read_text()
        script = step_script(workflow, "Create GitHub Release")
        for tag, flag in (("v1.2.3", "--latest"), ("v1.2.3-rc.1", "--prerelease")):
            with self.subTest(tag=tag), tempfile.TemporaryDirectory(prefix="b2-release-command-") as temporary:
                root = pathlib.Path(temporary)
                binary = root / "bin"
                binary.mkdir()
                gh = binary / "gh"
                gh.write_text("#!" + sys.executable + "\n"
                              "import json, os, pathlib, sys\n"
                              "pathlib.Path(os.environ['GH_CALL']).write_text(json.dumps(sys.argv[1:]))\n")
                gh.chmod(0o755)
                assets = []
                for module in ("core", "httpclient", "samples"):
                    path = pathlib.Path(module) / "build/libs" / ("bundle-" + module + ".jar")
                    (root / path).parent.mkdir(parents=True)
                    (root / path).write_bytes(b"bundle fixture")
                    assets.append(str(path))
                notes = root / "release-notes.md"
                notes.write_text("Release notes fixture\n")
                output = root / "gh-call.json"
                environment = dict(os.environ, PATH=str(binary) + os.pathsep + os.environ["PATH"],
                                   RELEASE_TAG=tag, RUNNER_TEMP=str(root), GH_CALL=str(output))
                result = subprocess.run(["bash", "-e", "-o", "pipefail", "-c", script],
                                        cwd=root, env=environment, text=True, capture_output=True)
                self.assertEqual(0, result.returncode, result.stdout + result.stderr)
                self.assertTrue(output.is_file(), "release command did not invoke gh")
                arguments = json.loads(output.read_text())
                self.assertEqual(["release", "create", tag], arguments[:3])
                self.assertIn("--verify-tag", arguments)
                self.assertIn("--title", arguments)
                self.assertEqual(tag, arguments[arguments.index("--title") + 1])
                self.assertIn("--notes-file", arguments)
                self.assertEqual(str(notes), arguments[arguments.index("--notes-file") + 1])
                self.assertIn(flag, arguments)
                self.assertCountEqual(assets, [value for value in arguments if value.endswith(".jar")])

    def test_publication_task_graph_is_checked_without_publishing(self):
        workflow = (ROOT / ".github/workflows/ci_cd.yml").read_text()
        self.assertIn("      - name: Validate publication task graph\n", workflow)
        script = step_script(workflow, "Validate publication task graph")
        for argument in ("./gradlew", "publishToSonatype", "closeAndReleaseSonatypeStagingRepository",
                         "createBundle", "--dry-run", "--no-daemon",
                         "-PsonatypeUsername=dry-run", "-PsonatypePassword=dry-run"):
            self.assertIn(argument, script)

    def test_runbook_describes_the_workflow_token_uses(self):
        guide = " ".join((ROOT / "RELEASE.md").read_text().split())
        self.assertIn("checkout, signing-key API lookups, milestone reads, and release creation", guide)


class PagesStatusTest(unittest.TestCase):
    def run_status(self, payload):
        return subprocess.run([sys.executable, str(ROOT / "scripts/release.py"), "pages-status"],
                              input=json.dumps(payload), text=True, capture_output=True)

    def test_operational_pages_passes(self):
        result = self.run_status({"components": [{"name": "Pages", "status": "operational"}]})
        self.assertEqual(0, result.returncode, result.stderr)

    def test_major_outage_blocks_deployment(self):
        result = self.run_status({"components": [{"name": "Pages", "status": "major_outage"}]})
        self.assertNotEqual(0, result.returncode)
        self.assertIn("major outage", result.stderr)

    def test_unreadable_status_is_not_treated_as_operational(self):
        result = self.run_status({"components": []})
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Pages status", result.stderr)


if __name__ == "__main__":
    unittest.main()

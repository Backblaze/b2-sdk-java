"""Check workflow-contract discrimination using disposable workflow copies."""

import pathlib
import tempfile
import unittest
from unittest import mock

import test_workflows as contracts


class WorkflowMutationTest(unittest.TestCase):
    def setUp(self):
        self.files = {name: pathlib.Path(".github/workflows") / name for name in ("ci_cd.yml", "deploy.yml")}
        self.files["dependabot.yml"] = pathlib.Path(".github/dependabot.yml")
        self.originals = {
            name: (contracts.ROOT / filename).read_text()
            for name, filename in self.files.items()
        }
        temporary = tempfile.TemporaryDirectory(prefix="b2-workflow-mutations-")
        self.addCleanup(temporary.cleanup)
        self.root = pathlib.Path(temporary.name)
        (self.root / ".github/workflows").mkdir(parents=True)
        for name, content in self.originals.items():
            (self.root / self.files[name]).write_text(content)

    def run_contract(self, method):
        result = unittest.TestResult()
        with mock.patch.object(contracts, "ROOT", self.root):
            contracts.WorkflowTest(method).run(result)
        self.assertEqual(1, result.testsRun)
        self.assertFalse(result.errors, result.errors)
        return result

    def assert_mutation_rejected(self, workflow, before, after, method):
        baseline = self.run_contract(method)
        self.assertTrue(baseline.wasSuccessful(), baseline.failures)
        original = self.originals[workflow]
        self.assertEqual(1, original.count(before), "mutation must change exactly one occurrence")
        (self.root / self.files[workflow]).write_text(original.replace(before, after, 1))
        result = self.run_contract(method)
        self.assertFalse(result.wasSuccessful(), "workflow mutation escaped its contract check")
        self.assertTrue(result.failures, "mutation must fail an assertion, not fixture setup")

    def test_missing_pages_artifact_action_is_rejected(self):
        self.assert_mutation_rejected(
            "ci_cd.yml", "uses: actions/upload-pages-artifact@", "uses: actions/upload-artifact@",
            "test_official_pages_actions_and_status_gate_are_connected")

    def test_missing_pages_deployment_is_rejected(self):
        self.assert_mutation_rejected(
            "ci_cd.yml", "uses: actions/deploy-pages@", "uses: actions/download-artifact@",
            "test_official_pages_actions_and_status_gate_are_connected")

    def test_missing_pages_status_command_is_rejected(self):
        self.assert_mutation_rejected(
            "ci_cd.yml", "curl --fail --show-error --silent --location --max-time 30",
            "printf --fail --show-error --silent --location --max-time 30",
            "test_official_pages_actions_and_status_gate_are_connected")

    def test_missing_release_creation_is_rejected(self):
        self.assert_mutation_rejected(
            "deploy.yml", "gh release create", "true",
            "test_release_command_uses_notes_and_each_module_bundle")

    def test_missing_release_notes_argument_is_rejected(self):
        self.assert_mutation_rejected(
            "deploy.yml", '--notes-file "$RUNNER_TEMP/release-notes.md" ', "",
            "test_release_command_uses_notes_and_each_module_bundle")

    def test_missing_release_assets_argument_is_rejected(self):
        self.assert_mutation_rejected(
            "deploy.yml", ' "${assets[@]}"', "",
            "test_release_command_uses_notes_and_each_module_bundle")

    def test_missing_release_gate_dependency_is_rejected(self):
        self.assert_mutation_rejected(
            "deploy.yml", "needs: release-checks", "needs: []",
            "test_release_checks_and_deploy_use_the_same_event_commit")

    def test_missing_publication_dry_run_is_rejected(self):
        self.assert_mutation_rejected(
            "ci_cd.yml", "--dry-run --no-daemon", "--no-daemon",
            "test_publication_task_graph_is_checked_without_publishing")

    def test_missing_signed_tag_verification_is_rejected(self):
        self.assert_mutation_rejected(
            "deploy.yml", 'bash scripts/verify-release-tag.sh "$RELEASE_TAG"', "true",
            "test_release_gate_commands_are_wired_before_deployment")

    def test_missing_milestone_gate_is_rejected(self):
        self.assert_mutation_rejected(
            "deploy.yml", 'run: python3 scripts/release.py milestone "$RELEASE_TAG"', "run: true",
            "test_release_gate_commands_are_wired_before_deployment")

    def test_missing_security_gate_is_rejected(self):
        self.assert_mutation_rejected(
            "deploy.yml", "run: python3 scripts/release.py security", "run: true",
            "test_release_gate_commands_are_wired_before_deployment")

    def test_missing_release_metadata_gate_is_rejected(self):
        self.assert_mutation_rejected(
            "deploy.yml",
            'python3 scripts/release.py --root "$RUNNER_TEMP/release-metadata" notes "$RELEASE_TAG" > /dev/null',
            "true", "test_release_gate_commands_are_wired_before_deployment")

    def test_missing_dependabot_action_updates_is_rejected(self):
        self.assert_mutation_rejected(
            "dependabot.yml",
            '  - package-ecosystem: "github-actions"\n    directory: "/"\n'
            '    schedule:\n      interval: "weekly"\n', "",
            "test_dependabot_updates_action_pins_weekly_from_repository_root")


if __name__ == "__main__":
    unittest.main()

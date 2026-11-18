"""Check workflow-contract discrimination using disposable workflow copies."""

import pathlib
import tempfile
import unittest
from unittest import mock

import test_workflows as contracts


class WorkflowMutationTest(unittest.TestCase):
    def setUp(self):
        self.originals = {
            name: (contracts.ROOT / ".github/workflows" / name).read_text()
            for name in ("ci_cd.yml", "deploy.yml")
        }
        temporary = tempfile.TemporaryDirectory(prefix="b2-workflow-mutations-")
        self.addCleanup(temporary.cleanup)
        self.root = pathlib.Path(temporary.name)
        (self.root / ".github/workflows").mkdir(parents=True)
        for name, content in self.originals.items():
            (self.root / ".github/workflows" / name).write_text(content)

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
        (self.root / ".github/workflows" / workflow).write_text(original.replace(before, after, 1))
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


if __name__ == "__main__":
    unittest.main()

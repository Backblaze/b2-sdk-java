#!/usr/bin/env python3
"""Release metadata checks. Uses the standard library and the pinned Changie binary."""

import argparse
import json
import os
import pathlib
import re
import shutil
import subprocess
import sys
import tempfile


VERSION = r"[0-9]+\.[0-9]+\.[0-9]+(?:-[0-9A-Za-z.-]+)?"


def project_version(root):
    versions = re.findall(r"^version=(.+)$", (root / "gradle.properties").read_text(), re.M)
    if len(versions) != 1 or not re.fullmatch(VERSION, versions[0]):
        raise ValueError("gradle.properties must contain exactly one semantic version property")
    return versions[0]


def release_notes(root, tag):
    if not re.fullmatch("v" + VERSION, tag):
        raise ValueError("expected a version tag such as v6.5.1")
    if tag != "v" + project_version(root):
        raise ValueError("tag does not match gradle.properties")
    path = root / ".changes" / (tag + ".md")
    if not path.is_file():
        raise ValueError("release notes are missing: " + path.name)
    notes = path.read_text().strip()
    heading = re.escape("## [" + tag[1:] + "]")
    section = re.search(r"^" + heading + r"[^\n]*\n.*?(?=^## \[|\Z)",
                        (root / "CHANGELOG.md").read_text(), re.M | re.S)
    if not section or notes != section.group().strip():
        raise ValueError("release notes and CHANGELOG.md disagree; run changie merge")
    return notes + "\n"


def changelog_check(root, base, dependabot=False):
    changie = os.environ.get("CHANGIE", "changie")
    fragments = list((root / ".changes/unreleased").glob("*.yaml"))
    with tempfile.TemporaryDirectory(prefix="b2-changelog-") as temporary:
        target = pathlib.Path(temporary)
        shutil.copy(root / ".changie.yaml", target)
        shutil.copy(root / "gradle.properties", target)
        shutil.copytree(root / ".changes", target / ".changes")
        subprocess.run([changie, "merge"], cwd=target, check=True)
        if (target / "CHANGELOG.md").read_bytes() != (root / "CHANGELOG.md").read_bytes():
            raise ValueError("CHANGELOG.md is generated; run changie merge")
        if project_version(target) != project_version(root):
            raise ValueError("gradle.properties must match the latest Changie version")
        latest = re.search(r"^## \[(" + VERSION + r")\]",
                           (root / "CHANGELOG.md").read_text(), re.M)
        if not latest:
            raise ValueError("CHANGELOG.md must contain a released version heading")
        release_notes(root, "v" + latest.group(1))
        if fragments:
            subprocess.run([changie, "batch", "patch", "--dry-run", "--allow-no-changes=false"],
                           cwd=target, check=True, stdout=subprocess.DEVNULL)
    if base:
        changes = subprocess.check_output(
            ["git", "diff", "--name-status", "--find-renames", base + "...HEAD"], cwd=root, text=True
        ).splitlines()
        workflow_only = changes and all(
            line.startswith("M\t.github/workflows/") and line.endswith((".yml", ".yaml"))
            for line in changes
        )
        if dependabot and workflow_only:
            print("changelog-check: validated automated workflow-only update; fragment not required")
            return
        added = [line.split("\t")[-1] for line in changes if line.startswith("A\t")]
        if not any(name.startswith(".changes/unreleased/") and name.endswith(".yaml") for name in added):
            release_changes = subprocess.check_output(
                ["git", "diff", "--name-status", "--no-renames", base + "...HEAD"], cwd=root, text=True
            ).splitlines()
            updated = [line.split("\t")[-1] for line in release_changes if line.startswith(("A\t", "M\t"))]
            tag = "v" + project_version(root)
            if ".changes/" + tag + ".md" not in updated or "gradle.properties" not in updated or fragments:
                raise ValueError("add a Changie fragment, or batch all fragments in a release-prep PR")
            release_notes(root, tag)
    print("changelog-check: generated changelog, fragments, and version agree")


def api_list(repo, resource):
    if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repo):
        raise ValueError("invalid repository name")
    result = subprocess.run(["gh", "api", "--paginate", "--slurp",
                             "repos/" + repo + "/" + resource], capture_output=True, text=True)
    if result.returncode:
        raise ValueError("cannot verify " + resource.split("?")[0] + "; check security-read access")
    try:
        pages = json.loads(result.stdout)
        if not isinstance(pages, list) or not all(isinstance(page, list) for page in pages):
            raise ValueError()
        items = [item for page in pages for item in page]
        if not all(isinstance(item, dict) for item in items):
            raise ValueError()
        return items
    except (ValueError, TypeError):
        raise ValueError("cannot verify " + resource.split("?")[0] + ": invalid API response") from None


def blocks_release(severity):
    if severity is None:
        return True  # An untriaged advisory cannot establish a clear release gate.
    if severity not in ("low", "medium", "high", "critical"):
        raise ValueError("cannot verify an unknown severity")
    return severity != "low"


def security_gate(repo):
    sources = (
        ("Code scanning", "code-scanning/alerts?state=open&ref=refs/heads/master&per_page=100"),
        ("Dependabot", "dependabot/alerts?state=open&per_page=100"),
        ("Secret scanning", "secret-scanning/alerts?state=open&per_page=100"),
        ("Repository advisories", "security-advisories?per_page=100"),
        ("Issues", "issues?state=open&per_page=100"),
    )
    blocked = False
    allowed_states = {
        "Code scanning": ("open", "fixed", "dismissed"),
        "Dependabot": ("open", "fixed", "dismissed", "auto_dismissed"),
        "Secret scanning": ("open", "resolved"),
        "Repository advisories": ("draft", "triage", "published", "closed", "withdrawn"),
        "Issues": ("open", "closed"),
    }
    for name, endpoint in sources:
        count = 0
        try:
            for item in api_list(repo, endpoint):
                state = item["state"]
                if state not in allowed_states[name]:
                    raise ValueError("cannot verify " + name + ": unknown finding state")
                if name == "Repository advisories":
                    count += state in ("draft", "triage") and blocks_release(item["severity"])
                elif state == "open":
                    if name == "Code scanning":
                        severity = item["rule"].get("security_severity_level")
                        count += severity is not None and blocks_release(severity)
                    elif name == "Dependabot":
                        count += blocks_release(item["security_advisory"]["severity"])
                    elif name == "Secret scanning":
                        count += 1
                    elif name == "Issues":
                        labels = [re.sub(r"[\s:/_-]+", "", label["name"].lower()) for label in item["labels"]]
                        count += any(label in ("medium", "high", "critical", "severitymedium",
                                               "severityhigh", "severitycritical") for label in labels)
        except (KeyError, TypeError, AttributeError):
            raise ValueError("cannot verify " + name + ": incomplete API response") from None
        print(name + (": HOLD (" + str(count) + " blocking finding(s))" if count else ": clear"))
        blocked |= count > 0
    if blocked:
        raise ValueError("security gate holds this release")
    print("security gate: clear")


def milestone_gate(repo, tag):
    if not re.fullmatch("v" + VERSION, tag):
        raise ValueError("expected a version tag")
    matches = [item for item in api_list(repo, "milestones?state=all&per_page=100") if item.get("title") == tag]
    if len(matches) != 1 or matches[0].get("open_issues") != 0:
        raise ValueError("milestone " + tag + " must exist and have no open issues or pull requests")
    print("milestone gate: clear (" + tag + ")")


def verify_commit(root, tag, expected):
    if not re.fullmatch("v" + VERSION, tag) or not re.fullmatch(r"[0-9a-f]{40}|[0-9a-f]{64}", expected):
        raise ValueError("expected a version tag and immutable event commit")
    actual = subprocess.check_output(
        ["git", "rev-parse", "--verify", "refs/tags/" + tag + "^{commit}"], cwd=root, text=True
    ).strip()
    if actual != expected:
        raise ValueError("release tag no longer matches the event commit")
    result = subprocess.run(["git", "merge-base", "--is-ancestor", expected, "origin/master"], cwd=root)
    if result.returncode:
        raise ValueError("release event commit is not on origin/master")
    print("release commit: matches event and origin/master")


def pages_status():
    try:
        components = json.load(sys.stdin)["components"]
        pages = [item for item in components if item["name"] in ("Pages", "GitHub Pages")]
        if len(pages) != 1:
            raise ValueError()
        status = pages[0]["status"]
    except (ValueError, KeyError, TypeError):
        raise ValueError("cannot verify GitHub Pages status") from None
    if status == "major_outage":
        raise ValueError("GitHub Pages has a major outage; hold Javadoc deployment")
    if status not in ("operational", "degraded_performance", "partial_outage", "under_maintenance"):
        raise ValueError("cannot verify GitHub Pages status")
    print("GitHub Pages status: " + status)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=pathlib.Path, default=pathlib.Path(__file__).resolve().parents[1])
    commands = parser.add_subparsers(dest="command", required=True)
    notes = commands.add_parser("notes")
    notes.add_argument("tag")
    changelog = commands.add_parser("changelog-check")
    changelog.add_argument("--base")
    changelog.add_argument("--dependabot", action="store_true")
    security = commands.add_parser("security")
    security.add_argument("--repo", default="Backblaze/b2-sdk-java")
    milestone = commands.add_parser("milestone")
    milestone.add_argument("tag")
    milestone.add_argument("--repo", default="Backblaze/b2-sdk-java")
    commands.add_parser("pages-status")
    commit = commands.add_parser("verify-commit")
    commit.add_argument("tag")
    commit.add_argument("sha")
    args = parser.parse_args()
    try:
        if args.command == "notes":
            print(release_notes(args.root, args.tag), end="")
        elif args.command == "changelog-check":
            changelog_check(args.root, args.base, args.dependabot)
        elif args.command == "security":
            security_gate(args.repo)
        elif args.command == "milestone":
            milestone_gate(args.repo, args.tag)
        elif args.command == "pages-status":
            pages_status()
        elif args.command == "verify-commit":
            verify_commit(args.root, args.tag, args.sha)
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        print("HOLD: " + str(error), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())

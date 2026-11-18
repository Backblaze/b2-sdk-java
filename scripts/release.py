#!/usr/bin/env python3
"""Release metadata checks. Uses the standard library and the pinned Changie binary."""

import argparse
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
            ["git", "diff", "--name-status", base + "...HEAD"], cwd=root, text=True
        ).splitlines()
        workflow_only = changes and all(
            line.startswith("M\t.github/workflows/") and line.endswith((".yml", ".yaml"))
            for line in changes
        )
        if dependabot and workflow_only:
            print("changelog-check: validated automated workflow-only update; fragment not required")
            return
        added = [line.split("\t")[-1] for line in changes if line.startswith(("A\t", "M\t"))]
        if not any(name.startswith(".changes/unreleased/") and name.endswith(".yaml") for name in added):
            tag = "v" + project_version(root)
            if ".changes/" + tag + ".md" not in added or "gradle.properties" not in added or fragments:
                raise ValueError("add a Changie fragment, or batch all fragments in a release-prep PR")
            release_notes(root, tag)
    print("changelog-check: generated changelog, fragments, and version agree")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=pathlib.Path, default=pathlib.Path(__file__).resolve().parents[1])
    commands = parser.add_subparsers(dest="command", required=True)
    notes = commands.add_parser("notes")
    notes.add_argument("tag")
    changelog = commands.add_parser("changelog-check")
    changelog.add_argument("--base")
    changelog.add_argument("--dependabot", action="store_true")
    args = parser.parse_args()
    try:
        if args.command == "notes":
            print(release_notes(args.root, args.tag), end="")
        elif args.command == "changelog-check":
            changelog_check(args.root, args.base, args.dependabot)
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        print("HOLD: " + str(error), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())

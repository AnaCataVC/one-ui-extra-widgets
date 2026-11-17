#!/usr/bin/env python3
"""Verify project invariants defined in AGENTS.md.

Checks:
1. Bilingual strings.xml parity between res/values/strings.xml and res/values-es/strings.xml.
2. Documentation hygiene in README*.md and docs/: no country flag emojis or absolute local paths.
3. Widget previewLayout presence in app/src/main/res/xml/widget_*.xml (warning by default, error with --strict).
"""

from __future__ import annotations

import argparse
import glob
import os
import re
import sys
import xml.etree.ElementTree as ET


def check_strings_parity() -> list[str]:
    en_path = "app/src/main/res/values/strings.xml"
    es_path = "app/src/main/res/values-es/strings.xml"

    en_keys = {
        el.attrib["name"]
        for el in ET.parse(en_path).getroot().findall("string")
        if el.attrib.get("translatable", "true").lower() != "false"
    }
    es_keys = {
        el.attrib["name"]
        for el in ET.parse(es_path).getroot().findall("string")
    }

    errors: list[str] = []
    missing_es = sorted(en_keys - es_keys)
    extra_es = sorted(es_keys - en_keys)
    if missing_es:
        errors.append(f"Missing Spanish translations in {es_path}: {', '.join(missing_es)}")
    if extra_es:
        errors.append(f"Extra keys in {es_path} not present in {en_path}: {', '.join(extra_es)}")
    return errors


def check_docs_hygiene() -> list[str]:
    errors: list[str] = []
    doc_files = ["README.md", "README.es.md"] + sorted(glob.glob("docs/**/*.md", recursive=True))
    flag_pattern = re.compile(r"[\U0001F1E6-\U0001F1FF]{2}")
    abs_path_pattern = re.compile(r"(?:/Users/|/home/|[A-Za-z]:\\\\Users\\\\)")

    for path in doc_files:
        if not os.path.isfile(path):
            continue
        with open(path, encoding="utf-8") as fh:
            for lineno, line in enumerate(fh, start=1):
                if flag_pattern.search(line):
                    errors.append(f"{path}:{lineno}: country flag emoji is forbidden by AGENTS.md")
                if abs_path_pattern.search(line):
                    errors.append(f"{path}:{lineno}: absolute local file path is forbidden by AGENTS.md")
    return errors


def check_widget_previews() -> list[str]:
    warnings: list[str] = []
    for path in sorted(glob.glob("app/src/main/res/xml/widget_*.xml")):
        with open(path, encoding="utf-8") as fh:
            content = fh.read()
        if "android:previewLayout" not in content:
            warnings.append(f"{path}: missing android:previewLayout attribute")
    return warnings


def main() -> int:
    parser = argparse.ArgumentParser(description="Check One UI Extra Widgets project invariants.")
    parser.add_argument(
        "--strict",
        action="store_true",
        help="Treat missing widget previewLayout attributes as errors.",
    )
    args = parser.parse_args()

    errors: list[str] = []
    errors.extend(check_strings_parity())
    errors.extend(check_docs_hygiene())

    preview_warnings = check_widget_previews()
    for warning in preview_warnings:
        prefix = "ERROR" if args.strict else "WARNING"
        print(f"[{prefix}] {warning}")
    if args.strict:
        errors.extend(preview_warnings)

    if errors:
        for err in errors:
            print(f"[ERROR] {err}", file=sys.stderr)
        return 1

    print("[OK] All blocking project invariants passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

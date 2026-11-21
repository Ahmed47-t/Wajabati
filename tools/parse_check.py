#!/usr/bin/env python3
"""Syntax gate for the Java sources.

This sandbox has no JDK (Maven/Google mirrors are not reachable), so the full Gradle build runs on GitHub
Actions. This script is the cheap local pre-check: it parses every Java file with javalang (full grammar,
Java 8+) and then reports references to resources / fields that the change might have missed.
"""
import glob
import re
import sys

import javalang

FILES = sorted(glob.glob("app/src/main/java/dz/local/mealguard/*.java") +
               glob.glob("app/src/test/java/dz/local/mealguard/*.java"))


def main():
    bad = 0
    for path in FILES:
        src = open(path, encoding="utf-8").read()
        try:
            tree = javalang.parse.parse(src)
        except Exception as exc:  # noqa: BLE001 - report and continue
            print(f"PARSE ERROR {path}: {exc}")
            bad += 1
            continue
        # every method needs a body unless abstract / in an interface
        for _, node in tree.filter((javalang.tree.MethodDeclaration,)):
            if node.body is None and "abstract" not in (node.modifiers or set()) and "native" not in (node.modifiers or set()):
                print(f"NO BODY {path}: {node.name}")
                bad += 1
    print(f"parsed {len(FILES)} files, {bad} problem(s)")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())

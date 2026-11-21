#!/usr/bin/env python3
"""Cross-reference check for the hand-written screen classes.

No JDK is available in this sandbox (Maven/Google mirrors are blocked), so the compiler cannot catch a renamed
field, a missing constant or a stale string key. This script does the cheap equivalent:

* every ``R.string`` used in Java exists in ``values/strings.xml``;
* every ``NavIcon`` constant used exists in ``NavIcon.java``;
* every ``Setup`` member used exists in ``Setup.java``;
* every field MainActivity's tests reach by name exists on MainActivity;
* every ``this::x`` method reference and ``x(...)`` call on ``MainActivity`` resolves to a declared method.

Exit code 1 on any finding, so it can gate a commit.
"""
import glob
import re
import sys

ROOT = "app/src"


def read(path):
    return open(path, encoding="utf-8").read()


def declared_names(stmt):
    """Field names of one declaration, ignoring initialisers such as new ArrayList<>() or a=1,b=2."""
    stmt = stmt.rstrip(";").replace("<>", "")
    stmt = re.sub(r"\([^()]*(?:\([^()]*\)[^()]*)*\)", "()", stmt)
    names = []
    for part in split_top(stmt):
        head = part.split("=")[0].strip()
        if head:
            name = head.split()[-1].strip()
            if re.fullmatch(r"[A-Za-z_$][A-Za-z0-9_$]*", name):
                names.append(name)
    return set(names)


def split_top(text):
    """Split a field declaration on commas that are not inside <>, () or {}."""
    parts, depth, current = [], 0, ""
    for ch in text:
        if ch in "<({[":
            depth += 1
        elif ch in ">)}]":
            depth -= 1
        if ch == "," and depth == 0:
            parts.append(current)
            current = ""
        else:
            current += ch
    parts.append(current)
    return [p.strip() for p in parts if p.strip()]


def main():
    problems = []

    main_activity = read(f"{ROOT}/main/java/dz/local/mealguard/MainActivity.java")
    setup = read(f"{ROOT}/main/java/dz/local/mealguard/Setup.java")
    nav_icon = read(f"{ROOT}/main/java/dz/local/mealguard/NavIcon.java")
    strings = read(f"{ROOT}/main/res/values/strings.xml")

    # 1. string resources
    defined = set(re.findall(r'<(?:string|string-array) name="([A-Za-z0-9_]+)"', strings))
    for path in glob.glob(f"{ROOT}/main/**/*.java", recursive=True):
        text = read(path)
        # ignore string keys that only appear inside comments
        text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
        text = re.sub(r"//[^\n]*", "", text)
        for key in set(re.findall(r"R\.string\.([A-Za-z0-9_]+)", text)):
            if key not in defined:
                problems.append(f"{path}: missing string resource {key}")

    # 2. NavIcon constants
    constants = set(re.findall(r"\b([A-Z][A-Z_0-9]+)=", nav_icon.split("{", 1)[1].split(";", 1)[0]))
    for path in glob.glob(f"{ROOT}/main/**/*.java", recursive=True):
        for name in set(re.findall(r"NavIcon\.([A-Z_0-9]+)", read(path))):
            if name not in constants:
                problems.append(f"{path}: unknown NavIcon.{name}")

    # 3. Setup members
    members = set()
    for stmt in re.findall(r"static\s+final\s+[^;]+;", setup):                     # constants, incl. a,b,c=1,2,3
        members |= set(re.findall(r"([A-Za-z_]\w*)\s*(?:=[^,;]*)?[,;]", stmt))
    members |= set(re.findall(r"static\s+[A-Za-z0-9_<>\[\]]+\s+(\w+)\s*\(", setup))  # helpers
    for name in set(re.findall(r"Setup\.([A-Za-z0-9_]+)", main_activity)):
        if name not in members:
            problems.append(f"MainActivity: unknown Setup.{name}")

    # 4+5. MainActivity fields and methods (tiny parser: it is a single class)
    field_block = main_activity.split("public final class MainActivity extends Activity {", 1)[1]
    fields = set()
    for stmt in re.findall(r"\bprivate\s+(?:final\s+|static\s+)*[^;]+;", field_block, flags=re.S):
        fields |= declared_names(stmt)
    methods = set(re.findall(r"private\s+(?:static\s+)?[A-Za-z0-9_.<>\[\], ]+\s+([A-Za-z0-9_]+)\s*\(", main_activity))
    tests = glob.glob(f"{ROOT}/test/**/*.java", recursive=True)
    for path in tests:
        src = read(path)
        names = set(re.findall(r'\bget\(a,"([A-Za-z0-9_]+)"', src))
        names |= set(re.findall(r'\bset\(a,"([A-Za-z0-9_]+)"', src))
        names |= set(re.findall(r'(?:get|set)\(\s*a\s*,\s*"([A-Za-z0-9_]+)"', src))
        names |= set(re.findall(r'\bget\(a,"([A-Za-z0-9_]+)"\)', src))
        names |= set(re.findall(r'\bset\(a,"([A-Za-z0-9_]+)"', src))
        for name in names:
            if name not in fields:
                problems.append(f"{path}: MainActivity has no field {name}")
    for name in set(re.findall(r"this::([A-Za-z0-9_]+)", main_activity)):
        if name not in methods:
            problems.append(f"MainActivity: unknown method reference {name}()")
    for name in set(re.findall(r'call\(\s*a\s*,\s*"([A-Za-z0-9_]+)"', "".join(read(p) for p in tests))):
        if name not in methods:
            problems.append(f"tests: MainActivity has no method {name}()")

    for p in problems:
        print("PROBLEM", p)
    print(f"checked strings={len(defined)} fields={len(fields)} methods={len(methods)} problems={len(problems)}")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())

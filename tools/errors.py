"""Parse javac output (gradle log) into structured errors and print statistics."""
import collections
import pathlib
import re
import sys

HEAD = re.compile(r"^(?P<file>[A-Za-z]:\\[^:]+\.java):(?P<line>\d+): error: (?P<msg>.*)$")


def parse(log_path):
    lines = pathlib.Path(log_path).read_text(encoding="utf-8", errors="replace").splitlines()
    errs = []
    i = 0
    n = len(lines)
    while i < n:
        m = HEAD.match(lines[i])
        if not m:
            i += 1
            continue
        e = {"file": m.group("file"), "line": int(m.group("line")), "msg": m.group("msg"),
             "src": None, "col": None, "symbol": None, "location": None, "extra": []}
        j = i + 1
        if j < n and not HEAD.match(lines[j]):
            e["src"] = lines[j]
            j += 1
        if j < n and lines[j].strip() == "^":
            caret = lines[j]
            e["col"] = len(caret) - len(caret.lstrip()) if caret.strip() == "^" else None
            # tabs are preserved by javac in the caret line
            e["col"] = caret.index("^")
            j += 1
        while j < n and not HEAD.match(lines[j]) and lines[j].startswith("  "):
            s = lines[j].strip()
            if s.startswith("symbol:"):
                e["symbol"] = s[len("symbol:"):].strip()
            elif s.startswith("location:"):
                e["location"] = s[len("location:"):].strip()
            else:
                e["extra"].append(s)
            j += 1
        errs.append(e)
        i = j
    return errs


if __name__ == "__main__":
    errs = parse(sys.argv[1])
    print("errors:", len(errs))
    kinds = collections.Counter()
    syms = collections.Counter()
    files = collections.Counter()
    for e in errs:
        msg = re.sub(r"'[^']*'", "'X'", e["msg"])
        kinds[msg[:90]] += 1
        files[pathlib.Path(e["file"]).name] += 1
        if e["symbol"]:
            loc = e["location"] or ""
            loc = re.sub(r"^variable \w+ of type ", "", loc)
            loc = re.sub(r"^(class|interface|enum) ", "", loc)
            loc = loc.split(".")[-1]
            syms[e["symbol"] + "  @ " + loc] += 1
    print("\n# kinds")
    for k, v in kinds.most_common(40):
        print(v, k)
    print("\n# symbols")
    top = int(sys.argv[2]) if len(sys.argv) > 2 else 150
    for k, v in syms.most_common(top):
        print(v, k)
    print("\n# files")
    for k, v in files.most_common(25):
        print(v, k)

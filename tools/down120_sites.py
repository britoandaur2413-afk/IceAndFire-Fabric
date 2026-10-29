"""1.20.1, fixes at javac's error sites (python -I tools/down120_sites.py build.log):
- an override 1.21 made protected that 1.20.1 has public: public
- a block's codec() (1.20.1 blocks have none) and its CODEC field: gone
"""
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import errors  # noqa: E402
from jedit import match  # noqa: E402

edits = {}
for e in errors.parse(sys.argv[1]):
    f = pathlib.Path(e["file"])
    if "attempting to assign weaker access privileges" in e["msg"] or ("cannot override" in e["msg"] and "weaker" in (e.get("detail") or "")):
        edits.setdefault(f, set()).add(("public", e["line"]))
    elif "cannot override" in e["msg"]:
        edits.setdefault(f, set()).add(("public", e["line"]))
    elif "does not override" in e["msg"]:
        edits.setdefault(f, set()).add(("override", e["line"]))

for f, todo in edits.items():
    lines = f.read_text(encoding="utf-8").split("\n")
    for kind, n in sorted(todo, key=lambda x: -x[1]):
        if kind == "public":
            lines[n - 1] = re.sub(r"\bprotected\b", "public", lines[n - 1], count=1)
    t = "\n".join(lines)
    # codec() methods and CODEC fields of blocks
    if any(k == "override" for k, _ in todo):
        while True:
            m = re.search(r"\n[ \t]*@Override\s*\n[ \t]*(?:public|protected) (?:@\w+ )?MapCodec<[^>]*(?:<[^>]*>)?[^>]*> codec\(\)\s*\{", t)
            if not m:
                break
            o = t.index("{", m.end() - 1)
            c = match(t, o)
            t = t[:m.start()] + t[c + 1:]
        t = re.sub(r"\n[ \t]*(?:public|private|protected)? ?static final MapCodec<[^;]*?> CODEC = [^;]*;", "", t)
    f.write_text(t, encoding="utf-8")
    print("sites:", f.name, len(todo))

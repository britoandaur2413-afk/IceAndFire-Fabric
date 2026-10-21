"""Print the javac errors of some files, one per line.

usage: python -I tools/errs.py build/compile.log [FileName.java | dir/ ...]
"""
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import errors  # noqa: E402

log = sys.argv[1]
wanted = sys.argv[2:]
for e in errors.parse(log):
    path = pathlib.Path(e["file"])
    name = path.name
    posix = path.as_posix()
    if wanted and not any(name == w or ("/" + w.strip("/") + "/") in posix for w in wanted):
        continue
    print(name, e["line"], e["msg"][:80], "|", e["symbol"] or "", "|", (e["src"] or "").strip()[:120])

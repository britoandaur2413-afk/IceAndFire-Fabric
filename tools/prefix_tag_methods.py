"""The getters the mod adds to the game's CompoundTag (compat/CompoundTagCompat) under names of its own:
getIntOr -> iceandfire_fabric$getIntOr and so on. Another mod that adds a method of the same name to CompoundTag
(ShinColle 1.0.1 did: getIntOr) made the game stop at once (IncompatibleClassChangeError: conflicting default methods).

usage: python -I tools/prefix_tag_methods.py interface   (renames them in CompoundTagCompat)
       python -I tools/prefix_tag_methods.py <javac log>  (renames the calls javac finds on a CompoundTag)
Run the second form after each compile until javac has nothing more to say about them.
"""
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import errors  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent
PREFIX = "iceandfire_fabric$"
NAMES = ["getIntOr", "getBooleanOr", "getFloatOr", "getLongOr", "getDoubleOr", "getByteOr", "getShortOr", "getStringOr",
         "getListOrEmpty", "read", "store", "storeNullable"]


def interface():
    f = ROOT / "src/main/java/com/iafabric/compat/CompoundTagCompat.java"
    t = f.read_text(encoding="utf-8")
    for name in NAMES:
        t = re.sub(r"(default (?:<T> )?[\w<>]+ )" + name + r"\(", lambda m, n=name: m.group(1) + PREFIX + n + "(", t)
    t = re.sub(r"(\n\t\t\t)store\(key, codec, value\);", lambda m: m.group(1) + PREFIX + "store(key, codec, value);", t)
    f.write_text(t, encoding="utf-8")


def calls(log):
    todo = {}
    for e in errors.parse(log):
        if e["msg"] != "cannot find symbol" or not e["symbol"] or not e["location"]:
            continue
        m = re.match(r"method (\w+)\(", e["symbol"])
        if not m or m.group(1) not in NAMES or not e["location"].endswith("CompoundTag"):
            continue
        todo.setdefault(pathlib.Path(e["file"]), []).append((e["line"], e["col"] or 0, m.group(1)))
    n = 0
    for f, sites in todo.items():
        lines = f.read_text(encoding="utf-8").split("\n")
        # right to left on a line, so the columns still hold
        for line, col, name in sorted(sites, key=lambda s: (s[0], -s[1])):
            text = lines[line - 1]
            at = text.find("." + name + "(", max(0, col - 1))
            if at < 0:
                at = text.find("." + name + "(")
            if at < 0:
                print("not found:", f.name, line, name)
                continue
            lines[line - 1] = text[:at + 1] + PREFIX + text[at + 1:]
            n += 1
        f.write_text("\n".join(lines), encoding="utf-8")
    print("calls renamed:", n, "in", len(todo), "files")


if __name__ == "__main__":
    if sys.argv[1] == "interface":
        interface()
    else:
        calls(sys.argv[1])

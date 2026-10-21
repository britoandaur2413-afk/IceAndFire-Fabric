"""Fix javac error sites with simple, message-keyed rules.

usage: python -I tools/fix_sites.py build/compile.log
"""
import collections
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import errors as errparse  # noqa: E402
import jtools  # noqa: E402
import fix_text  # noqa: E402

NBT_DEFAULTS = fix_text.NBT_GETTERS

FIELD_TO_GETTER = {
    ("level", "Entity"): "level()",
    ("onGround", "Entity"): "onGround()",
    ("isClientSide", "Level"): "isClientSide()",
    ("random", "Level"): "getRandom()",
    ("x", "ChunkPos"): "x()",
    ("z", "ChunkPos"): "z()",
    ("pos", "StructureBlockInfo"): "pos()",
    ("state", "StructureBlockInfo"): "state()",
    ("nbt", "StructureBlockInfo"): "nbt()",
    ("selected", "Inventory"): "getSelectedSlot()",
    ("level", "BlockEntity"): "getLevel()",
    ("dimensions", "Entity"): "getDimensions(this.getPose())",
    ("xo", None): None,
}


def ident_at(line, col):
    while col < len(line) and line[col] in ". ":
        col += 1
    m = re.compile(r"[A-Za-z_$][\w$]*").match(line, col)
    return m


def fix_line_nbt(line, wanted):
    # x.getInt("a") -> x.getIntOr("a", 0) for the getters whose Optional is not accepted here
    def repl_one(name):
        nonlocal line
        line = fix_text.rewrite_calls(line, "." + name + "(", lambda a, n=name: rewrite_getter(n, a))

    for name in wanted:
        repl_one(name)
    return line


def rewrite_getter(name, args):
    parts = jtools.split_args(args)
    if len(parts) != 1 or not parts[0]:
        return None
    return "." + name + "Or(" + args + ", " + NBT_DEFAULTS[name] + ")"


def apply(errs):
    by_file = collections.defaultdict(list)
    for e in errs:
        by_file[e["file"]].append(e)
    fixed = 0
    for f, es in by_file.items():
        p = pathlib.Path(f)
        lines = p.read_text(encoding="utf-8").split("\n")
        # process right-to-left within a line so columns stay valid
        es.sort(key=lambda e: (e["line"], -(e["col"] or 0)))
        done = set()
        for e in reversed(es):
            key = (e["line"], e["col"], e["msg"])
            if key in done or e["col"] is None:
                continue
            done.add(key)
            li = e["line"] - 1
            line = lines[li]
            msg = e["msg"]
            if line.lstrip().startswith("import ") and (msg.startswith("cannot find symbol") or "does not exist" in msg):
                lines[li] = "//REMOVED_IMPORT " + line.strip()
                fixed += 1
                continue
            m = re.match(r"(\w+) has (?:private|protected) access in (\w+)", msg)
            if m:
                name, owner = m.group(1), m.group(2)
                rep = FIELD_TO_GETTER.get((name, owner))
                im = ident_at(line, e["col"])
                if rep and im and im.group(0) == name:
                    after = line[im.end():]
                    am = re.match(r"\s*=(?!=)\s*", after)
                    if am and name == "onGround":
                        # assignment: this.onGround = x;  ->  this.setOnGround(x);
                        end = line.find(";", im.end())
                        value = line[im.end() + am.end():end]
                        line = line[:im.start()] + "setOnGround(" + value + ")" + line[end:]
                    elif not am:
                        line = line[:im.start()] + rep + line[im.end():]
                    lines[li] = line
                    fixed += 1
                continue
            if ("constructor BlockPos in class BlockPos cannot be applied" in msg
                    or "possible lossy conversion from double to int" in msg
                    or "possible lossy conversion from float to int" in msg) and "new BlockPos(" in line:
                lines[li] = line.replace("new BlockPos(", "BlockPos.containing(")
                fixed += 1
                continue
            if ("Reference<SoundEvent> cannot be converted to SoundEvent" in msg
                    or "Holder<SoundEvent> cannot be converted to SoundEvent" in msg):
                new = re.sub(r"\b(SoundEvents\.[A-Z0-9_]+)\b(?!\.value\(\))", r"\1.value()", line)
                if new != line:
                    lines[li] = new
                    fixed += 1
                continue
            if "Holder<MobEffect> cannot be converted to MobEffect" in msg or "Holder<Attribute> cannot be converted to Attribute" in msg:
                new = re.sub(r"\b((?:MobEffects|Attributes)\.[A-Z0-9_]+)\b(?!\.value\(\))", r"\1.value()", line)
                if new != line:
                    lines[li] = new
                    fixed += 1
                continue
            if msg.startswith("bad operand types for binary operator") or "bad type in conditional expression" in msg:
                new = fix_line_nbt(line, list(NBT_DEFAULTS))
                if new != line:
                    lines[li] = new
                    fixed += 1
                continue
            if "Optional<CompoundTag> cannot be converted" in msg or "Optional<ListTag> cannot be converted" in msg:
                new = re.sub(r"\.getCompound\(", ".getCompoundOrEmpty(", line)
                new = re.sub(r"\.getList\(([^,()]+), \d+\)", r".getListOrEmpty(\1)", new)
                if new != line:
                    lines[li] = new
                    fixed += 1
                continue
            m = re.match(r"incompatible types: Optional<(Integer|Float|Double|Boolean|Byte|Short|Long|String)> cannot be converted", msg)
            if m:
                lines[li] = fix_line_nbt(line, list(NBT_DEFAULTS))
                if lines[li] != line:
                    fixed += 1
                continue
        text = "\n".join(lines)
        text = re.sub(r"(?m)^//REMOVED_IMPORT [^\n]*\n", "", text)
        p.write_text(text, encoding="utf-8", newline="\n")
    return fixed


if __name__ == "__main__":
    errs = errparse.parse(sys.argv[1])
    print("fixed", apply(errs))

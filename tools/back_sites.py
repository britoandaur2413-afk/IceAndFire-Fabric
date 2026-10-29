"""Fixes at the places javac points to (26.3 -> 1.21.1). Run after a compile:

    python -I tools/back_sites.py build/compile.log
"""
import collections
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import errors  # noqa: E402
from jedit import drop_arg, match, skip_literal  # noqa: E402


def offset(lines, line, col):
    return sum(len(l) + 1 for l in lines[:line - 1]) + col


def expr_end(text, at):
    """End of the expression starting at `at` (the next ',', ')' or ';' outside brackets)."""
    depth = 0
    j = at
    while j < len(text):
        k = skip_literal(text, j)
        if k != j:
            j = k
            continue
        c = text[j]
        if c in "([{":
            depth += 1
        elif c in ")]}":
            if depth == 0:
                return j
            depth -= 1
        elif c in ",;" and depth == 0:
            return j
        j += 1
    return j


def expr_start(text, at):
    """javac points a call's errors at its '(' : the start of the whole expression ending there."""
    j = at
    while j > 0:
        c = text[j - 1]
        if c.isalnum() or c in "_$.":
            j -= 1
        elif c in ")]":
            depth = 0
            k = j - 1
            while k >= 0:
                if text[k] in ")]":
                    depth += 1
                elif text[k] in "([":
                    depth -= 1
                    if depth == 0:
                        break
                k -= 1
            j = k
        else:
            break
    return j


def fix_one(text, at, e):
    if text[at:at + 1] == "(" and e["msg"].startswith("incompatible types"):
        at = expr_start(text, at)
    """The text with the error at offset `at` fixed, or None."""
    msg, sym, loc = e["msg"], e["symbol"] or "", e["location"] or ""
    rest = text[at:]
    # (target, level) -> ...  in a 1.21.1 Predicate<LivingEntity>
    if msg.startswith("incompatible types: incompatible parameter types in lambda expression"):
        m = re.match(r"\((\w+),\s*\w+\)(\s*->)", rest)
        if m:
            return text[:at] + m.group(1) + m.group(2) + rest[m.end():]
    if msg == "incompatible types: Item cannot be converted to ItemStack":
        end = expr_end(text, at)
        return text[:at] + "new ItemStack(" + text[at:end].strip() + ")" + text[end:]
    if msg == "incompatible types: ItemStack cannot be converted to Item":
        end = expr_end(text, at)
        expr = text[at:end].strip()
        m = re.fullmatch(r"new ItemStack\(([^,]+)\)", expr)
        return text[:at] + (m.group(1) if m else expr + ".getItem()") + text[end:]
    if msg == "cannot find symbol" and sym == "method getMaxY()" and rest.startswith(".getMaxY()"):
        s = expr_start(text, at)
        return text[:s] + "(" + text[s:at] + ".getMaxBuildHeight() - 1)" + rest[len(".getMaxY()"):]
    if msg == "cannot find symbol" and sym == "method getMinY()" and rest.startswith(".getMinY()"):
        return text[:at] + ".getMinBuildHeight()" + rest[len(".getMinY()"):]
    # 26.x calls with the level first, to methods that 1.21.1 takes without it
    if "cannot be applied to given types" in msg:
        req = next((x[len("required:"):].strip() for x in e["extra"] if x.startswith("required:")), None)
        found = next((x[len("found:"):].strip() for x in e["extra"] if x.startswith("found:")), None)
        if found and req is not None:
            f = [s.strip() for s in found.split(",")]
            r = [] if req == "no arguments" else [s.strip() for s in req.split(",")]
            if f[0] in ("ServerLevel", "Level") and len(f) == len(r) + 1 and (not r or r[0] not in ("ServerLevel", "Level")):
                k = text.find("(", at)
                if k >= 0:
                    return drop_arg(text, k, 0)
    if msg == "cannot find symbol" and sym == "method keySet()" and "CompoundTag" in loc and rest.startswith(".keySet()"):
        return text[:at] + ".getAllKeys()" + rest[len(".keySet()"):]
    # entity.is(entityTypeTag): 1.21.1 asks the type
    if msg == "incompatible types: TagKey<EntityType<?>> cannot be converted to Entity":
        k = text.rfind(".is(", 0, at + 1)
        if k >= 0 and text[k + 4:at].strip() == "":
            return text[:k] + ".getType()" + text[k:]
    if msg == "cannot find symbol" and "ChunkPos" in loc:
        for old, new in ((".x()", ".x"), (".z()", ".z"), (".pack()", ".toLong()")):
            if sym == "method " + old[1:] and rest.startswith(old):
                return text[:at] + new + rest[len(old):]
    if msg == "cannot find symbol":
        # SoundEvents.X.value(): 1.21.1 constants are plain SoundEvents
        if sym == "method value()" and "SoundEvent" in loc:
            if rest.startswith(".value()"):
                return text[:at] + rest[len(".value()"):]
        # 26.x Optional getters: x.orElse(y) on what is already the value
        m = re.match(r"method orElse\(", sym)
        if m and re.search(r"(int\[\]|String|class int|long\[\])", loc):
            if rest.startswith(".orElse("):
                close = match(text, at + len(".orElse"))
                return text[:at] + text[close + 1:]
    return None


def main():
    log = sys.argv[1]
    by_file = collections.defaultdict(list)
    for e in errors.parse(log):
        if e["col"] is None:
            continue
        by_file[e["file"]].append(e)
    fixed = 0
    for f, errs in by_file.items():
        path = pathlib.Path(f)
        text = path.read_text(encoding="utf-8")
        lines = text.split("\n")
        # bottom-up so earlier offsets stay valid
        for e in sorted(errs, key=lambda e: (e["line"], e["col"]), reverse=True):
            at = offset(lines, e["line"], e["col"])
            new = fix_one(text, at, e)
            if new is not None:
                text = new
                fixed += 1
        path.write_text(text, encoding="utf-8")
    print("fixed:", fixed)


if __name__ == "__main__":
    main()

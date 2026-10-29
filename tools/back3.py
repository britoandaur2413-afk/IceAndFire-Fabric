"""Call-shape fixes from 26.3 to 1.21.1 (arguments added or changed by 26.x).

usage: python -I tools/back3.py [src dirs...]
"""
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from jedit import args, drop_arg, match  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent


def calls(text, name):
    """Offsets of the '(' of every call of a method with this name, last first."""
    guard = r"(?<![\w$])" if name[0].isalnum() else ""
    return [m.end() - 1 for m in re.finditer(guard + re.escape(name) + r"\(", text)][::-1]


def fix(text):
    # EntityType.create(level, reason) -> create(level)
    for p in calls(text, ".create"):
        spans, close = args(text, p)
        if len(spans) == 2:
            second = text[spans[1][0]:spans[1][1]].strip()
            if "MobSpawnType" in second or re.fullmatch(r"\w*[rR]eason", second):
                text = drop_arg(text, p, 1)
    # knockback(strength, x, z, source, amount[, b]) -> knockback(strength, x, z)
    for p in calls(text, ".knockback"):
        spans, close = args(text, p)
        while len(spans) > 3:
            text = drop_arg(text, p, len(spans) - 1)
            spans, close = args(text, p)
    # cooldowns are per item in 1.21.1
    for p in calls(text, "addCooldown"):
        spans, close = args(text, p)
        if spans:
            first = text[spans[0][0]:spans[0][1]]
            m = re.fullmatch(r"\s*new ItemStack\((.+)\)\s*", first)
            if m and "," not in m.group(1):
                text = text[:spans[0][0]] + m.group(1) + text[spans[0][1]:]
    # startRiding(vehicle, force, sendEvent) -> startRiding(vehicle, force)
    for p in calls(text, "startRiding"):
        spans, close = args(text, p)
        if len(spans) == 3:
            text = drop_arg(text, p, 2)
    # particles: 26.x added "always show" next to "force"
    def boolish(s):
        return s.strip() in ("true", "false") or re.fullmatch(r"\s*!?\w+\s*", s) is not None and not re.fullmatch(r"\s*-?[\d.]+[FfDd]?\s*", s)
    for p in calls(text, "addParticle"):
        spans, close = args(text, p)
        if len(spans) == 9 and all(text[s:e].strip() in ("true", "false") for s, e in spans[1:3]):
            text = drop_arg(text, p, 2)
    for p in calls(text, "sendParticles"):
        spans, close = args(text, p)
        lits = [text[s:e].strip() for s, e in spans]
        if len(spans) == 11 and lits[1] in ("true", "false") and lits[2] in ("true", "false"):
            text = drop_arg(text, p, 2)
            text = drop_arg(text, p, 1)
        elif len(spans) == 12 and lits[2] in ("true", "false") and lits[3] in ("true", "false"):
            text = drop_arg(text, p, 3)
    # DustParticleOptions took a packed color in 26.x
    for p in calls(text, "new DustParticleOptions"):
        spans, close = args(text, p)
        if len(spans) == 2:
            first = text[spans[0][0]:spans[0][1]]
            if "Vector3f" not in first and "toVector3f" not in first:
                text = text[:spans[0][0]] + "net.minecraft.world.phys.Vec3.fromRGB24(" + first.strip() + ").toVector3f()" + text[spans[0][1]:]
    return text


def main():
    dirs = sys.argv[1:] or ["src/main/java", "src/client/java"]
    changed = 0
    for d in dirs:
        for f in (ROOT / d).rglob("*.java"):
            old = f.read_text(encoding="utf-8")
            try:
                new = fix(old)
            except Exception as e:
                print("FAILED", f, e)
                continue
            if new != old:
                f.write_text(new, encoding="utf-8")
                changed += 1
    print("files changed:", changed)


if __name__ == "__main__":
    main()

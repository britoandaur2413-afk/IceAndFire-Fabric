"""1.20.1, second pass (runs again without harm):
- block entities read and write without the registries (ValueInput/ValueOutput get none), container helpers and simple
  containers likewise; a SimpleContainer's items are its field (access widener)
- sounds and effects that 1.21 holds in Holders are plain objects
- step height is no attribute (setMaxUpStep), experience reward takes nothing
- trades cost item stacks
"""
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from jedit import args  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent

SIMPLE = [
    (r"new (ValueInput|ValueOutput)\((\w+), registries\)", r"new \1(\2)"),
    (r"\bSoundEvents\.(\w+)\.value\(\)", r"SoundEvents.\1"),
    (r"\bMobEffects\.(\w+)\.value\(\)", r"MobEffects.\1"),
    (r"\bHolder<MobEffect>", "MobEffect"),
    (r"\bHolder<SoundEvent>", "SoundEvent"),
    (r"\bHolder<Attribute>", "Attribute"),
    (r"\bHolder<Potion>", "Potion"),
    (r"(\w+)\.getAttribute\(Attributes\.STEP_HEIGHT\)\.setBaseValue\(([^;]+)\);", r"\1.setMaxUpStep((float) (\2));"),
    (r"\bgetExperienceReward\(\([^()]*\) this\.level\(\), [^()]+\)", "getExperienceReward()"),
    (r"\.createTag\(this\.level\(\)\.registryAccess\(\)\)", ".createTag()"),
    (r"\b(\w*[iI]nventory)\.getItems\(\)", r"\1.items"),
    (r"\bItemCost\b", "ItemStack"),
    (r"import net\.minecraft\.world\.item\.trading\.ItemStack;\n", ""),
]


def drop_last_arg(t, call, keep):
    """call(a, b, ..., registries) -> the first `keep` arguments, when there are more."""
    out = []
    pos = 0
    for m in re.finditer(re.escape(call) + r"\(", t):
        paren = m.end() - 1
        if paren < pos:
            continue
        spans, close = args(t, paren)
        if len(spans) > keep:
            last = t[spans[-1][0]:spans[-1][1]]
            if re.search(r"registr|[Pp]rovider|lookup|level\(\)\.registryAccess", last):
                out.append(t[pos:spans[keep - 1][1]])
                out.append(")")
                pos = close + 1
    out.append(t[pos:])
    return "".join(out)


def convert(f):
    t = f.read_text(encoding="utf-8")
    o = t
    for a, b in SIMPLE:
        t = re.sub(a, b, t)
    for call, keep in (("ContainerHelper.loadAllItems", 2), ("ContainerHelper.saveAllItems", 3), (".fromTag", 1), (".createTag", 0)):
        t = drop_last_arg(t, call, keep)
    # saveAllItems(tag, items, registries) with three arguments: the third was the registries
    t = re.sub(r"(ContainerHelper\.saveAllItems\([^;]*?), (?:\w+\.)?registries\(\)\)", r"\1)", t)
    if t != o:
        f.write_text(t, encoding="utf-8")
        return True
    return False


if __name__ == "__main__":
    n = 0
    for d in sys.argv[1:] or ["src/main/java", "src/client/java", "src/gametest/java"]:
        for f in (ROOT / d).rglob("*.java"):
            n += convert(f)
    print("down120b:", n, "files")

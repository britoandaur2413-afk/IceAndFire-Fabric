"""1.20.1, third pass (runs again without harm): renamed members and swapped constructors.
- attribute modifiers by id: Modifiers.of / Modifiers.uuid (compat), amount() -> getAmount()
- torches and ore blocks take their properties first, Properties.ofFullCopy -> copy
- names: isSameItemSameComponents, getInBlockState, getDefaultLootTable().location(), loot context killers,
  white smoke, potions in stacks, map decorations, trades without Optional costs, wrapAsHolder
"""
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from jedit import args  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent

SIMPLE = [
    (r"\bItemStack\.isSameItemSameComponents\(", "ItemStack.isSameItemSameTags("),
    (r"\bthis\.getInBlockState\(\)", "this.getFeetBlockState()"),
    (r"\.getDefaultLootTable\(\)\.location\(\)", ".getDefaultLootTable()"),
    (r"\bLootContextParams\.ATTACKING_ENTITY\b", "LootContextParams.KILLER_ENTITY"),
    (r"\bLootContextParams\.DIRECT_ATTACKING_ENTITY\b", "LootContextParams.DIRECT_KILLER_ENTITY"),
    (r"\bParticleTypes\.WHITE_SMOKE\b", "ParticleTypes.CLOUD"),
    (r"\bPotionContents\.createItemStack\(([^,()]+), ([^()]+)\)", r"net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(\1), \2)"),
    (r"import net\.minecraft\.world\.item\.alchemy\.PotionContents;\n", ""),
    (r"\bMapDecorationTypes\.(\w+)", r"net.minecraft.world.level.saveddata.maps.MapDecoration.Type.\1"),
    (r"import net\.minecraft\.world\.level\.saveddata\.maps\.MapDecorationTypes;\n", ""),
    (r"\bBuiltInRegistries\.MOB_EFFECT\.wrapAsHolder\(([^()]+(?:\[[^\]]*\([^()]*\)[^\]]*\])?[^()]*)\)", r"\1"),
    (r"\bBlockBehaviour\.Properties\.ofFullCopy\(", "BlockBehaviour.Properties.copy("),
    (r"\bthis\.setSecondsOnFire\(([0-9.]+)F\)", r"this.setSecondsOnFire((int) \1F)"),
    (r"\.(removeModifier|getModifier|hasModifier)\((IafMod\.id\([^()]*\)|[A-Z_]+_ID|[A-Z_]+|\w+Id)\)", r".\1(com.iafabric.compat.Modifiers.uuid(\2))"),
    (r"\bimport net\.minecraft\.world\.level\.Spawner;\n", ""),
    (r" implements Spawner \{", " {"),
]


def modifiers(t):
    """new AttributeModifier(id, amount, operation) with three arguments -> Modifiers.of(...)."""
    out = []
    pos = 0
    for m in re.finditer(r"new (?:net\.minecraft\.world\.entity\.ai\.attributes\.)?AttributeModifier\(", t):
        paren = m.end() - 1
        if paren < pos:
            continue
        spans, close = args(t, paren)
        if len(spans) == 3:
            out.append(t[pos:m.start()])
            out.append("com.iafabric.compat.Modifiers.of(")
            pos = paren + 1
    out.append(t[pos:])
    return "".join(out)


def swap_first_two(t, pattern, test):
    out = []
    pos = 0
    for m in re.finditer(pattern, t):
        paren = m.end() - 1
        if paren < pos:
            continue
        spans, close = args(t, paren)
        if len(spans) == 2:
            a, b = t[spans[0][0]:spans[0][1]], t[spans[1][0]:spans[1][1]]
            if test(a.strip()):
                out.append(t[pos:spans[0][0]])
                out.append(b.strip() + ", " + a.strip())
                pos = spans[1][1]
    out.append(t[pos:])
    return "".join(out)


def convert(f):
    t = f.read_text(encoding="utf-8")
    o = t
    for a, b in SIMPLE:
        t = re.sub(a, b, t)
    t = modifiers(t)
    # TorchBlock/WallTorchBlock(particle, props) -> (props, particle); DropExperienceBlock(xp, props) -> (props, xp)
    if re.search(r"extends (?:Wall)?TorchBlock|extends DropExperienceBlock", t):
        t = swap_first_two(t, r"\bsuper\(", lambda a: a.startswith("ParticleTypes.") or "UniformInt" in a or "ConstantInt" in a or "Int.of" in a)
    if t != o:
        f.write_text(t, encoding="utf-8")
        return True
    return False


if __name__ == "__main__":
    n = 0
    for d in sys.argv[1:] or ["src/main/java", "src/client/java", "src/gametest/java"]:
        for f in (ROOT / d).rglob("*.java"):
            n += convert(f)
    print("down120d:", n, "files")

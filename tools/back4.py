"""More 26.3 -> 1.21.1 renames: Fabric API 'Level' names, game rules, chunk positions, arrows, swings.

usage: python -I tools/back4.py [src dirs...]
"""
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from jedit import match  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent

SIMPLE = [
    (r"\bServerLevelEvents\b", "ServerWorldEvents"),
    (r"\bEND_LEVEL_TICK\b", "END_WORLD_TICK"),
    (r"\bSTART_LEVEL_TICK\b", "START_WORLD_TICK"),
    (r"\bServerEntityLevelChangeEvents\b", "ServerEntityWorldChangeEvents"),
    (r"\bAFTER_PLAYER_CHANGE_LEVEL\b", "AFTER_PLAYER_CHANGE_WORLD"),
    (r"\bAFTER_ENTITY_CHANGE_LEVEL\b", "AFTER_ENTITY_CHANGE_WORLD"),
    (r"\.getBiomeHolder\(\)", ".getBiomeRegistryEntry()"),
    (r"\.isBrightOutside\(\)", ".isDay()"),
    (r"\b(Blocks|Items)\.IRON_CHAIN\b", r"\1.CHAIN"),
    (r"\bGameRules\.MOB_GRIEFING\b", "GameRules.RULE_MOBGRIEFING"),
    (r"\bGameRules\.MOB_DROPS\b", "GameRules.RULE_DOMOBLOOT"),
    (r"\bGameRules\.ENTITY_DROPS\b", "GameRules.RULE_DOENTITYDROPS"),
    (r"(?m)^import net\.minecraft\.world\.level\.gamerules\.GameRules;", "import net.minecraft.world.level.GameRules;"),
    (r"\bthis\.isInGround\(\)", "this.inGround"),
    (r"\.getSwingAnimation\(", ".getAttackAnim("),
    (r"\bswing\(([\w.]+),\s*SwingAnimation\.\w+,\s*(\w+)\)", r"swing(\1, \2)"),
    (r"(?m)^import net\.minecraft\.world\.item\.component\.SwingAnimation;\n", ""),
    (r"\.getInventory\(\)\.getSelectedItem\(\)", ".getInventory().getSelected()"),
    (r"\.getSelectedSlot\(\)", ".selected"),
    (r"\bsetHomeTo\(", "restrictTo("),
    (r"\bgetHomePosition\(\)", "getRestrictCenter()"),
    (r"\bgetHomeRadius\(\)", "getRestrictRadius()"),
    (r"\bhasHome\(\)", "hasRestriction()"),
    (r"\bclearHome\(\)", "clearRestriction()"),
    (r"\bisWithinHome\(", "isWithinRestriction("),
    (r"\bswing\(([^,()]+(?:\(\))?),\s*SwingAnimation\.\w+,\s*(\w+)\)", r"swing(\1, \2)"),
    (r"\bChunkPos\.pack\(", "ChunkPos.asLong("),
    (r"\bChunkPos\.containing\(", "new ChunkPos("),
]


def biome_resolver(text):
    pat = re.compile(r"(\w+)\.biomeResolver\(\)\.getNoiseBiome\(")
    pos = 0
    while True:
        m = pat.search(text, pos)
        if not m:
            return text
        ctx = m.group(1)
        paren = m.end() - 1
        close = match(text, paren)
        text = (text[:m.start()] + f"{ctx}.biomeSource().getNoiseBiome(" + text[paren + 1:close]
                + f", {ctx}.randomState().sampler())" + text[close + 1:])
        pos = m.start() + 1


def fix(text):
    for pat, rep in SIMPLE:
        text = re.sub(pat, rep, text)
    return biome_resolver(text)


def main():
    dirs = sys.argv[1:] or ["src/main/java", "src/client/java"]
    changed = 0
    for d in dirs:
        for f in (ROOT / d).rglob("*.java"):
            old = f.read_text(encoding="utf-8")
            new = fix(old)
            if new != old:
                f.write_text(new, encoding="utf-8")
                changed += 1
    print("files changed:", changed)


if __name__ == "__main__":
    main()

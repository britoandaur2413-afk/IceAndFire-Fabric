"""26.x color collections, newer blocks, teleports, tickets and Optional tag reads -> 1.21.1.

usage: python -I tools/back6.py [src dirs...]
"""
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from jedit import args, drop_arg  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent

COLLECTIONS = ["CARPET", "STAINED_GLASS_PANE", "STAINED_GLASS", "WALL_BANNER", "BANNER", "BED", "WOOL", "CANDLE", "CANDLE_CAKE",
               "CONCRETE_POWDER", "CONCRETE", "GLAZED_TERRACOTTA", "TERRACOTTA", "SHULKER_BOX", "DYE", "BUNDLE"]


def color_name(c):
    return re.sub(r"([a-z])([A-Z])", r"\1_\2", c).upper()


def colors(text):
    names = "|".join(COLLECTIONS)
    text = re.sub(r"\b(Blocks|Items)\.(" + names + r")\.(\w+)\(\)", lambda m: f"{m.group(1)}.{color_name(m.group(3))}_{m.group(2)}", text)
    text = re.sub(r"\b(Blocks|Items)\.(" + names + r")\.pick\(DyeColor\.(\w+)\)", lambda m: f"{m.group(1)}.{m.group(3)}_{m.group(2)}", text)
    # copper: Blocks.COPPER_BLOCK.waxed().unaffected() and friends
    text = re.sub(r"\bBlocks\.COPPER_BLOCK\.waxed\(\)\.unaffected\(\)", "Blocks.WAXED_COPPER_BLOCK", text)
    text = re.sub(r"\bBlocks\.COPPER_BLOCK\.unaffected\(\)", "Blocks.COPPER_BLOCK", text)
    return text


SIMPLE = [
    # blocks newer than 1.21.1 in "is it a plant" checks: dropped
    (r"\s*\|\|\s*state\.is\(Blocks\.(?:PALE_\w+|CREAKING_HEART|RESIN_\w+|LEAF_LITTER|BUSH|SHORT_DRY_GRASS|TALL_DRY_GRASS|CACTUS_FLOWER|FIREFLY_BUSH|WILDFLOWERS)\)", ""),
    # blocks newer than 1.21.1: the nearest older ones
    (r"\bBlocks\.WILDFLOWERS\b", "Blocks.PINK_PETALS"),
    (r"\bBlocks\.FIREFLY_BUSH\b", "Blocks.FLOWERING_AZALEA"),
    (r"\bBlocks\.WOOL_STAIRS\.pick\(DyeColor\.\w+\)", "Blocks.PURPUR_STAIRS"),
    (r"\bFlowerBedBlock\b", "PinkPetalsBlock"),
    (r"(?m)^import net\.minecraft\.world\.level\.block\.FlowerBedBlock;", "import net.minecraft.world.level.block.PinkPetalsBlock;"),
    (r"\bDirection\.getApproximateNearest\(", "Direction.getNearest("),
    (r"\bRaid\.getOminousBannerInstance\(", "Raid.getLeaderBannerInstance("),
    (r"\.getRespawnData\(\)\.pos\(\)", ".getSharedSpawnPos()"),
    (r"\.isPermanentlyInvulnerable\(\)", ".isInvulnerable()"),
    (r"\.setPermanentlyInvulnerable\(", ".setInvulnerable("),
    (r"\.placeItemBackInInventory\(([^,()]+),\s*Prediction\.\w+\)", r".placeItemBackInInventory(\1)"),
    (r"(?m)^import net\.minecraft\.util\.Prediction;\n", ""),
    (r"\bRelative\.ROTATION\b", "RelativeMovement.ROTATION"),
    (r"\bRelative\.ALL\b", "RelativeMovement.ALL"),
    (r"(?m)^import net\.minecraft\.world\.entity\.Relative;", "import net.minecraft.world.entity.RelativeMovement;"),
    (r"\.addTicketWithRadius\(([^,]+),([^,]+),([^;]+?)\);", r".addRegionTicket(\1,\2,\3, net.minecraft.util.Unit.INSTANCE);"),
    (r"\.removeTicketWithRadius\(([^,]+),([^,]+),([^;]+?)\);", r".removeRegionTicket(\1,\2,\3, net.minecraft.util.Unit.INSTANCE);"),
    # 26.x tag reads give Optionals
    (r"(\w+)\.getLong\(\"(\w+)\"\)\.map\(BlockPos::of\)\.orElse\(null\)", r'(\1.contains("\2") ? BlockPos.of(\1.getLong("\2")) : null)'),
    (r"(\w+)\.getLong\(\"(\w+)\"\)\.orElse\(null\)", r'(\1.contains("\2") ? (Long) \1.getLong("\2") : null)'),
    (r"(\w+)\.getIntArray\(\"(\w+)\"\)\.map\(UUIDUtil::uuidFromIntArray\)\.orElse\(null\)", r'(\1.hasUUID("\2") ? \1.getUUID("\2") : null)'),
]


def teleports(text):
    # teleportTo(level, x, y, z, relatives, yaw, pitch, setCamera) -> 1.21.1 has no last flag
    for m in list(re.finditer(r"\bteleportTo\(", text))[::-1]:
        p = m.end() - 1
        spans, close = args(text, p)
        if len(spans) == 8:
            text = drop_arg(text, p, 7)
    return text


def fix(text):
    text = colors(text)
    for pat, rep in SIMPLE:
        text = re.sub(pat, rep, text)
    return teleports(text)


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

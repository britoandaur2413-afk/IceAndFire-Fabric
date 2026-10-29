"""1.20.1, blocks used by a player: 1.21's useItemOn (with an item) and useWithoutItem (without) become 1.20.1's one
use(state, level, pos, player, hand, hit). The item comes from the hand; "pass to the default interaction" goes on to
what useWithoutItem did (kept as a helper of the block's own). Runs again without harm."""
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from jedit import match  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent

ITEM_ON = re.compile(r"(?:@Override\s*)?(?:public|protected) (?:@\w+ )?ItemInteractionResult useItemOn\(\s*(?:@\w+\s+)?ItemStack (\w+),\s*(?:@\w+\s+)?BlockState (\w+),"
                     r"\s*(?:@\w+\s+)?Level (\w+),\s*(?:@\w+\s+)?BlockPos (\w+),\s*(?:@\w+\s+)?Player (\w+),\s*(?:@\w+\s+)?InteractionHand (\w+),"
                     r"\s*(?:@\w+\s+)?BlockHitResult (\w+)\s*\)")
WITHOUT = re.compile(r"(@Override\s*)?(?:public|protected) (?:@\w+ )?InteractionResult useWithoutItem\(\s*(?:@\w+\s+)?BlockState (\w+),\s*(?:@\w+\s+)?Level (\w+),"
                     r"\s*(?:@\w+\s+)?BlockPos (\w+),\s*(?:@\w+\s+)?Player (\w+),\s*(?:@\w+\s+)?BlockHitResult (\w+)\s*\)")
RESULTS = [
    (r"ItemInteractionResult\.SUCCESS\b", "InteractionResult.SUCCESS"),
    (r"ItemInteractionResult\.CONSUME_PARTIAL\b", "InteractionResult.CONSUME_PARTIAL"),
    (r"ItemInteractionResult\.CONSUME\b", "InteractionResult.CONSUME"),
    (r"ItemInteractionResult\.FAIL\b", "InteractionResult.FAIL"),
    (r"ItemInteractionResult\.SKIP_DEFAULT_BLOCK_INTERACTION\b", "InteractionResult.PASS"),
    (r"ItemInteractionResult\.sidedSuccess\(", "InteractionResult.sidedSuccess("),
]


def convert(f):
    t = f.read_text(encoding="utf-8")
    o = t
    m = ITEM_ON.search(t)
    w = WITHOUT.search(t)
    if m:
        stack, state, level, pos, player, hand, hit = m.groups()
        o_body = t.index("{", m.end())
        c_body = match(t, o_body)
        body = t[o_body + 1:c_body]
        fallback = f"this.useWithoutItem({state}, {level}, {pos}, {player}, {hit})" if w else "InteractionResult.PASS"
        body = re.sub(r"ItemInteractionResult\.PASS_TO_DEFAULT_BLOCK_INTERACTION\b", fallback, body)
        body = re.sub(r"super\.useItemOn\([^;]*\)", f"super.use({state}, {level}, {pos}, {player}, {hand}, {hit})", body)
        for a, b in RESULTS:
            body = re.sub(a, b, body)
        head = (f"@Override\n\tpublic InteractionResult use(BlockState {state}, Level {level}, BlockPos {pos}, Player {player}, "
                f"InteractionHand {hand}, BlockHitResult {hit}) {{\n\t\tItemStack {stack} = {player}.getItemInHand({hand});")
        t = t[:m.start()] + head + body + t[c_body:]
        w = WITHOUT.search(t)
        if w and w.group(1):
            # kept as the block's own helper (no override any more)
            t = t[:w.start()] + t[w.start():w.end()].replace(w.group(1), "", 1) + t[w.end():]
    elif w and w.group(1):
        state, level, pos, player, hit = w.group(2), w.group(3), w.group(4), w.group(5), w.group(6)
        head = (f"@Override\n\tpublic InteractionResult use(BlockState {state}, Level {level}, BlockPos {pos}, Player {player}, "
                f"InteractionHand hand, BlockHitResult {hit})")
        t = t[:w.start()] + head + t[w.end():]
        t = re.sub(r"super\.useWithoutItem\(([^;]*?), (\w+)\)", r"super.use(\1, hand, \2)", t)
    for a, b in RESULTS:
        t = re.sub(a, b, t)
    t = t.replace("import net.minecraft.world.ItemInteractionResult;\n", "")
    if t != o:
        if "import net.minecraft.world.InteractionResult;" not in t:
            t = re.sub(r"^(package [\w.]+;\n)", r"\1\nimport net.minecraft.world.InteractionResult;", t, count=1, flags=re.M)
        if "import net.minecraft.world.InteractionHand;" not in t and "InteractionHand " in t:
            t = re.sub(r"^(package [\w.]+;\n)", r"\1\nimport net.minecraft.world.InteractionHand;", t, count=1, flags=re.M)
        if "import net.minecraft.world.item.ItemStack;" not in t and "ItemStack " in t:
            t = re.sub(r"^(package [\w.]+;\n)", r"\1\nimport net.minecraft.world.item.ItemStack;", t, count=1, flags=re.M)
        f.write_text(t, encoding="utf-8")
        return True
    return False


if __name__ == "__main__":
    n = 0
    for d in sys.argv[1:] or ["src/main/java"]:
        for f in (ROOT / d).rglob("*.java"):
            n += convert(f)
    print("down120c:", n, "files")

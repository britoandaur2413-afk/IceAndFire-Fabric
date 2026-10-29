"""Item methods from 26.3 to 1.21.1: use, onCraftedBy, hurtEnemy, inventoryTick, releaseUsing.

usage: python -I tools/back5.py [src dirs...]
"""
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from jedit import match  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent


def ensure_import(text, fq):
    if re.search(r"(?m)^import " + re.escape(fq) + ";", text):
        return text
    m = re.search(r"(?m)^package [\w.]+;\n", text)
    return text[:m.end()] + "\nimport " + fq + ";" + text[m.end():]


def body_of(text, decl_end):
    o = text.index("{", decl_end)
    return o, match(text, o)


def indent_at(text, i):
    line_start = text.rfind("\n", 0, i) + 1
    return re.match(r"[ \t]*", text[line_start:]).group(0)


def use(text):
    pat = re.compile(r"public InteractionResult use\(\s*(?:final\s+)?Level (\w+),\s*(?:final\s+)?Player (\w+),\s*(?:final\s+)?InteractionHand (\w+)\s*\)")
    changed = False
    pos = 0
    while True:
        m = pat.search(text, pos)
        if not m:
            break
        level, player, hand = m.groups()
        text = text[:m.start()] + f"public InteractionResultHolder<ItemStack> use(Level {level}, Player {player}, InteractionHand {hand})" + text[m.end():]
        o, c = body_of(text, m.start())
        inner = text[o:c]
        held = f"{player}.getItemInHand({hand})"
        for k, v in (("SUCCESS", "success"), ("SUCCESS_SERVER", "success"), ("CONSUME", "consume"), ("PASS", "pass"), ("FAIL", "fail")):
            inner = re.sub(r"return InteractionResult\." + k + r"\.heldItemTransformedTo\((.+?)\);", r"return InteractionResultHolder." + v + r"(\1);", inner)
            inner = re.sub(r"return InteractionResult\." + k + r";", f"return InteractionResultHolder.{v}({held});", inner)
        text = text[:o] + inner + text[c:]
        changed = True
        pos = m.start() + 1
    if changed:
        text = ensure_import(text, "net.minecraft.world.InteractionResultHolder")
        text = ensure_import(text, "net.minecraft.world.item.ItemStack")
    return text


def crafted(text):
    pat = re.compile(r"public void onCraftedBy\(\s*ItemStack (\w+),\s*Player (\w+)\s*\)")
    m = pat.search(text)
    if not m:
        return text
    stack, player = m.groups()
    text = text[:m.start()] + f"public void onCraftedBy(ItemStack {stack}, Level craftLevel, Player {player})" + text[m.end():]
    text = re.sub(r"super\.onCraftedBy\(\s*(\w+),\s*(\w+)\s*\)", r"super.onCraftedBy(\1, craftLevel, \2)", text)
    return ensure_import(text, "net.minecraft.world.level.Level")


def hurt_enemy(text):
    pat = re.compile(r"public void hurtEnemy\(\s*ItemStack (\w+),\s*LivingEntity (\w+),\s*LivingEntity (\w+)\s*\)")
    m = pat.search(text)
    if not m:
        return text
    text = text[:m.start()] + m.group(0).replace("public void", "public boolean") + text[m.end():]
    o, c = body_of(text, m.start())
    inner = text[o:c]
    inner = re.sub(r"\breturn;", "return true;", inner)
    stripped = inner.rstrip()
    last = re.search(r"([^;{}]*;)\s*$", stripped)
    if last and last.group(1).strip().startswith("super.hurtEnemy("):
        inner = inner[:last.start(1)] + inner[last.start(1):].replace("super.hurtEnemy(", "return super.hurtEnemy(", 1)
    elif not (last and last.group(1).strip().startswith("return ")):
        ind = indent_at(text, m.start()) + ("\t" if "\t" in indent_at(text, m.start()) else "    ")
        inner = stripped + f"\n{ind}return true;\n{indent_at(text, m.start())}"
    text = text[:o] + inner + text[c:]
    return text


def inventory_tick(text):
    pat = re.compile(r"public void inventoryTick\(\s*ItemStack (\w+),\s*ServerLevel (\w+),\s*Entity (\w+),\s*(?:@Nullable\s+)?EquipmentSlot (\w+)\s*\)")
    m = pat.search(text)
    if not m:
        return text
    stack, level, entity, slot = m.groups()
    text = text[:m.start()] + f"public void inventoryTick(ItemStack {stack}, Level tickLevel, Entity {entity}, int slotId, boolean isSelected)" + text[m.end():]
    o, c = body_of(text, m.start())
    base = indent_at(text, m.start())
    step = "\t" if "\t" in base else "    "
    ind = base + step
    head = (f"\n{ind}if (!(tickLevel instanceof ServerLevel {level})) {{\n{ind}{step}return;\n{ind}}}"
            f"\n{ind}EquipmentSlot {slot} = com.iafabric.compat.ItemSlots.slotOf({entity}, {stack}, isSelected);")
    text = text[:o + 1] + head + text[o + 1:]
    text = re.sub(r"super\.inventoryTick\(\s*(\w+),\s*(\w+),\s*(\w+),\s*(\w+)\s*\)", r"super.inventoryTick(\1, tickLevel, \3, slotId, isSelected)", text)
    text = ensure_import(text, "net.minecraft.world.level.Level")
    return text


def release_using(text):
    pat = re.compile(r"public boolean releaseUsing\(")
    m = pat.search(text)
    if not m:
        return text
    text = text[:m.start()] + "public void releaseUsing(" + text[m.end():]
    o, c = body_of(text, m.start())
    inner = text[o:c]
    inner = re.sub(r"\breturn (?:true|false|super\.releaseUsing\([^;]*\));", lambda r: "return;" if "super" not in r.group(0) else r.group(0).replace("return ", "") + " return;", inner)
    text = text[:o] + inner + text[c:]
    return text


def fix(text):
    for step in (use, crafted, hurt_enemy, inventory_tick, release_using):
        text = step(text)
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

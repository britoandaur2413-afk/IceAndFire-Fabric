"""Method-level fixes from 26.3 to 1.21.1: signatures that lost or changed parameters, and their calls.

usage: python -I tools/back2.py [src dirs...]
"""
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from jedit import match, args, drop_arg  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent

# methods whose 26.x version takes the ServerLevel first; True = only called on the server
LEVEL_FIRST = {
    "doHurtTarget": True,
    "getBaseExperienceReward": True,
    "dropFromLootTable": True,
    "customServerAiStep": True,
    "shouldDropLoot": True,
    "actuallyHurt": True,
    "kill": True,
    "dropEquipment": True,
    "equipItemIfPossible": True,
    "isInvulnerableTo": False,
}
# calls only (the method is not overridden by the mod)
LEVEL_FIRST_CALLS = {"spawnAtLocation"}

KEYWORDS = {"return", "new", "throw", "case", "else", "assert", "yield"}
LEVELISH = re.compile(r"(?i)\b(?:\w*level\w*|\w*world\w*)\b|\(ServerLevel\)")


def ensure_import(text, fq):
    if re.search(r"(?m)^import " + re.escape(fq) + ";", text):
        return text
    m = re.search(r"(?m)^package [\w.]+;\n", text)
    return text[:m.end()] + "\nimport " + fq + ";" + text[m.end():]


def is_call(text, i):
    """Whether the name starting at text[i] is a call (not a declaration)."""
    before = text[:i].rstrip()
    if before.endswith(".") or before.endswith("::"):
        return True
    m = re.search(r"([\w>\]]+)$", before)
    if not m:
        return True
    return m.group(1) in KEYWORDS


def body_after(text, close_paren):
    """(open, close) of the method body after the parameter list, or None (abstract/interface)."""
    j = close_paren + 1
    while j < len(text) and text[j] not in "{;":
        j += 1
    if j >= len(text) or text[j] == ";":
        return None
    return j, match(text, j)


def indent_at(text, i):
    line_start = text.rfind("\n", 0, i) + 1
    m = re.match(r"[ \t]*", text[line_start:])
    return m.group(0)


def level_first_decls(text):
    for name, server_only in LEVEL_FIRST.items():
        pos = 0
        while True:
            m = re.compile(r"\b" + name + r"\(\s*(?:final\s+)?ServerLevel\s+(\w+)\s*(,\s*)?").search(text, pos)
            if not m:
                break
            if is_call(text, m.start()):
                pos = m.end()
                continue
            var = m.group(1)
            paren = m.start() + len(name)
            text = text[:paren + 1] + text[m.end():]
            close = match(text, paren)
            b = body_after(text, close)
            if b and re.search(r"\b" + var + r"\b", text[b[0]:b[1]]):
                ind = indent_at(text, b[0]) + "\t"
                if server_only:
                    line = f"\n{ind}ServerLevel {var} = (ServerLevel) this.level();"
                else:
                    line = f"\n{ind}ServerLevel {var} = this.level() instanceof ServerLevel serverLevel ? serverLevel : null;"
                text = text[:b[0] + 1] + line + text[b[0] + 1:]
            pos = paren
    return text


def level_first_calls(text):
    for name in list(LEVEL_FIRST) + list(LEVEL_FIRST_CALLS):
        pos = 0
        while True:
            m = re.compile(r"\b" + name + r"\(").search(text, pos)
            if not m:
                break
            paren = m.end() - 1
            pos = m.end()
            if not is_call(text, m.start()):
                continue
            spans, close = args(text, paren)
            if len(spans) >= 1 and LEVELISH.search(text[spans[0][0]:spans[0][1]]):
                first = text[spans[0][0]:spans[0][1]]
                if "DamageSource" in first or "damageSource" in first or "source" == first.strip():
                    continue
                text = drop_arg(text, paren, 0)
    return text


def hurt_server(text):
    # declarations: hurtServer(ServerLevel level, DamageSource source, float amount) -> hurt(source, amount)
    pat = re.compile(r"\bhurtServer\(\s*(?:final\s+)?ServerLevel\s+(\w+)\s*,\s*(?:final\s+)?DamageSource\s+(\w+)\s*,\s*(?:final\s+)?float\s+(\w+)\s*\)")
    pos = 0
    while True:
        m = pat.search(text, pos)
        if not m:
            break
        if is_call(text, m.start()):
            pos = m.end()
            continue
        lv, src, amt = m.groups()
        text = text[:m.start()] + f"hurt(DamageSource {src}, float {amt})" + text[m.end():]
        close = text.index(")", m.start())
        b = body_after(text, close)
        if b:
            ind = indent_at(text, b[0]) + "\t"
            line = (f"\n{ind}if (!(this.level() instanceof ServerLevel {lv})) {{\n{ind}\treturn super.hurt({src}, {amt});\n{ind}}}")
            text = text[:b[0] + 1] + line + text[b[0] + 1:]
        pos = m.start() + 4
    # calls: x.hurtServer(level, source, amount) -> x.hurt(source, amount)
    pos = 0
    while True:
        m = re.compile(r"\bhurtServer\(").search(text, pos)
        if not m:
            break
        paren = m.end() - 1
        spans, close = args(text, paren)
        if len(spans) == 3:
            text = drop_arg(text, paren, 0)
        text = text[:m.start()] + "hurt" + text[m.start() + len("hurtServer"):]
        pos = m.start() + 4
    return text


def overlay(text):
    pos = 0
    while True:
        m = re.compile(r"\.sendOverlayMessage\(").search(text, pos)
        if not m:
            break
        paren = m.end() - 1
        close = match(text, paren)
        text = text[:m.start()] + ".displayClientMessage(" + text[paren + 1:close] + ", true)" + text[close + 1:]
        pos = m.start() + 1
    return text


def hover_text(text):
    pat = re.compile(r"\bappendHoverText\(\s*ItemStack\s+(\w+),\s*((?:Item\.)?TooltipContext)\s+(\w+),\s*TooltipDisplay\s+(\w+),\s*Consumer<Component>\s+(\w+),\s*TooltipFlag\s+(\w+)\s*\)")
    pos = 0
    changed = False
    while True:
        m = pat.search(text, pos)
        if not m:
            break
        stack, ctxt, ctx, disp, out, flag = m.groups()
        text = text[:m.start()] + f"appendHoverText(ItemStack {stack}, {ctxt} {ctx}, List<Component> {out}, TooltipFlag {flag})" + text[m.end():]
        close = text.index(")", m.start() + len("appendHoverText("))
        # the declaration's own ')' may be inside generics? no: List<Component> has none
        close = match(text, m.start() + len("appendHoverText"))
        b = body_after(text, close)
        if b:
            inner = text[b[0]:b[1]]
            inner = re.sub(r"\b" + out + r"\.accept\(", out + ".add(", inner)
            inner = re.sub(r"\b" + out + r"::accept\b", out + "::add", inner)
            # super.appendHoverText(stack, ctx, display, out, flag): drop the display
            sm = re.search(r"super\.appendHoverText\(", inner)
            if sm:
                inner = drop_arg(inner, sm.end() - 1, 2)
            text = text[:b[0]] + inner + text[b[1]:]
        changed = True
        pos = m.start() + 1
    if changed:
        text = ensure_import(text, "java.util.List")
        text = re.sub(r"(?m)^import net\.minecraft\.world\.item\.component\.TooltipDisplay;\n", "", text)
    return text


USE_ITEM_ON = {
    "InteractionResult.SUCCESS_SERVER": "ItemInteractionResult.SUCCESS",
    "InteractionResult.SUCCESS": "ItemInteractionResult.SUCCESS",
    "InteractionResult.CONSUME": "ItemInteractionResult.CONSUME",
    "InteractionResult.PASS": "ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION",
    "InteractionResult.TRY_WITH_EMPTY_HAND": "ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION",
    "InteractionResult.FAIL": "ItemInteractionResult.FAIL",
}


def use_item_on(text):
    pat = re.compile(r"\bInteractionResult(\s+useItemOn\()")
    pos = 0
    changed = False
    while True:
        m = pat.search(text, pos)
        if not m:
            break
        text = text[:m.start()] + "ItemInteractionResult" + m.group(1) + text[m.end():]
        paren = text.index("(", m.start())
        close = match(text, paren)
        b = body_after(text, close)
        if b:
            inner = text[b[0]:b[1]]
            for k, v in USE_ITEM_ON.items():
                inner = re.sub(r"(?<![\w.])" + re.escape(k) + r"\b", v, inner)
            inner = re.sub(r"\bInteractionResult(\s+\w+\s*=)", r"ItemInteractionResult\1", inner)
            text = text[:b[0]] + inner + text[b[1]:]
        changed = True
        pos = m.start() + 1
    if changed:
        text = ensure_import(text, "net.minecraft.world.ItemInteractionResult")
    return text


def other(text):
    text = re.sub(r"\bconsidersEntityAsAlly\(", "isAlliedTo(", text)
    # InteractionResult values that 1.21.1 lacks (outside useItemOn)
    text = re.sub(r"\bInteractionResult\.SUCCESS_SERVER\b", "InteractionResult.SUCCESS", text)
    text = re.sub(r"\bInteractionResult\.TRY_WITH_EMPTY_HAND\b", "InteractionResult.PASS", text)
    return text


def fix(text):
    text = use_item_on(text)
    text = level_first_decls(text)
    text = level_first_calls(text)
    text = hurt_server(text)
    text = overlay(text)
    text = hover_text(text)
    text = other(text)
    return text


def main():
    dirs = sys.argv[1:] or ["src/main/java", "src/client/java"]
    changed = 0
    for d in dirs:
        for f in (ROOT / d).rglob("*.java"):
            old = f.read_text(encoding="utf-8")
            try:
                new = fix(old)
            except Exception as e:  # report and go on
                print("FAILED", f, e)
                continue
            if new != old:
                f.write_text(new, encoding="utf-8")
                changed += 1
    print("files changed:", changed)


if __name__ == "__main__":
    main()

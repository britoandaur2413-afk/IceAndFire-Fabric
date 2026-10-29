"""Screens from 26.3 to 1.21.1: input events back to plain numbers, blits without render pipelines."""
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from jedit import args, match  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent / "src/client/java"


def body_after(text, i):
    o = text.index("{", i)
    return o, match(text, o)


def mouse(text):
    m = re.search(r"public boolean mouseClicked\(\s*MouseButtonEvent (\w+),\s*boolean (\w+)\s*\)", text)
    if not m:
        return text
    ev, dbl = m.groups()
    text = text[:m.start()] + "public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)" + text[m.end():]
    o, c = body_after(text, m.start())
    inner = text[o:c]
    inner = inner.replace(f"{ev}.x()", "mouseX").replace(f"{ev}.y()", "mouseY").replace(f"{ev}.button()", "mouseButton")
    inner = re.sub(r"super\.mouseClicked\(\s*" + ev + r"\s*,\s*" + dbl + r"\s*\)", "super.mouseClicked(mouseX, mouseY, mouseButton)", inner)
    text = text[:o] + inner + text[c:]
    return re.sub(r"(?m)^import net\.minecraft\.client\.input\.MouseButtonEvent;\n", "", text)


def keys(text):
    m = re.search(r"public boolean keyPressed\(\s*(?:net\.minecraft\.client\.input\.)?KeyEvent (\w+)\s*\)", text)
    if not m:
        return text
    ev = m.group(1)
    text = text[:m.start()] + "public boolean keyPressed(int keyCode, int scanCode, int modifiers)" + text[m.end():]
    o, c = body_after(text, m.start())
    inner = text[o:c]
    inner = re.sub(r"\.matches\(\s*" + ev + r"\s*\)", ".matches(keyCode, scanCode)", inner)
    inner = re.sub(r"super\.keyPressed\(\s*" + ev + r"\s*\)", "super.keyPressed(keyCode, scanCode, modifiers)", inner)
    inner = inner.replace(f"{ev}.key()", "keyCode").replace(f"{ev}.scancode()", "scanCode").replace(f"{ev}.modifiers()", "modifiers")
    text = text[:o] + inner + text[c:]
    return re.sub(r"(?m)^import net\.minecraft\.client\.input\.KeyEvent;\n", "", text)


def blits(text):
    out = []
    pos = 0
    pat = re.compile(r"\.blit\(RenderPipelines\.GUI_TEXTURED,\s*")
    while True:
        m = pat.search(text, pos)
        if not m:
            out.append(text[pos:])
            break
        paren = text.index("(", m.start())
        spans, close = args(text, paren)
        a = [text[s:e].strip() for s, e in spans][1:]
        if len(a) == 9:
            new = ".blit(" + ", ".join(a) + ")"
        elif len(a) == 11:
            tex, x, y, u, v, w, h, rw, rh, tw, th = a
            new = f".blit({tex}, {x}, {y}, {w}, {h}, {u}, {v}, {rw}, {rh}, {tw}, {th})"
        else:
            print("blit with", len(a), "args left:", text[m.start():close + 1][:120])
            new = text[m.start():close + 1]
        out.append(text[pos:m.start()])
        out.append(new)
        pos = close + 1
    text = "".join(out)
    return text


for f in ROOT.rglob("*.java"):
    t = f.read_text(encoding="utf-8")
    o = t
    t = mouse(t)
    t = keys(t)
    t = blits(t)
    t = t.replace("extends Button.Plain {", "extends Button {")
    if "RenderPipelines." not in t:
        t = re.sub(r"(?m)^import net\.minecraft\.client\.renderer\.RenderPipelines;\n", "", t)
    if t != o:
        f.write_text(t, encoding="utf-8")
        print("gui:", f.name)

"""Client renames from 26.3 to 1.21.1: poses, GUI drawing, colors, screens.

usage: python -I tools/cback.py [src dirs...]   (default src/client/java)
"""
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from jedit import args  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent

SIMPLE = [
    (r"\bGuiGraphicsExtractor\b", "GuiGraphics"),
    (r"(?m)^import net\.minecraft\.client\.gui\.GuiGraphics;\n(?=(?:.*\n)*?import net\.minecraft\.client\.gui\.GuiGraphics;)", ""),
    (r"\.pose\(\)\.pushMatrix\(\)", ".pose().pushPose()"),
    (r"\.pose\(\)\.popMatrix\(\)", ".pose().popPose()"),
    (r"\bRenderType\.entityCutoutZOffset\(", "RenderType.entityCutoutNoCullZOffset("),
    (r"\.getDeltaTracker\(\)", ".getTimer()"),
    (r"\bARGB\.", "FastColor.ARGB32."),
    (r"(?m)^import net\.minecraft\.util\.ARGB;", "import net.minecraft.util.FastColor;"),
    (r"\.gui\.screen\(\)", ".screen"),
    (r"\.gui\.setScreen\(", ".setScreen("),
    (r"\bSoundSource\.UI\b", "SoundSource.MASTER"),
    (r"\bg\.outline\(", "g.renderOutline("),
    (r"\b(g|graphics)\.text\(", r"\1.drawString("),
    (r"\b(g|graphics)\.centeredText\(", r"\1.drawCenteredString("),
    (r"\b(g|graphics)\.item\(", r"\1.renderItem("),
    (r"\b(g|graphics)\.setComponentTooltipForNextFrame\(", r"\1.renderComponentTooltip("),
    (r"\b(g|graphics)\.setTooltipForNextFrame\(", r"\1.renderTooltip("),
    (r"\.blitSprite\(RenderPipelines\.GUI_TEXTURED,\s*", ".blitSprite("),
]


def pose_calls(text):
    """g.pose().translate(x, y) / scale(x, y) / rotate(a): the 1.21.1 GUI pose is a 3D PoseStack."""
    out = []
    pos = 0
    pat = re.compile(r"\.pose\(\)\.(translate|scale|rotate)\(")
    while True:
        m = pat.search(text, pos)
        if not m:
            out.append(text[pos:])
            return "".join(out)
        paren = m.end() - 1
        spans, close = args(text, paren)
        a = [text[s:e].strip() for s, e in spans]
        kind = m.group(1)
        if kind == "translate" and len(a) == 2:
            new = f".pose().translate({a[0]}, {a[1]}, 0.0F)"
        elif kind == "scale" and len(a) == 2:
            new = f".pose().scale({a[0]}, {a[1]}, 1.0F)"
        elif kind == "rotate" and len(a) == 1:
            new = f".pose().mulPose(com.mojang.math.Axis.ZP.rotation({a[0]}))"
        else:
            new = text[m.start():close + 1]
        out.append(text[pos:m.start()])
        out.append(new)
        pos = close + 1


def rotations(text):
    """poseStack.rotateDegrees(axis, deg) / rotate(axis, rad) -> mulPose(axis.rotationDegrees(deg))."""
    out = []
    pos = 0
    pat = re.compile(r"\.(rotateDegrees|rotate)\(")
    while True:
        m = pat.search(text, pos)
        if not m:
            out.append(text[pos:])
            return "".join(out)
        paren = m.end() - 1
        spans, close = args(text, paren)
        a = [text[s:e].strip() for s, e in spans]
        if len(a) == 2 and re.match(r"(?:com\.mojang\.math\.)?Axis\.[XYZ][PN]$", a[0]):
            fn = "rotationDegrees" if m.group(1) == "rotateDegrees" else "rotation"
            new = f".mulPose({a[0]}.{fn}({a[1]}))"
        else:
            new = text[m.start():close + 1]
        out.append(text[pos:m.start()])
        out.append(new)
        pos = close + 1


def fix(text):
    for pat, rep in SIMPLE:
        text = re.sub(pat, rep, text)
    text = pose_calls(text)
    return rotations(text)


def main():
    dirs = sys.argv[1:] or ["src/client/java"]
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

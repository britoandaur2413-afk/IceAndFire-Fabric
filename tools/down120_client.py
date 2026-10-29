"""1.20.1, client code (runs again without harm):
- vertex chains: 1.21's addVertex(...).setColor(...).setUv(...)... -> 1.20.1's vertex(...).color(...).uv(...)...endVertex()
  (a Pose is told from a Matrix4f by how the first argument is declared or made; what cannot be told is listed)
- colors: FastColor.ARGB32.colorFromFloat(a, r, g, b) -> Argb.of(a, r, g, b); opaque(c) -> c | 0xFF000000
- screens: 1.21 draws a screen's background from Screen.render; 1.20.1 does not, so render(...) calls it first, and the
  background override (four arguments) is the screen's own (it calls 1.20.1's one-argument background)
"""
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from jedit import args, match  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent

CHAIN = {"setColor": "color", "setUv": "uv", "setOverlay": "overlayCoords", "setLight": "uv2", "setNormal": "normal",
         "setUv1": "overlayCoords", "setUv2": "uv2"}
unknown = []


def kind(t, expr, at):
    """'pose' or 'matrix' for the first argument of addVertex / setNormal, None when it cannot be told."""
    e = expr.strip()
    if re.search(r"\.last\(\)$", e) or re.search(r"\bPose\(\)$", e):
        return "pose"
    if re.search(r"\.pose\(\)$", e) or e.startswith("new Matrix4f"):
        return "matrix"
    if re.fullmatch(r"\w+", e):
        # the nearest declaration before the use
        best = None
        for m in re.finditer(r"(?:PoseStack\.Pose|\bPose|Matrix4f|\bvar)\s+" + re.escape(e) + r"\b", t[:at]):
            best = m
        if best:
            d = best.group(0)
            if d.startswith("Matrix4f"):
                return "matrix"
            if "Pose" in d:
                return "pose"
            rest = t[best.end():best.end() + 120]
            mm = re.match(r"\s*=\s*([^;]+);", rest)
            if mm:
                return kind(t, mm.group(1), best.start())
    return None


def chains(t, name):
    out = []
    pos = 0
    for m in re.finditer(r"\.addVertex\(", t):
        paren = m.end() - 1
        if paren < pos:
            continue
        spans, close = args(t, paren)
        a = [t[s:e].strip() for s, e in spans]
        if len(a) == 4:
            k = kind(t, a[0], m.start())
            if k == "pose":
                head = f".vertex({a[0]}.pose(), {a[1]}, {a[2]}, {a[3]})"
            elif k == "matrix":
                head = f".vertex({a[0]}, {a[1]}, {a[2]}, {a[3]})"
            else:
                unknown.append(f"{name}: addVertex({a[0]}, ...)")
                continue
        elif len(a) == 3:
            head = f".vertex({a[0]}, {a[1]}, {a[2]})"
        else:
            unknown.append(f"{name}: addVertex with {len(a)} arguments")
            continue
        parts = [head]
        j = close + 1
        while True:
            mm = re.match(r"(\s*)\.(\w+)\(", t[j:])
            if not mm or mm.group(2) not in CHAIN:
                break
            p2 = j + mm.end() - 1
            s2, c2 = args(t, p2)
            b = [t[s:e].strip() for s, e in s2]
            meth = mm.group(2)
            if meth == "setNormal" and len(b) == 4:
                k = kind(t, b[0], j)
                if k == "pose":
                    b = [b[0] + ".normal()"] + b[1:]
                else:
                    unknown.append(f"{name}: setNormal({b[0]}, ...)")
            parts.append(f"{mm.group(1)}.{CHAIN[meth]}({', '.join(b)})")
            j = c2 + 1
        parts.append(".endVertex()")
        out.append(t[pos:m.start()])
        out.append("".join(parts))
        pos = j
    out.append(t[pos:])
    return "".join(out)


def screens(t):
    if not re.search(r"extends (?:AbstractContainerScreen|Screen)\b", t):
        return t
    own = re.search(r"(@Override\s*\n\s*)?public void renderBackground\(GuiGraphics (\w+), int (\w+), int (\w+), float (\w+)\)", t)
    if own:
        if own.group(1):
            t = t[:own.start()] + t[own.start() + len(own.group(1)):]
        t = re.sub(r"super\.renderBackground\((\w+), \w+, \w+, \w+\);", r"super.renderBackground(\1);", t)
    r = re.search(r"public void render\(GuiGraphics (\w+), int (\w+), int (\w+), float (\w+)\)\s*\{", t)
    if r:
        g, mx, my, pt = r.groups()
        call = f"this.renderBackground({g}, {mx}, {my}, {pt});" if own else f"this.renderBackground({g});"
        o = r.end() - 1
        c = match(t, o)
        if "this.renderBackground(" not in t[o:c]:
            indent = re.match(r"\n([ \t]*)", t[o + 1:]).group(1)
            t = t[:o + 1] + "\n" + indent + call + t[o + 1:]
    elif "extends AbstractContainerScreen" in t and "void renderBg(" in t:
        # 1.21 dimmed behind a container screen in Screen.render; 1.20.1 screens do it in their own render
        i = t.rstrip().rfind("}")
        t = t[:i] + """
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTicks);
        this.renderTooltip(g, mouseX, mouseY);
    }
}
"""
    return t


def convert(f):
    t = f.read_text(encoding="utf-8")
    o = t
    t = chains(t, f.name)
    t = re.sub(r"\bFastColor\.ARGB32\.colorFromFloat\(", "com.iafabric.client.Argb.of(", t)
    t = re.sub(r"\bFastColor\.ARGB32\.opaque\(([^()]+)\)", r"(\1 | 0xFF000000)", t)
    t = screens(t)
    if t != o:
        f.write_text(t, encoding="utf-8")
        return True
    return False


if __name__ == "__main__":
    n = 0
    for d in sys.argv[1:] or ["src/client/java", "src/gametest/java"]:
        for f in (ROOT / d).rglob("*.java"):
            n += convert(f)
    print("down120_client:", n, "files")
    for u in unknown:
        print("  not told:", u)

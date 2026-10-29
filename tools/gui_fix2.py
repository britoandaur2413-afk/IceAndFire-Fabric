"""Screens, part 2: 26.x extract* methods back to render*, container screens' background, sizes."""
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parent.parent / "src/client/java"

for f in ROOT.rglob("*.java"):
    t = f.read_text(encoding="utf-8")
    o = t
    container = re.search(r"extends AbstractContainerScreen<", t) is not None
    if container:
        t = re.sub(r"(?:public|protected) void extractBackground\(\s*GuiGraphics (\w+),\s*int (\w+),\s*int (\w+),\s*float (\w+)\s*\)",
                   r"protected void renderBg(GuiGraphics \1, float \4, int \2, int \3)", t)
        t = re.sub(r"(?m)^[ \t]*super\.extractBackground\([^;]*\);\n", "", t)
        # 26.x passed the image size to the constructor
        t = re.sub(r"super\(([^;()]+), ([^;()]+), ([^;()]+), (\d+), (\d+)\);",
                   r"super(\1, \2, \3);\n        this.imageWidth = \4;\n        this.imageHeight = \5;", t)
    if "/client/gui/" in f.as_posix() or "/mixin/client/" in f.as_posix() or container:
        t = re.sub(r"\bextractBackground\(", "renderBackground(", t)
        t = re.sub(r"\bextractRenderState\(", "render(", t)
        t = re.sub(r"\bextractLabels\(", "renderLabels(", t)
        t = re.sub(r"\bextractTooltip\(", "renderTooltip(", t)
        t = re.sub(r"\bextractWidget\(", "renderWidget(", t)
        t = re.sub(r"\bextractContents\(", "renderWidget(", t)
    t = re.sub(r"\bInventoryScreen\.extractEntityInInventoryFollowsMouse\(", "InventoryScreen.renderEntityInInventoryFollowsMouse(", t)
    t = re.sub(r"\b(ms|graphics|guiGraphics)\.item\(", r"\1.renderItem(", t)
    t = re.sub(r"\bmc\.gui\.hud\.isHidden\(\)", "mc.options.hideGui", t)
    t = re.sub(r"\bBuiltInRegistries\.(\w+)\.getValue\(", r"BuiltInRegistries.\1.get(", t)
    t = re.sub(r"(\.getIntArray\(\"\w+\"\))\.orElse\(new int\[0\]\)", r"\1", t)
    t = re.sub(r"\bMinecraft\.getInstance\(\)\.hasShiftDown\(\)", "net.minecraft.client.gui.screens.Screen.hasShiftDown()", t)
    if t != o:
        f.write_text(t, encoding="utf-8")
        print("gui2:", f.name)

"""26.x Fabric game tests as 1.21.1 ones: the annotation is the game's (timeoutTicks, template, batch), the class a
FabricGameTest, and the test environments become batches (day: noon, rain: rain; the others were only difficulty normal,
the game test server's own)."""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
BATCHES = {"day": "level.setDayTime(6000);", "rain": "level.setWeatherParameters(0, 24000, true, false);"}


def annotation(m):
    args = m.group(1) or ""
    out = ["template = EMPTY_STRUCTURE"]
    for part in [a.strip() for a in args.split(",") if a.strip()]:
        k, v = [x.strip() for x in part.split("=", 1)]
        if k == "maxTicks":
            out.append(f"timeoutTicks = {v}")
        elif k == "environment":
            name = v.strip('"').split(":")[1]
            if name in BATCHES:
                out.append(f'batch = "{name}"')
        elif k == "skyAccess":
            out.append(f"skyAccess = {v}")
        else:
            raise ValueError(part)
    return "@GameTest(" + ", ".join(out) + ")"


def convert(f):
    t = f.read_text(encoding="utf-8")
    if "net.fabricmc.fabric.api.gametest.v1.GameTest;" not in t:
        return
    t = t.replace("import net.fabricmc.fabric.api.gametest.v1.GameTest;",
                  "import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;\nimport net.minecraft.gametest.framework.GameTest;")
    t = re.sub(r"@GameTest(?:\(([^)]*)\))?", annotation, t)
    t = re.sub(r"(public class \w+)( \{)", r"\1 implements FabricGameTest\2", t, count=1)
    used = [b for b in BATCHES if f'batch = "{b}"' in t]
    if used:
        t = t.replace("import net.minecraft.gametest.framework.GameTest;",
                      "import net.minecraft.gametest.framework.BeforeBatch;\nimport net.minecraft.gametest.framework.GameTest;")
        methods = "".join(
            f'\n\t@BeforeBatch(batch = "{b}")\n\tpublic void {b}Batch(net.minecraft.server.level.ServerLevel level) {{\n\t\t{BATCHES[b]}\n\t}}\n'
            for b in used)
        i = t.index("{", t.index("implements FabricGameTest")) + 1
        t = t[:i] + methods + t[i:]
    f.write_text(t, encoding="utf-8")
    print("gametest:", f.name)


for d in sys.argv[1:] or ["src/gametest/java"]:
    for f in (ROOT / d).rglob("*.java"):
        convert(f)

"""26.x Fabric client tests onto the 1.21.1 adapter (com.iafabric.test.ct): its imports, screens and the HUD the 1.21.1 way."""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
API = "net.fabricmc.fabric.api.client.gametest.v1."


def convert(f):
    t = f.read_text(encoding="utf-8")
    o = t
    t = re.sub(r"import net\.fabricmc\.fabric\.api\.client\.gametest\.v1\.(?:context\.|world\.|)(\w+);",
               r"import com.iafabric.test.ct.\1;", t)
    t = t.replace(API + "context.", "com.iafabric.test.ct.").replace(API, "com.iafabric.test.ct.")
    t = re.sub(r"\bmc\.gui\.screen\(\)", "mc.screen", t)
    t = re.sub(r"\bmc\.gui\.setScreen\(", "mc.setScreen(", t)
    t = re.sub(r"\bmc\.gui\.hud\.isHidden\(\)", "mc.options.hideGui", t)
    t = re.sub(r"\bmc\.gui\.hud\.toggle\(\);", "mc.options.hideGui = !mc.options.hideGui;", t)
    t = re.sub(r"\bmc\.player\.getActivePostEffects\(\)",
               '(mc.gameRenderer.currentEffect() == null ? "none" : mc.gameRenderer.currentEffect().getName())', t)
    t = re.sub(r"\b(pools|structures|items|blocks|entities|features)\.getValue\(", r"\1.get(", t)
    # 26.x game rule names
    for new, old in (("advance_time", "doDaylightCycle"), ("advance_weather", "doWeatherCycle"), ("spawn_mobs", "doMobSpawning")):
        t = t.replace(f'"gamerule {new} ', f'"gamerule {old} ')
    if t != o:
        f.write_text(t, encoding="utf-8")
        print("client test:", f.name)


for name in sys.argv[1:]:
    convert(ROOT / "src/gametest/java/com/iafabric/test" / name)

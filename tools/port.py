"""First pass of the port: copy the 1.18.2 Forge sources (port/orig) into the Fabric source sets with
package renames and plain text rules. Run once per fresh start; later fixes are made on the output.

usage: python -I tools/port.py [--force]
"""
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import jtools  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent
ORIG = ROOT / "port" / "orig"
MAIN = ROOT / "src" / "main" / "java" / "com" / "iceandfire_fabric"
CLIENT = ROOT / "src" / "client" / "java" / "com" / "iceandfire_fabric"

# files that only make sense on the client (outside the client/ folder)
CLIENT_FILES = {"event/ClientEvents.java", "event/PlayerRenderEvents.java"}
# files dropped from the port (Forge-only glue rewritten by hand)
DROP = {"IceAndFire.java", "CommonProxy.java", "client/ClientProxy.java"}


def rewrite_new_rl(text):
    out = []
    i = 0
    key = "new ResourceLocation("
    while True:
        j = text.find(key, i)
        if j < 0:
            out.append(text[i:])
            break
        out.append(text[i:j])
        open_i = j + len(key) - 1
        close = jtools.match_paren(text, open_i)
        if close < 0:
            out.append(text[j:])
            break
        args = jtools.split_args(text[open_i + 1:close])
        if len(args) == 2:
            out.append("Identifier.fromNamespaceAndPath(" + text[open_i + 1:close] + ")")
        else:
            out.append("Identifier.parse(" + text[open_i + 1:close] + ")")
        i = close + 1
    return "".join(out)


TEXT_RULES = [
    (r"\bcom\.github\.alexthe666\.iceandfire\b", "com.iafabric"),
    (r"\bcom\.github\.alexthe666\.citadel\b", "com.iafabric.citadel"),
    (r"\bnet\.minecraft\.resources\.ResourceLocation\b", "net.minecraft.resources.Identifier"),
    (r"\bResourceLocation\b", "Identifier"),
    (r"\bIceAndFire\.MODID\b", "IafMod.MOD_ID"),
    (r"\bIceAndFire\.LOGGER\b", "IafMod.LOGGER"),
    (r"\bimport com\.iceandfire_fabric\.IceAndFire;", "import com.iafabric.IafMod;"),
    (r"\bVector3f\.([XYZ]P)\.rotationDegrees\(", r"com.mojang.math.Axis.\1.rotationDegrees("),
    (r"\bVector3f\.([XYZ]P)\.rotation\(", r"com.mojang.math.Axis.\1.rotation("),
    (r"\bimport com\.mojang\.math\.Vector3f;", "import org.joml.Vector3f;"),
    (r"\bimport com\.mojang\.math\.Vector4f;", "import org.joml.Vector4f;"),
    (r"\bimport com\.mojang\.math\.Matrix4f;", "import org.joml.Matrix4f;"),
    (r"\bimport com\.mojang\.math\.Matrix3f;", "import org.joml.Matrix3f;"),
    (r"\bimport com\.mojang\.math\.Quaternion;", "import org.joml.Quaternionf;"),
    (r"\bQuaternion\b", "Quaternionf"),
    (r"\.mulPose\((com\.mojang\.math\.Axis\.[XYZ]P)\.rotationDegrees\(", r".rotateDegrees(\1, ("),
    (r"\.mulPose\((com\.mojang\.math\.Axis\.[XYZ]P)\.rotation\(", r".rotate(\1, ("),
    (r"@OnlyIn\(Dist\.\w+\)\s*", ""),
    (r"import net\.minecraftforge\.api\.distmarker\.\w+;\n", ""),
    (r"\bMth\.wrapDegrees\b", "Mth.wrapDegrees"),
    (r"\bnet\.minecraft\.world\.entity\.player\.Player\b", "net.minecraft.world.entity.player.Player"),
]

STRING_RULES = [
    (r'"iceandfire:', '"iceandfire_fabric:'),
    (r'"iceandfire"', '"iceandfire_fabric"'),
    (r'"/assets/iceandfire/', '"/assets/iceandfire_fabric/'),
    (r'"iceandfire\.', '"iceandfire_fabric.'),
    (r'"item\.iceandfire\.', '"item.iceandfire_fabric.'),
    (r'"entity\.iceandfire\.', '"entity.iceandfire_fabric.'),
    (r'"block\.iceandfire\.', '"block.iceandfire_fabric.'),
]


def convert(text):
    for pat, rep in TEXT_RULES:
        text = re.sub(pat, rep, text)
    for pat, rep in STRING_RULES:
        text = re.sub(pat, rep, text)
    text = rewrite_new_rl(text)
    # .rotate(Axis.XP, (a)) produced above: the original call closed with "))", leave the extra paren balanced
    return text


def target_for(rel):
    rel_s = rel.as_posix()
    if rel_s.startswith("client/") or rel_s in CLIENT_FILES:
        return CLIENT / rel
    return MAIN / rel


def main():
    force = "--force" in sys.argv
    count = 0
    for src in sorted(ORIG.rglob("*.java")):
        rel = src.relative_to(ORIG)
        if rel.as_posix() in DROP:
            continue
        dst = target_for(rel)
        if dst.exists() and not force:
            continue
        text = src.read_text(encoding="utf-8")
        dst.parent.mkdir(parents=True, exist_ok=True)
        dst.write_text(convert(text), encoding="utf-8", newline="\n")
        count += 1
    print("written", count)


if __name__ == "__main__":
    main()

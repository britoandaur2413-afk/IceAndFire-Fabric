"""Blocks whose 1.21.1 superclass has an abstract codec() get one (a unit codec of the block itself)."""
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parent.parent
ABSTRACT = set("""AbstractBannerBlock AbstractCandleBlock AbstractCauldronBlock AbstractChestBlock AbstractFurnaceBlock
AbstractSkullBlock BaseCoralPlantTypeBlock BaseEntityBlock BaseFireBlock BasePressurePlateBlock BaseRailBlock BaseTorchBlock
BushBlock CrossCollisionBlock DiodeBlock DirectionalBlock FaceAttachedHorizontalDirectionalBlock FallingBlock GrowingPlantBlock
GrowingPlantBodyBlock GrowingPlantHeadBlock HorizontalDirectionalBlock MultifaceBlock PipeBlock RodBlock SignBlock
SpreadingSnowyDirtBlock""".split())

for f in (ROOT / "src/main/java").rglob("*.java"):
    t = f.read_text(encoding="utf-8")
    m = re.search(r"\bpublic\s+(?:abstract\s+)?class\s+\w+(?:<[^>]*>)?\s+extends\s+(\w+)", t)
    if not m or m.group(1) not in ABSTRACT or "abstract class" in m.group(0):
        continue
    if re.search(r"\bcodec\(\)\s*\{", t):
        continue
    sup = m.group(1)
    end = t.rstrip().rfind("}")
    ind = "\t" if "\n\t" in t else "    "
    method = (f"\n{ind}@Override\n{ind}protected MapCodec<? extends {sup}> codec() {{\n{ind}{ind}return MapCodec.unit(this);\n{ind}}}\n")
    t = t[:end].rstrip() + "\n" + method + "}\n"
    if "import com.mojang.serialization.MapCodec;" not in t:
        t = re.sub(r"(?m)^(package [\w.]+;\n)", r"\1\nimport com.mojang.serialization.MapCodec;", t, count=1)
    f.write_text(t, encoding="utf-8")
    print("codec:", f.name, sup)

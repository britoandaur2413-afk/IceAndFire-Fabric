"""Features and structure processors: 26.x interfaces with codec() -> 1.21.1 abstract classes."""
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from jedit import match  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent


def drop_method(text, signature_regex):
    """Removes a method (with its @Override) whose declaration matches."""
    m = re.search(r"(?:[ \t]*@Override\s*\n)?[ \t]*" + signature_regex + r"[^{;]*\{", text)
    if not m:
        return text
    close = match(text, m.end() - 1)
    end = close + 1
    if text[end:end + 1] == "\n":
        end += 1
    return text[:m.start()] + text[end:]


for f in (ROOT / "src/main/java/com/iafabric").rglob("*.java"):
    t = f.read_text(encoding="utf-8")
    old = t
    # features
    if re.search(r"\bimplements\b[^{]*\bFeature\b", t) and "IafFeature" not in t.split("class", 1)[-1][:200]:
        t = re.sub(r"\bimplements Feature, TypedFeature\b", "extends IafFeature implements TypedFeature", t)
        t = re.sub(r"\bimplements TypedFeature, Feature\b", "extends IafFeature implements TypedFeature", t)
        t = re.sub(r"\bimplements Feature\s*\{", "extends IafFeature {", t)
        if t != old:
            t = re.sub(r"(?m)^[ \t]*private final MapCodec<\w+> featureCodec = MapCodec\.unit\(this\);\n\n?", "", t)
            t = drop_method(t, r"public MapCodec<\w+> codec\(\)")
            if "package com.iafabric.world.gen;" not in t:
                t = re.sub(r"(?m)^(package [\w.]+;\n)", r"\1\nimport com.iafabric.world.gen.IafFeature;", t, count=1)
    # processors
    if re.search(r"\bimplements StructureProcessor\b", t):
        name = re.search(r"class (\w+) implements StructureProcessor", t).group(1)
        t = t.replace(f"class {name} implements StructureProcessor", f"class {name} extends StructureProcessor")
        t = drop_method(t, r"public MapCodec<\? extends StructureProcessor> codec\(\)")
        field = name.upper()
        end = t.rstrip().rfind("}")
        t = (t[:end].rstrip() + "\n\n    @Override\n    protected StructureProcessorType<?> getType() {\n"
             f"        return IafProcessors.{field};\n    }}\n}}\n")
        # 26.x passed the template's relative position, 1.21.1 its block info
        t = re.sub(r"(processBlock\(LevelReader \w+, BlockPos \w+, BlockPos \w+, )BlockPos (\w+)(, StructureTemplate\.StructureBlockInfo)",
                   r"\1StructureTemplate.StructureBlockInfo \2Info\3", t)
        m = re.search(r"StructureTemplate\.StructureBlockInfo (\w+)Info, StructureTemplate\.StructureBlockInfo", t)
        if m:
            t = re.sub(r"\b" + m.group(1) + r"\b(?!Info)", m.group(1) + "Info.pos()", t)
            t = t.replace(f"StructureTemplate.StructureBlockInfo {m.group(1)}Info.pos()Info", f"StructureTemplate.StructureBlockInfo {m.group(1)}Info")
    if t != old:
        f.write_text(t, encoding="utf-8")
        print("worldgen:", f.name)

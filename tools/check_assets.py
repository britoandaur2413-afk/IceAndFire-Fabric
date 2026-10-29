"""Static check of the client assets: what would show as a missing (magenta) model or texture.

Follows items/*.json -> models -> parents -> textures and blockstates -> models, against the mod's
files and the list of vanilla models/textures (exported from the client jar into the scratchpad).

usage: python -I tools/check_assets.py <vanilla_models.txt> <vanilla_textures.txt>
"""
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "assets"
NS = "iceandfire_fabric"


def load_list(path, prefix, suffix):
    out = set()
    for line in pathlib.Path(path).read_text(encoding="utf-8").split():
        if line.startswith(prefix) and line.endswith(suffix):
            out.add("minecraft:" + line[len(prefix):-len(suffix)])
    return out


def norm(ref):
    return ref if ":" in ref else "minecraft:" + ref


def main():
    vmodels = load_list(sys.argv[1], "assets/minecraft/models/", ".json")
    vtextures = load_list(sys.argv[2], "assets/minecraft/textures/", ".png")
    problems = []
    seen = {}

    def model_exists(ref):
        ref = norm(ref)
        if ref in vmodels:
            return True
        ns, path = ref.split(":", 1)
        return (ROOT / ns / "models" / (path + ".json")).exists()

    def texture_exists(ref):
        ref = norm(ref)
        if ref in vtextures:
            return True
        ns, path = ref.split(":", 1)
        return (ROOT / ns / "textures" / (path + ".png")).exists()

    def check_model(ref, origin):
        ref = norm(ref)
        if ref in seen:
            return
        seen[ref] = True
        if not model_exists(ref):
            problems.append("missing model %s (from %s)" % (ref, origin))
            return
        if ref in vmodels:
            return
        ns, path = ref.split(":", 1)
        d = json.loads((ROOT / ns / "models" / (path + ".json")).read_text(encoding="utf-8"))
        if "parent" in d:
            check_model(d["parent"], ref)
        for key, tex in d.get("textures", {}).items():
            if tex.startswith("#"):
                continue
            if not texture_exists(tex):
                problems.append("missing texture %s (model %s)" % (tex, ref))
        if "loader" in d:
            problems.append("forge loader in model %s" % ref)

    def walk_definition(node, origin):
        if isinstance(node, dict):
            if node.get("type") == "minecraft:special" and "base" in node:
                check_model(node["base"], origin)
            if node.get("type") == "minecraft:model" and "model" in node:
                check_model(node["model"], origin)
            for v in node.values():
                walk_definition(v, origin)
        elif isinstance(node, list):
            for v in node:
                walk_definition(v, origin)

    for f in sorted((ROOT / NS / "items").glob("*.json")):
        walk_definition(json.loads(f.read_text(encoding="utf-8")), "items/" + f.name)
    for f in sorted((ROOT / NS / "blockstates").glob("*.json")):
        d = json.loads(f.read_text(encoding="utf-8"))
        if "forge_marker" in d:
            problems.append("forge blockstate " + f.name)
        def models_in(node):
            if isinstance(node, dict):
                if "model" in node and isinstance(node["model"], str):
                    yield node["model"]
                for v in node.values():
                    yield from models_in(v)
            elif isinstance(node, list):
                for v in node:
                    yield from models_in(v)
        for m in models_in(d):
            check_model(m, "blockstates/" + f.name)
    for p in problems:
        print(p)
    print(len(problems), "problems")


if __name__ == "__main__":
    main()

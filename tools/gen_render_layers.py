"""Which of the mod's blocks are drawn cut out or see-through, from their textures (as 26.x decides it by itself):
a texture with half-clear pixels makes the block translucent, one with clear pixels cut out. Writes
assets/iceandfire_fabric/render_layers.json, which IafRenderLayers reads on the client. Run again after adding blocks."""
import json
import pathlib
import zipfile

from PIL import Image

TOOLS = pathlib.Path(__file__).resolve().parent
RES = TOOLS.parent / "src/main/resources/assets"
NS = "iceandfire_fabric"
VANILLA_MODELS = TOOLS / "ref/vanilla/assets/minecraft/models"
CLIENT_JAR = pathlib.Path.home() / ".gradle/caches/fabric-loom/1.21.1/minecraft-client.jar"
jar = zipfile.ZipFile(CLIENT_JAR)


def split(rid):
    ns, path = rid.split(":", 1) if ":" in rid else ("minecraft", rid)
    return ns, path


def load_model(rid):
    ns, path = split(rid)
    if ns == "minecraft":
        f = VANILLA_MODELS / (path + ".json")
    else:
        f = RES / ns / "models" / (path + ".json")
    if not f.exists():
        return None
    return json.loads(f.read_text(encoding="utf-8"))


def textures_of(rid, seen=None):
    """The model's texture variables, its parents' under its own."""
    seen = seen or set()
    if rid in seen:
        return {}
    seen.add(rid)
    model = load_model(rid)
    if model is None:
        return {}
    textures = {}
    if "parent" in model:
        textures.update(textures_of(model["parent"], seen))
    textures.update(model.get("textures", {}))
    return textures


def resolve(textures, value, depth=0):
    while isinstance(value, str) and value.startswith("#") and depth < 10:
        value = textures.get(value[1:])
        depth += 1
    return value


def texture_kind(rid):
    ns, path = split(rid)
    if ns == "minecraft":
        try:
            data = jar.open(f"assets/minecraft/textures/{path}.png")
        except KeyError:
            return "solid"
        im = Image.open(data)
    else:
        f = RES / ns / "textures" / (path + ".png")
        if not f.exists():
            return "solid"
        im = Image.open(f)
    alpha = im.convert("RGBA").getchannel("A")
    hist = alpha.histogram()
    if any(hist[1:255]):
        return "translucent"
    if hist[0]:
        return "cutout"
    return "solid"


def models_of(state):
    out = []

    def add(v):
        if isinstance(v, list):
            for x in v:
                add(x)
        elif isinstance(v, dict) and "model" in v:
            out.append(v["model"])

    for v in state.get("variants", {}).values():
        add(v)
    for part in state.get("multipart", []):
        add(part.get("apply"))
    return out


ORDER = {"solid": 0, "cutout": 1, "translucent": 2}
# cut out in the 1.18 mod too (IafClientSetup), whatever their textures say
ORIGINAL_CUTOUT = {"copper_pile", "gold_pile", "silver_pile", "lectern", "podium_oak", "podium_birch", "podium_spruce", "podium_jungle",
                   "podium_acacia", "podium_dark_oak", "pixie_house_mushroom_brown", "pixie_house_mushroom_red", "pixie_house_oak",
                   "pixie_house_birch", "pixie_house_spruce", "pixie_house_dark_oak"}
layers = {"cutout": [], "translucent": []}
for f in sorted((RES / NS / "blockstates").glob("*.json")):
    state = json.loads(f.read_text(encoding="utf-8"))
    kind = "solid"
    for model in set(models_of(state)):
        textures = textures_of(model)
        for key, value in textures.items():
            if key == "particle":
                continue
            tex = resolve(textures, value)
            if isinstance(tex, str):
                k = texture_kind(tex)
                if ORDER[k] > ORDER[kind]:
                    kind = k
    if kind == "solid" and f.stem in ORIGINAL_CUTOUT:
        kind = "cutout"
    if kind != "solid":
        layers[kind].append(f.stem)
(RES / NS / "render_layers.json").write_text(json.dumps(layers, indent=2) + "\n", encoding="utf-8")
print("cutout:", len(layers["cutout"]), "translucent:", len(layers["translucent"]))
print("translucent:", layers["translucent"])

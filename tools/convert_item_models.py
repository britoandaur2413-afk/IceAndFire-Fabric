"""26.x item model definitions (assets/<ns>/items/*.json) as 1.21.1 item models (models/item/*.json):
plain models stay, a different model becomes a parent, special models get builtin/entity (IafSpecialItemRenderer
draws them), range_dispatch and condition properties become overrides (IafItemProperties gives their values).
The items folder goes at the end. Runs again without harm."""
import json
import pathlib
import shutil

ASSETS = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/assets/iceandfire_fabric"
NS = "iceandfire_fabric"
ITEMS = ASSETS / "items"
MODELS = ASSETS / "models/item"
# 26.x properties and the 1.21.1 item properties standing for them
PROPERTIES = {"minecraft:using_item": "minecraft:pulling", "minecraft:use_duration": "minecraft:pull"}


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


def save(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def model_file(model_id):
    ns, path = model_id.split(":", 1)
    assert ns == NS and path.startswith("item/"), model_id
    return MODELS / (path[len("item/"):] + ".json")


def make_builtin(model_id):
    f = model_file(model_id)
    m = load(f)
    if m.get("parent") != "minecraft:builtin/entity":
        save(f, {"parent": "minecraft:builtin/entity", **{k: v for k, v in m.items() if k != "parent"}})


def plain(model):
    assert model["type"] == "minecraft:model", model
    return model["model"]


def overrides(model):
    """The overrides for a range_dispatch or condition model (nested ones add their predicate to the outer one)."""
    kind = model["type"]
    if kind == "minecraft:model":
        return model["model"], []
    if kind == "minecraft:range_dispatch":
        prop = PROPERTIES.get(model["property"], model["property"])
        base, inner = overrides(model["fallback"])
        out = list(inner)
        for e in model["entries"]:
            out.append({"predicate": {prop: e["threshold"]}, "model": plain(e["model"])})
        return base, out
    if kind == "minecraft:condition":
        prop = PROPERTIES.get(model["property"], model["property"])
        base, _ = overrides(model["on_false"])
        true_base, true_overrides = overrides(model["on_true"])
        out = [{"predicate": {prop: 1}, "model": true_base}]
        for o in true_overrides:
            out.append({"predicate": {prop: 1, **o["predicate"]}, "model": o["model"]})
        return base, out
    raise ValueError(kind)


def convert():
    if not ITEMS.exists():
        print("item models: done already")
        return
    for f in sorted(ITEMS.glob("*.json")):
        name = f.stem
        model = load(f)["model"]
        own = MODELS / f"{name}.json"
        kind = model["type"]
        if kind == "minecraft:model":
            if model["model"] != f"{NS}:item/{name}":
                save(own, {"parent": model["model"]})
        elif kind == "minecraft:special":
            base = model["base"]
            make_builtin(base)
            if base != f"{NS}:item/{name}":
                save(own, {"parent": base})
        elif kind == "minecraft:select" and name == "tide_trident":
            # in hand: the trident's own in-hand model, drawn by IafSpecialItemRenderer; elsewhere
            # TideTridentModelMixin shows item/tide_trident_inventory
            save(own, {"parent": "minecraft:item/trident_in_hand", "textures": {"particle": f"{NS}:item/tide_trident"}})
        elif kind in ("minecraft:range_dispatch", "minecraft:condition"):
            base, over = overrides(model)
            m = load(model_file(base))
            for o in over:
                o["predicate"] = {k.replace("minecraft:", ""): v for k, v in o["predicate"].items()}
            save(own, {**m, "overrides": over})
        else:
            raise ValueError(f"{name}: {kind}")
    shutil.rmtree(ITEMS)
    print("item models: converted")


if __name__ == "__main__":
    convert()

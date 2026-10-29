"""The exiled pixies' lantern (their mark where a quest leads): its textures, models, blockstate and item.

A pale mossy stone with a crystal in the color of each of the twenty (her dress's, from gen_exiled_pixies.py), and a
grey crystal for a lantern whose place was reached.

    python tools/gen_exiled_lantern.py
"""
import json
import os
import random

from PIL import Image

ASSETS = 'src/main/resources/assets/iceandfire_fabric/'
DRESS = [0xF29AB8, 0x8EDDBE, 0xB5AEF0, 0xEED27E, 0xA6B9F2, 0xE3A8E0, 0xF6BE9C, 0xCBCBD6, 0x8FDDEB, 0xE3D3B0,
         0xCDA9EE, 0xC8E68E, 0xF4A390, 0xD9AFC2, 0xEEE58E, 0xA8E3A0, 0xBFD3A9, 0x9CCBF0, 0xF7C68A, 0x8FD6CF]


def rgb(c):
    return ((c >> 16) & 255, (c >> 8) & 255, c & 255)


def mix(a, b, t):
    return tuple(int(round(x + (y - x) * t)) for x, y in zip(a, b))


def stone():
    rnd = random.Random(7)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            v = 196 + rnd.randint(-14, 10)
            c = (v, v, v + 6)
            # cracks
            if (x + 2 * y) % 11 == 0 and rnd.random() < 0.6:
                c = mix(c, (120, 120, 130), 0.6)
            # moss, more on the top rows
            if rnd.random() < (0.35 if y < 5 else 0.12):
                c = mix(c, (126, 168, 112), 0.55 + rnd.random() * 0.25)
            img.putpixel((x, y), c + (255,))
    return img


def crystal(color):
    """A faceted crystal: white at its light, its color in its shades (a pastel glow)."""
    img = Image.new('RGBA', (16, 16))
    base = rgb(color)
    for y in range(16):
        for x in range(16):
            # facets: a light diagonal band, darker edges, brighter top
            facet = ((x + y) // 4) % 2
            edge = min(x, 15 - x) / 7.5
            light = 0.25 + 0.45 * edge + 0.2 * (1 - y / 15) + (0.12 if facet else 0)
            light = max(0.0, min(1.0, light))
            c = mix(mix(base, (90, 90, 110), 0.25), base, min(1.0, light * 1.4))
            c = mix(c, (255, 255, 255), max(0.0, light - 0.55) * 1.6)
            if (x - y) % 7 == 0 and 3 < x < 13:
                c = mix(c, (255, 255, 255), 0.55)
            img.putpixel((x, y), c + (255,))
    return img


def face(texture, uv):
    return {"texture": texture, "uv": uv}


def box(frm, to, texture, rotation=None):
    w = to[0] - frm[0]
    h = to[1] - frm[1]
    d = to[2] - frm[2]
    side_x = [frm[0], 16 - to[1], to[0], 16 - frm[1]]
    side_z = [frm[2], 16 - to[1], to[2], 16 - frm[1]]
    top = [frm[0], frm[2], to[0], to[2]]
    element = {
        "from": frm, "to": to,
        "faces": {
            "north": face(texture, side_x), "south": face(texture, side_x),
            "east": face(texture, side_z), "west": face(texture, side_z),
            "up": face(texture, top), "down": face(texture, top),
        },
    }
    if rotation is not None:
        element["rotation"] = rotation
    return element


def model(crystal_texture):
    return {
        "parent": "minecraft:block/block",
        "textures": {
            "stone": "iceandfire_fabric:block/exiled_lantern_stone",
            "crystal": crystal_texture,
            "particle": crystal_texture,
        },
        "elements": [
            box([3, 0, 3], [13, 2, 13], "#stone"),
            box([5, 2, 5], [11, 4, 11], "#stone"),
            box([6.5, 4, 6.5], [9.5, 7, 9.5], "#stone"),
            box([6, 7, 6], [10, 13, 10], "#crystal", {"angle": 45, "axis": "y", "origin": [8, 10, 8]}),
            box([7, 13, 7], [9, 15.5, 9], "#crystal", {"angle": 45, "axis": "y", "origin": [8, 14, 8]}),
        ],
    }


def main():
    tex = ASSETS + 'textures/block/'
    stone().save(tex + 'exiled_lantern_stone.png')
    for i, color in enumerate(DRESS):
        crystal(color).save(tex + 'exiled_lantern_crystal_%d.png' % i)
    crystal(0xC4C4CC).save(tex + 'exiled_lantern_crystal_off.png')
    models = ASSETS + 'models/block/'
    for i in range(len(DRESS)):
        with open(models + 'exiled_lantern_%d.json' % i, 'w', encoding='utf-8') as f:
            json.dump(model('iceandfire_fabric:block/exiled_lantern_crystal_%d' % i), f, indent=2)
    with open(models + 'exiled_lantern_off.json', 'w', encoding='utf-8') as f:
        json.dump(model('iceandfire_fabric:block/exiled_lantern_crystal_off'), f, indent=2)
    variants = {}
    for i in range(len(DRESS)):
        variants['lit=true,variant=%d' % i] = {"model": "iceandfire_fabric:block/exiled_lantern_%d" % i}
        variants['lit=false,variant=%d' % i] = {"model": "iceandfire_fabric:block/exiled_lantern_off"}
    with open(ASSETS + 'blockstates/exiled_lantern.json', 'w', encoding='utf-8') as f:
        json.dump({"variants": variants}, f, indent=2)
    with open(ASSETS + 'items/exiled_lantern.json', 'w', encoding='utf-8') as f:
        json.dump({"model": {"type": "minecraft:model", "model": "iceandfire_fabric:block/exiled_lantern_0"}}, f, indent=2)
    print('lantern assets written')


if __name__ == '__main__':
    os.chdir(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..'))
    main()

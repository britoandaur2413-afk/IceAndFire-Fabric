"""The dread's undead dragons: bare old bones (the ice dragon's skeleton, no hide, no wing membranes) with a green
soul fire in their eyes, nothing like Black Frost (black scales, ice-blue glow).

    python tools/gen_dread_dragon.py
"""
import colorsys
import os

from PIL import Image

MODELS = 'src/main/resources/assets/iceandfire_fabric/textures/models/'


def lum(p):
    return (0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]) / 255.0


def mix(a, b, t):
    return tuple(int(round(x + (y - x) * t)) for x, y in zip(a, b))


def bones():
    src = Image.open(MODELS + 'icedragon/ice_skeleton_5.png').convert('RGBA')
    out = Image.new('RGBA', src.size, (0, 0, 0, 0))
    dark = (54, 50, 40)      # old marrow in the cracks
    ivory = (222, 210, 176)  # weathered bone
    w, h = src.size
    for y in range(h):
        for x in range(w):
            p = src.getpixel((x, y))
            if p[3] == 0:
                continue
            t = lum(p)
            c = mix(dark, ivory, min(1.0, max(0.0, (t - 0.18) / 0.72)))
            # a sickly green in the deepest cracks (the soul that moves them)
            if t < 0.32:
                c = mix(c, (64, 120, 70), 0.35)
            out.putpixel((x, y), c + (p[3],))
    out.save(MODELS + 'dread/dread_dragon.png')


def eyes():
    src = Image.open(MODELS + 'dread/black_frost_eyes.png').convert('RGBA')
    out = Image.new('RGBA', src.size, (0, 0, 0, 0))
    for y in range(src.size[1]):
        for x in range(src.size[0]):
            p = src.getpixel((x, y))
            if p[3] == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(*[c / 255.0 for c in p[:3]])
            # the same glow, as green soul fire
            r, g, b = colorsys.hsv_to_rgb(0.36, min(1.0, 0.55 + s * 0.4), max(0.6, v))
            out.putpixel((x, y), (int(r * 255), int(g * 255), int(b * 255), p[3]))
    out.save(MODELS + 'dread/dread_dragon_eyes.png')


if __name__ == '__main__':
    os.chdir(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..'))
    bones()
    eyes()
    print('dread dragon textures written')

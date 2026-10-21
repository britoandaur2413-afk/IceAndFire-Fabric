"""The twenty exiled pixies' skins, made from the pixies' own (same design, faded half to white).

Each one keeps the pixie's shading: her hair from the blonde pixie's shades, her dress, wings and bands from the blue
pixie's, her eyes and skin as they are drawn. Their colors are about half as strong as a pixie's and drain toward
white downward (the hem of her dress, the tips of her wings): when a pixie loses her owner she loses her color.

    python tools/gen_exiled_pixies.py [preview.png]

writes src/main/resources/assets/iceandfire_fabric/textures/models/pixie/exiled/exiled_0..19.png (and a front view of all of
them, if a preview path is given).
"""
import colorsys
import os
import sys

from PIL import Image

SRC = 'src/main/resources/assets/iceandfire_fabric/textures/models/pixie/'
OUT = SRC + 'exiled/'

# name, hair, dress, eyes (as a pixie's would be: they are faded below)
VARIANTS = [
    ('albia', '#F5F4F8', '#F29AB8', '#E0508A'),
    ('brisa', '#CCEBDC', '#8EDDBE', '#2FA57C'),
    ('ceniza', '#B5B1BA', '#B5AEF0', '#6F5CC9'),
    ('lumi', '#F5F4F8', '#EED27E', '#D49A12'),
    ('selene', '#CDD3DF', '#A6B9F2', '#4C66D1'),
    ('opala', '#BCDEDC', '#E3A8E0', '#1FA9C2'),
    ('nacar', '#EEDC9E', '#F6BE9C', '#D9823F'),
    ('bruma', '#CDD3DF', '#CBCBD6', '#6E7C9A'),
    ('escarcha', '#CFE3F5', '#8FDDEB', '#16A9C6'),
    ('marfil', '#EEDC9E', '#E3D3B0', '#B08A2E'),
    ('lirio', '#DCCDEE', '#CDA9EE', '#9C4FD1'),
    ('rocio', '#CCEBDC', '#C8E68E', '#5BA82E'),
    ('alba', '#E3B49A', '#F4A390', '#DE5E3E'),
    ('perla', '#F0CDBF', '#D9AFC2', '#B85878'),
    ('vela', '#E3B49A', '#EEE58E', '#CFA00E'),
    ('hiedra', '#B5B1BA', '#A8E3A0', '#3E9E5A'),
    ('sauce', '#DCCDEE', '#BFD3A9', '#6F9142'),
    ('cirro', '#CFE3F5', '#9CCBF0', '#2F72CC'),
    ('azahar', '#F0CDBF', '#F7C68A', '#DB7F22'),
    ('ondina', '#BCDEDC', '#8FD6CF', '#23918C'),
]

# where each part's color drains toward white (top: how much is gone at its top row, bottom: at its last row)
PARTS = [
    ((0, 16, 24, 34), 0.00, 0.30),   # body: her dress keeps its color at the top
    ((0, 34, 24, 48), 0.20, 0.40),   # arms and legs (their bands)
    ((0, 48, 24, 58), 0.25, 0.75),   # skirt, its hem palest
    ((28, 20, 40, 48), 0.30, 0.30),  # wings: the color stays at their tips, as a pixie's, half of it
    ((48, 20, 60, 46), 0.30, 0.30),
]
EYE_ROWS = range(8, 16)


def rgb(hex_color):
    return tuple(int(hex_color[i:i + 2], 16) / 255 for i in (1, 3, 5))


def mix(a, b, t):
    return tuple(x + (y - x) * t for x, y in zip(a, b))


def hsv(color):
    return colorsys.rgb_to_hsv(*color)


def tinted(color, amount, light):
    """The color at a strength (0 white, 1 itself), light from 0 (its shadow) to 1 (its light): never grey."""
    h, s, v = hsv(color)
    s = s * max(0.0, min(1.0, amount))
    v = min(1.0, v * (0.86 + 0.14 * light) + (1 - v) * (1 - amount) * 0.9)
    return colorsys.hsv_to_rgb(h, s, v)


def fade_at(x, y):
    for (x0, y0, x1, y1), top, bottom in PARTS:
        if x0 <= x < x1 and y0 <= y < y1:
            return top + (bottom - top) * (y - y0) / max(1, y1 - y0 - 1)
    return 0.3


def main():
    pixies = [Image.open(SRC + 'pixie_%d.png' % i).convert('RGBA') for i in range(5)]
    blonde, blue = pixies[0], pixies[2]
    w, h = blonde.size
    # the blonde hair's shades, to spread them over each one's hair
    hair_values = []
    for y in range(h):
        for x in range(w):
            p = blue.getpixel((x, y))
            if p[3] and colorsys.rgb_to_hsv(*[c / 255 for c in p[:3]])[2] < 0.3:
                hair_values.append(colorsys.rgb_to_hsv(*[c / 255 for c in blonde.getpixel((x, y))[:3]])[2])
    low, high = min(hair_values), max(hair_values)
    os.makedirs(OUT, exist_ok=True)
    made = []
    for index, (name, hair, dress, eyes) in enumerate(VARIANTS):
        hair, dress, eyes = rgb(hair), rgb(dress), rgb(eyes)
        out = Image.new('RGBA', (w, h), (0, 0, 0, 0))
        for y in range(h):
            for x in range(w):
                p = [im.getpixel((x, y)) for im in pixies]
                if p[0][3] == 0:
                    continue
                alpha = p[0][3]
                shared = all(abs(p[0][c] - q[c]) < 6 for q in p for c in range(3))
                bh, bs, bv = colorsys.rgb_to_hsv(*[c / 255 for c in p[2][:3]])
                if shared:
                    # skin and whites: a little paler, a little colder
                    c = mix([v / 255 for v in p[0][:3]], (0.97, 0.96, 1.0), 0.25)
                elif bv < 0.3:
                    # hair, shaded like the blonde pixie's
                    t = (colorsys.rgb_to_hsv(*[c / 255 for c in blonde.getpixel((x, y))[:3]])[2] - low) / max(0.01, high - low)
                    hh, hs, hv = hsv(hair)
                    c = colorsys.hsv_to_rgb(hh, min(1.0, hs * (1.25 - 0.35 * t)), hv * (0.82 + 0.18 * t))
                elif y in EYE_ROWS and x < 32:
                    # her eyes (and the flower in her hair): faded too, but still the color in her
                    c = tinted(eyes, 0.85, bv)
                else:
                    # dress, wings and bands: as strong as the blue pixie's is there, draining downward
                    c = tinted(dress, min(1.0, bs * 1.25) * (1 - fade_at(x, y)), bv)
                out.putpixel((x, y), tuple(int(round(max(0, min(1, v)) * 255)) for v in c) + (alpha,))
        out.save(OUT + 'exiled_%d.png' % index)
        made.append(out)
    if len(sys.argv) > 1:
        preview(made, sys.argv[1])
    print('wrote', len(made), 'skins to', OUT)


def preview(skins, path, scale=6):
    """A front view of each (face, body, arms, legs, skirt, wings), in a row, with the pixies' own for comparison."""
    originals = [Image.open(SRC + 'pixie_%d.png' % i).convert('RGBA') for i in range(5)]
    doll_w, doll_h = 26, 40
    allskins = originals + skins
    sheet = Image.new('RGBA', (len(allskins) * (doll_w + 2) * scale, doll_h * scale), (70, 74, 84, 255))
    for i, skin in enumerate(allskins):
        doll = Image.new('RGBA', (doll_w, doll_h), (0, 0, 0, 0))

        def put(box, at):
            doll.alpha_composite(skin.crop(box), at)

        put((30, 22, 36, 46), (1, 9))     # left wing behind
        put((50, 22, 56, 42), (19, 9))    # right wing behind
        put((8, 8, 16, 16), (9, 0))       # face
        put((4, 20, 10, 30), (10, 8))     # body
        put((2, 36, 4, 48), (8, 9))       # arms
        put((2, 36, 4, 48), (16, 9))
        put((4, 52, 12, 58), (9, 18))     # skirt
        put((12, 36, 14, 48), (10, 24))   # legs
        put((12, 36, 14, 48), (14, 24))
        big = doll.resize((doll_w * scale, doll_h * scale), Image.NEAREST)
        sheet.alpha_composite(big, (i * (doll_w + 2) * scale, 0))
    sheet.save(path)


if __name__ == '__main__':
    main()

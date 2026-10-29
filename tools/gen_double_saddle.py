"""Draws the double dragon saddle: the texture of its model on the dragon (LayerDragonSaddle) and its item's icon.
Run from the project folder:
    python tools/gen_double_saddle.py

The model's texture space is 64x32 and its picture 128x64: two texels a model unit, as the dragons' own skins and
armor (512x256 pictures on 256x128 models). Every box is sized in half units, so each face is whole texels. Faces are
shaded softly within themselves (a padded seat lighter in its middle, a strap lit on one edge), never framed by a
dark line round their edges."""
import random

from PIL import Image

ASSETS = 'src/main/resources/assets/iceandfire_fabric/textures/'
SCALE = 2

LEATHER = {'hi': (198, 134, 76), 'li': (176, 114, 60), 'mi': (150, 94, 47), 'sh': (124, 75, 37), 'dk': (96, 57, 28)}
STRAP = {'hi': (122, 80, 43), 'mi': (98, 62, 32), 'sh': (78, 47, 24), 'dk': (58, 34, 17)}
BRASS = {'hi': (242, 208, 112), 'mi': (206, 163, 67), 'sh': (152, 113, 39)}
CLOTH = {'hi': (170, 46, 47), 'li': (148, 35, 39), 'mi': (126, 28, 32), 'sh': (100, 21, 25), 'dk': (72, 14, 18)}
GOLD = {'hi': (250, 216, 110), 'mi': (216, 172, 68), 'sh': (162, 121, 41)}

# the boxes as LayerDragonSaddle makes them: texture corner (u, v) and size (w, h, d), in the model's texture space
FRONT_CLOTH = (0, 0, 6.5, 2.5, 3)
BACK_CLOTH = (0, 6, 7.5, 2.5, 4.5)
CUSHION = (26, 0, 3, 0.5, 2.5)
CANTLE = (26, 4, 2.5, 1, 0.5)
POMMEL = (34, 4, 1, 0.5, 0.5)
FRONT_STRAP = (0, 14, 7, 7.5, 1)
BACK_STRAP = (0, 23, 8, 6.5, 1)


def faces(box):
    """A box's faces in the picture (the game's box mapping, in texels): (x0, y0, x1, y1) each."""
    u, v, w, h, d = (round(n * SCALE) for n in box)
    side = v + d
    return {
        'top': (u + d, v, u + d + w, v + d),
        'bottom': (u + d + w, v, u + d + 2 * w, v + d),
        'west': (u, side, u + d, side + h),
        'front': (u + d, side, u + d + w, side + h),
        'east': (u + d + w, side, u + 2 * d + w, side + h),
        'back': (u + 2 * d + w, side, u + 2 * d + 2 * w, side + h),
    }


def put(img, x, y, color, rnd, grain=5):
    # a little grain in every texel: leather and cloth are never flat
    n = rnd.randint(-grain, grain)
    img.putpixel((x, y), tuple(max(0, min(255, c + n)) for c in color) + (255,))


def fill(img, rect, color_at, rnd, grain=5):
    x0, y0, x1, y1 = rect
    for y in range(y0, y1):
        for x in range(x0, x1):
            put(img, x, y, color_at(x - x0, y - y0, x1 - x0, y1 - y0), rnd, grain)


def weave(x, y, w, h):
    """Crimson twill: fine diagonal ribs, a light one every fourth texel (soft: the cloth, not a pattern on it)."""
    return CLOTH['li'] if (x - y) % 4 == 0 else CLOTH['mi']


def cloth_side(studs):
    """A side of a cloth: the twill, a gold braid along its hem (two rows), a row of small gold studs over it."""
    def at(x, y, w, h):
        if y == h - 2:
            return GOLD['hi'] if x % 2 == 0 else GOLD['mi']
        if y == h - 1:
            return GOLD['mi'] if x % 2 == 1 else GOLD['sh']
        if studs and y == 1 and (x - (w % 3) // 2) % 3 == 1:
            return GOLD['hi']
        color = weave(x, y, w, h)
        return CLOTH['sh'] if y == h - 3 and color == CLOTH['mi'] else color
    return at


def cushion_top(x, y, w, h):
    """A padded seat: lighter toward its middle, a little darker toward its edges (no line round it)."""
    cx, cy = (w - 1) / 2, (h - 1) / 2
    d = max(abs(x - cx) / (w / 2), abs(y - cy) / (h / 2))
    if d < 0.45:
        return LEATHER['hi']
    if d < 0.8:
        return LEATHER['li']
    corner = (x in (0, w - 1)) and (y in (0, h - 1))
    return LEATHER['sh'] if corner else LEATHER['mi']


def strap_side(buckle_at):
    """A strap down the dragon's side: lit on one edge, a brass buckle a little below the cloth's hem."""
    def at(x, y, w, h):
        if buckle_at <= y < buckle_at + 4:
            k = y - buckle_at
            if k == 0:
                return BRASS['hi'] if x == 0 else BRASS['mi']
            if k == 3:
                return BRASS['sh']
            return BRASS['mi'] if x == 0 else STRAP['dk']
        return STRAP['hi'] if x == 0 else STRAP['mi']
    return at


def model(rnd):
    img = Image.new('RGBA', (64 * SCALE, 32 * SCALE), (0, 0, 0, 0))
    for box in (FRONT_CLOTH, BACK_CLOTH):
        f = faces(box)
        fill(img, f['top'], weave, rnd)
        fill(img, f['bottom'], lambda x, y, w, h: CLOTH['dk'], rnd, 3)
        for side in ('west', 'east'):
            fill(img, f[side], cloth_side(True), rnd)
        for side in ('front', 'back'):
            fill(img, f[side], cloth_side(False), rnd)
    f = faces(CUSHION)
    fill(img, f['top'], cushion_top, rnd, 4)
    fill(img, f['bottom'], lambda x, y, w, h: LEATHER['dk'], rnd, 3)
    for side in ('west', 'front', 'east', 'back'):
        fill(img, f[side], lambda x, y, w, h: LEATHER['sh'], rnd, 4)
    f = faces(CANTLE)
    # its back: a rolled top edge, lit; its face to the rider lighter than its back
    fill(img, f['top'], lambda x, y, w, h: LEATHER['hi'] if 0 < x < w - 1 else LEATHER['li'], rnd, 4)
    fill(img, f['front'], lambda x, y, w, h: LEATHER['li'] if y == 0 else LEATHER['mi'], rnd, 4)
    fill(img, f['back'], lambda x, y, w, h: LEATHER['mi'] if y == 0 else LEATHER['sh'], rnd, 4)
    for side in ('west', 'east'):
        fill(img, f[side], lambda x, y, w, h: LEATHER['mi'] if y == 0 else LEATHER['sh'], rnd, 4)
    fill(img, f['bottom'], lambda x, y, w, h: LEATHER['dk'], rnd, 3)
    f = faces(POMMEL)
    # its horn, capped in brass
    fill(img, f['top'], lambda x, y, w, h: BRASS['hi'] if x == 0 else BRASS['mi'], rnd, 3)
    for side in ('west', 'front', 'east', 'back'):
        fill(img, f[side], lambda x, y, w, h: LEATHER['li'] if side == 'front' else LEATHER['mi'], rnd, 4)
    fill(img, f['bottom'], lambda x, y, w, h: LEATHER['dk'], rnd, 3)
    for box in (FRONT_STRAP, BACK_STRAP):
        f = faces(box)
        fill(img, f['top'], lambda x, y, w, h: STRAP['hi'] if y == 0 else STRAP['mi'], rnd, 4)
        fill(img, f['bottom'], lambda x, y, w, h: STRAP['sh'], rnd, 3)
        for side in ('west', 'east'):
            fill(img, f[side], strap_side(6), rnd, 4)
        for side in ('front', 'back'):
            fill(img, f[side], lambda x, y, w, h: STRAP['mi'] if y % 3 else STRAP['sh'], rnd, 3)
    img.save(ASSETS + 'models/misc/double_dragon_saddle.png')


# the icon: two seats on one cloth, side on (its front to the left), a stirrup under each. Each column of a seat is
# leather from its top (the seat's line: its horn in front, low in the middle, its back high behind) down to the cloth.
SEAT_TOPS = [3, 5, 6, 6, 5, 3, 2]
ICON_MATERIALS = {
    # material: (lit, middle, shadow, edge)
    'leather': (LEATHER['hi'], LEATHER['mi'], LEATHER['sh'], (70, 41, 20)),
    'cloth': (CLOTH['hi'], CLOTH['mi'], CLOTH['sh'], (62, 12, 16)),
    'trim': (GOLD['hi'], GOLD['mi'], GOLD['sh'], (112, 82, 26)),
    'strap': (STRAP['hi'], STRAP['mi'], STRAP['sh'], (44, 26, 13)),
    'iron': ((214, 214, 214), (170, 170, 170), (128, 128, 128), (70, 70, 70)),
}


def icon():
    shape = {}
    for start in (0, 8):
        for i, top in enumerate(SEAT_TOPS):
            for y in range(top, 8):
                shape[(start + i, y)] = 'leather'
    for x in range(0, 16):
        for y in (8, 9):
            shape[(x, y)] = 'cloth'
        shape[(x, 10)] = 'trim'
    for x in (3, 11):
        shape[(x, 11)] = 'strap'
        shape[(x, 12)] = 'strap'
        # the stirrup: its bar under the strap, its sides, its tread
        for dx in (-1, 0, 1):
            shape[(x + dx, 13)] = 'iron'
            shape[(x + dx, 15)] = 'iron'
        shape[(x - 1, 14)] = shape[(x + 1, 14)] = 'iron'
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for (x, y), material in shape.items():
        lit, middle, shadow, edge = ICON_MATERIALS[material]
        same = lambda dx, dy: shape.get((x + dx, y + dy)) == material
        if material in ('trim', 'iron', 'strap'):
            # (thin: a band of one color, darker at its ends and below)
            color = edge if not shape.get((x - 1, y)) or not shape.get((x + 1, y)) else lit if material == 'trim' else middle
            if material == 'trim' and x % 2 == 1 and edge != color:
                color = middle
            if material == 'iron' and y == 15:
                color = shadow
        elif not shape.get((x, y - 1)):
            # its top catches the light
            color = lit
        elif not shape.get((x + 1, y)):
            # (the light comes from the left: its right side is dark, its left side not)
            color = edge
        elif not shape.get((x - 1, y)):
            color = shadow
        elif not same(0, -2):
            color = middle
        elif not same(0, 1):
            color = shadow
        else:
            color = middle
        img.putpixel((x, y), color + (255,))
    img.save(ASSETS + 'item/double_dragon_saddle.png')


if __name__ == '__main__':
    model(random.Random(7))
    icon()
    print('double saddle: model texture (128x64) and icon')

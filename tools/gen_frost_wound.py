"""The Frost Cauterization effect's icon (18x18): a heart half frozen, its ice cracking into it, in the outline of the
Frost Ward's icon. Run from the project folder: python tools/gen_frost_wound.py"""
from PIL import Image

OUT = 'src/main/resources/assets/iceandfire_fabric/textures/mob_effect/frost_wound.png'
# the heart's shape (1) on 18x18
SHAPE = [
    '..................',
    '..................',
    '...#####..#####...',
    '..#######.#######.',
    '.################.',
    '.################.',
    '.################.',
    '.################.',
    '..##############..',
    '...############...',
    '....##########....',
    '.....########.....',
    '......######......',
    '.......####.......',
    '........##........',
    '..................',
    '..................',
    '..................',
]
OUTLINE = (24, 22, 48, 255)
RED = [(168, 26, 40, 255), (204, 42, 54, 255), (232, 80, 84, 255)]
ICE = [(70, 140, 210, 255), (120, 188, 236, 255), (210, 240, 255, 255)]
CRACK = (240, 252, 255, 255)


def main():
    img = Image.new('RGBA', (18, 18), (0, 0, 0, 0))
    inside = {(x, y) for y, row in enumerate(SHAPE) for x, c in enumerate(row) if c == '#'}
    for x, y in inside:
        # the ice creeps in from the upper right, a jagged edge
        frozen = x + (y * 0.55) > 10.5 + (1 if (x * 7 + y * 3) % 5 == 0 else 0)
        if frozen:
            shade = ICE[1] if (x + y) % 3 else ICE[0]
            if y <= 4 and x >= 12:
                shade = ICE[2]
        else:
            shade = RED[1]
            if x <= 3 or y >= 11:
                shade = RED[0]
            if (x, y) in {(3, 4), (4, 4), (3, 5)}:
                shade = RED[2]
        img.putpixel((x, y), shade)
    # its crack, from the ice into the red
    for x, y in [(11, 5), (10, 6), (10, 7), (9, 8), (8, 9), (9, 10)]:
        img.putpixel((x, y), CRACK)
    # a little snowflake on the ice
    for x, y in [(14, 5), (13, 6), (14, 6), (15, 6), (14, 7)]:
        img.putpixel((x, y), ICE[2])
    # the outline: every empty pixel next to the heart
    for y in range(18):
        for x in range(18):
            if (x, y) in inside:
                continue
            if any((x + dx, y + dy) in inside for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                img.putpixel((x, y), OUTLINE)
    img.save(OUT)
    print('written', OUT)


if __name__ == '__main__':
    main()

"""The exiled pixie's waypoint pin (textures/gui/exiled_pin.png): cells of 64 pixels, drawn white to be tinted.

0 the pin (a drop with a round head), 1 its dark outline, 2 the arrow at the screen's edge, 3 her mark (a little
pixie's wings) for the head of the pin, 4 a chest (a hidden chest she shows).

    python tools/gen_exiled_pin.py
"""
import os

from PIL import Image, ImageDraw

CELL = 64
SS = 4  # drawn bigger, made smaller: soft edges


def canvas():
    return Image.new('L', (CELL * SS, CELL * SS), 0)


def s(v):
    return int(round(v * SS))


def pin_mask(grow=0.0):
    img = canvas()
    d = ImageDraw.Draw(img)
    r = 19 + grow
    cx, cy = 32, 26
    d.ellipse([s(cx - r), s(cy - r), s(cx + r), s(cy + r)], fill=255)
    d.polygon([(s(cx - r * 0.72), s(cy + r * 0.62)), (s(cx + r * 0.72), s(cy + r * 0.62)), (s(cx), s(60.5 + grow * 0.8))], fill=255)
    return img.resize((CELL, CELL), Image.LANCZOS)


def arrow_mask():
    img = canvas()
    d = ImageDraw.Draw(img)
    d.polygon([(s(16), s(14)), (s(54), s(32)), (s(16), s(50)), (s(26), s(32))], fill=255)
    return img.resize((CELL, CELL), Image.LANCZOS)


def wings_mask():
    img = canvas()
    d = ImageDraw.Draw(img)
    cx, cy = 32, 26
    # four wings, a body
    d.ellipse([s(cx - 15), s(cy - 13), s(cx - 2), s(cy + 1)], fill=255)
    d.ellipse([s(cx + 2), s(cy - 13), s(cx + 15), s(cy + 1)], fill=255)
    d.ellipse([s(cx - 12), s(cy + 1), s(cx - 2), s(cy + 10)], fill=255)
    d.ellipse([s(cx + 2), s(cy + 1), s(cx + 12), s(cy + 10)], fill=255)
    d.ellipse([s(cx - 2.6), s(cy - 9), s(cx + 2.6), s(cy + 11)], fill=0)
    d.ellipse([s(cx - 1.8), s(cy - 8), s(cx + 1.8), s(cy + 9)], fill=255)
    d.ellipse([s(cx - 3), s(cy - 14), s(cx + 3), s(cy - 8)], fill=255)
    return img.resize((CELL, CELL), Image.LANCZOS)


def chest_mask():
    img = canvas()
    d = ImageDraw.Draw(img)
    cx, cy = 32, 27
    d.rounded_rectangle([s(cx - 13), s(cy - 10), s(cx + 13), s(cy + 10)], radius=s(3), fill=255)
    d.rectangle([s(cx - 13), s(cy - 2), s(cx + 13), s(cy)], fill=0)
    d.rectangle([s(cx - 3), s(cy - 4), s(cx + 3), s(cy + 3)], fill=255)
    return img.resize((CELL, CELL), Image.LANCZOS)


def main():
    sheet = Image.new('RGBA', (CELL * 5, CELL), (0, 0, 0, 0))
    body = pin_mask()
    # the body: white, a little shade toward its tip
    cell = Image.new('RGBA', (CELL, CELL), (0, 0, 0, 0))
    for y in range(CELL):
        shade = 255 - int(max(0, y - 30) * 1.2)
        for x in range(CELL):
            a = body.getpixel((x, y))
            if a:
                cell.putpixel((x, y), (shade, shade, shade, a))
    sheet.paste(cell, (0, 0))
    outline = pin_mask(2.2)
    sheet.paste(Image.merge('RGBA', [Image.new('L', (CELL, CELL), 34), Image.new('L', (CELL, CELL), 26), Image.new('L', (CELL, CELL), 46),
                                     outline.point(lambda v: v * 210 // 255)]), (CELL, 0))
    for i, mask in ((2, arrow_mask()), (3, wings_mask()), (4, chest_mask())):
        white = Image.merge('RGBA', [Image.new('L', (CELL, CELL), 255)] * 3 + [mask])
        sheet.paste(white, (CELL * i, 0))
    sheet.save('src/main/resources/assets/iceandfire_fabric/textures/gui/exiled_pin.png')
    print('pin written')


if __name__ == '__main__':
    os.chdir(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..'))
    main()

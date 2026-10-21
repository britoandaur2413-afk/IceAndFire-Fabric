"""Draws the mod's own effect textures (white with alpha, tinted in game): warning rings, a warning line, magic
circles (frost, dread, storm, flame) and a shock wave. Run from the project folder:
    python tools/gen_vfx_textures.py
"""
import math
import os
import random

from PIL import Image, ImageDraw, ImageFilter

OUT = 'src/main/resources/assets/iceandfire_fabric/textures/vfx/'
SS = 4  # drawn this many times bigger, then made smaller (smooth edges)


def canvas(size):
    return Image.new('L', (size * SS, size * SS), 0)


def finish(alpha, size, name, blur=0):
    if blur:
        alpha = alpha.filter(ImageFilter.GaussianBlur(blur * SS))
    alpha = alpha.resize((size, size), Image.LANCZOS)
    white = Image.new('L', (size, size), 255)
    image = Image.merge('RGBA', (white, white, white, alpha))
    image.save(OUT + name)
    print('wrote', name)


def ring(draw, c, r_out, r_in, value):
    draw.ellipse((c - r_out, c - r_out, c + r_out, c + r_out), fill=value)
    draw.ellipse((c - r_in, c - r_in, c + r_in, c + r_in), fill=0)


def circle_line(draw, c, r, width, value):
    draw.ellipse((c - r, c - r, c + r, c + r), outline=value, width=width)


def telegraph_ring():
    size = 128
    a = canvas(size)
    d = ImageDraw.Draw(a)
    c = size * SS / 2
    R = c - 2 * SS
    # a bright edge with a soft glow inside
    for i in range(24):
        t = i / 23
        circle_line(d, c, R - t * 18 * SS, 2 * SS, int(200 * (1 - t) ** 2))
    circle_line(d, c, R, 5 * SS, 255)
    circle_line(d, c, R - 9 * SS, 2 * SS, 170)
    # ticks round it
    for k in range(24):
        ang = math.pi * 2 * k / 24
        r0, r1 = R - 12 * SS, R - (17 if k % 2 == 0 else 14) * SS
        d.line((c + math.cos(ang) * r0, c + math.sin(ang) * r0, c + math.cos(ang) * r1, c + math.sin(ang) * r1), fill=220, width=2 * SS)
    finish(a, size, 'telegraph_ring.png')


def telegraph_fill():
    size = 128
    a = canvas(size)
    d = ImageDraw.Draw(a)
    c = size * SS / 2
    R = c - 3 * SS
    for i in range(40):
        t = i / 39
        r = R * (1 - t)
        v = int(90 + 120 * (1 - t) ** 3)
        d.ellipse((c - r, c - r, c + r, c + r), fill=v)
    circle_line(d, c, R, 3 * SS, 255)
    finish(a, size, 'telegraph_fill.png', blur=0.6)


def telegraph_line():
    w, h = 32, 128
    a = Image.new('L', (w * SS, h * SS), 0)
    d = ImageDraw.Draw(a)
    W, H = w * SS, h * SS
    # a lane with bright edges and arrows pointing ahead (up the picture)
    d.rectangle((0, 0, W, H), fill=70)
    d.rectangle((0, 0, 3 * SS, H), fill=255)
    d.rectangle((W - 3 * SS, 0, W, H), fill=255)
    for k in range(4):
        y0 = H - (k * 32 + 10) * SS
        d.line((6 * SS, y0, W / 2, y0 - 12 * SS, W - 6 * SS, y0), fill=230, width=3 * SS, joint='curve')
    finish_rect(a, w, h, 'telegraph_line.png')


def finish_rect(alpha, w, h, name):
    alpha = alpha.resize((w, h), Image.LANCZOS)
    white = Image.new('L', (w, h), 255)
    Image.merge('RGBA', (white, white, white, alpha)).save(OUT + name)
    print('wrote', name)


def glyph(d, x, y, s, rnd, value, width):
    """A made-up rune: three to five strokes on a 3x3 grid of points."""
    pts = [(x + (i % 3 - 1) * s, y + (i // 3 - 1) * s) for i in range(9)]
    strokes = rnd.randint(3, 5)
    last = rnd.randrange(9)
    for _ in range(strokes):
        nxt = rnd.randrange(9)
        if nxt == last:
            nxt = (nxt + 4) % 9
        d.line((pts[last], pts[nxt]), fill=value, width=width)
        last = nxt if rnd.random() < 0.7 else rnd.randrange(9)


def rune_band(d, c, r, count, size, seed, value, width):
    rnd = random.Random(seed)
    for k in range(count):
        ang = math.pi * 2 * k / count
        x = c + math.cos(ang) * r
        y = c + math.sin(ang) * r
        glyph(d, x, y, size, rnd, value, width)


def polygon_star(d, c, r, points, step, value, width):
    verts = [(c + math.cos(math.pi * 2 * k / points - math.pi / 2) * r, c + math.sin(math.pi * 2 * k / points - math.pi / 2) * r) for k in range(points)]
    for k in range(points):
        d.line((verts[k], verts[(k + step) % points]), fill=value, width=width)


def rune_frost():
    size = 256
    a = canvas(size)
    d = ImageDraw.Draw(a)
    c = size * SS / 2
    R = c - 3 * SS
    circle_line(d, c, R, 4 * SS, 255)
    circle_line(d, c, R - 8 * SS, 2 * SS, 230)
    rune_band(d, c, R - 20 * SS, 18, 5 * SS, 11, 240, 2 * SS)
    circle_line(d, c, R - 32 * SS, 3 * SS, 255)
    # a six-pointed frost star with snowflake arms
    polygon_star(d, c, R - 36 * SS, 6, 2, 220, 3 * SS)
    for k in range(6):
        ang = math.pi * 2 * k / 6 - math.pi / 2
        x1, y1 = c + math.cos(ang) * (R - 40 * SS), c + math.sin(ang) * (R - 40 * SS)
        d.line((c, c, x1, y1), fill=200, width=2 * SS)
        for f in (0.45, 0.7):
            bx, by = c + math.cos(ang) * (R - 40 * SS) * f, c + math.sin(ang) * (R - 40 * SS) * f
            for side in (-1, 1):
                a2 = ang + side * math.pi / 4
                d.line((bx, by, bx + math.cos(a2) * 14 * SS, by + math.sin(a2) * 14 * SS), fill=200, width=2 * SS)
    circle_line(d, c, 26 * SS, 3 * SS, 255)
    rune_band(d, c, 16 * SS, 6, 3 * SS, 12, 220, 2 * SS)
    finish(a, size, 'rune_frost.png')


def rune_dread():
    size = 256
    a = canvas(size)
    d = ImageDraw.Draw(a)
    c = size * SS / 2
    R = c - 3 * SS
    circle_line(d, c, R, 5 * SS, 255)
    # thorns round the edge
    for k in range(36):
        ang = math.pi * 2 * k / 36
        r0, r1 = R - 4 * SS, R - 13 * SS
        a1, a2 = ang - 0.05, ang + 0.05
        d.polygon([(c + math.cos(a1) * r0, c + math.sin(a1) * r0), (c + math.cos(ang) * r1, c + math.sin(ang) * r1),
                   (c + math.cos(a2) * r0, c + math.sin(a2) * r0)], fill=230)
    circle_line(d, c, R - 16 * SS, 2 * SS, 220)
    rune_band(d, c, R - 28 * SS, 14, 6 * SS, 21, 240, 3 * SS)
    circle_line(d, c, R - 40 * SS, 3 * SS, 255)
    # a seven-pointed star, sharp
    polygon_star(d, c, R - 44 * SS, 7, 3, 230, 3 * SS)
    polygon_star(d, c, R - 74 * SS, 7, 2, 180, 2 * SS)
    # an eye in the middle
    d.ellipse((c - 18 * SS, c - 9 * SS, c + 18 * SS, c + 9 * SS), outline=255, width=3 * SS)
    d.ellipse((c - 5 * SS, c - 5 * SS, c + 5 * SS, c + 5 * SS), fill=255)
    finish(a, size, 'rune_dread.png')


def rune_storm():
    size = 256
    a = canvas(size)
    d = ImageDraw.Draw(a)
    c = size * SS / 2
    R = c - 3 * SS
    circle_line(d, c, R, 4 * SS, 255)
    rune_band(d, c, R - 16 * SS, 16, 5 * SS, 31, 230, 2 * SS)
    circle_line(d, c, R - 28 * SS, 3 * SS, 240)
    # six bolts out from the middle ring (none of them meet)
    for k in range(6):
        ang = math.pi * 2 * k / 6
        pts = []
        for j in range(5):
            t = j / 4
            r = 30 * SS + (R - 66 * SS) * t
            off = (7 * SS if j % 2 else -7 * SS) * (1 if 0 < j < 4 else 0)
            pts.append((c + math.cos(ang) * r + math.cos(ang + math.pi / 2) * off, c + math.sin(ang) * r + math.sin(ang + math.pi / 2) * off))
        d.line(pts, fill=240, width=3 * SS, joint='curve')
    circle_line(d, c, R - 34 * SS, 2 * SS, 170)
    circle_line(d, c, 22 * SS, 3 * SS, 255)
    finish(a, size, 'rune_storm.png')


def rune_flame():
    size = 256
    a = canvas(size)
    d = ImageDraw.Draw(a)
    c = size * SS / 2
    R = c - 3 * SS
    circle_line(d, c, R, 4 * SS, 255)
    # flames licking inward
    for k in range(20):
        ang = math.pi * 2 * k / 20
        base = R - 4 * SS
        tip = R - (22 if k % 2 == 0 else 15) * SS
        a1, a2 = ang - 0.12, ang + 0.12
        d.polygon([(c + math.cos(a1) * base, c + math.sin(a1) * base), (c + math.cos(ang + 0.05) * tip, c + math.sin(ang + 0.05) * tip),
                   (c + math.cos(a2) * base, c + math.sin(a2) * base)], fill=210)
    circle_line(d, c, R - 26 * SS, 3 * SS, 255)
    rune_band(d, c, R - 38 * SS, 12, 5 * SS, 41, 230, 2 * SS)
    polygon_star(d, c, R - 50 * SS, 8, 3, 230, 3 * SS)
    circle_line(d, c, 20 * SS, 3 * SS, 255)
    finish(a, size, 'rune_flame.png')


def rune_life():
    """A circle of leaves and vines (the green pixie's)."""
    size = 256
    a = canvas(size)
    d = ImageDraw.Draw(a)
    c = size * SS / 2
    R = c - 3 * SS
    circle_line(d, c, R, 4 * SS, 255)
    # a vine winding round, with leaves
    pts = []
    for k in range(361):
        ang = math.pi * 2 * k / 360
        r = R - 16 * SS + math.sin(ang * 12) * 6 * SS
        pts.append((c + math.cos(ang) * r, c + math.sin(ang) * r))
    d.line(pts, fill=230, width=3 * SS)
    for k in range(24):
        ang = math.pi * 2 * k / 24
        r = R - 16 * SS + math.sin(ang * 12) * 6 * SS
        x, y = c + math.cos(ang) * r, c + math.sin(ang) * r
        side = 1 if k % 2 == 0 else -1
        a2 = ang + side * math.pi / 2
        tip = (x + math.cos(a2) * 10 * SS, y + math.sin(a2) * 10 * SS)
        d.polygon([(x, y), (x + math.cos(a2 + 0.6) * 6 * SS, y + math.sin(a2 + 0.6) * 6 * SS), tip,
                   (x + math.cos(a2 - 0.6) * 6 * SS, y + math.sin(a2 - 0.6) * 6 * SS)], fill=210)
    circle_line(d, c, R - 34 * SS, 3 * SS, 255)
    rune_band(d, c, R - 46 * SS, 10, 5 * SS, 51, 220, 2 * SS)
    # a flower of six petals in the middle
    for k in range(6):
        ang = math.pi * 2 * k / 6
        px, py = c + math.cos(ang) * 22 * SS, c + math.sin(ang) * 22 * SS
        d.ellipse((px - 14 * SS, py - 14 * SS, px + 14 * SS, py + 14 * SS), outline=230, width=3 * SS)
    circle_line(d, c, 9 * SS, 3 * SS, 255)
    finish(a, size, 'rune_life.png')


def wave():
    size = 128
    a = canvas(size)
    d = ImageDraw.Draw(a)
    c = size * SS / 2
    R = c - 3 * SS
    for i in range(14):
        t = i / 13
        circle_line(d, c, R - t * 14 * SS, 2 * SS, int(255 * (1 - t) ** 1.5))
    finish(a, size, 'wave.png', blur=0.5)


def spark():
    """A small soft star, for motes round a charge."""
    size = 32
    a = canvas(size)
    d = ImageDraw.Draw(a)
    c = size * SS / 2
    for i in range(12):
        t = i / 11
        r = (c - SS) * (1 - t)
        d.ellipse((c - r, c - r, c + r, c + r), fill=int(255 * t ** 1.4))
    d.line((c, 2 * SS, c, size * SS - 2 * SS), fill=255, width=2 * SS)
    d.line((2 * SS, c, size * SS - 2 * SS, c), fill=255, width=2 * SS)
    finish(a, size, 'spark.png')


def beam():
    """A column of light: bright in the middle, soft to the sides, fading at the top."""
    w, h = 32, 128
    a = Image.new('L', (w * SS, h * SS), 0)
    px = a.load()
    W, H = w * SS, h * SS
    for y in range(H):
        fade = min(1.0, (H - y) / (H * 0.15)) * min(1.0, y / (H * 0.6) + 0.25)
        for x in range(W):
            t = abs(x - W / 2) / (W / 2)
            px[x, y] = int(255 * fade * max(0.0, 1 - t) ** 1.8)
    finish_rect(a, w, h, 'beam.png')


def stream():
    """A jet of frost: streaks along it (tiling end to end), bright in the middle, soft to the sides."""
    w, h = 64, 128
    W, H = w * SS, h * SS
    a = Image.new('L', (W, H), 0)
    px = a.load()
    rnd = random.Random(7)
    # streaks: thin bright lines along the jet, each a wave that tiles over the height
    streaks = [(rnd.random(), rnd.random() * math.pi * 2, rnd.uniform(0.4, 1.0), rnd.randint(1, 3)) for _ in range(26)]
    for y in range(H):
        v = y / H
        for x in range(W):
            u = x / W
            side = abs(u - 0.5) * 2
            base = max(0.0, 1 - side) ** 1.6 * 0.55
            glow = 0.0
            for (su, phase, power, waves) in streaks:
                cu = 0.15 + su * 0.7 + 0.04 * math.sin(phase + v * math.pi * 2 * waves)
                dist = abs(u - cu) * W / SS
                if dist < 2.5:
                    glow = max(glow, power * (1 - dist / 2.5) * (0.6 + 0.4 * math.sin(phase * 3 + v * math.pi * 2 * waves * 2)))
            px[x, y] = int(255 * min(1.0, base + glow * max(0.0, 1 - side) ** 0.5))
    finish_rect(a.filter(ImageFilter.GaussianBlur(SS * 0.6)), w, h, 'stream.png')


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    telegraph_ring()
    telegraph_fill()
    telegraph_line()
    rune_frost()
    rune_dread()
    rune_storm()
    rune_flame()
    rune_life()
    wave()
    spark()
    beam()
    stream()

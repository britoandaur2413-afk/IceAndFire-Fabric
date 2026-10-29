"""Pixel-art frames for the dragons' breath: a puff that is born a spark, swells into a bright ball, grows wild and
dissolves (fire into smoke, frost into mist and snow, lightning into stray sparks). 10 frames of 32x32 per element,
plus 3 small specks (8x8) each: embers, snowflakes, sparks.

python -I tools/breath_frames.py src/main/resources/assets/iceandfire_fabric/textures/particle [sheet.png]"""
import math
import sys

import numpy as np
from PIL import Image

OUT = sys.argv[1]
SHEET = sys.argv[2] if len(sys.argv) > 2 else None
N = 32


def hexc(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def value_noise(seed, cells, size=N):
    rng = np.random.default_rng(seed)
    g = rng.random((cells + 1, cells + 1))
    xs = np.linspace(0, cells, size, endpoint=False)
    xi = xs.astype(int)
    xf = xs - xi
    xf = xf * xf * (3 - 2 * xf)
    a = g[np.ix_(xi, xi)]
    b = g[np.ix_(xi + 1, xi)]
    c = g[np.ix_(xi, xi + 1)]
    d = g[np.ix_(xi + 1, xi + 1)]
    fy = xf[:, None]
    fx = xf[None, :]
    return (a * (1 - fy) + b * fy) * (1 - fx) + (c * (1 - fy) + d * fy) * fx


def fbm(seed, size=N):
    return (value_noise(seed, 4, size) * 0.55 + value_noise(seed + 1, 8, size) * 0.3 + value_noise(seed + 2, 16, size) * 0.15)


def grid(size=N):
    c = (size - 1) / 2.0
    y, x = np.mgrid[0:size, 0:size]
    dx = x - c
    dy = y - c
    return dx, dy, np.sqrt(dx * dx + dy * dy), np.arctan2(dy, dx)


def swirl_noise(seed, twist):
    # a noise turned more toward the outside: the puff churns as it grows
    dx, dy, r, ang = grid()
    base = fbm(seed, N * 2)
    ang2 = ang + twist * (r / 16.0)
    sx = np.clip((N - 1) / 2.0 + r * np.cos(ang2), 0, N - 1) * 2
    sy = np.clip((N - 1) / 2.0 + r * np.sin(ang2), 0, N - 1) * 2
    return base[sy.astype(int), sx.astype(int)]


def puff(seed, radius, core, rough, holes, twist, turb=0.0):
    """A field from 1 (its heart) to 0 (outside): a ball of this radius, rough at its edge, churning inside, with holes
    eaten in it."""
    dx, dy, r, ang = grid()
    n = swirl_noise(seed, twist)
    edge = radius * (1.0 + rough * (n - 0.5) * 1.6)
    f = 1.0 - r / np.maximum(edge, 0.01)
    f = np.clip(f, -1, 1)
    # the heart: brighter where the noise is high, a little off the middle
    heart = np.exp(-((dx + 1.0) ** 2 + (dy + 1.0) ** 2) / max(0.5, (radius * core) ** 2)) * core * 1.4
    f = f + heart * 0.6
    if turb > 0:
        f = f + (swirl_noise(seed + 5, twist + 1.0) - 0.5) * turb * np.clip(1.2 - r / max(radius, 1.0), 0, 1)
    if holes > 0:
        h = fbm(seed + 77)
        f = f - np.clip(h - (1.0 - holes), 0, 1) * 2.2
    return f


def paint(field, palette, cuts, alpha_edge=None):
    """Bands of the field to the palette (heart first): hard steps, as pixel art."""
    img = np.zeros((N, N, 4), dtype=np.uint8)
    for i, cut in enumerate(cuts):
        mask = field > cut
        if i == 0:
            prev = np.zeros_like(mask)
        sel = mask & ~prev
        img[sel] = palette[min(i, len(palette) - 1)]
        prev = prev | mask
    return img


def bands(field, palette, cuts):
    img = np.zeros((N, N, 4), dtype=np.uint8)
    # cuts high to low: the first is the heart
    done = np.zeros((N, N), dtype=bool)
    for color, cut in zip(palette, cuts):
        sel = (field > cut) & ~done
        img[sel] = color
        done |= sel
    return img


def clean(img):
    # no lone pixel floating alone outside the puff (it reads as dirt), keep those that touch another one
    a = img[:, :, 3] > 0
    pad = np.pad(a, 1)
    neighbours = sum(np.roll(np.roll(pad, i, 0), j, 1) for i in (-1, 0, 1) for j in (-1, 0, 1) if (i, j) != (0, 0))[1:-1, 1:-1]
    lone = a & (neighbours == 0)
    img[lone] = (0, 0, 0, 0)
    return img


def spark(color_heart, color_ray, color_tip):
    img = np.zeros((N, N, 4), dtype=np.uint8)
    c = 15
    for d in range(1, 4):
        col = color_ray if d < 3 else color_tip
        for (x, y) in ((c + d, c), (c - d, c), (c, c + d), (c, c - d)):
            img[y, x] = col
    for (x, y) in ((c + 1, c + 1), (c - 1, c - 1), (c + 1, c - 1), (c - 1, c + 1)):
        img[y, x] = color_tip
    img[c, c] = color_heart
    img[c + 1, c] = color_heart
    img[c, c + 1] = color_heart
    img[c + 1, c + 1] = color_ray
    return img


# radius, heart, roughness, holes, twist: the life of the puff
LIFE = [
    None,
    (4.2, 0.75, 0.10, 0.0, 0.0),
    (7.5, 0.60, 0.22, 0.0, 0.4),
    (10.5, 0.45, 0.35, 0.0, 0.8),
    (12.5, 0.32, 0.45, 0.10, 1.2),
    (13.5, 0.22, 0.55, 0.22, 1.6),
    (14.2, 0.12, 0.60, 0.36, 2.0),
    (14.6, 0.05, 0.65, 0.52, 2.4),
    (15.0, 0.0, 0.70, 0.68, 2.8),
    (15.2, 0.0, 0.75, 0.82, 3.2),
]
CUTS = [0.80, 0.62, 0.46, 0.32, 0.18, 0.07, -0.02]


def fire():
    W, Y, Y2, O, R, DR, DDR = '#fffbe6', '#ffe873', '#ffb52e', '#ff7a1a', '#e83f12', '#9c1f0e', '#5a1208'
    s1, s2, s3, s4, s5 = ('#5a2a20', 240), ('#4a3c3c', 225), ('#3b3335', 200), ('#2f292a', 160), ('#262122', 110)

    def pal(*cols):
        return [hexc(c) if isinstance(c, str) else hexc(c[0], c[1]) for c in cols]

    palettes = [
        None,
        pal(W, W, Y, Y2, O, R, (DR, 200)),
        pal(W, Y, Y2, O, R, DR, (DDR, 190)),
        pal(W, Y, Y2, O, R, DR, (DDR, 190)),
        pal(Y, Y2, O, R, DR, (DDR, 220), s1),
        pal(Y2, O, R, DR, s1, s2, s3),
        pal(O, R, DR, s1, s2, s3, s4),
        pal(R, DR, s1, s2, s3, s4, s5),
        pal(DR, s1, s2, s3, s4, s5, s5),
        pal(s1, s2, s3, s4, s5, s5, s5),
    ]
    turb = [0, 0.0, 0.15, 0.3, 0.4, 0.45, 0.5, 0.5, 0.5, 0.5]
    frames = [spark(hexc('#ffffff'), hexc('#ffe873'), hexc('#ff9a2e'))]
    for i in range(1, 10):
        radius, core, rough, holes, twist = LIFE[i]
        f = puff(100 + i * 13, radius, core, rough, holes, twist, turb[i])
        frames.append(clean(bands(f, palettes[i], CUTS)))
    return frames


def frost():
    cold = [hexc('#ffffff'), hexc('#ccf4ff'), hexc('#97e2fb'), hexc('#6fcaf5'), hexc('#3d93dc'), hexc('#2a5fb0'), hexc('#1d3d80', 180)]
    mist = [hexc('#ffffff', 230), hexc('#e8f7ff', 210), hexc('#cdeefc', 190), hexc('#a9d9f2', 160), hexc('#86bfe3', 130), hexc('#6d9fcf', 100), hexc('#5a86bd', 70)]
    frames = [spark(hexc('#ffffff'), hexc('#c9f5ff'), hexc('#6fcaf5'))]
    for i in range(1, 10):
        radius, core, rough, holes, twist = LIFE[i]
        f = puff(300 + i * 17, radius, core, rough * 0.8, holes, twist * 0.6, [0, 0, 0.1, 0.2, 0.3, 0.35, 0.4, 0.4, 0.4, 0.4][i])
        t = max(0.0, (i - 4) / 5.0)
        pal = [tuple(int(a * (1 - t) + b * t) for a, b in zip(p, q)) for p, q in zip(cold, mist)]
        img = clean(bands(f, pal, CUTS))
        # shards of ice in it: hard little crystals (a cross of light), more as it grows, and snow as it dies
        rng = np.random.default_rng(900 + i)
        shards = 0 if i < 2 else min(7, i)
        for _ in range(shards):
            ang = rng.random() * math.tau
            dist = rng.random() * radius * 0.85
            x = int(round(15.5 + math.cos(ang) * dist))
            y = int(round(15.5 + math.sin(ang) * dist))
            if 1 <= x < N - 1 and 1 <= y < N - 1:
                img[y, x] = hexc('#ffffff')
                if i < 7:
                    for (ax, ay) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        if img[y + ay, x + ax, 3] > 0:
                            img[y + ay, x + ax] = hexc('#d8f8ff')
        frames.append(img)
    return frames


def line(img, x0, y0, x1, y1, color):
    n = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
    for k in range(n + 1):
        x = int(round(x0 + (x1 - x0) * k / max(n, 1)))
        y = int(round(y0 + (y1 - y0) * k / max(n, 1)))
        if 0 <= x < N and 0 <= y < N:
            img[y, x] = color


def arc(img, rng, x, y, ang, reach, color, depth=0, limit=14.0):
    """A crack of lightning: straight bits with sharp turns, forking now and then (kept inside the picture)."""
    left = reach
    while left > 0:
        seg = 2 + rng.random() * 3
        ang += (rng.random() - 0.5) * 1.6
        nx, ny = x + math.cos(ang) * seg, y + math.sin(ang) * seg
        if math.hypot(nx - 15.5, ny - 15.5) > limit:
            break
        line(img, x, y, nx, ny, color)
        x, y = nx, ny
        left -= seg
        if depth == 0 and rng.random() < 0.35 and left > 4:
            arc(img, rng, x, y, ang + (0.9 if rng.random() < 0.5 else -0.9), left * 0.5, color, 1, limit)


def lightning():
    bright = [hexc('#ffffff'), hexc('#d9efff'), hexc('#9fd8ff'), hexc('#6aa8ff'), hexc('#6a5cff', 230), hexc('#5a2fc1', 200), hexc('#3a1a80', 150)]
    fade = [hexc('#c8b8ff', 220), hexc('#9a86ff', 200), hexc('#6a5cff', 180), hexc('#5a3ad1', 160), hexc('#43249e', 140), hexc('#331a7a', 110), hexc('#26135c', 80)]
    frames = [spark(hexc('#ffffff'), hexc('#bfe6ff'), hexc('#7a6cff'))]
    arcs = [0, 0, 1, 2, 3, 3, 3, 2, 1, 0]
    for i in range(1, 10):
        radius, core, rough, holes, twist = LIFE[i]
        f = puff(500 + i * 19, radius * 0.8, core, rough * 0.7, holes, twist * 0.5, [0, 0, 0.15, 0.3, 0.4, 0.45, 0.5, 0.5, 0.5, 0.5][i])
        t = max(0.0, (i - 4) / 5.0)
        pal = [tuple(int(a * (1 - t) + b * t) for a, b in zip(p, q)) for p, q in zip(bright, fade)]
        img = clean(bands(f, pal, CUTS))
        rng = np.random.default_rng(700 + i)
        cracks = np.zeros_like(img)
        for a in range(arcs[i]):
            ang = rng.random() * math.tau
            start = radius * 0.3
            arc(cracks, rng, 15.5 + math.cos(ang) * start, 15.5 + math.sin(ang) * start, ang, radius * 0.9,
                hexc('#ffffff') if i < 6 else hexc('#c8e8ff', 230), limit=min(14.5, radius * 1.15))
        # a soft violet glow beside each crack, then the crack over it
        lit = cracks[:, :, 3] > 0
        pad = np.pad(lit, 1)
        near = np.zeros_like(lit)
        for (ax, ay) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            near |= np.roll(np.roll(pad, ay, 0), ax, 1)[1:-1, 1:-1]
        glow = near & ~lit & (img[:, :, 3] == 0)
        img[glow] = hexc('#7a6cff', 120)
        img[lit] = cracks[lit]
        if i >= 8:
            # what is left: stray sparks
            for _ in range(5):
                x, y = rng.integers(5, N - 5, size=2)
                img[y, x] = hexc('#e6f4ff')
        frames.append(img)
    return frames


def specks():
    out = {}
    s = 8

    def blank():
        return np.zeros((s, s, 4), dtype=np.uint8)

    # embers: a hot grain, cooling
    e = []
    for heart, rim in (('#fffbe6', '#ffb52e'), ('#ffe873', '#e83f12'), ('#ff7a1a', '#7a1a0c')):
        img = blank()
        img[3:5, 3:5] = hexc(rim)
        img[3, 3] = hexc(heart)
        img[4, 4] = hexc(heart) if heart != '#ff7a1a' else hexc(rim)
        e.append(img)
    out['ember'] = e
    # snowflakes: a little star, then a dot
    f = []
    img = blank()
    for (x, y) in ((3, 1), (3, 2), (3, 4), (3, 5), (1, 3), (2, 3), (4, 3), (5, 3), (2, 2), (4, 4), (2, 4), (4, 2)):
        img[y, x] = hexc('#d8f8ff')
    img[3, 3] = hexc('#ffffff')
    f.append(img)
    img = blank()
    for (x, y) in ((3, 2), (3, 4), (2, 3), (4, 3)):
        img[y, x] = hexc('#bfeeff')
    img[3, 3] = hexc('#ffffff')
    f.append(img)
    img = blank()
    img[3:5, 3:5] = hexc('#cdeefc', 200)
    f.append(img)
    out['snow'] = f
    # sparks: a little forked crack, then a dot
    k = []
    img = blank()
    for (x, y) in ((1, 1), (2, 2), (3, 3), (4, 3), (5, 4), (6, 6), (4, 5)):
        img[y, x] = hexc('#e6f4ff')
    img[3, 3] = hexc('#ffffff')
    k.append(img)
    img = blank()
    for (x, y) in ((2, 3), (3, 3), (4, 4), (5, 4)):
        img[y, x] = hexc('#9fd8ff')
    img[3, 3] = hexc('#ffffff')
    k.append(img)
    img = blank()
    img[3, 3] = hexc('#ffffff')
    img[3, 4] = hexc('#7a6cff', 200)
    k.append(img)
    out['spark'] = k
    return out


def save(frames, name):
    for i, f in enumerate(frames):
        Image.fromarray(f, 'RGBA').save('%s/%s_%d.png' % (OUT, name, i))


sets = {'breath_fire': fire(), 'breath_ice': frost(), 'breath_lightning': lightning()}
for name, frames in sets.items():
    save(frames, name)
sp = specks()
for name, frames in sp.items():
    save(frames, 'breath_' + name)

if SHEET:
    scale = 6
    rows = list(sets.values())
    w = (N * scale + 8) * 10
    sheet = Image.new('RGBA', (w, (N * scale + 8) * 3 + 8 * scale + 16), (40, 40, 48, 255))
    for r, frames in enumerate(rows):
        for i, f in enumerate(frames):
            im = Image.fromarray(f, 'RGBA').resize((N * scale, N * scale), Image.NEAREST)
            sheet.alpha_composite(im, (i * (N * scale + 8), r * (N * scale + 8)))
    y = 3 * (N * scale + 8)
    x = 0
    for name, frames in sp.items():
        for f in frames:
            im = Image.fromarray(f, 'RGBA').resize((8 * scale, 8 * scale), Image.NEAREST)
            sheet.alpha_composite(im, (x, y))
            x += 8 * scale + 8
        x += 24
    sheet.save(SHEET)
print('ok')

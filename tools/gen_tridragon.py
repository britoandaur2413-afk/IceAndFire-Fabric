"""The three-headed dragon's model and texture, made from the three dragons' own models.

An abyssal beast (the user: dangerous, a natural beast, not a king): near black with the amethyst's violet in it, old
bone horns, claws and talons, violet crystals growing out of it that shine in the dark (an orb half sunk in each brow,
crystals breaking out behind it, a cluster in its chest, along its spine, at its tail's end), glowing veins in its
wings. Its arms are its front wings (as a dragon's, and as the user's reference wyvern): the lightning dragon's wings as
they fold on the ground, a third longer and with muscle, a hand of three talons at each wrist, so it stands raised on
them; two more wings, half raised, on its lower back (the lightning dragon's as it hovers). Its other pieces: the
lightning dragon's body (a broad chest), hind legs and tail (half as big again); three necks on its shoulders (the fire
dragon's in the middle, the tallest, one joint longer; the ice dragon's on its right; the lightning dragon's on its
left), every picture made dark amethyst shade for shade.

Each piece keeps the picture it was made for: the texture is six dragon pictures side by side (each 512x256 for a model
space of 256x128), and each piece's texture offset moves to its picture's place. Writes
models/tabula/tridragon/tridragon_ground.tbl and textures/models/tridragon/tridragon(_eyes).png in the tests' resources
only (the model is not in the mod until the user wants it there), and tools/models/tridragon.bbmodel for Blockbench.
    python -I tools/gen_tridragon.py
"""
import copy
import io
import json
import math
import pathlib
import random
import zipfile

import numpy as np
from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parent.parent
ASSETS = ROOT / 'src/main/resources/assets/iceandfire_fabric'
TBL = ASSETS / 'models/tabula'
TEX = ASSETS / 'textures/models'
# (where it goes: the tests' resources, not the mod's)
OUT = ROOT / 'src/gametest/resources/assets/iceandfire_fabric'
OUT_TBL = OUT / 'models/tabula/tridragon'
OUT_TEX = OUT / 'textures/models/tridragon'

# a dragon's texture space (its pictures are twice as big), and where each picture goes (column, row)
PAGE_W, PAGE_H, PX = 256, 128, 2
PAGES = {'body': (0, 0), 'fire': (1, 0), 'wing': (2, 0), 'ice': (0, 1), 'light': (1, 1), 'extra': (2, 1)}
# its own pieces' places on the extra page (model units, each a patch any of their boxes fits in): old bone (horns,
# talons, spikes), violet crystal, the orbs, plain skin (its muscles)
BONE_UV, CRYSTAL_UV, ORB_UV, SKIN_UV = (0, 0), (0, 32), (0, 64), (64, 0)
# its body and all on it, as big as a dragon's times CORE; its arms (its front wings) and back wings FRONT and BACK times a
# dragon's (its body's size taken out of theirs); its thick tail and its hind legs TAIL and HIND times its body's
CORE, FRONT, BACK, TAIL, HIND = 1.2, 1.35, 0.8, 1.3, 1.1
# the ground under its feet, in model units (the game draws a model 24 units over its feet)
GROUND = 24.0
rng = random.Random(3)


def load(kind, pose='ground'):
    return json.loads(zipfile.ZipFile(TBL / f'{kind}dragon/{kind}dragon_{pose}.tbl').read('model.json'))


def walk(cube):
    yield cube
    for child in cube.get('children', []):
        yield from walk(child)


def find(cubes, name):
    for root in cubes:
        for c in walk(root):
            if c['name'] == name:
                return c
    raise KeyError(name)


def new_id():
    return ''.join(rng.choice('abcdefghijklmnopqrstuvwxyz0123456789') for _ in range(20))


def taken(cube, page, prefix=''):
    """A copy of a piece and all it holds, on its picture's page, its names prefixed, with ids of its own."""
    cube = copy.deepcopy(cube)
    col, row = PAGES[page]
    for c in walk(cube):
        c['txOffset'] = [c['txOffset'][0] + col * PAGE_W, c['txOffset'][1] + row * PAGE_H]
        c['name'] = prefix + c['name']
        c['identifier'] = new_id()
    return cube


def piece(name, pos, rot, off, dim, tex, page='extra', scale=(1, 1, 1), mirror=False):
    col, row = PAGES[page]
    return {'name': name, 'dimensions': list(dim), 'position': list(pos), 'offset': list(off), 'rotation': list(rot),
            'scale': list(scale), 'txOffset': [tex[0] + col * PAGE_W, tex[1] + row * PAGE_H], 'txMirror': mirror,
            'mcScale': 0.0, 'opacity': 100.0, 'hidden': False, 'identifier': new_id(), 'children': []}


def shard(name, pos, rot, size):
    """A crystal growing out of it: a long box from where it grows (a little in it) to its point, up before it turns."""
    w, h, d = size
    return piece(name, pos, rot, (-w / 2, -h, -d / 2), size, CRYSTAL_UV)


def bone(name, pos, rot, length, width=1):
    """A horn, talon or spike of bone: from its root up to its point before it turns."""
    return piece(name, pos, rot, (-width / 2, -length, -width / 2), (width, length, width), BONE_UV)


# ---- where its pieces are (as the game poses a model: move to the pivot, turn z, y, x, scale) ----


def matrix(c):
    rx, ry, rz = (math.radians(a) for a in c['rotation'])
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    turn = (np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]) @ np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
            @ np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]]))
    m = np.eye(4)
    m[:3, :3] = turn @ np.diag(c.get('scale', [1, 1, 1]))
    m[:3, 3] = c['position']
    return m


def turned(x=0.0, y=0.0, z=0.0):
    return matrix({'rotation': [x, y, z], 'position': [0, 0, 0]})[:3, :3]


def euler(r):
    """A turn's angles (x, y, z degrees; the game turns z, then y, then x)."""
    return [math.degrees(math.atan2(r[2, 1], r[2, 2])), math.degrees(math.asin(max(-1.0, min(1.0, -r[2, 0])))),
            math.degrees(math.atan2(r[1, 0], r[0, 0]))]


def about(axis, angle):
    """The turn by this angle (radians) about this axis."""
    axis = axis / np.linalg.norm(axis)
    k = np.array([[0, -axis[2], axis[1]], [axis[2], 0, -axis[0]], [-axis[1], axis[0], 0]])
    return np.eye(3) + math.sin(angle) * k + (1 - math.cos(angle)) * k @ k


def frames(roots):
    """Every piece's place and turn in the model's space: {id(piece): 4x4}."""
    out = {}

    def go(c, parent):
        out[id(c)] = parent @ matrix(c)
        for child in c.get('children', []):
            go(child, out[id(c)])

    for r in roots:
        go(r, np.eye(4))
    return out


def corners(roots):
    """Every piece's box corners in the model's space: {name: 8x3}."""
    out = {}

    def go(c, parent):
        m = parent @ matrix(c)
        o, d = np.array(c['offset']), np.array(c['dimensions'])
        pts = np.array([[o[0] + d[0] * (k & 1), o[1] + d[1] * (k >> 1 & 1), o[2] + d[2] * (k >> 2 & 1), 1] for k in range(8)])
        out.setdefault(c['name'], (m @ pts.T).T[:, :3])
        for child in c.get('children', []):
            go(child, m)

    for r in roots:
        go(r, np.eye(4))
    return out


def lowest(boxes, test):
    return max((pts[:, 1].max() for name, pts in boxes.items() if test(name)), default=-1e9)


fire, ice, light, light_hover = load('fire'), load('ice'), load('lightning'), load('lightning', 'hovering')

# ---- its body: the lightning dragon's, a broad chest over it (three necks, two great arms); its tail half as big again
body = taken(find(light['cubes'], 'BodyUpper'), 'body')
body['scale'] = [CORE] * 3
body['children'] = [c for c in body['children'] if c['name'] not in ('Neck1', 'armR1', 'armL1')]
lower = find([body], 'BodyLower')
tail = find([lower], 'Tail1')
tail['scale'] = [TAIL] * 3
thighs = [find([lower], 'Thigh' + side) for side in 'RL']
for thigh in thighs:
    thigh['scale'] = [HIND] * 3
BODY_REST, THIGH_REST, TAIL_REST = body['rotation'][0], thighs[0]['rotation'][0], tail['rotation'][0]
body['children'].append(piece('Chest', (0.0, 0.0, 0.0), (0, 0, 0), (-4.5, -1.3, -0.7), (9, 8, 6), SKIN_UV))

# ---- its arms: its front wings as a dragon folds them on the ground, longer, with muscle, and a hand of three talons
# (its elbow folded FOLD degrees more than a dragon's, then the arm swung whole about its shoulder so its hand is below
# it, a little out and ahead: REACH from the shoulder for its right arm; its elbow pointing back and out: ELBOW; its
# forearm rolled about itself so its folded fingers lean CAPE degrees from straight out)
FOLD, REACH, ELBOW, CAPE = 35.0, np.array([-0.27, 1.0, -0.58]), np.array([-0.5, -0.3, 0.8]), 190.0
fronts, hands, arms = [], [], []


def folded(fore, claw, degrees):
    """The forearm turned about its elbow's hinge (the line square to the arm and forearm), the wrist toward the shoulder."""
    elbow = np.array(fore['position'], dtype=float)
    turn = turned(*fore['rotation'])
    wrist = elbow + turn @ np.array(claw['position'], dtype=float)
    hinge = np.cross(-elbow, wrist - elbow)
    for angle in (math.radians(degrees), -math.radians(degrees)):
        again = about(hinge, angle) @ turn
        if np.linalg.norm(elbow + again @ np.array(claw['position'], dtype=float)) < np.linalg.norm(wrist):
            fore['rotation'] = euler(again)
            return
for side, sign in (('R', 1), ('L', -1)):
    wing = taken(find(light['cubes'], 'arm' + side + '1'), 'wing')
    wing['scale'] = [FRONT / CORE] * 3
    fore = find([wing], 'arm' + side + '2')
    folded(fore, find([wing], 'Claw' + side), FOLD)
    # its two long fingers' ends curled back into its folded wing (they would stand over its shoulders as horns)
    for name, curl in (('fingerR.2' if side == 'R' else 'fingerL1.2', 34.0), (f'finger{side}2.2', 24.0)):
        find([wing], name)['rotation'][0] += curl
    wing['children'].append(piece('Shoulder' + side, (0.0, 0.0, 0.0), (0, 0, 0), (-1.5, -1.0, -2.5), (3, 9, 5), SKIN_UV))
    fore['children'].append(piece('Forearm' + side, (0.0, 0.0, 0.0), (0, 0, 0), (-1.5, -1.0, -1.9), (3, 10, 4), SKIN_UV))
    # (its hand where its wrist's claw was: the claw goes, the hand's talons are its claws now)
    claw = find([wing], 'Claw' + side)
    hand = piece('Hand' + side, claw['position'], (0, 0, 0), (-1.5, -0.8, -2.6), (3, 3, 3), SKIN_UV)
    for i, yaw in enumerate((-24, 0, 24)):
        hand['children'].append(bone(f'Talon{side}{i}', ((i - 1) * 1.05, 1.4, -2.3), (100, yaw, 0), 3))
    fore['children'] = [c for c in fore['children'] if c is not claw] + [hand]
    fronts.append(wing)
    hands.append((hand, fore, 12.0 * sign))
    arms.append((wing, fore, hand, list(wing['rotation']), list(fore['rotation']), sign))

# ---- two more wings on its lower back, half raised, as a dragon's when it hovers (smaller than its arms) ----
for side, sign in (('R', 1), ('L', -1)):
    wing = taken(find(light_hover['cubes'], 'arm' + side + '1'), 'wing', 'B_')
    wing['position'] = [-2.6 * sign, -1.0, 2.4]
    wing['scale'] = [BACK / CORE] * 3
    lower['children'].append(wing)

# ---- three necks: the fire dragon's in the middle (one joint longer: the tallest), the ice and lightning dragons' at
# its sides, spread ----
middle = taken(find(fire['cubes'], 'Neck1'), 'fire')
middle['position'] = [0.0, -0.2, 1.4]
neck2, neck3 = find([middle], 'Neck2'), find([middle], 'Neck3')
joint = taken(neck2, 'fire', 'M_')
joint['txOffset'] = list(neck2['txOffset'])
joint['children'] = [neck3]
joint['position'] = list(neck3['position'])
neck2['children'] = [c for c in neck2['children'] if c is not neck3] + [joint]
right = taken(find(ice['cubes'], 'Neck1'), 'ice', 'R_')
left = taken(find(light['cubes'], 'Neck1'), 'light', 'L_')
for neck, sign in ((right, 1), (left, -1)):
    neck['position'] = [-2.7 * sign, 0.8, 1.8]
    neck['rotation'] = [0.0, 30.0 * sign, -10.0 * sign]


def browed(head, prefix, shards):
    """An orb sunk in its brow (violet, it shines) and crystals breaking out of its skull behind it, each head's its own
    way (grown, not set: no two alike, nothing even like a crown): (x, how far back, pitch, yaw, roll, height)."""
    (ox, oy, oz), (w, _, d) = head['offset'], head['dimensions']
    head['children'].append(piece(prefix + 'Orb', (0.0, oy + 0.35, oz + 1.5), (0, 0, 0), (-1, -1, -1), (2, 2, 2), ORB_UV))
    for i, (x, back, pitch, yaw, roll, h) in enumerate(shards):
        head['children'].append(shard(f'{prefix}Crystal{i}', (x, oy + 0.4, oz + back), (pitch, yaw, roll), (1, h, 1)))


browed(find([middle], 'Head'), '', ((0.6, 2.6, -28, 0, -16, 3), (-0.7, 3.2, -48, -12, 22, 2), (0.1, 3.8, -64, 0, 4, 4)))
browed(find([right], 'R_Head'), 'R_', ((-0.8, 2.5, -36, 0, 24, 3), (0.5, 3.1, -22, 10, -10, 2)))
browed(find([left], 'L_Head'), 'L_', ((0.9, 2.8, -40, 8, -20, 2), (-0.3, 2.4, -26, 0, 8, 3), (-1.0, 3.6, -58, 0, 26, 2)))

# crystals bursting from its chest, between its necks and its arms
for i, (pos, rot, size) in enumerate((((0.3, 3.0, -0.4), (80, 6, -8), (2, 4, 2)), ((-1.5, 4.2, -0.3), (70, -22, 14), (1, 3, 1)),
                                      ((1.4, 4.8, -0.3), (88, 18, -6), (1, 2, 1)), ((-0.4, 5.6, -0.3), (100, -6, 10), (1, 2, 1)))):
    body['children'].append(shard(f'ChestCrystal{i}', pos, rot, size))
# small crystals along its spine, between its spikes
for i, (parent, pos) in enumerate(((body, (0.0, -1.5, 2.3)), (body, (0.0, -1.5, 5.0)), (lower, (0.0, -1.2, 1.2)), (lower, (0.0, -1.1, 4.7)))):
    parent['children'].append(shard(f'SpineCrystal{i}', pos, (-24, 0, (i % 2 * 2 - 1) * 12), (1, 2, 1)))
# its tail's end: crystals breaking out of its club
club = find([tail], 'Club')
(cx, cy, cz), (cw, ch, cd) = club['offset'], club['dimensions']
for i, (x, z, yaw, pitch, size) in enumerate(((0.0, cz + cd - 1.0, 0, -62, (2, 4, 2)), (-1.0, cz + 4.0, -30, -40, (1, 3, 1)),
                                              (1.0, cz + 4.0, 30, -40, (1, 3, 1)), (0.0, cz + 2.0, 0, -20, (1, 2, 1)))):
    club['children'].append(shard(f'TailCrystal{i}', (x, cy + 0.2, z), (pitch, yaw, 0), size))

body['children'] = [middle, right, left] + fronts + body['children']

# ---- its stance: its body raised on its long arms until their hands stand on the ground with its feet ----
is_foot = lambda n: n.startswith(('ToeR', 'ToeL', 'LegR', 'LegL'))
is_hand = lambda n: n.startswith(('HandR', 'HandL', 'TalonR', 'TalonL'))
# (the rest of its arms: their bones and folded skins)
is_arm = lambda n: n.startswith(('arm', 'finger', 'membrane'))


def swung(wing, fore, hand, rest, fore_rest, sign):
    """The arm as folded at rest, turned whole about its shoulder: its wrist toward REACH, its elbow toward ELBOW; its
    forearm rolled about itself (its wrist stays) so its folded fingers go up along it and back."""
    wing['rotation'], fore['rotation'] = list(rest), list(fore_rest)
    f = frames([body])
    shoulder, elbow, wrist = f[id(wing)][:3, 3], f[id(fore)][:3, 3], f[id(hand)][:3, 3]
    mirror = np.diag([sign, 1.0, 1.0])
    u, v = (wrist - shoulder) / np.linalg.norm(wrist - shoulder), mirror @ REACH / np.linalg.norm(REACH)
    axis = np.cross(u, v)
    q = about(axis, math.atan2(np.linalg.norm(axis), u @ v)) if np.linalg.norm(axis) > 1e-9 else np.eye(3)
    e, d = q @ (elbow - shoulder), mirror @ ELBOW
    e, d = e - (e @ v) * v, d - (d @ v) * v
    q = about(v, math.atan2(v @ np.cross(e, d), e @ d)) @ q
    parent = f[id(body)][:3, :3] / CORE
    wing['rotation'] = euler(parent.T @ q @ parent @ turned(*rest))
    m = frames([body])[id(fore)][:3, :3] / FRONT
    along, fingers, out = m[:, 1], m[:, 2], mirror @ np.array([-1.0, 0.0, 0.0])
    out = out - (out @ along) * along
    want = about(along, math.radians(CAPE) * sign) @ out
    fore['rotation'] = euler(turned(*fore_rest) @ about(np.array([0.0, 1.0, 0.0]), math.atan2(along @ np.cross(fingers, want), fingers @ want)))


def stand(tilt):
    """Its body raised this much (degrees), its legs kept upright, its arms swung under it, its hands flat (talons
    ahead, a little out), its feet on the ground: where its hands' lowest point then is."""
    body['rotation'][0] = BODY_REST - tilt
    for thigh in thighs:
        thigh['rotation'][0] = THIGH_REST + tilt
    for arm in arms:
        swung(*arm)
    for hand, fore, yaw in hands:
        m = frames([body])[id(fore)][:3, :3]
        hand['rotation'] = euler((m / np.linalg.norm(m[:, 0])).T @ turned(y=yaw))
    body['position'][1] += GROUND - lowest(corners([body]), is_foot)
    return lowest(corners([body]), is_hand)


TILT = min(range(0, 121), key=lambda t: abs(stand(t / 2) - GROUND)) / 2
stand(TILT)
body_pitch = BODY_REST - TILT


def bent(chain, pitches):
    """Each joint of a neck turned so it lies at this pitch in the world (degrees; up is negative): the head last."""
    before = body_pitch
    for cube, pitch in zip(chain, pitches):
        cube['rotation'][0] = pitch - before
        before = pitch


bent([middle, neck2, joint, neck3, find([middle], 'Head')], (-36, -48, -50, -40, 26))
for neck, prefix in ((right, 'R_'), (left, 'L_')):
    bent([neck] + [find([neck], prefix + n) for n in ('Neck2', 'Neck3', 'Head')], (-14, -24, -20, 26))


is_tail = lambda n: n.startswith(('Tail', 'Club', 'spike1', 'spike2'))


def tail_low(t):
    tail['rotation'][0] = TAIL_REST + t
    return lowest(corners([body]), is_tail)


# its tail along the ground: as low as it goes, not into it
tail['rotation'][0] = TAIL_REST + min((t / 2 for t in range(-80, 121)), key=lambda t: abs(tail_low(t) - (GROUND - 0.4)))
boxes = corners([body])
under = sorted(n for n, pts in boxes.items() if pts[:, 1].max() > GROUND + 0.3)
if under:
    print('under the ground:', ', '.join(under))
print('raised %.1f degrees; feet %.2f, hands %.2f, tail %.2f, back wings %.2f..%.2f (the ground %.0f)' % (
    TILT, lowest(boxes, is_foot), lowest(boxes, is_hand), lowest(boxes, is_tail),
    min(p[:, 1].min() for n, p in boxes.items() if n.startswith('B_')), lowest(boxes, lambda n: n.startswith('B_')), GROUND))

model = {
    'modelName': 'Tri_Dragon_Ground', 'authorName': 'GAMITA_MODELS', 'projVersion': 4, 'metadata': [],
    'textureWidth': PAGE_W * 3, 'textureHeight': PAGE_H * 2, 'scale': [1.0, 1.0, 1.0], 'cubeGroups': [],
    'cubes': [body], 'anims': [], 'cubeCount': sum(1 for _ in walk(body)),
}

# ---- its texture ----


def picture(kind, variant, eyes=False):
    return Image.open(TEX / f'{kind}dragon/{variant}_5{"_eyes" if eyes else ""}.png').convert('RGBA')


LUMA = np.array([0.299, 0.587, 0.114])


def palette(img):
    """A picture's colours from dark to light."""
    a = np.array(img).astype(np.float32)
    px = a[a[:, :, 3] > 0][:, :3]
    return px[np.argsort(px @ LUMA)]


def shaded(img, colours, region=None):
    """A picture in other colours, shade for shade: its darkest pixel the palette's darkest, its lightest the lightest."""
    a = np.array(img).astype(np.float32)
    seen = a[:, :, 3] > 0
    if region is not None:
        seen &= region
    px = a[seen][:, :3]
    if len(px) == 0:
        return img
    ranks = np.argsort(np.argsort(px @ LUMA, kind='stable'), kind='stable') / max(1, len(px) - 1)
    a[seen, :3] = colours[(ranks * (len(colours) - 1)).round().astype(int)]
    return Image.fromarray(a.round().clip(0, 255).astype(np.uint8))


def ramp(stops, n=256):
    stops = np.array(stops, dtype=np.float32)
    t = np.linspace(0, len(stops) - 1, n)
    i = np.minimum(t.astype(int), len(stops) - 2)
    f = (t - i)[:, None]
    return stops[i] * (1 - f) + stops[i + 1] * f


# old bone, dark at the root, pale toward the point
BONE = ramp([[30, 24, 28], [66, 58, 56], [112, 102, 92], [164, 154, 136], [206, 198, 178]])


def deep(img, light=0.5):
    """A picture darkened toward the abyss, its violet kept."""
    a = np.array(img).astype(np.float32)
    a[:, :, :3] *= np.array([0.9, 0.78, 1.08]) * light
    return Image.fromarray(a.clip(0, 255).round().astype(np.uint8))


def uv_mask(img, cubes, test):
    """Where the pieces passing the test have their pictures (their whole box UV patch)."""
    mask = np.zeros((img.height, img.width), dtype=bool)
    for c in cubes:
        if test(c):
            w, h, d = c['dimensions']
            u, v = c['txOffset']
            mask[int(v * PX):int((v + d + h) * PX), int(u * PX):int((u + 2 * (w + d)) * PX)] = True
    return mask


horny = lambda c: c['name'].startswith(('Horn', 'Claw', 'Toe'))


def boned(img, cubes):
    """Its horns, claws and toes in old bone."""
    return shaded(img, BONE, uv_mask(img, cubes, horny))


def grown(skin, shine=False):
    """Its own pieces' pictures: old bone, violet crystal (pale facets), the orbs (a bright heart), plain skin (a patch
    of its body's, over and over); what shines of them when shine."""
    a = np.zeros((PAGE_H * PX, PAGE_W * PX, 4), dtype=np.float32)
    noise = np.random.default_rng(7)
    ys, xs = np.mgrid[0:PAGE_H * PX, 0:PAGE_W * PX]
    # bone: pale at the point (the top of its patch), dark at the root
    u0, v0 = BONE_UV[0] * PX, BONE_UV[1] * PX
    box = (ys >= v0) & (ys < v0 + 32 * PX) & (xs >= u0) & (xs < u0 + 64 * PX)
    t = np.clip((ys - v0 - 2) / (10.0 * PX), 0, 1) + noise.normal(0, 0.06, ys.shape)
    k = (np.clip(1 - t * 0.85, 0, 1) * 255).astype(int)
    if not shine:
        a[box, :3], a[box, 3] = BONE[k[box]], 255
    # crystal: facets side by side, pale to deep violet, a light tip
    u0, v0 = CRYSTAL_UV[0] * PX, CRYSTAL_UV[1] * PX
    box = (ys >= v0) & (ys < v0 + 32 * PX) & (xs >= u0) & (xs < u0 + 64 * PX)
    facets = np.array([[60, 18, 104], [104, 40, 178], [150, 76, 226], [196, 136, 255]], dtype=np.float32)
    f = facets[(xs // PX + (ys // (3 * PX))) % 4] * (1.0 + 0.35 * np.clip(1 - (ys - v0) / (3.0 * PX), 0, 1))[..., None]
    a[box, :3], a[box, 3] = (f[box] * (0.85 if shine else 1.0)).clip(0, 255), 255
    # the orbs: deep violet, a pale heart
    u0, v0 = ORB_UV[0] * PX, ORB_UV[1] * PX
    box = (ys >= v0) & (ys < v0 + 16 * PX) & (xs >= u0) & (xs < u0 + 32 * PX)
    cx, cy = (xs % (2 * PX)) - PX + 0.5, (ys % (2 * PX)) - PX + 0.5
    core = (np.abs(cx) < PX * 0.6) & (np.abs(cy) < PX * 0.6)
    orb = np.where(core[..., None], np.array([240, 190, 255.0]), np.array([132, 48, 220.0]))
    a[box, :3], a[box, 3] = orb[box], 255
    # skin: a patch of its body's side, over and over
    if not shine:
        u0, v0 = SKIN_UV[0] * PX, SKIN_UV[1] * PX
        tile = np.array(skin).astype(np.float32)
        box = (ys >= v0) & (ys < v0 + 64 * PX) & (xs >= u0)
        a[box] = tile[(ys[box] - v0) % tile.shape[0], (xs[box] - u0) % tile.shape[1]]
    return Image.fromarray(a.round().clip(0, 255).astype(np.uint8))


def veins(img, cubes):
    """What shines in its wings: the lightest lines of their skins (their veins), dim violet; the rest dark. A skin's
    picture is on its west face only (the game draws that face from both sides); what shines is drawn one side at a time,
    so its veins go on the east face too, mirrored (the east face's picture is the west's turned over)."""
    a = np.array(img).astype(np.float32)
    out = np.zeros_like(a)
    for c in cubes:
        if c['dimensions'][0] != 0:
            continue
        mask = uv_mask(img, [c], lambda _: True) & (a[:, :, 3] > 0)
        lum = a[:, :, :3] @ LUMA
        if mask.any():
            line = mask & (lum > np.percentile(lum[mask], 91))
            out[line] = [96, 36, 170, 255]
            (u, v), (_, h, d) = c['txOffset'], c['dimensions']
            rows, west = slice(int((v + d) * PX), int((v + d + h) * PX)), slice(int(u * PX), int((u + d) * PX))
            out[rows, int((u + d) * PX):int((u + 2 * d) * PX)] = out[rows, west][:, ::-1]
    return Image.fromarray(out.astype(np.uint8))


amethyst = picture('lightning', 'amythest')
ABYSS = palette(deep(amethyst))
# its eyes: the violet of its crystals (the amethyst dragon's are blue)
EYES = ramp([[70, 14, 120], [150, 56, 228], [214, 150, 255], [246, 220, 255]])
light_cubes = list(walk(find(light['cubes'], 'BodyUpper')))
wing_cubes = [c for side in 'RL' for c in walk(find(light['cubes'], 'arm' + side + '1'))]
body_page = boned(deep(amethyst), light_cubes)
# (the skin: a patch of its body's side, BodyUpper's)
skin = body_page.crop((67 * PX, 54 * PX, 74 * PX, 61 * PX))
tex = Image.new('RGBA', (PAGE_W * 3 * PX, PAGE_H * 2 * PX), (0, 0, 0, 0))
for page, img in (('body', body_page), ('fire', boned(shaded(picture('fire', 'red'), ABYSS), list(walk(find(fire['cubes'], 'BodyUpper'))))),
                  ('wing', boned(deep(amethyst, 0.42), wing_cubes)),
                  ('ice', boned(shaded(picture('ice', 'sapphire'), ABYSS), list(walk(find(ice['cubes'], 'BodyUpper'))))),
                  ('light', body_page), ('extra', grown(skin))):
    col, row = PAGES[page]
    tex.paste(img, (col * PAGE_W * PX, row * PAGE_H * PX))

# what shines on it (drawn full bright over it, added to it): each head's eyes (violet), its wings' veins, its crystals
# and orbs
eyes = Image.new('RGBA', tex.size, (0, 0, 0, 0))
for page, img in (('fire', shaded(picture('fire', 'red', True), EYES)), ('ice', shaded(picture('ice', 'sapphire', True), EYES)),
                  ('light', shaded(picture('lightning', 'amythest', True), EYES)), ('wing', veins(amethyst, wing_cubes)),
                  ('extra', grown(skin, shine=True))):
    col, row = PAGES[page]
    eyes.paste(img, (col * PAGE_W * PX, row * PAGE_H * PX))

OUT_TEX.mkdir(parents=True, exist_ok=True)
tex.save(OUT_TEX / 'tridragon.png')
eyes.save(OUT_TEX / 'tridragon_eyes.png')
OUT_TBL.mkdir(parents=True, exist_ok=True)
buf = io.BytesIO()
tex.save(buf, 'PNG')
# (its entries dated the same every time: the same model makes the same file)
with zipfile.ZipFile(OUT_TBL / 'tridragon_ground.tbl', 'w', zipfile.ZIP_DEFLATED) as z:
    for name, data in (('model.json', json.dumps(model)), ('texture.png', buf.getvalue())):
        z.writestr(zipfile.ZipInfo(name, (2026, 1, 1, 0, 0, 0)), data, zipfile.ZIP_DEFLATED)
print('tridragon:', model['cubeCount'], 'pieces, texture', tex.size)


# ---- the same model for Blockbench (its Generic Model format, as the user's reference): one bone per piece, its box inside


def bbmodel(model, png, path):
    """Each piece a bone (pivot and turn as the game's: x and y flipped, roots 24 over the ground) holding its box; a
    piece's own scale (the arms, wings, tail) is baked into its boxes, each face's UV where the game's box UV puts it for
    the box's real size. (The Generic Model format keeps each face's own UV; Modded Entity makes every box box UV again
    from its scaled size, so the scaled pieces showed other parts of the picture, the tail clear through in places.)"""
    import base64
    import uuid
    seen = {}
    elements = []

    def uid():
        return str(uuid.UUID(int=rng.getrandbits(128)))

    def unique(name):
        n = seen.get(name, 0)
        seen[name] = n + 1
        return name if n == 0 else f'{name}_{n + 1}'

    def faces(w, h, d, u, v, mirror):
        """Blockbench's box UV layout (Cube.mapAutoUV)."""
        lay = [['east', [0, d], [d, h]], ['west', [d + w, d], [d, h]], ['up', [d + w, d], [-w, -d]], ['down', [d + 2 * w, 0], [-w, d]],
               ['south', [2 * d + w, d], [w, h]], ['north', [d, d], [w, h]]]
        if mirror:
            for f in lay:
                f[1][0] += f[2][0]
                f[2][0] *= -1
            lay[0][0], lay[1][0] = 'west', 'east'
        return {name: {'uv': [u + fr[0], v + fr[1], u + fr[0] + sz[0], v + fr[1] + sz[1]], 'texture': 0} for name, fr, sz in lay}

    def emit(cube, parent_origin, parent_scale, root):
        px, py, pz = cube['position']
        if root:
            origin = [-px, 24 - py, pz]
        else:
            origin = [parent_origin[0] - px * parent_scale, parent_origin[1] - py * parent_scale, parent_origin[2] + pz * parent_scale]
        own = parent_scale * cube['scale'][0]
        rx, ry, rz = cube['rotation']
        ox, oy, oz = cube['offset']
        w, h, d = cube['dimensions']
        u, v = cube['txOffset']
        box = {'name': unique(cube['name']), 'box_uv': own == 1, 'rescale': False, 'locked': False, 'render_order': 'default',
               'allow_mirror_modeling': True, 'inflate': cube.get('mcScale', 0.0),
               'from': [origin[0] - (ox + w) * own, origin[1] - (oy + h) * own, origin[2] + oz * own],
               'to': [origin[0] - ox * own, origin[1] - oy * own, origin[2] + (oz + d) * own],
               'autouv': 0, 'color': len(elements) % 8, 'origin': list(origin), 'uv_offset': [u, v],
               'mirror_uv': bool(cube.get('txMirror')), 'faces': faces(w, h, d, u, v, bool(cube.get('txMirror'))), 'type': 'cube',
               'uuid': uid()}
        elements.append(box)
        group = {'name': box['name'], 'origin': list(origin), 'rotation': [-rx, -ry, rz], 'color': 0, 'uuid': uid(), 'export': True,
                 'mirror_uv': False, 'isOpen': False, 'locked': False, 'visibility': True, 'autouv': 0, 'children': [box['uuid']]}
        for child in cube.get('children', []):
            group['children'].append(emit(child, origin, own, False))
        return group

    outliner = [emit(c, None, 1.0, True) for c in model['cubes']]
    project = {
        'meta': {'format_version': '4.10', 'model_format': 'free', 'box_uv': False},
        'name': 'tridragon', 'model_identifier': '', 'visible_box': [1, 1, 0],
        'resolution': {'width': model['textureWidth'], 'height': model['textureHeight']},
        'elements': elements, 'outliner': outliner,
        'textures': [{'path': '', 'name': 'tridragon.png', 'folder': '', 'namespace': '', 'id': '0', 'width': PAGE_W * 3 * PX,
                      'height': PAGE_H * 2 * PX, 'uv_width': model['textureWidth'], 'uv_height': model['textureHeight'],
                      'particle': False, 'render_mode': 'default', 'render_sides': 'auto', 'visible': True, 'internal': True,
                      'saved': False, 'uuid': uid(), 'source': 'data:image/png;base64,' + base64.b64encode(png).decode()}],
    }
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(project), encoding='utf-8')
    print('blockbench:', path, len(elements), 'boxes')


# (Blockbench draws one picture: what shines on it painted into it, as the game shows it)
lit = np.array(tex).astype(np.float32)
glow = np.array(eyes).astype(np.float32)
lit[:, :, :3] += glow[:, :, :3] * glow[:, :, 3:] / 255.0
shown = io.BytesIO()
Image.fromarray(lit.clip(0, 255).round().astype(np.uint8)).save(shown, 'PNG')
bbmodel(model, shown.getvalue(), ROOT / 'tools/models/tridragon.bbmodel')

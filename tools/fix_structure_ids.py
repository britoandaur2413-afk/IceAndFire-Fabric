"""Renames the 1.18 ids (iceandfire:...) inside the structure templates to the port's (iceandfire_fabric:...).

NBT strings are length-prefixed (2 bytes, big endian), so each renamed string gets its new length.
usage: python -I tools/fix_structure_ids.py
"""
import glob
import gzip
import pathlib
import re
import struct

ROOT = pathlib.Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "data" / "iceandfire_fabric" / "structure"
RENAMED = {b"iceandfire:iaf_lectern": b"iceandfire_fabric:lectern"}
PATTERN = re.compile(rb"(..)(iceandfire:[a-z_0-9/]+)", re.S)


def fix(data):
    count = 0

    def repl(m):
        nonlocal count
        (length,) = struct.unpack(">H", m.group(1))
        old = m.group(2)
        if length != len(old):
            return m.group(0)
        new = RENAMED.get(old, b"iceandfire_fabric:" + old[len(b"iceandfire:"):])
        count += 1
        return struct.pack(">H", len(new)) + new

    return PATTERN.sub(repl, data), count


for path in sorted(ROOT.glob("**/*.nbt")):
    data = gzip.decompress(path.read_bytes())
    new, count = fix(data)
    if count:
        path.write_bytes(gzip.compress(new))
    left = len(re.findall(rb"iceandfire:", new))
    print("%s: %d renamed, %d left" % (path.relative_to(ROOT), count, left))

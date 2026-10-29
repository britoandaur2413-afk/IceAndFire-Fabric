"""A small reader and writer for gzipped NBT files (structure templates)."""
import gzip
import struct


class Tag:
    def __init__(self, kind, value, list_kind=None):
        self.kind = kind
        self.value = value
        self.list_kind = list_kind

    def __repr__(self):
        return f"Tag({self.kind}, {self.value!r})"


def _read(b, i, kind):
    if kind == 1:
        return Tag(1, struct.unpack_from(">b", b, i)[0]), i + 1
    if kind == 2:
        return Tag(2, struct.unpack_from(">h", b, i)[0]), i + 2
    if kind == 3:
        return Tag(3, struct.unpack_from(">i", b, i)[0]), i + 4
    if kind == 4:
        return Tag(4, struct.unpack_from(">q", b, i)[0]), i + 8
    if kind == 5:
        return Tag(5, struct.unpack_from(">f", b, i)[0]), i + 4
    if kind == 6:
        return Tag(6, struct.unpack_from(">d", b, i)[0]), i + 8
    if kind == 7:
        n = struct.unpack_from(">i", b, i)[0]
        return Tag(7, b[i + 4:i + 4 + n]), i + 4 + n
    if kind == 8:
        n = struct.unpack_from(">H", b, i)[0]
        return Tag(8, b[i + 2:i + 2 + n].decode("utf-8", "surrogatepass")), i + 2 + n
    if kind == 9:
        lk = b[i]
        n = struct.unpack_from(">i", b, i + 1)[0]
        i += 5
        out = []
        for _ in range(n):
            t, i = _read(b, i, lk)
            out.append(t)
        return Tag(9, out, lk), i
    if kind == 10:
        out = {}
        while True:
            k = b[i]
            i += 1
            if k == 0:
                return Tag(10, out), i
            n = struct.unpack_from(">H", b, i)[0]
            name = b[i + 2:i + 2 + n].decode("utf-8", "surrogatepass")
            i += 2 + n
            t, i = _read(b, i, k)
            out[name] = t
    if kind == 11:
        n = struct.unpack_from(">i", b, i)[0]
        return Tag(11, list(struct.unpack_from(f">{n}i", b, i + 4))), i + 4 + 4 * n
    if kind == 12:
        n = struct.unpack_from(">i", b, i)[0]
        return Tag(12, list(struct.unpack_from(f">{n}q", b, i + 4))), i + 4 + 8 * n
    raise ValueError(f"tag kind {kind}")


def _write(t, out):
    k = t.kind
    if k == 1:
        out += struct.pack(">b", t.value)
    elif k == 2:
        out += struct.pack(">h", t.value)
    elif k == 3:
        out += struct.pack(">i", t.value)
    elif k == 4:
        out += struct.pack(">q", t.value)
    elif k == 5:
        out += struct.pack(">f", t.value)
    elif k == 6:
        out += struct.pack(">d", t.value)
    elif k == 7:
        out += struct.pack(">i", len(t.value)) + t.value
    elif k == 8:
        s = t.value.encode("utf-8", "surrogatepass")
        out += struct.pack(">H", len(s)) + s
    elif k == 9:
        lk = t.list_kind if t.value == [] else t.value[0].kind
        out += bytes([lk or 0]) + struct.pack(">i", len(t.value))
        for v in t.value:
            _write(v, out)
    elif k == 10:
        for name, v in t.value.items():
            s = name.encode("utf-8", "surrogatepass")
            out += bytes([v.kind]) + struct.pack(">H", len(s)) + s
            _write(v, out)
        out += b"\0"
    elif k == 11:
        out += struct.pack(f">i{len(t.value)}i", len(t.value), *t.value)
    elif k == 12:
        out += struct.pack(f">i{len(t.value)}q", len(t.value), *t.value)


def load(path):
    b = gzip.decompress(open(path, "rb").read())
    assert b[0] == 10
    n = struct.unpack_from(">H", b, 1)[0]
    t, _ = _read(b, 3 + n, 10)
    return t


def save(path, root):
    out = bytearray([10]) + struct.pack(">H", 0)
    _write(root, out)
    open(path, "wb").write(gzip.compress(bytes(out)))


def plain(t):
    """The tag as plain Python values (for looking at it)."""
    if t.kind == 10:
        return {k: plain(v) for k, v in t.value.items()}
    if t.kind == 9:
        return [plain(v) for v in t.value]
    return t.value

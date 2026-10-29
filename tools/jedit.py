"""Small Java text helpers: matching brackets (skipping strings, chars and comments), argument spans, bodies."""

OPEN = {"(": ")", "{": "}", "[": "]"}


def skip_literal(text, i):
    """If text[i] starts a string, char or comment, the index after it; else i."""
    c = text[i]
    if text.startswith('"""', i):
        j = text.find('"""', i + 3)
        return len(text) if j < 0 else j + 3
    if c == '"' or c == "'":
        j = i + 1
        while j < len(text) and text[j] != c:
            j += 2 if text[j] == "\\" else 1
        return j + 1
    if text.startswith("//", i):
        j = text.find("\n", i)
        return len(text) if j < 0 else j
    if text.startswith("/*", i):
        j = text.find("*/", i + 2)
        return len(text) if j < 0 else j + 2
    return i


def match(text, i):
    """Index of the bracket closing the one at text[i]."""
    stack = [OPEN[text[i]]]
    j = i + 1
    while j < len(text):
        k = skip_literal(text, j)
        if k != j:
            j = k
            continue
        c = text[j]
        if c in OPEN:
            stack.append(OPEN[c])
        elif c in ")}]":
            if not stack or stack[-1] != c:
                raise ValueError(f"unbalanced at {j}")
            stack.pop()
            if not stack:
                return j
        j += 1
    raise ValueError("no match")


def args(text, open_idx):
    """Spans (start, end) of the top-level arguments of the call whose '(' is at open_idx."""
    close = match(text, open_idx)
    spans = []
    start = open_idx + 1
    j = start
    depth = 0
    while j < close:
        k = skip_literal(text, j)
        if k != j:
            j = k
            continue
        c = text[j]
        if c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
        elif c == "<":
            pass
        elif c == "," and depth == 0:
            spans.append((start, j))
            start = j + 1
        j += 1
    if text[start:close].strip():
        spans.append((start, close))
    return spans, close


def drop_arg(text, open_idx, n=0):
    """The text with argument n of the call at open_idx removed."""
    spans, close = args(text, open_idx)
    if n >= len(spans):
        return text
    s, e = spans[n]
    if n + 1 < len(spans):
        # drop "arg, "
        e = spans[n + 1][0]
        while e < close and text[e] == " ":
            e += 1
        return text[:s] + text[e:]
    if n > 0:
        s = spans[n - 1][1]
    return text[:s] + text[e:]


def body(text, from_idx):
    """(open, close) of the first {...} block at or after from_idx."""
    o = text.index("{", from_idx)
    return o, match(text, o)

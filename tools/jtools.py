"""Small Java source helpers shared by the conversion scripts (no real parser, just balanced scanning)."""
import re

IDENT = re.compile(r"[A-Za-z_$][\w$]*")


def skip_string(text, i):
    """i points at a quote char, return index after the closing quote"""
    q = text[i]
    i += 1
    n = len(text)
    while i < n:
        c = text[i]
        if c == "\\":
            i += 2
            continue
        if c == q:
            return i + 1
        i += 1
    return n


def match_paren(text, i):
    """i points at '(' or '[' or '{', return index of the matching closer"""
    pairs = {"(": ")", "[": "]", "{": "}"}
    stack = []
    n = len(text)
    while i < n:
        c = text[i]
        if c in "\"'":
            i = skip_string(text, i)
            continue
        if c == "/" and i + 1 < n and text[i + 1] == "/":
            j = text.find("\n", i)
            i = n if j < 0 else j
            continue
        if c == "/" and i + 1 < n and text[i + 1] == "*":
            j = text.find("*/", i + 2)
            i = n if j < 0 else j + 2
            continue
        if c in pairs:
            stack.append(pairs[c])
        elif c in ")]}":
            if not stack:
                return -1
            exp = stack.pop()
            if c != exp:
                return -1
            if not stack:
                return i
        i += 1
    return -1


def split_args(argtext):
    """split a call's argument text on top-level commas"""
    args = []
    depth = 0
    cur = []
    i = 0
    n = len(argtext)
    angle = 0
    while i < n:
        c = argtext[i]
        if c in "\"'":
            j = skip_string(argtext, i)
            cur.append(argtext[i:j])
            i = j
            continue
        if c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
        if c == "," and depth == 0:
            args.append("".join(cur).strip())
            cur = []
        else:
            cur.append(c)
        i += 1
    last = "".join(cur).strip()
    if last or args:
        args.append(last)
    return args


def find_receiver_start(text, dot):
    """text[dot] == '.', return start index of the receiver expression before it"""
    i = dot - 1
    while i >= 0 and text[i] in " \t\r\n":
        i -= 1
    while i >= 0:
        c = text[i]
        if c in ")]":
            # find matching opener backwards
            closer = c
            opener = "(" if c == ")" else "["
            depth = 0
            while i >= 0:
                if text[i] == closer:
                    depth += 1
                elif text[i] == opener:
                    depth -= 1
                    if depth == 0:
                        break
                i -= 1
            i -= 1
            # method name before '('
            while i >= 0 and (text[i].isalnum() or text[i] in "_$"):
                i -= 1
        elif c.isalnum() or c in "_$":
            while i >= 0 and (text[i].isalnum() or text[i] in "_$"):
                i -= 1
        else:
            break
        # continue over a dot chain
        j = i
        while j >= 0 and text[j] in " \t\r\n":
            j -= 1
        if j >= 0 and text[j] == ".":
            i = j - 1
            while i >= 0 and text[i] in " \t\r\n":
                i -= 1
            continue
        break
    start = i + 1
    # a cast like ((Foo) bar).x is handled by the paren branch; strip leading spaces
    while start < dot and text[start] in " \t\r\n":
        start += 1
    # 'new X(...)' receiver
    m = re.search(r"new\s+$", text[max(0, start - 8):start])
    if m:
        start = start - (len(text[max(0, start - 8):start]) - m.start())
    return start


def statement_end(text, i):
    """return index of the ';' ending the statement that contains index i (top-level)"""
    depth = 0
    n = len(text)
    while i < n:
        c = text[i]
        if c in "\"'":
            i = skip_string(text, i)
            continue
        if c in "([{":
            depth += 1
        elif c in ")]}":
            if depth == 0:
                return i
            depth -= 1
        elif c == ";" and depth == 0:
            return i
        elif c == "," and depth == 0:
            return i
        i += 1
    return n


def line_col_to_index(text, line, col):
    """1-based line, 0-based col"""
    idx = 0
    for _ in range(line - 1):
        idx = text.index("\n", idx) + 1
    return idx + col


def segments(text):
    """yield (is_code, chunk)"""
    i = 0
    n = len(text)
    start = 0
    while i < n:
        c = text[i]
        if c in "\"'":
            if start < i:
                yield True, text[start:i]
            j = skip_string(text, i)
            yield False, text[i:j]
            i = start = j
            continue
        if c == "/" and i + 1 < n and text[i + 1] in "/*":
            if start < i:
                yield True, text[start:i]
            if text[i + 1] == "/":
                j = text.find("\n", i)
                j = n if j < 0 else j
            else:
                j = text.find("*/", i + 2)
                j = n if j < 0 else j + 2
            yield False, text[i:j]
            i = start = j
            continue
        i += 1
    if start < n:
        yield True, text[start:]


def sub_code(pattern, repl, text, flags=0):
    rx = re.compile(pattern, flags) if isinstance(pattern, str) else pattern
    out = []
    for is_code, chunk in segments(text):
        out.append(rx.sub(repl, chunk) if is_code else chunk)
    return "".join(out)



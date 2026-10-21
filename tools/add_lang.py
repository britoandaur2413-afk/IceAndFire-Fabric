"""Adds texts to en_us.json and every es_*.json (keeps the files' layout; a key already there is changed).
    python tools/add_lang.py keys.json
keys.json: {"some.key": ["English text", "Texto en español"], ...}"""
import glob
import json
import os
import re
import sys

LANG = 'src/main/resources/assets/iceandfire_fabric/lang/'
LINE = re.compile(r'^(\s*)"((?:[^"\\]|\\.)*)"(\s*:\s*)"((?:[^"\\]|\\.)*)"(,?)(\s*)$')


def put(path, values):
    with open(path, encoding='utf-8') as f:
        lines = f.read().split('\n')
    done = set()
    for i, line in enumerate(lines):
        m = LINE.match(line)
        if m:
            key = json.loads('"' + m.group(2) + '"')
            if key in values:
                lines[i] = m.group(1) + '"' + m.group(2) + '"' + m.group(3) + json.dumps(values[key], ensure_ascii=False) + m.group(5) + m.group(6)
                done.add(key)
    missing = [(k, v) for k, v in values.items() if k not in done]
    if missing:
        end = max(i for i, l in enumerate(lines) if l.strip() == '}')
        last = max(i for i in range(end) if lines[i].strip())
        if not lines[last].rstrip().endswith(','):
            lines[last] = lines[last].rstrip() + ','
        added = ['  %s: %s,' % (json.dumps(k, ensure_ascii=False), json.dumps(v, ensure_ascii=False)) for k, v in missing]
        added[-1] = added[-1][:-1]
        lines[last + 1:last + 1] = added
    text = '\n'.join(lines)
    json.loads(text)
    with open(path, 'w', encoding='utf-8', newline='') as f:
        f.write(text)


def main():
    with open(sys.argv[1], encoding='utf-8') as f:
        keys = json.load(f)
    put(LANG + 'en_us.json', {k: v[0] for k, v in keys.items()})
    for path in sorted(glob.glob(LANG + 'es_*.json')):
        put(path, {k: v[1] for k, v in keys.items()})
    print(f'{len(keys)} texts in en_us and {len(glob.glob(LANG + "es_*.json"))} Spanish files')


if __name__ == '__main__':
    main()

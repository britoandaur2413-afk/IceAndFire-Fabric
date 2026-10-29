"""Checks the translations: every key the code and the data ask for is in en_us and es_es, the lang files
are valid JSON, and every language has every English key. Run from the project folder:
    python tools/check_lang.py
Prints each problem and the count; exit code 1 when there is one."""
import glob
import json
import os
import re
import sys

LANG = 'src/main/resources/assets/iceandfire_fabric/lang/'
# Strings that look like keys but are not text (an NBT tag name, a file name).
NOT_TEXT = {'iceandfire_fabric.bolt_skip_loot', 'iceandfire_fabric.json'}
PREFIXES = ('iceandfire_fabric', 'item', 'block', 'entity', 'message', 'gui', 'container', 'bestiary', 'dragon', 'key',
            'effect', 'death', 'advancements', 'silvertools', 'myrmex', 'ingredient', 'tooltip', 'itemGroup')


def slash(path):
    return path.replace(os.sep, '/')


def load(name):
    with open(LANG + name, encoding='utf-8') as f:
        return json.load(f)


def used_keys():
    used = {}
    call = re.compile(r'(?:translatable(?:WithFallback)?|I18n\.get|I18n\.exists)\(\s*"([^"]+)"')
    literal = re.compile(r'"((?:' + '|'.join(PREFIXES) + r')\.[A-Za-z0-9_.]+)"')
    for root in ('src/main/java', 'src/client/java'):
        for path in glob.glob(root + '/**/*.java', recursive=True):
            with open(path, encoding='utf-8') as f:
                text = f.read()
            for pattern in (call, literal):
                for m in pattern.finditer(text):
                    used.setdefault(m.group(1), slash(path))
    for path in glob.glob('src/main/resources/data/**/*.json', recursive=True):
        with open(path, encoding='utf-8') as f:
            text = f.read()
        for m in re.finditer(r'"(?:translate|translation_key)"\s*:\s*"([^"]+)"', text):
            used.setdefault(m.group(1), slash(path))
    for path in glob.glob('src/main/resources/assets/**/sounds.json', recursive=True):
        with open(path, encoding='utf-8') as f:
            text = f.read()
        for m in re.finditer(r'"subtitle"\s*:\s*"([^"]+)"', text):
            used.setdefault(m.group(1), slash(path))
    return used


def main():
    problems = []
    langs = {}
    for path in sorted(glob.glob(LANG + '*.json')):
        name = os.path.basename(path)
        try:
            langs[name] = load(name)
        except ValueError as e:
            problems.append(f'{name}: not valid JSON ({e})')
    en = langs.get('en_us.json', {})
    used = used_keys()
    # Keys built from parts in code ("item.iceandfire_fabric." + name) end in a dot: not checkable here.
    for key, where in sorted(used.items()):
        if key.endswith('.') or '.minecraft.' in key or key.startswith('key.keyboard.') or key.startswith('key.mouse.'):
            continue
        if key.endswith('_') or key in NOT_TEXT:
            continue
        for name in ('en_us.json', 'es_es.json'):
            if name in langs and key not in langs[name]:
                problems.append(f'{name}: missing {key} (used in {where.split("/")[-1]})')
    # every language has every English text (the mod's own ones: tools/translations, put by tools/put_translations.py)
    for name, keys in langs.items():
        for key in en:
            if key not in keys:
                problems.append(f'{name}: missing {key}')
    # A translation takes as many arguments (%s, %d, %1$s) as the English text, or the game shows an error.
    spec = re.compile(r'%(?:\d+\$)?[sdf]')
    for name, keys in langs.items():
        for key, value in keys.items():
            if key in en and len(spec.findall(value.replace('%%', ''))) != len(spec.findall(en[key].replace('%%', ''))):
                problems.append(f'{name}: {key} has other arguments than in English: {value!r}')
    for p in problems:
        print(p)
    print(f'{len(problems)} problems')
    return 1 if problems else 0


if __name__ == '__main__':
    sys.exit(main())

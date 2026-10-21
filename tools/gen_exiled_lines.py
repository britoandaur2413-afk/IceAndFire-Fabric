"""Writes an exiled pixie's lines (tools/exiled_lines_data.py) to the lang files and how many there are of each kind
to ExiledLines.java. Lines of a kind that no longer exist are taken out of the lang files. Run from the project folder:
    python tools/gen_exiled_lines.py"""
import glob
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import add_lang  # noqa: E402
import exiled_lines_data as data  # noqa: E402

LANG = 'src/main/resources/assets/iceandfire_fabric/lang/'
JAVA = 'src/main/java/com/iafabric/pixie/ExiledLines.java'
PREFIXES = ('exiled.iceandfire_fabric.sees.', 'exiled.iceandfire_fabric.friends.', 'exiled.iceandfire_fabric.bond.')
LINE = re.compile(r'^\s*"((?:[^"\\]|\\.)*)"\s*:')


def keys():
    out = {}

    def put(key, lines):
        for i, (es, en) in enumerate(lines):
            for text in (es, en):
                assert '%' not in text, (key, i)
                assert len(text) <= 120, (key, i, len(text))
            out['%s.%d' % (key, i)] = [en, es]

    for kind, lines in data.POOLS.items():
        put('exiled.iceandfire_fabric.sees.' + kind, lines)
    put('exiled.iceandfire_fabric.friends.sad', data.FRIENDS_SAD)
    put('exiled.iceandfire_fabric.friends.look', data.FRIENDS_LOOK)
    put('exiled.iceandfire_fabric.friends.cake', data.FRIENDS_CAKE)
    put('exiled.iceandfire_fabric.bond', data.BOND)
    return out


def drop_stale(path, wanted):
    """Takes out of a lang file the lines of these kinds that are not wanted any more."""
    with open(path, encoding='utf-8') as f:
        lines = f.read().split('\n')
    kept = []
    for line in lines:
        m = LINE.match(line)
        if m:
            key = json.loads('"' + m.group(1) + '"')
            if key.startswith(PREFIXES) and key not in wanted:
                continue
        kept.append(line)
    # the last entry has no comma
    end = max(i for i, l in enumerate(kept) if l.strip() == '}')
    last = max(i for i in range(end) if kept[i].strip())
    kept[last] = kept[last].rstrip().rstrip(',')
    text = '\n'.join(kept)
    json.loads(text)
    with open(path, 'w', encoding='utf-8', newline='') as f:
        f.write(text)


def java():
    entries = ',\n'.join('\t\tMap.entry("%s", %d)' % (kind, len(lines)) for kind, lines in data.POOLS.items())
    ours = ', '.join('"%s"' % kind for kind in data.OURS)
    return '''package com.iafabric.pixie;

import java.util.Map;
import java.util.Set;

/**
 * How many lines of each kind an exiled pixie has (their texts are in the lang files: exiled.iceandfire_fabric.sees.KIND.N,
 * friends.sad/look/cake.N, bond.N). Made by tools/gen_exiled_lines.py from tools/exiled_lines_data.py: change them
 * there.
 */
public final class ExiledLines {
	/** Her lines about each kind of creature (a creature of the game's own is its id's name; villagers and witches too). */
	public static final Map<String, Integer> SEES = Map.ofEntries(
%s);
	/** The kinds that are Ice and Fire's creatures. */
	public static final Set<String> OURS = Set.of(%s);
	/** At her friends' sanctuary: a sad word, how the young pixies look at her, the cake. */
	public static final int FRIENDS_SAD = %d;
	public static final int FRIENDS_LOOK = %d;
	public static final int FRIENDS_CAKE = %d;
	/** When her player bonds another pixie. */
	public static final int BOND = %d;

	private ExiledLines() {
	}
}
''' % (entries, ours, len(data.FRIENDS_SAD), len(data.FRIENDS_LOOK), len(data.FRIENDS_CAKE), len(data.BOND))


def main():
    wanted = keys()
    for path in [LANG + 'en_us.json'] + sorted(glob.glob(LANG + 'es_*.json')):
        drop_stale(path, wanted)
    add_lang.put(LANG + 'en_us.json', {k: v[0] for k, v in wanted.items()})
    for path in sorted(glob.glob(LANG + 'es_*.json')):
        add_lang.put(path, {k: v[1] for k, v in wanted.items()})
    with open(JAVA, 'w', encoding='utf-8', newline='\n') as f:
        f.write(java())
    print('%d lines (%d kinds of creatures) in the lang files; %s written' % (len(wanted), len(data.POOLS), JAVA))


if __name__ == '__main__':
    main()

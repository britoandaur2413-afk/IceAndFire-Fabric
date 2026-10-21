"""Puts the mod's own texts into the languages besides English and Spanish: tools/translations/<lang>.json (written
by hand, {key: text}) and the banner patterns' names (made here from each language's colors and pattern names, in
its own word order). A text a language file already has is kept, unless OVERRIDE lists its key. Run from the
project folder:
    python tools/put_translations.py
Then python tools/check_lang.py."""
import glob
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import add_lang  # noqa: E402

LANG = 'src/main/resources/assets/iceandfire_fabric/lang/'
HERE = os.path.dirname(os.path.abspath(__file__))

COLORS = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple',
          'blue', 'brown', 'green', 'red', 'black']
PATTERNS = ['amphithere', 'bird', 'dread', 'eye', 'fae', 'feather', 'fire', 'fire_head', 'gorgon', 'hippocampus',
            'hippogryph_head', 'ice', 'ice_head', 'lightning', 'lightning_head', 'mermaid', 'sea_serpent', 'troll',
            'weezer']

# Each language: its 16 colors (in COLORS' order, in the form its banner names need), its 19 pattern names (in
# PATTERNS' order) and how the two go together.
BANNERS = {
    'de_de': (
        ['Weißes', 'Orangefarbenes', 'Magentafarbenes', 'Hellblaues', 'Gelbes', 'Hellgrünes', 'Rosafarbenes', 'Graues',
         'Hellgraues', 'Türkises', 'Violettes', 'Blaues', 'Braunes', 'Grünes', 'Rotes', 'Schwarzes'],
        ['Amphithere-Wappen', 'Vogel-Wappen', 'Grauen-Wappen', 'Zyklopenaugen-Wappen', 'Feen-Wappen', 'Feder-Wappen',
         'Feuerdrachen-Wappen', 'Feuerdrachenkopf-Wappen', 'Gorgonenkopf-Wappen', 'Hippokamp-Wappen',
         'Hippogryphenkopf-Wappen', 'Eisdrachen-Wappen', 'Eisdrachenkopf-Wappen', 'Blitzdrachen-Wappen',
         'Blitzdrachenkopf-Wappen', 'Sirenen-Wappen', 'Seeschlangen-Wappen', 'Trollkopf-Wappen', 'Weezer-Wappen'],
        '{color} {pattern}'),
    'fr_fr': (
        ['blanc', 'orange', 'magenta', 'bleu clair', 'jaune', 'vert clair', 'rose', 'gris', 'gris clair', 'cyan',
         'violet', 'bleu', 'marron', 'vert', 'rouge', 'noir'],
        ["Motif d'amphiptère", "Motif d'oiseau", "Motif de l'Épouvante", "Motif d'œil de cyclope", 'Motif de fée',
         'Motif de plume', 'Motif de dragon de feu', 'Motif de tête de dragon de feu', 'Motif de tête de gorgone',
         "Motif d'hippocampe", "Motif de tête d'hippogriffe", 'Motif de dragon de glace',
         'Motif de tête de dragon de glace', 'Motif de dragon électrique', 'Motif de tête de dragon électrique',
         'Motif de sirène', 'Motif de serpent de mer', 'Motif de tête de troll', 'Motif Weezer'],
        '{pattern} {color}'),
    'pt_br': (
        ['branco', 'laranja', 'magenta', 'azul-claro', 'amarelo', 'verde-limão', 'rosa', 'cinza', 'cinza-claro', 'ciano',
         'roxo', 'azul', 'marrom', 'verde', 'vermelho', 'preto'],
        ['Padrão de anfíptero', 'Padrão de ave', 'Padrão medonho', 'Padrão de olho de ciclope', 'Padrão de fada',
         'Padrão de pena', 'Padrão de dragão de fogo', 'Padrão de cabeça de dragão de fogo',
         'Padrão de cabeça de górgona', 'Padrão de hipocampo', 'Padrão de cabeça de hipogrifo',
         'Padrão de dragão de gelo', 'Padrão de cabeça de dragão de gelo', 'Padrão de dragão elétrico',
         'Padrão de cabeça de dragão elétrico', 'Padrão de sirena', 'Padrão de serpente marinha',
         'Padrão de cabeça de trol', 'Padrão Weezer'],
        '{pattern} {color}'),
    'pl_pl': (
        ['Biały', 'Pomarańczowy', 'Karmazynowy', 'Jasnoniebieski', 'Żółty', 'Jasnozielony', 'Różowy', 'Szary',
         'Jasnoszary', 'Turkusowy', 'Fioletowy', 'Niebieski', 'Brązowy', 'Zielony', 'Czerwony', 'Czarny'],
        ['wzór amfitery', 'wzór ptaka', 'wzór grozy', 'wzór oka cyklopa', 'wzór wróżki', 'wzór pióra',
         'wzór ognistego smoka', 'wzór głowy ognistego smoka', 'wzór głowy gorgony', 'wzór hipokampa',
         'wzór głowy hipogryfa', 'wzór lodowego smoka', 'wzór głowy lodowego smoka', 'wzór burzowego smoka',
         'wzór głowy burzowego smoka', 'wzór syreny', 'wzór węża morskiego', 'wzór głowy trola', 'wzór Weezer'],
        '{color} {pattern}'),
    'ru_ru': (
        ['Белый', 'Оранжевый', 'Пурпурный', 'Голубой', 'Жёлтый', 'Лаймовый', 'Розовый', 'Серый', 'Светло-серый',
         'Бирюзовый', 'Фиолетовый', 'Синий', 'Коричневый', 'Зелёный', 'Красный', 'Чёрный'],
        ['узор амфитеры', 'узор птицы', 'узор ужаса', 'узор глаза циклопа', 'узор пикси', 'узор пера',
         'узор огненного дракона', 'узор головы огненного дракона', 'узор головы горгоны', 'узор гиппокампа',
         'узор головы гиппогрифа', 'узор ледяного дракона', 'узор головы ледяного дракона', 'узор грозового дракона',
         'узор головы грозового дракона', 'узор сирены', 'узор левиафана', 'узор головы тролля', 'узор Weezer'],
        '{color} {pattern}'),
    'uk_ua': (
        ['Білий', 'Помаранчевий', 'Пурпуровий', 'Блакитний', 'Жовтий', 'Лаймовий', 'Рожевий', 'Сірий', 'Світло-сірий',
         'Бірюзовий', 'Фіолетовий', 'Синій', 'Коричневий', 'Зелений', 'Червоний', 'Чорний'],
        ['візерунок амфіптери', 'візерунок птаха', 'візерунок жаху', 'візерунок ока циклопа', 'візерунок піксі',
         'візерунок пера', 'візерунок вогняного дракона', 'візерунок голови вогняного дракона',
         'візерунок голови горгони', 'візерунок гіпокампа', 'візерунок голови гіпогрифа',
         'візерунок крижаного дракона', 'візерунок голови крижаного дракона', 'візерунок громового дракона',
         'візерунок голови громового дракона', 'візерунок сирени', 'візерунок левіафана', 'візерунок голови троля',
         'візерунок Weezer'],
        '{color} {pattern}'),
    'ja_jp': (
        ['白色', '橙色', '赤紫色', '空色', '黄色', '黄緑色', '桃色', '灰色', '薄灰色', '青緑色', '紫色', '青色', '茶色', '緑色',
         '赤色', '黒色'],
        ['アンピプテラ', '鳥', 'ドレッド', 'サイクロプスの眼', '妖精', '羽根', 'ファイアドラゴン', 'ファイアドラゴンの頭',
         'ゴルゴンの首', 'ヒッポカムポス', 'ヒッポグリフの頭', 'アイスドラゴン', 'アイスドラゴンの頭', 'ライトニングドラゴン',
         'ライトニングドラゴンの頭', 'セイレーン', 'シーサーペント', 'トロールの頭', 'Weezer'],
        '{color}の{pattern}模様'),
    'ko_kr': (
        ['하얀색', '주황색', '자홍색', '하늘색', '노란색', '연두색', '분홍색', '회색', '회백색', '청록색', '보라색', '파란색',
         '갈색', '초록색', '빨간색', '검은색'],
        ['엠피데어', '새', '드레드', '사이클롭스의 눈', '픽시', '깃털', '파이어 드래곤', '파이어 드래곤 머리', '고르곤 머리',
         '해마', '히포그리프 머리', '아이스 드래곤', '아이스 드래곤 머리', '라이트닝 드래곤', '라이트닝 드래곤 머리', '세이렌',
         '씨 서펀트', '트롤 머리', 'Weezer'],
        '{color} {pattern} 무늬'),
    'zh_cn': (
        ['白色', '橙色', '品红色', '淡蓝色', '黄色', '黄绿色', '粉红色', '灰色', '淡灰色', '青色', '紫色', '蓝色', '棕色', '绿色',
         '红色', '黑色'],
        ['翼蚺', '鸟', '悚怖', '独眼巨人之眼', '小精灵', '羽毛', '火龙', '火龙头', '蛇发女妖头', '海马', '骏鹰头', '冰龙', '冰龙头',
         '电龙', '电龙头', '塞壬', '海蟒', '食人妖头', 'Weezer'],
        '{color}{pattern}图案'),
    'vi_vn': (
        ['trắng', 'cam', 'đỏ tươi', 'xanh nhạt', 'vàng', 'xanh chanh', 'hồng', 'xám', 'xám nhạt', 'lục lam', 'tím',
         'xanh dương', 'nâu', 'xanh lá', 'đỏ', 'đen'],
        ['Amphithere', 'chim', 'Kinh hoàng', 'mắt Cyclops', 'tiên', 'lông vũ', 'Rồng Lửa', 'đầu Rồng Lửa', 'đầu Gorgon',
         'Hippocampus', 'đầu Hippogryph', 'Rồng Băng', 'đầu Rồng Băng', 'Rồng Sét', 'đầu Rồng Sét', 'Siren', 'Hải Xà',
         'đầu Troll', 'Weezer'],
        'Họa tiết {pattern} màu {color}'),
}

# Texts these languages had from the original mod that the port changed (the queen has a name now).
OVERRIDE = {
    'entity.iceandfire_fabric.dread_queen': {
        'de_de': 'Cryona, die Königin des Grauens', 'fr_fr': "Cryona, la Reine de l'Épouvante",
        'pt_br': 'Cryona, a Rainha Medonha', 'pl_pl': 'Cryona, Królowa Grozy', 'ru_ru': 'Криона, Королева ужаса',
        'uk_ua': 'Кріона, Королева жаху', 'ja_jp': 'ドレッドクイーン、クライオナ', 'ko_kr': '드레드 퀸, 크라이오나',
        'zh_cn': '悚怖女皇克莱奥娜', 'vi_vn': 'Cryona, Nữ hoàng Kinh hoàng'},
    # (its old first line said the second one too)
    'item.iceandfire_fabric.hydra_heart.desc_0': {'vi_vn': 'Khi ở thanh công cụ, hồi phục ngày càng mạnh'},
}


def banners(lang):
    colors, patterns, form = BANNERS[lang]
    assert len(colors) == len(COLORS) and len(patterns) == len(PATTERNS), lang
    return {'block.minecraft.banner.iceandfire_fabric.%s.%s' % (p, c): form.format(color=colors[j], pattern=patterns[i])
            for i, p in enumerate(PATTERNS) for j, c in enumerate(COLORS)}


def main():
    with open(LANG + 'en_us.json', encoding='utf-8') as f:
        en = json.load(f)
    total = 0
    for lang in sorted(BANNERS):
        path = LANG + lang + '.json'
        with open(path, encoding='utf-8') as f:
            have = json.load(f)
        with open(os.path.join(HERE, 'translations', lang + '.json'), encoding='utf-8') as f:
            texts = json.load(f)
        texts.update(banners(lang))
        put = {k: v for k, v in texts.items() if k in en and k not in have}
        for key, by_lang in OVERRIDE.items():
            if by_lang.get(lang) and have.get(key) != by_lang[lang]:
                put[key] = by_lang[lang]
        if put:
            add_lang.put(path, put)
        total += len(put)
        print('%s: %d texts put' % (lang, len(put)))
    print('%d in all' % total)


if __name__ == '__main__':
    main()

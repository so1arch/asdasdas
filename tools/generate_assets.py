#!/usr/bin/env python3
"""Generates all models, textures, recipes and lang files. Run: python3 tools/generate_assets.py (needs Pillow)."""
import json, os
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
RES = os.path.join(ROOT, "src/main/resources")
A = os.path.join(RES, "assets/medievalcombat")
D = os.path.join(RES, "data/medievalcombat")
MOD = "medievalcombat"

# If swords point up-LEFT instead of up-RIGHT in hand, change this to 45.
TILT = -45


def wjson(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


def wimg(path, img):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)

# ------------------------------------------------------------------ palette for 3D swords
PAL = [(205, 210, 218), (150, 156, 168), (98, 103, 116), (196, 160, 66),
       (72, 48, 32), (112, 76, 46), (122, 86, 52), (62, 66, 80),
       (236, 240, 246), (40, 40, 48), (160, 120, 50), (180, 184, 194)]
# 0 steel light  1 steel mid  2 steel dark  3 brass  4 leather dark  5 leather light
# 6 wood  7 fuller  8 steel bright (edges)  9 black  10 dark brass  11 steel pommel

pal = Image.new("RGBA", (16, 16))
for i, c in enumerate(PAL):
    for x in range(4):
        for y in range(4):
            pal.putpixel(((i % 4) * 4 + x, (i // 4) * 4 + y), c + (255,))
wimg(f"{A}/textures/item/palette.png", pal)


def uv(i):
    cx, cy = (i % 4) * 4, (i // 4) * 4
    return [cx + 1, cy + 1, cx + 3, cy + 3]


def cube(x1, y1, z1, x2, y2, z2, col, sides=None):
    sides = sides or {}
    faces = {}
    for f in ["north", "south", "east", "west", "up", "down"]:
        faces[f] = {"uv": uv(sides.get(f, col)), "texture": "#0"}
    return {"from": [round(x1, 3), round(y1, 3), round(z1, 3)],
            "to": [round(x2, 3), round(y2, 3), round(z2, 3)], "faces": faces}


def build_sword(s):
    els = []
    y = -1.0
    pw, ph = s["pommel"]
    els.append(cube(8 - pw / 2, y, 8 - pw / 2, 8 + pw / 2, y + ph, 8 + pw / 2, s["pcol"]))
    y += ph
    g = s["grip"]
    seg = 0
    gy = y
    while gy < y + g - 1e-6:
        h = min(1.0, y + g - gy)
        els.append(cube(7.2, gy, 7.2, 8.8, gy + h, 8.8, 4 if seg % 2 == 0 else 5))
        gy += h
        seg += 1
    y += g
    if s.get("rondel"):
        els.append(cube(6, y, 6, 10, y + 0.8, 10, s["gcol"]))
        y += 0.8
    else:
        gw, gt = s["guard"]
        els.append(cube(8 - gw / 2, y, 7.0, 8 + gw / 2, y + gt, 9.0, s["gcol"],
                        {"up": 10, "down": 10}))
        els.append(cube(7.3, y - 0.1, 6.6, 8.7, y + gt + 0.1, 9.4, s["gcol"]))  # guard boss
        y += gt
    blade_start = y
    for (ln, bw, bt) in s["blade"]:
        els.append(cube(8 - bw / 2, y, 8 - bt / 2, 8 + bw / 2, y + ln, 8 + bt / 2, 0,
                        {"east": 8, "west": 8, "up": 8, "down": 1}))
        y += ln
    # fuller groove (only on the two broad faces)
    bw0, bt0 = s["blade"][0][1], s["blade"][0][2]
    if bt0 >= 0.9 and not s.get("no_fuller"):
        fl = (y - blade_start) * 0.75
        els.append(cube(8 - 0.3, blade_start + 0.5, 8 - bt0 / 2 - 0.06,
                        8 + 0.3, blade_start + 0.5 + fl, 8 + bt0 / 2 + 0.06, 7))
    lastw, lastt = s["blade"][-1][1], s["blade"][-1][2]
    t = s["tip"]
    els.append(cube(8 - lastw * 0.35, y, 8 - lastt * 0.4, 8 + lastw * 0.35, y + t * 0.55, 8 + lastt * 0.4, 0,
                    {"east": 8, "west": 8, "up": 8}))
    y += t * 0.55
    els.append(cube(8 - lastw * 0.12, y, 8 - lastt * 0.25, 8 + lastw * 0.12, y + t * 0.45, 8 + lastt * 0.25, 8))
    for e in els:
        e["rotation"] = {"origin": [8, 8, 8], "axis": "z", "angle": TILT}
    return els


DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
    "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "gui": {"rotation": [18, -32, 0], "translation": [0, 0, 0], "scale": [0.95, 0.95, 0.95]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
}

SWORDS = {
    "arming_sword": dict(pommel=(2.4, 2.0), pcol=3, grip=4, guard=(7.5, 1.2), gcol=3,
                         blade=[(8, 2.4, 0.9), (5, 2.1, 0.8)], tip=2.2),
    "longsword": dict(pommel=(2.8, 2.4), pcol=11, grip=5, guard=(8.5, 1.1), gcol=2,
                      blade=[(10, 2.4, 1.0), (6, 2.2, 0.9)], tip=2.6),
    "falchion": dict(pommel=(2.2, 1.8), pcol=10, grip=3.5, guard=(4.5, 1.1), gcol=10,
                     blade=[(5, 2.6, 0.9), (5, 3.5, 0.9), (2, 3.3, 0.8)], tip=2.0),
    "dagger": dict(pommel=(3.6, 0.8), pcol=3, grip=3, rondel=True, gcol=3,
                   blade=[(4, 1.9, 0.8), (3, 1.6, 0.8)], tip=2.2, no_fuller=True),
    "estoc": dict(pommel=(2.4, 2.0), pcol=11, grip=5, guard=(6.0, 1.0), gcol=2,
                  blade=[(10, 1.5, 1.5), (8, 1.4, 1.4)], tip=2.6, no_fuller=True),
}

for name, spec in SWORDS.items():
    wjson(f"{A}/models/item/{name}.json", {
        "gui_light": "front",
        "textures": {"0": f"{MOD}:item/palette", "particle": f"{MOD}:item/palette"},
        "elements": build_sword(spec),
        "display": DISPLAY,
    })

# ------------------------------------------------------------------ armor textures
MATS = {
    "gambeson": dict(base=(206, 190, 150), dark=(150, 132, 98), light=(232, 218, 180)),
    "mail": dict(base=(150, 156, 168), dark=(84, 90, 102), light=(200, 206, 216)),
    "brigandine": dict(base=(112, 52, 48), dark=(66, 28, 28), light=(156, 86, 74)),
    "plate": dict(base=(196, 202, 214), dark=(112, 118, 132), light=(238, 242, 250)),
}


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def px(mat, x, y):
    m = MATS[mat]
    if mat == "gambeson":
        if (x + y) % 6 == 0 or (x - y) % 6 == 0:
            return m["dark"]
        return m["light"] if (x % 3 == 1 and y % 3 == 1) else m["base"]
    if mat == "mail":
        c = m["base"] if (x + y) % 2 == 0 else m["light"]
        return m["dark"] if (x % 4 == 0 and y % 2 == 0) else c
    if mat == "brigandine":
        if x % 4 == 1 and y % 4 == 1:
            return (205, 208, 218)
        return m["dark"] if y % 8 == 0 else m["base"]
    c = lerp(m["light"], m["dark"], (x % 16) / 20.0)
    return m["dark"] if y % 16 == 0 else c


def armor_layer(mat, layer):
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    for x in range(64):
        for y in range(32):
            img.putpixel((x, y), px(mat, x, y) + (255,))
    if layer == 1:
        # open face on the helmet: transparent window on the front of the head
        slit = (11, 13) if mat == "plate" else (10, 14)
        for x in range(9, 15):
            for y in range(slit[0], slit[1]):
                img.putpixel((x, y), (0, 0, 0, 0))
    return img


for mat in MATS:
    wimg(f"{A}/textures/models/armor/{mat}_layer_1.png", armor_layer(mat, 1))
    wimg(f"{A}/textures/models/armor/{mat}_layer_2.png", armor_layer(mat, 2))

# ------------------------------------------------------------------ armor icons
MASKS = {
    "helmet": ["", "",
               "....XXXXXXXX....", "...XXXXXXXXXX...", "..XXXXXXXXXXXX..", "..XXXXXXXXXXXX..",
               "..XXXXXXXXXXXX..", "..XXXXXXXXXXXX..", "..XXX......XXX..", "..XXX......XXX..",
               "..XXX......XXX.."],
    "chestplate": ["",
                   "..XXX......XXX..", ".XXXXX....XXXXX.", ".XXXXXXXXXXXXXX.", ".XXXXXXXXXXXXXX.",
                   ".XXX.XXXXXX.XXX.", ".XXX.XXXXXX.XXX.", ".XXX.XXXXXX.XXX.", "..XX.XXXXXX.XX..",
                   "....XXXXXXXX....", "....XXXXXXXX....", "....XXXXXXXX....", "....XXXXXXXX...."],
    "leggings": ["", "",
                 "..XXXXXXXXXXXX..", "..XXXXXXXXXXXX..", "..XXXXXXXXXXXX..", "..XXXXX..XXXXX..",
                 "..XXXX....XXXX..", "..XXXX....XXXX..", "..XXXX....XXXX..", "..XXXX....XXXX..",
                 "..XXXX....XXXX..", "..XXXX....XXXX..", "..XXXX....XXXX.."],
    "boots": ["", "", "", "", "", "", "", "",
              "..XXXX....XXXX..", "..XXXX....XXXX..", "..XXXX....XXXX..", ".XXXXX....XXXXX.",
              ".XXXXX....XXXXX."],
}


def icon(kind, mat):
    rows = MASKS[kind]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    mask = set()
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch == "X":
                mask.add((x, y))
    for (x, y) in mask:
        edge = any((x + dx, y + dy) not in mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        c = px(mat, x, y)
        if edge:
            c = tuple(max(0, v - 70) for v in MATS[mat]["dark"])
        elif (x - 1, y) not in mask or (x, y - 1) not in mask:
            c = tuple(min(255, v + 30) for v in c)
        img.putpixel((x, y), c + (255,))
    return img


TYPES = [("helmet", "helmet"), ("chestplate", "chestplate"), ("leggings", "leggings"), ("boots", "boots")]
for mat in MATS:
    for kind, suffix in TYPES:
        wimg(f"{A}/textures/item/{mat}_{suffix}.png", icon(kind, mat))
        wjson(f"{A}/models/item/{mat}_{suffix}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{mat}_{suffix}"}})

# steel ingot
ing = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
INGOT = ["", "", "", "", "....XXXXXXXXXX..", "...XXXXXXXXXXX..", "..XXXXXXXXXXXX..",
         "..XXXXXXXXXXX...", "..XXXXXXXXXX...."]
mk = {(x, y) for y, r in enumerate(INGOT) for x, ch in enumerate(r) if ch == "X"}
for (x, y) in mk:
    edge = any((x + dx, y + dy) not in mk for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
    c = (60, 66, 80) if edge else lerp((222, 228, 238), (140, 148, 164), (y - 4) / 5.0)
    ing.putpixel((x, y + 3), c + (255,))
wimg(f"{A}/textures/item/steel_ingot.png", ing)
wjson(f"{A}/models/item/steel_ingot.json",
      {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/steel_ingot"}})

# ------------------------------------------------------------------ recipes
def shaped(name, pattern, key, count=1):
    wjson(f"{D}/recipes/{name}.json", {
        "type": "minecraft:crafting_shaped", "category": "equipment", "pattern": pattern,
        "key": {k: {"item": v} for k, v in key.items()},
        "result": {"item": f"{MOD}:{name}", "count": count}})

I, S, L, ST = "minecraft:iron_ingot", "minecraft:stick", "minecraft:leather", f"{MOD}:steel_ingot"
wjson(f"{D}/recipes/steel_ingot.json", {
    "type": "minecraft:blasting", "category": "misc", "ingredient": {"item": I},
    "result": f"{MOD}:steel_ingot", "experience": 0.7, "cookingtime": 200})

shaped("arming_sword", [" I ", " I ", "LSL"], {"I": I, "S": S, "L": L})
shaped("falchion", ["  I", " II", "LS "], {"I": I, "S": S, "L": L})
shaped("dagger", ["I", "S"], {"I": I, "S": S})
shaped("longsword", [" T ", " T ", "TST"], {"T": ST, "S": S})
shaped("estoc", ["  T", " T ", "LS "], {"T": ST, "S": S, "L": L})

W, B = "minecraft:white_wool", "minecraft:iron_bars"
SETS = {"gambeson": {"W": W, "L": L}, "mail": {"B": B}, "brigandine": {"I": I, "L": L}, "plate": {"T": ST}}
PATTERNS = {
    "gambeson": {"helmet": ["WWW", "W W"], "chestplate": ["W W", "WLW", "WWW"],
                 "leggings": ["WLW", "W W", "W W"], "boots": ["L L", "W W"]},
    "mail": {"helmet": ["BBB", "B B"], "chestplate": ["B B", "BBB", "BBB"],
             "leggings": ["BBB", "B B", "B B"], "boots": ["B B", "B B"]},
    "brigandine": {"helmet": ["III", "ILI"], "chestplate": ["I I", "ILI", "III"],
                   "leggings": ["III", "L L", "I I"], "boots": ["L L", "I I"]},
    "plate": {"helmet": ["TTT", "T T"], "chestplate": ["T T", "TTT", "TTT"],
              "leggings": ["TTT", "T T", "T T"], "boots": ["T T", "T T"]},
}
for mat, kinds in PATTERNS.items():
    for kind, pat in kinds.items():
        shaped(f"{mat}_{kind}", pat, SETS[mat])

# ------------------------------------------------------------------ lang
EN = {
    "itemGroup.medievalcombat": "Medieval Combat",
    "item.medievalcombat.steel_ingot": "Steel Ingot",
    "item.medievalcombat.arming_sword": "Arming Sword",
    "item.medievalcombat.longsword": "Longsword",
    "item.medievalcombat.falchion": "Falchion",
    "item.medievalcombat.dagger": "Rondel Dagger",
    "item.medievalcombat.estoc": "Estoc",
    "tooltip.medievalcombat.damage": "Slash: %s  Thrust: %s",
    "tooltip.medievalcombat.stamina": "Stamina cost: %s",
    "message.medievalcombat.parry": "Parry! Counterattack now!",
    "message.medievalcombat.parried": "Your attack was parried!",
    "message.medievalcombat.guard_broken": "Guard broken!",
}
RU = {
    "itemGroup.medievalcombat": "Средневековый бой",
    "item.medievalcombat.steel_ingot": "Стальной слиток",
    "item.medievalcombat.arming_sword": "Арминговый меч",
    "item.medievalcombat.longsword": "Длинный меч",
    "item.medievalcombat.falchion": "Фальшион",
    "item.medievalcombat.dagger": "Рондельный кинжал",
    "item.medievalcombat.estoc": "Эсток",
    "tooltip.medievalcombat.damage": "Рубящий: %s  Колющий: %s",
    "tooltip.medievalcombat.stamina": "Расход выносливости: %s",
    "message.medievalcombat.parry": "Парирование! Контратакуйте!",
    "message.medievalcombat.parried": "Вашу атаку парировали!",
    "message.medievalcombat.guard_broken": "Защита сломлена!",
}
NAMES_EN = {"gambeson": ("Gambeson Cap", "Gambeson", "Padded Chausses", "Padded Boots"),
            "mail": ("Mail Coif", "Mail Hauberk", "Mail Chausses", "Mail Boots"),
            "brigandine": ("Brigandine Helm", "Brigandine", "Brigandine Greaves", "Brigandine Boots"),
            "plate": ("Bascinet", "Plate Cuirass", "Plate Cuisses", "Sabatons")}
NAMES_RU = {"gambeson": ("Стёганый подшлемник", "Гамбезон", "Стёганые поножи", "Стёганые сапоги"),
            "mail": ("Кольчужный койф", "Кольчужный хауберк", "Кольчужные чулки", "Кольчужные сапоги"),
            "brigandine": ("Шлем к бригантине", "Бригантина", "Бригантинные поножи", "Бригантинные сапоги"),
            "plate": ("Бацинет", "Латный кирасный доспех", "Латные набедренники", "Сабатоны")}
for mat in MATS:
    for i, (kind, suffix) in enumerate(TYPES):
        EN[f"item.{MOD}.{mat}_{suffix}"] = NAMES_EN[mat][i]
        RU[f"item.{MOD}.{mat}_{suffix}"] = NAMES_RU[mat][i]
wjson(f"{A}/lang/en_us.json", EN)
wjson(f"{A}/lang/ru_ru.json", RU)
print("assets generated")

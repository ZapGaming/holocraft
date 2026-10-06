#!/usr/bin/env python3
"""Preflight: lay every sheet's rows x columns over each other and list what will fail.

Checks every cell is filled, typed, and that every reference resolves:
  holocure sprite/sound/string -> the player's HoloCure (data.win via HOLOCURE_DIR, else tools/holocure_index.json)
  ref <sheet>.<col>            -> a row in that sheet (ref systems.<group> -> a systems row in that group)
  systems.java                 -> the Java file exists (missing = unimplemented)
  ui.mc_path                   -> exists in the Minecraft 1.21.4 client jar
Exit 0 only when clean.
"""
import glob, json, os, re, sys, zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SHEETS = os.path.join(ROOT, "sheets")
JAVA = os.path.join(ROOT, "mod", "src", "main", "java", "fail", "holocraft")
sys.path.insert(0, os.path.join(ROOT, "tools"))

problems, notes = [], []
sheets = {}
for p in sorted(glob.glob(os.path.join(SHEETS, "*.json"))):
    s = json.load(open(p))
    sheets[s["sheet"]] = s

idx = json.load(open(os.path.join(ROOT, "tools", "holocure_index.json")))
sprites, sounds = idx["sprites"], set(idx["sounds"])
strings = None
hc = os.environ.get("HOLOCURE_DIR")
if hc and os.path.exists(os.path.join(hc, "data.win")):
    from gmdata import GM
    g = GM(os.path.join(hc, "data.win"))
    sprites = {n: len(v["frames"]) for n, v in g.sprites().items()}
    strings = set(x for x in g.strings() if x)
    objects = {g.str_at(g.u32(q)) for q in g.plist(g.ch["OBJT"][0])}
    rooms = {g.str_at(g.u32(q)) for q in g.plist(g.ch["ROOM"][0])}
else:
    notes.append("HOLOCURE_DIR not set: holocure strings unverified, sprites/sounds checked against the index")

mc_assets = None
jars = glob.glob(os.path.expanduser("~/.gradle/caches/fabric-loom/1.21.4/minecraft-client.jar")) + \
       glob.glob(os.path.expanduser("~/.gradle/caches/fabric-loom/minecraftMaven/**/minecraft-client*1.21.4*.jar"), recursive=True)
if jars:
    with zipfile.ZipFile(jars[0]) as z:
        mc_assets = set(z.namelist())
else:
    notes.append("Minecraft 1.21.4 client jar not in the gradle cache yet: ui.mc_path unverified")

def ids(sheet, col="id"):
    return {r[col] for r in sheets[sheet]["rows"]}

checked = 0
for name, s in sheets.items():
    cols = s["columns"]
    for i, row in enumerate(s["rows"]):
        key = row.get("id", row.get("mc_path", row.get("event", row.get("level", i))))
        where = f"{name}[{key}]"
        for extra in set(row) - set(cols):
            problems.append(f"{where}: cell '{extra}' has no column")
        for col, spec in cols.items():
            checked += 1
            v = row.get(col)
            if v is None or v == "":
                problems.append(f"{where}.{col}: empty")
                continue
            t = spec.split(";")[0].strip()
            if t == "int" and not isinstance(v, int):
                problems.append(f"{where}.{col}: want int, got {v!r}")
            elif t == "float" and not isinstance(v, (int, float)):
                problems.append(f"{where}.{col}: want float, got {v!r}")
            elif t == "bool" and not isinstance(v, bool):
                problems.append(f"{where}.{col}: want bool, got {v!r}")
            elif "|" in t and not t.startswith("ref"):
                if v not in [x.strip() for x in t.split("|")]:
                    problems.append(f"{where}.{col}: {v!r} not one of {t}")
            elif t == "holocure sprite":
                if v not in sprites:
                    problems.append(f"{where}.{col}: sprite {v} not in HoloCure")
            elif t == "holocure sound":
                if v not in sounds:
                    problems.append(f"{where}.{col}: sound {v} not in HoloCure")
            elif t == "holocure object":
                if strings is not None and v not in objects:
                    problems.append(f"{where}.{col}: object {v} not in HoloCure")
            elif t == "holocure room":
                if strings is not None and v not in rooms:
                    problems.append(f"{where}.{col}: room {v} not in HoloCure")
            elif t == "holocure font":
                if v not in idx["fonts"]:
                    problems.append(f"{where}.{col}: font {v} not in HoloCure")
            elif t == "holocure string":
                if strings is not None and v not in strings:
                    problems.append(f"{where}.{col}: text {v!r} not in HoloCure")
            elif t.startswith("ref "):
                target = t[4:].split()[0]
                literals = [x.strip().strip("'") for x in t[4:].split(" or ")[1:]]
                if v in literals:
                    continue
                sh, _, sub = target.partition(".")
                if sh == "systems":
                    ok = any(r["id"] == v and r["group"] == sub for r in sheets["systems"]["rows"])
                else:
                    ok = sh in sheets and v in ids(sh, sub or "id")
                if not ok:
                    problems.append(f"{where}.{col}: {v!r} does not resolve in {target}")
        if name == "ui":
            f = row.get("frame", 0)
            if row.get("source") in sprites and f >= sprites[row["source"]]:
                problems.append(f"{where}.frame: {f} >= {sprites[row['source']]} frames")
            if mc_assets is not None and "assets/minecraft/textures/" + row["mc_path"] not in mc_assets:
                problems.append(f"{where}.mc_path: not in the 1.21.4 client jar")
        if name == "systems":
            if not os.path.exists(os.path.normpath(os.path.join(JAVA, row["java"]))):
                problems.append(f"{where}.java: {row['java']} not written yet (unimplemented)")

# every stage can be played: a fan at minute 0, and bosses to end the night
for st in sheets["stages"]["rows"]:
    if not any(f["stage"] in (st["id"], "any") and f["minute_from"] == 0 for f in sheets["fans"]["rows"]):
        problems.append(f"stages[{st['id']}]: no fan appears at minute 0")
    if not any(b["stage"] == st["id"] for b in sheets["bosses"]["rows"]):
        problems.append(f"stages[{st['id']}]: no boss, so the night never ends")
    if not any(pr["stage"] == st["id"] for pr in sheets["props"]["rows"]):
        problems.append(f"stages[{st['id']}]: no props")

# every hook referenced as a dialog trigger must be a systems hook; every systems row must be used or be a hook
used = set()
for s in sheets.values():
    for col, spec in s["columns"].items():
        if spec.startswith("ref systems"):
            used |= {r[col] for r in s["rows"]}
for r in sheets["systems"]["rows"]:
    if r["group"] != "hooks" and r["id"] not in used:
        problems.append(f"systems[{r['id']}]: defined but no row uses it")

for n in notes:
    print("note:", n)
print(f"{len(sheets)} sheets, {sum(len(s['rows']) for s in sheets.values())} rows, {checked} cells")
if problems:
    print(f"{len(problems)} open:")
    for p in problems:
        print("  -", p)
    sys.exit(1)
print("preflight clean")

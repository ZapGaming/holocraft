#!/usr/bin/env python3
"""Builds the two release files Melty installs (see melty.json):

  dist/holocraft-<v>.jar               the mod; Melty puts it in the instance's mods folder
  dist/HoloCraft-Minecraft-<v>.zip     its Minecraft: portable Prism Launcher with a HoloCraft instance
                                       (1.21.4 + Fabric Loader, Fabric API, e4mc), extracted into {managed}

Third-party files come from vendor/ (fetched by tools/fetch_vendor.sh from their official releases).
Nothing from HoloCure is packaged: the mod builds its art and sound from the player's own copy on first start.
"""
import json, os, shutil, sys, zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
props = dict(l.strip().split("=", 1) for l in open(os.path.join(ROOT, "mod/gradle.properties")) if "=" in l)
V, MC, LOADER = props["mod_version"], props["minecraft_version"], props["loader_version"]
VENDOR, DIST = os.path.join(ROOT, "vendor"), os.path.join(ROOT, "dist")
PRISM_ZIP = "PrismLauncher-Windows-MinGW-w64-Portable-11.1.1.zip"
MODS = ["fabric-api-0.119.4+1.21.4.jar", "e4mc-fabric-6.2.3.jar"]
INSTANCE = "HoloCraft"
FIXED = (1980, 1, 1, 0, 0, 0)

def add(z, name, data):
    info = zipfile.ZipInfo(name, FIXED)
    info.compress_type = zipfile.ZIP_DEFLATED
    info.external_attr = 0o644 << 16
    z.writestr(info, data)

def main():
    os.makedirs(DIST, exist_ok=True)
    jar = os.path.join(ROOT, f"mod/build/libs/holocraft-{V}.jar")
    if not os.path.isfile(jar):
        sys.exit(f"build the mod first: {jar} missing")
    shutil.copyfile(jar, os.path.join(DIST, f"holocraft-{V}.jar"))

    out = os.path.join(DIST, f"HoloCraft-Minecraft-{V}.zip")
    with zipfile.ZipFile(out, "w") as z, zipfile.ZipFile(os.path.join(VENDOR, PRISM_ZIP)) as prism:
        for e in sorted(prism.infolist(), key=lambda e: e.filename):
            if e.is_dir():
                continue
            add(z, "Prism/" + e.filename, prism.read(e))
        # every first-run question the launcher can ask is answered, except signing in to Minecraft
        add(z, "Prism/prismlauncher.cfg", "\n".join([
            "[General]", "Language=en_US", "AutomaticJavaDownload=true", "AutomaticJavaSwitch=true",
            "UserAskedAboutAutomaticJavaDownload=true", "ApplicationTheme=system", "IconTheme=pe_colored",
            "BackgroundCat=kitteh", "PastebinURL=", "AutoUpdate=false", "SelectedInstance=" + INSTANCE, ""]))
        inst = f"Prism/instances/{INSTANCE}/"
        add(z, inst + "instance.cfg", "\n".join([
            "[General]", "ConfigVersion=1.2", "InstanceType=OneSix", "name=HoloCraft", "iconKey=holocraft",
            "OverrideMemory=true", "MinMemAlloc=1024", "MaxMemAlloc=4096", "notes=HoloCraft: HoloCure's world through Minecraft. Made for Melty.", ""]))
        add(z, inst + "mmc-pack.json", json.dumps({"formatVersion": 1, "components": [
            {"uid": "net.minecraft", "version": MC, "important": True},
            {"uid": "net.fabricmc.intermediary", "version": MC, "dependencyOnly": True},
            {"uid": "net.fabricmc.fabric-loader", "version": LOADER}]}, indent=1))
        add(z, inst + ".minecraft/options.txt", "\n".join([
            "version:4189", "tutorialStep:none", "onboardAccessibility:false", "skipMultiplayerWarning:true",
            "joinedFirstServer:true", "renderDistance:12", "simulationDistance:8", "lang:en_us", ""]))
        for m in MODS:
            add(z, inst + ".minecraft/mods/" + m, open(os.path.join(VENDOR, m), "rb").read())
        add(z, "Prism/icons/holocraft.png", open(os.path.join(ROOT, "mod/src/main/resources/assets/holocraft/icon.png"), "rb").read())
        add(z, "Prism/HOLOCRAFT-CREDITS.txt", open(os.path.join(ROOT, "CREDITS.md")).read())
    for f in (f"holocraft-{V}.jar", f"HoloCraft-Minecraft-{V}.zip"):
        p = os.path.join(DIST, f)
        print(p, os.path.getsize(p))

main()

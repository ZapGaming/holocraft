#!/usr/bin/env python3
"""Writes mythos/fabric-loader-<loader>-<mc>.json: Fabric's profile merged into Mojang's version json the way the
vanilla launcher merges it (a library is group:artifact[:classifier]; the child's copy wins). The Mythos Launcher
concatenates inherited libraries without that de-duplication, which puts ASM 9.6 and Fabric's ASM on the same
classpath and Fabric refuses to start, so HoloCraft ships it pre-merged with no inheritsFrom."""
import json, os, sys, urllib.request

MC, LOADER = "1.21.4", "0.19.5"
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

def get(url): return json.load(urllib.request.urlopen(url, timeout=60))

def key(lib):
    p = lib["name"].split(":")
    return ":".join(p[:2] + p[3:])  # drop the version, keep a classifier

def main():
    manifest = get("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
    parent = get(next(v["url"] for v in manifest["versions"] if v["id"] == MC))
    child = get(f"https://meta.fabricmc.net/v2/versions/loader/{MC}/{LOADER}/profile/json")
    out = dict(parent)
    seen, libs = set(), []
    for lib in child["libraries"] + parent["libraries"]:
        if key(lib) in seen: continue
        seen.add(key(lib)); libs.append(lib)
    out["libraries"] = libs
    for k in ("id", "mainClass", "type", "releaseTime", "time"):
        if k in child: out[k] = child[k]
    args = {k: list(parent["arguments"].get(k, [])) + list(child.get("arguments", {}).get(k, [])) for k in ("game", "jvm")}
    out["arguments"] = args
    out.pop("inheritsFrom", None)
    dst = os.path.join(ROOT, "mythos", out["id"] + ".json")
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    json.dump(out, open(dst, "w"), indent=1)
    print(dst, len(libs), "libraries")

main()

# HoloCraft

HoloCure - Save the Fans! played through Minecraft: Java 1.21.4 (Fabric). The player is in HoloCure's stages with
Minecraft's hotbar/armour/menus; idols are hotbar weapons; HoloCure's level-up replaces XP. Built for Melty.

- **Sheets are the source of truth** (`sheets/*.json`, one row per thing, `tools/gen.py` turns rows into Java
  records at build). Change the sheet before the code. `tools/preflight.py` must say "preflight clean" before a
  build; every `systems` row names the Java file implementing it.
- **Nothing from HoloCure ships.** `client/HoloCureImport.java` reads the player's own `data.win` (found by
  `HoloCureLocator`: env `HOLOCRAFT_HOLOCURE`, config, Steam libraries, itch) and writes a resource pack into
  `<game dir>/holocraft/pack`. The pack stamp includes a hash of every sprite the sheets reference, so adding a
  sprite column rebuilds it.
- **Two views of one world** (`ViewMode`, key V). WORLD_3D: props/fans/idols are thick pixel models built from the
  sprites (`PixelModels`, item-model-style extrusion; props `shape3d` cross|slab). ORIGINAL_2D: orthographic,
  straight down, 640x360 HoloCure pixels at `stages.map_px_per_block` (`GameRendererProjectionMixin`); sprites
  lie flat and are lifted a hair per block south so depth testing gives HoloCure's draw order (`FlatView`).
  The ground needs nothing: straight down it is already the floor tiles.
- Traps: vanilla `Frustum.coverBoxAroundSetPosition` loops forever under an ortho projection (`FrustumMixin`);
  looking straight down, chunk cave-culling drops everything past one chunk border, so the flat view turns
  `chunkCullingEnabled` off. e4mc's mixins target intermediary names by regex, so **co-op cannot be tested in the
  loom dev client** — test it in a production launch (portablemc `fabric:1.21.4:0.19.5` with the release jar).
- Stages (0.2.0): a stage world plays its own stage every night (`WaveDirector.stage` reads the overworld's
  `StageChunkGenerator`); an ordinary world walks the stages in order. Fans carry a `stage` column (`any` = all).
  World presets, biomes and the world_preset tag are generated per stages row by `tools/gen.py`; each stage's
  music is `holocraft:music.stage_<id>`. Run preflight with `HOLOCURE_DIR` set so names are checked against the
  real data.win strings (weapon/special/stage/boss names must be HoloCure's own).
- Co-op: `HOLOCRAFT_HOST=1` opens the world to LAN on load; bundled e4mc relays it and logs
  `Domain assigned: <address>`; joiners use Prism `--launch HoloCraft --server <address>`. Verified 2026-10-06 with
  two production clients over the real e4mc relay (`-Dholocraft.lanOffline=true` only because test accounts are
  signed out).
- Release: `cd mod && ./gradlew build` → `tools/package.py` → `dist/holocraft-<v>.jar` +
  `dist/HoloCraft-Minecraft-<v>.zip` (portable Prism 11.1.1 + instance + Fabric API + e4mc; `tools/fetch_vendor.sh`
  fills `vendor/`). Recipe mirrors Melty's *Mario 64 in Minecraft*.
- **Melty status:** HoloCure is not in Melty's game catalog (2026-10-06), so the listing can't name it; an
  `external` requirement makes one_click_check say no. Draft only until Melty adds it.
- **Mythos Launcher** (Zandy's own offline launcher, `%APPDATA%\.mythoslauncher`) runs HoloCraft as an "edition":
  its ApiUrl points at the Space route `failure.zo.space/api/mythos/manifest` (Zandy's editions proxied + a HoloCraft
  entry whose overlay only sets Fabric's main class), the base is a local `fabric-loader-0.19.5-1.21.4` version json,
  and mods go in `instances\1.21.4-holocraft-0.2.0\mods`. That json must be the **pre-merged** one from
  `tools/mythos_version.py` (`mythos/`): Mythos concatenates inherited libraries without de-duplicating, so the
  plain Fabric profile puts two ASM versions on the classpath and Fabric refuses to start. Verified by driving
  Mythos's own `MythosLauncher.Core` install/launch code on Zo to the HoloCraft title screen.

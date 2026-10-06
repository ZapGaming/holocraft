# HoloCraft

HoloCure's world, played through Minecraft. You are in HoloCure's stages, fighting its fans and bosses, but your kit
works like Minecraft's: idols sit in your 1-9 hotbar as weapons, HoloCure items go in your armour slots, and every
menu is HoloCure-styled HoloCraft.

## What players get

- **Three HoloCure stages as worlds.** Make a world on Minecraft's Create World screen and pick a world type:
  "HoloCure: Grassy Plains", "HoloCure: Holo Office" or "HoloCure: Halloween Castle". The ground is the stage's own
  floor tiles as blocks, and every tree, pillar, desk, locker, couch, barrel, bookshelf and pumpkin stands where
  HoloCure puts it, repeating endlessly like HoloCure's map. You can still dig and build. Each stage world plays its
  own stage every night, harder each time, with its own music, fans and bosses.
- **Two views, one world. Press V.**
  - Minecraft view: the stage in 3D. Props, fans, bosses and your idol are thick pixel models built from HoloCure's
    sprites; trees are solid from every side.
  - HoloCure view: straight down, flat, at HoloCure's own pixel scale. Everything is drawn as HoloCure draws it.
    W/A/S/D move up/left/down/right on screen. Nothing moves when you switch, only how it is drawn.
- **You are an idol.** Pick a starter (Amelia Watson, Mori Calliope or Gawr Gura); level-ups bring in more:
  Ninomae Ina'nis, Takanashi Kiara, Ceres Fauna, Ouro Kronii, Nanashi Mumei, Hakos Baelz, IRyS and Tsukumo Sana,
  each with her HoloCure weapon and special. Your player is drawn as the idol in your hand, standing or running.
- **Idols are weapons.** The selected hotbar idol attacks by itself; scroll to swap mid-fight; right-click fires her
  special.
- **Nights are HoloCure runs.** Fans pour in and the timer runs, then two bosses arrive with HoloCure's dialog:
  - Grassy Plains: Shrimp, Deadbeat, Takodachi, KFP, Investigator; Smol Ame, then Fubuzilla.
  - Holo Office: 35P, Subatomo, Hoshiyomi, Nousagi; Shubangelion, then Lunazilla.
  - Halloween Castle: Melfriend, Luknight, Irystocrat, Ankimo, Chocomate; pumpkin Smol Ame, then Cursed Bubba.
- **HoloCure's level-up replaces XP.** Each level offers three picks: a new idol for your hotbar, an idol upgrade, or a
  HoloCure item to wear (Study Glasses, Energy Drink, Body Pillow, Gorilla's Paw, Hamburger).
- **HoloCure-dressed Minecraft.** Title screen, menus, dialogs, fonts and sounds are HoloCure's, branded HoloCraft.
- **Solo or co-op.** Play alone, or friends join your world through Melty's join link. Each Play opens your world to
  friends over the e4mc relay, so nobody forwards ports. Up to 8 players (Minecraft's own limit for an opened world);
  fan waves scale with the number of players.

## Needs

- Minecraft: Java Edition (sign in once in the bundled launcher).
- HoloCure - Save the Fans! installed from Steam (free). HoloCraft reads its art, music and sounds from your copy on
  first start and builds them into a resource pack on your PC; none of HoloCure's files are shipped.

## Build

`tools/preflight.py` (sheets are the source of truth; must be clean) → `cd mod && ./gradlew build` →
`tools/package.py` → `dist/`. See CREDITS.md.

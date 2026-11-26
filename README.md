# Plus Ultra — a My Hero Academia fan mod (NeoForge 1.21.1)

Unofficial fan project. *My Hero Academia* belongs to Kohei Horikoshi / Shueisha.
All code, textures and data in this project are original; nothing is taken from Clover Craft or any other mod.

## Features

| System | What it does |
|---|---|
| **Quirks** | 6 Quirks (One For All, Half-Cold Half-Hot, Explosion, Hardening, Zero Gravity, Engine), 3 abilities each |
| **Stamina** | Resource bar with regen, used by every ability (free in creative and in the Training Ground) |
| **Levels / Plus Ultra** | Using abilities earns XP. Level scales damage and max stamina. The 3rd ability unlocks at level 5 (configurable) |
| **Mission Board** | Right-click for a Patrol (kill 10 hostiles) or Villain mission. Sneak + right-click abandons it |
| **Villain boss** | Tough melee boss with boss bar and a periodic Decay shockwave. Drops emeralds and sometimes a Quirk Orb |
| **Hero ranks** | Missions give hero points: Trainee → Rookie → Pro → Top 10 → Number One. Hero License item shows your rank |
| **Hero Agencies** | `/agency create|join|leave|info|list`. Members within 16 blocks get +20% stamina regen each (max +60%) |
| **Training Ground** | `/training` / `/training leave`. Flat, always-daytime dimension; abilities cost no stamina |
| **Items** | Quirk Orb (awakens a random Quirk), Quirk Eraser (removes it), Hero License, Mission Board, Villain Spawn Egg |

Keys (rebindable): **Z / X / C** = abilities 1 / 2 / 3.

Admin commands: `/plusultra quirk set <player> <id>`, `/plusultra quirk clear <player>`, `/plusultra level set <player> <n>`, `/plusultra info`.

Config: `config/plusultra-common.toml` (damage multiplier, stamina regen, max level, Plus Ultra unlock level,
whether quirks can hurt players, whether Explosion abilities break blocks).

## Building

Requires JDK 21 and Gradle 8.8+ (the Gradle wrapper is not included).

    gradle wrapper        # once, to generate ./gradlew
    ./gradlew runClient   # test in a dev client
    ./gradlew build       # jar ends up in build/libs/

Versions are in `gradle.properties` (`neo_version`) and `build.gradle` (ModDevGradle plugin version).
Bump them if Gradle can't resolve them.

## Status

Written without access to Minecraft/NeoForge jars, so it has **not been compiled or run yet**. Expect to fix a few
small API mismatches on the first build. Likely spots: `SoundEvents` constants that are `Holder`s rather than
`SoundEvent`s in your NeoForge version, `AttachmentType` builder overloads, and `RegisterGuiLayersEvent` naming.

## Project layout

    core/      registries (items, blocks, entities, attachments, tabs, dimension key) + server events
    quirk/     Quirk enum, per-player data, ability effects, ability manager (cost / cooldown / XP)
    mission/   mission board block, mission logic, ranks
    agency/    saved-data Hero Agencies
    entity/    Villain boss
    item/      Quirk Orb, Quirk Eraser, Hero License
    net/       payloads: UseAbility (client→server), Sync (server→client)
    client/    keybinds, HUD, villain renderer
    command/   /plusultra, /agency, /training

## Extending

- **New Quirk**: add an enum constant in `Quirk`, write its three ability methods in `Abilities`, add lang keys
  `quirk.plusultra.<id>` and `ability.plusultra.<id>.<ability>`.
- **Better visuals/animations**: swap the vanilla humanoid renderer for GeckoLib models; add Photon/particle effects.
- **New mission types**: add a constant and a case in `Missions.assign / onKill / describe`.

## v0.2 additions: talents, fusion, visuals

Keys (rebindable): Z/X/C abilities, **V** talent tree, **G** swap active Quirk, **B** Fusion Strike.

- **Skill points / talent tree**: every level-up gives 1 point. Spend on Power (+8% damage), Endurance (+10 stamina),
  Recovery (+10% regen), Focus (-5% cooldowns), Efficiency (-5% stamina cost). Respec is free in the screen.
- **Quirk fusion**: reach level 8 (config `fusionUnlockLevel`), craft a Fusion Catalyst
  (amethyst shards, diamonds, nether star + Quirk Orb) and use it to gain a second random Quirk.
  G swaps which one is on Z/X/C (each keeps its own cooldowns). B fires both Quirks' first abilities together
  at 1.3x power (needs Plus Ultra level). A Quirk Eraser removes the fused Quirk first, then the main one.
  Villains drop a Fusion Catalyst 8% of the time.
- **Visuals**: ring/beam/spiral/sphere particle effects for every ability, glowing auras on buffs, level-up burst,
  new HUD (cooldown-fill ability rows, stamina numbers, fusion lines), new villain model (horns, spiked pads, cloak,
  glowing eyes, rage phase below 50% health), redrawn item/block textures.
- Admin: `/plusultra quirk fuse <player> <quirk>`; `/plusultra level set` now also resets talents and grants points.

Most likely compile fixes if any: `DustParticleOptions(Vector3f, float)` in Fx.java (1.21.1 takes a Vector3f),
`EyesLayer.renderType()` in VillainEyesLayer.java.

## v0.3 additions: more Quirks, support items, villains, raids

- **8 new Quirks** (3 abilities each): Creation, Invisibility, Fierce Wings, Dark Shadow, Black Whip, Acid,
  Earphone Jack, Tape. They roll from Quirk Orbs automatically. (`/plusultra quirk set <player> <id>`: creation,
  invisibility, fierce_wings, dark_shadow, black_whip, acid, earphone_jack, tape.)
- **Support items**: Grappling Hook (right-click, pulls you to the block you look at, 250 uses), Support Gloves
  (right-click impact punch, works without a Quirk), Stamina Potion (+60 stamina) and Greater Stamina Potion
  (full stamina and cooldown reset), Hero Costume (helmet, jacket, pants, boots: +5% stamina regen per piece,
  +10% Quirk damage with the full set).
- **Villains**: Decay Lord (shockwave), Nomu (leap slam, regenerates when enraged, large), Hero Killer (blinks behind
  you and paralyses), Blue Flame (blue fire cone), Alliance Thug (minion). All have spawn eggs and loot tables.
  Villain missions now pick a random boss.
- **Villain Alliance raids**: 25% of Mission Board rolls start a 3-wave raid (thugs, thugs + Hero Killer,
  boss + escorts). Fails if you die, leave the area or run out of 12 minutes. Admin:
  `/plusultra raid start|stop <player>`.
- **Textures**: detailed 64x64 skins for every villain with glowing eyes, new armor layers and item icons.

Most likely compile fixes if any: `Item#getUseDuration(ItemStack, LivingEntity)` in StaminaPotionItem,
`ArmorMaterial` constructor in ModArmor, `LivingEntityRenderer#scale` in VillainRenderer.

## v0.4 additions: animations

- **Custom particle system**: 33 sprite particles (lightning bolts, sparks, starburst flashes, feathers, tape, ice
  shards, wisps, drops, sound-wave rings...) defined in `ModParticles`, drawn by `StyledParticle`
  (tint, glow, fade, spin, flutter, trails). Add a new one by adding a line to `ModParticles.DEFS`, a sprite in
  `textures/particle/` and `particles/<id>.json`.
- **Animation toolkit** (`Anim`): projectiles that fly, strands that extend, spirals that wrap and tighten, expanding
  rings, pillars, gather/charge-ups, jagged lightning and trails that follow the player. `QFx` holds the
  signature effects (MHA explosion, green lightning, wings, feather volleys) and a distinct aura per Quirk.
- Every one of the 42 abilities has a choreographed animation; the arm swing plays when a Quirk is used.
  Gameplay numbers are unchanged. Ranged hits (feathers, cannon, acid, tape, whip) now land when the
  projectile arrives.

Most likely compile fixes if any: `StyledParticle` (Particle field names: roll/oRoll/random/level),
`RegisterParticleProvidersEvent#registerSpriteSet`.

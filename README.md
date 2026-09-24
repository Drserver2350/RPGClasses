# RPGClasses — 20 RPG Classes for Paper 1.21.11

A complete class system: **20 classes, 60 active skills, 20 passives, mana, levels & XP, a
class-selection GUI, a live action-bar HUD, and a custom resource pack that the plugin
hosts and pushes to players automatically.**

Tested on Paper 1.21.11 build 132 · Java 21.

---

## Install (30 seconds)

1. Drop `RPGClasses-1.1.0.jar` into your server's `plugins/` folder.
2. Start the server. The plugin will:
   * extract the resource pack to `plugins/RPGClasses/pack.zip`
   * start a tiny HTTP server on port **8123** and serve the pack from it
   * detect your public IP and log the URL, e.g.  
     `Built-in pack server ready: http://1.2.3.4:8123/pack.zip`
3. **Open port 8123 (TCP)** in your firewall / host panel so clients can download the pack.
   If the auto-detected IP is wrong, set `resource-pack.builtin.public-address` in
   `plugins/RPGClasses/config.yml` (use your server's domain/IP) and `/rpgadmin reload`.

> Can't open a port? Set `resource-pack.mode: url`, upload `pack.zip` anywhere
> (Dropbox direct link, GitHub raw, your web host) and put the link in `resource-pack.url`.

## How players use it

| Action | How |
|---|---|
| Choose a class | `/class` (GUI with icons + full descriptions, confirm screen) |
| Get your items | Given automatically on class pick — a **Power Orb** + your **signature weapon** (`/class items` to restore) |
| Select a skill | Hold the orb/weapon and press **F** (swap hands) → next skill, **Sneak + F** → previous |
| Cast the selected skill | **Right-click** with the Power Orb or your class weapon (bow: use the orb) |
| Cast directly | `/cast 1`, `/cast 2`, `/cast 3` |
| View skills & passive | `/skills`, `/class info` |
| Change class | `/class reset` (10-min cooldown by default; progress per class is saved) |

The HUD (action bar) always shows: class · level · XP bar · mana bar · selected skill & cooldown.

**Progression:** kill mobs / players and mine ores/logs to earn class XP. Level cap 30.
Skills unlock at level **1 / 5 / 10**, cooldowns shrink up to 20 % at max level, +1 heart every
5 levels, skill damage scales with level.

## The 20 classes

| # | Class | Role | Passive | Skills (Lv1 / Lv5 / Lv10) |
|--|--|--|--|--|
| 1 | **Warrior** | Melee DPS | Battle Hardened | Cleave / Charge / War Cry |
| 2 | **Paladin** | Hybrid | Divine Favor (lifesteal, +25 % vs undead) | Holy Strike / Lay on Hands / Consecration |
| 3 | **Berserker** | Melee DPS | Blood Frenzy (up to +60 % dmg at low HP) | Reckless Swing / Bloodlust / Earthshatter |
| 4 | **Guardian** | Tank | Stalwart (-15 % dmg, block reflects) | Taunt / Shield Wall / Bastion |
| 5 | **Ranger** | Ranged DPS | Eagle Eye (+25 % arrows, headshots ×2) | Multishot / Piercing Shot / Arrow Storm |
| 6 | **Assassin** | Melee DPS | Backstab (+75 % from behind) | Shadowstep / Vanish / Death Mark |
| 7 | **Samurai** | Melee DPS | Bushido (combo stacks) | Iaijutsu / Parry Stance / Thousand Cuts |
| 8 | **Mage** | Caster | Arcane Mind (+50 % mana & regen) | Arcane Missile / Blink / Meteor |
| 9 | **Pyromancer** | Caster | Ember Heart (fire immune, hits ignite) | Fireball / Flame Wave / Inferno |
| 10 | **Cryomancer** | Caster | Frostbite (hits slow & freeze) | Ice Lance / Frost Nova / Blizzard |
| 11 | **Stormcaller** | Caster | Static Charge (every 5th hit = lightning) | Chain Lightning / Gale Leap / Tempest |
| 12 | **Necromancer** | Caster | Soul Harvest (kills restore mana/HP) | Life Drain / Raise Dead / Plague |
| 13 | **Warlock** | Caster | Dark Pact (cast with HP when out of mana) | Shadow Bolt / Curse of Agony / Void Rift |
| 14 | **Druid** | Hybrid | Wild Growth (regen on grass) | Entangle / Healing Rain / Bear Form |
| 15 | **Cleric** | Support | Sanctity (+30 % healing, burns undead) | Heal / Smite / Divine Shield |
| 16 | **Shaman** | Support | Spirit Link (shares damage taken) | Lightning Totem / Healing Totem / Ancestral Fury |
| 17 | **Bard** | Support | Inspiration (ally speed, +25 % XP) | Song of Courage / Dissonance / Encore |
| 18 | **Monk** | Melee DPS | Inner Peace (unarmed +6, no fall dmg, 15 % dodge) | Palm Strike / Chi Burst / Fists of Fury |
| 19 | **Alchemist** | Ranged DPS | Chemist (poison immune) | Acid Flask / Elixir / Toxic Cloud |
| 20 | **Artificer** | Ranged DPS | Tinkerer (durability, mining speed) | Grapple / Deploy Turret / Cluster Bomb |

Every class also has base stat modifiers (health / speed / damage / armor) shown in the GUI.

## Commands & permissions

| Command | Permission | Description |
|---|---|---|
| `/class [menu\|info\|list\|items\|reset\|<class>]` | `rpgclasses.use` (default) | Class menu / info / restore items |
| `/class set <player> <class>` | `rpgclasses.admin` | Force-set a class |
| `/cast <1-3>` | default | Cast a skill |
| `/skills` | default | Skill list |
| `/rpgadmin reload\|setlevel\|addxp\|pack\|packinfo` | `rpgclasses.admin` (op) | Admin tools |

## Configuration (`plugins/RPGClasses/config.yml`)

* `resource-pack.*` – enabled, required (kick on decline), prompt text, mode (`builtin`/`url`), port, public address
* `leveling.*` – max level, XP curve, XP per mob HP / player kill / block
* `mana.*` – base mana, mana per level, regen per second
* `casting.hud` – toggle the action-bar HUD
* `class-change.*` – allow resets, cooldown

## Resource pack

`pack.zip` (all via the `minecraft:item_model` component, namespace `rpgclasses:`) contains:

* **20 class icons** (`rpgclasses:<classid>`) used in the GUI
* **20 Power Orbs** (`rpgclasses:orb_<classid>`) — the casting item, one glowing orb per class
* **8 custom weapons** (`rpgclasses:weapon_longsword|katana|greataxe|longbow|dagger|staff|warhammer|lute`) — each class gets a named, unbreakable, soulbound signature weapon with its own stats
* **8 custom sounds** (`sounds.json`): `rpgclasses:cast.generic|fire|ice|holy|dark|thunder`, `levelup`, `ui.select` — played on cast / level-up / skill switch
* **6 custom particles** (`rpgclasses:particle_rune|skull|star|leaf|ember|snow`) — rendered as item particles so no vanilla particle is overwritten; each class has a themed particle burst on cast

`pack.mcmeta` targets pack format 75 (1.21.11) with a wide compatibility range.

Soulbound items can't be dropped, stored in containers, put in frames or lost on death.
Regenerate/modify with `python3 packgen/build_pack.py` (needs Pillow), then rebuild.

## Building from source

```
export JAVA_HOME=/path/to/jdk21
mvn clean package          # -> target/RPGClasses-1.1.0.jar
```

## Project layout

```
src/main/java/com/arena/rpgclasses/
  RPGClassesPlugin.java      main class / wiring
  model/                     RPGClass, Skill, PlayerData
  skills/ClassRegistry.java  ALL 20 classes & 60 skills (edit here to balance/add)
  manager/                   ClassManager (stats, XP), ManaManager (HUD), SkillManager (casting), ItemManager (orb/weapons), DataManager (YAML)
  listener/                  PlayerListener (join, pack, input), CombatListener (passives, XP, hooks)
  gui/ClassGUI.java          class selection menu
  pack/PackManager.java      pack extraction, built-in HTTP server, delivery
  command/Commands.java      all commands + tab completion
  util/FX.java               particle/targeting/damage helpers
packgen/build_pack.py        generates the resource pack
```

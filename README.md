# SafariContest

A standalone client-side Fabric mod for Hypixel SkyBlock dungeon players who want Miria contest reminders and ticket coordination. Ported from [SparklingMutuals](https://github.com/johnceoskyblock/sparkling-mutuals).

## Features

- Miria contest HUD with remaining time, bracket, score, and ticket status throughout SkyBlock.
- Automatic party message when your ticket is earned: **r** (default), **!m7**, **!t5**, or **other** with your own text.
- Automatic party message at each new contest: **!dt contest** (default), or **other** with your own text. Every mod user sends their own message anywhere in SkyBlock, including dungeons; no leader requirement.
- Basic (Uncommon or higher) and First-Class (Legendary / 5,000 points) ticket targets.
- Incomplete contest title/sound warnings at 5, 3, and 1 minute, individually configurable.
- Personal Torrhus Canyon Honeyhive beacons, persistent one-hour loot timers, Queen Bee refill detection, and nearby hologram corrections. Hives hide once your selected ticket target is complete.

Open settings with **/safaricontest** or **/sc**. The two message controls are dropdown menus; choose **other** to enable the adjacent custom text field. Blank custom text falls back to the default. Messages are always sent as the body of `/pc`.

## Install

Choose the JAR matching Minecraft **26.1.2**, **26.2**, or **26.3** from [Releases](https://github.com/johnceoskyblock/SafariContest/releases). Put it in your `mods` folder alongside Fabric API and Fabric Language Kotlin. Requires Fabric Loader 0.19.5+ and Java 25+. Hypixel Mod API is bundled.

This mod runs independently; SparklingMutuals is not required. If using both mods, disable overlapping announcements and hive/HUD features in one to avoid duplicates. No Safari entry automation, party entry guard, sparkling lookups, or backend account service is included.

## Timing and persistence

The original local-clock schedule is preserved: contests start at :15, :35, and :55, with a 30-second end gap. Keep your computer clock accurate. The initial observation establishes a baseline; joining or enabling the feature mid-contest does not send a catch-up start message. Starts are sent only when a new window is observed within its first ten seconds with a loaded world and SkyBlock sidebar. A solo start still attempts `/pc`; Hypixel may report that you are not in a party.

Ticket detection continues with the HUD hidden, requires current Miria scoreboard evidence at least ten seconds into the window, and waits for a confirmed party before sending. Ticket announcements are remembered per player/window/target across restarts. Start sends are also remembered per player/window. Settings and personal hive data live under `config/safari-contest/`. Progress is retained across same-profile reconnects within the current client session; account/profile changes isolate that evidence. Hive cooldowns persist across full restarts.

## Build and verify

With Java 25 installed:

```sh
./gradlew buildAllTargets
```

Windows: `gradlew.bat buildAllTargets`. Tested installable JARs are collected under `build/distributions/1.0.0/`. Each target also runs the ported Honeyhive regression tests and contest/message tests. GitHub Actions builds all three targets, exposes a required `build` check, and publishes tested JARs for matching `v<mod_version>` tags.

For an offline settings smoke check: `gradlew.bat :26.1.2:runClient -PclientSmoke`. This opens the settings after startup without joining a server. Use `/safaricontest debug` in game to log nearby Honeyhive diagnostics.

Source provenance: contest, Honeyhive, sidebar, location, party membership parsing, and rendering logic ported from SparklingMutuals commit `6d8472f518e4da6b7b55e97e4edd3ef7b5b68acb`. Original authors: JohnCEO and JohnKuudra. CC0-1.0; see [LICENSE](LICENSE).

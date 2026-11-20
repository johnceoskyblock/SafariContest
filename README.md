# SafariContest

A standalone client-side Fabric mod for Hypixel SkyBlock dungeon players who want Miria contest reminders and ticket coordination. Ported from [SparklingMutuals](https://github.com/johnceoskyblock/sparkling-mutuals).

## Features

- Condensed one-row HUD: **Miria Contest** and a green checkmark or red X. No timer, bracket or score rows.
- SparklingMutuals-style configuration with sidebar pages, search and the same settings themes. Move the HUD by dragging, resize by scrolling, nudge with arrow keys, snap to edges/center, or right-click it for appearance settings.
- Automatic party message when your ticket is earned: **r** (default), **!m7**, **!t5**, or **other** with your own text.
- Automatic party message at each new contest: **!dt contest** (default), or **other** with your own text. Only sends inside a **dungeon or Kuudra instance**, with no leader requirement. Dungeon Hub, private islands, Garden, Torrhus and other locations do not send it.
- Basic (Uncommon or higher) and First-Class (Legendary / 5,000 points) ticket targets.
- Incomplete contest title/sound warnings at 5, 3, and 1 minute, individually configurable.
- Personal Torrhus Canyon Honeyhive beacons, persistent one-hour loot timers, Queen Bee refill detection, and nearby hologram corrections. Hives hide once your selected ticket target is complete.

Open settings with **/s** or **/safaricontest**. Use **/s gui** for the HUD layout editor. The former `/sc` alias is removed. A short welcome block shows only SafariContest and the clickable `/s` command when you join SkyBlock.

The two message controls are dropdown menus; choose **other** to reveal a custom text field. Blank custom text falls back to the default. Messages are always sent as the body of `/pc`.

## Party readiness

**Every ticket message requires your own `!dt contest` in the current party and current contest.** Your server-confirmed party-chat echo arms the message, whether sent manually or by the automatic contest-start feature. Joining a party with an already-earned ticket does not send anything. Another member's request, your `!dt food`, `1s`, `sec`, or a different custom start message does not arm it. Leaving, joining/replacing parties, disconnecting, and a new contest clear this permission; ordinary same-party server transfers and roster/leader updates retain it. Permission is not restored from disk.

Only party messages containing a standalone `!dt` token (including a reason such as `!dt contest`), or consisting of exactly `1s` or `sec`, create a request for their sender. Matching ignores case and surrounding whitespace. `DT Requested by JohnCEO`, `Players needing DT: JohnCEO`, `a sec`, `sec!`, `one second`, and `!dtfoo` do not count. Any other party message clears only its sender's pending request, treating that sender as ready. Requests belong to the current contest and persist across relogs. Leaving players are removed; changing/disbanding your party clears requests.

When your ticket message starts with `!`:

- **Party member:** if another party member has an outstanding request, send **r** instead of the command. Your own automatic contest request only authorizes your ticket message; it never causes the `r` fallback. This counts as your one ticket announcement; the command is not sent later.
- **Party leader:** never substitute **r** for the command. Wait until every other requester sends a non-request message (including `r` or `ready`), then send the configured command once. Your own request does not block you once your ticket is earned. A pending command expires at the end of that contest.
- **Unknown leader:** wait for server-confirmed party information before deciding whether to send a command.

After your own contest request, if nobody is waiting, send the configured command normally. Ticket messages that do not start with `!` (including an explicitly selected `r`) also require that request, and then send normally. These rules are also explained in the Party settings page.

## Install

Choose the JAR matching Minecraft **26.1.2**, **26.2**, or **26.3** from [Releases](https://github.com/johnceoskyblock/SafariContest/releases). Put it in your `mods` folder alongside Fabric API and Fabric Language Kotlin. Requires Fabric Loader 0.19.5+ and Java 25+. Hypixel Mod API is bundled.

This mod runs independently; SparklingMutuals is not required. If using both mods, disable overlapping announcements and hive/HUD features in one to avoid duplicates. No Safari entry automation, party entry guard, sparkling lookups, or backend account service is included.

## Timing and persistence

The original local-clock schedule is preserved: contests start at :15, :35, and :55, with a 30-second end gap. Keep your computer clock accurate. The initial observation establishes a baseline; joining, enabling the feature, or entering an instance mid-contest does not send a catch-up start message. Starts require a new window observed within its first ten seconds with a loaded world, SkyBlock sidebar and a confirmed dungeon/Kuudra Mod API location. Unknown locations fail closed. A solo start inside an eligible instance still attempts `/pc`; Hypixel may report that you are not in a party.

Ticket detection continues with the HUD hidden, requires current Miria scoreboard evidence at least ten seconds into the window, and waits for a confirmed party before sending. Ticket announcements are remembered per player/window/target across restarts. Start sends are also remembered per player/window. Settings and personal hive data live under `config/safari-contest/`. Progress is retained across same-profile reconnects within the current client session; account/profile changes isolate that evidence. Hive cooldowns persist across full restarts.

## Build and verify

With Java 25 installed:

```sh
./gradlew buildAllTargets
```

Windows: `gradlew.bat buildAllTargets`. Tested installable JARs are collected under `build/distributions/1.1.0/`. Each target runs the Honeyhive, contest, party readiness, instance gating, welcome and HUD layout tests. GitHub Actions builds all three targets, exposes a required `build` check, and publishes tested JARs for matching `v<mod_version>` tags. Re-releasing an existing tag replaces its JAR assets.

For an offline settings smoke check: `gradlew.bat :26.1.2:runClient -PclientSmoke`. This opens the settings after startup without joining a server. Use `/safaricontest debug` in game to log nearby Honeyhive diagnostics.

Source provenance: original contest, Honeyhive, sidebar, location, party membership parsing, and rendering logic ported from SparklingMutuals commit `6d8472f518e4da6b7b55e97e4edd3ef7b5b68acb`. HUD layout/snapping, settings palettes/buttons and welcome styling follow the local SparklingMutuals implementation. Original authors: JohnCEO and JohnKuudra. CC0-1.0; see [LICENSE](LICENSE). Adapted SafariUtils UI/snapping/sprite material retains its [MIT notice](licenses/SafariUtils-MIT.txt).

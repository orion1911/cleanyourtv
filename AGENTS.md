# AGENTS.md

Instructions for coding agents (Claude Code, Codex, Gemini CLI, Cursor, Copilot and others) working in
this repository. People should start with [README.md](README.md) and [CONTRIBUTING.md](CONTRIBUTING.md);
this file repeats their rules in the form an agent needs, and links to them for the detail.

## First: which job are you doing?

| You were asked to | Do this |
|---|---|
| Clean up a TV: debloat it, install the launcher, push a theme | Read [PROMPT.md](PROMPT.md) and follow it exactly, on the real TV. Use the tools in `tools/` and the launcher in `launcher/` rather than writing new ones; the only file you need to change is `launcher/app/src/main/res/values/config.xml`, or a theme's `config.json`, for this TV. Do not edit the repo's docs, tests or other code, and do not open issues or pull requests unless the person asks you to. |
| Change this repo: a TV list, a fix, a translation, a theme, docs | Skip the next section and read on from [Repo map](#repo-map). |

### Cleaning a TV: the rules that are not negotiable

[PROMPT.md](PROMPT.md) is the procedure. Its safety rules, in brief:

- **Disable, never uninstall.** `pm disable-user --user 0`, one package per shell call. No
  `pm uninstall`, no root, nothing flashed. Every step must be undone by `tools/restore.sh`.
- **Back up first.** Package list, disabled list, `getprop` and settings into `backup/` before the first
  change. `tools/disable.sh` also writes its own record in `~/.cleanyourtv/`; keep the list you applied.
- **Leave alone what the TV needs to work:** the TV app and input services behind HDMI and the tuner,
  the input/source menu, remote and Bluetooth pairing, the on-screen keyboard, Play Store and Play
  Services. When a package name says nothing, read `dumpsys package <name>`, then ask.
- **Never disable Android's core packages:** the permission controller, package installer, ext
  services/shared and module metadata (`com.android.*` and `com.google.android.*`),
  `com.google.android.overlay.modules*`, `com.android.providers.*` (calendar, contacts, user dictionary
  and blocked numbers aside), `com.android.systemui`, `com.android.shell`, `com.android.phone`,
  `com.android.settings`, `com.android.tv.settings`, `android`. Without them Android may not boot (#884);
  `tools/disable.sh`, `disable.ps1` and `check.py --fix` refuse such a list, with no override.
- **Never start a factory or test package** (`com.cvte.tv.hkcfac`, `mediatek.factorymenu.ui` and the like),
  not even to look. On some boards that flips a factory-mode flag the shell cannot undo.
- **Keep the Home guard.** The stock launcher is disabled only after the new launcher is installed and
  confirmed as Home (`dumpsys activity activities | grep mFocusedApp`). On Google TV, check
  `dumpsys wifi | grep -o 'cname=[^ ]*'` before disabling setupwraith: if it owns the saved Wi-Fi
  network, disabling it takes the TV off Wi-Fi and ADB. `tools/disable.sh` and
  `launcher/build-and-install.sh` already enforce this. Use `FORCE_HOME=1` or `FORCE_SETUPWRAITH=1` only in
  the situation the script's own message describes, and only with the person's agreement.
  - On a CVTE board (any `com.cvte.*` package), leave its stock launcher (`com.cvte.tv.launcher.*`) enabled
    unless the `examples/` folder for that exact board disables it. Make the new launcher Home with
    `cmd package set-home-activity`, reboot once, and confirm Home and ADB come back before anything else.
    Don't enable a remote-key helper in the same pass: a JVC set hung on its boot animation with ADB gone
    after both were done before one reboot (#741).
- **Ask before anything the family uses.** Streaming apps, the voice assistant, Chromecast, Miracast and
  the firmware updater go into one checklist question; disable only what the person does not tick.
- **Disable in small batches** and screenshot between them (`tools/tv.py --screenshot shots/01.png`).
  Live TV, HDMI and some streaming apps give empty screenshots; that is the TV, not damage.
- **An injected key is not proof** for a remote button that goes through an accessibility helper: ask the
  person to press the real button. `am force-stop com.gathin.tvhome` switches those helpers off.
- **Close ADB afterwards.** USB debugging and Network/ADB debugging off, authorisations revoked, reboot,
  then `tools/adb-closed.py <tv ip>` must report the port closed. Port 5555 is plaintext.
- **Stop and say so** when the TV refuses: `pm disable-user` refused for every package, a sideload that
  fails verification, or an SDK below the launcher's `minSdk` 26. Do not retry in a loop, and do not lower
  `minSdk` or hand over an APK built elsewhere.

The person's backups (`backup/`), screenshots (`shots/`), notes (`work/`) and the ADB key
(`tools/adbkey/`) are listed in `.gitignore` and must never be committed. If the person wants to
share their TV's list afterwards, follow [`docs/workflows/tv-list.md`](docs/workflows/tv-list.md).

## Repo map

| Path | What it is |
|---|---|
| `PROMPT.md` | The prompt people paste into their agent. Brand-neutral; safety rules live here. |
| `README.md` | Front page, with the TV table (generated from `examples/*/tv.json` by `tools/gen_tables.py`) and the Tools table. `README.zh-CN.md` and `README.vi.md` are translations. |
| `CONTRIBUTING.md` | The contributor guide for people. "Where does my idea go?" is the routing table. |
| `SECURITY.md` | Private vulnerability reporting. |
| `SHOWCASE.md` | Projects built on this repo that live in their own repositories. |
| `CODE_OF_CONDUCT.md` | Includes the rule against publishing device identifiers, Wi-Fi names and IPs. |
| `examples/` | One folder per tested TV model: `debloat.txt`, `tv.json` (its row in the TV tables), sometimes a `README.md` and a `config.json`. Indexed in `examples/README.md`, whose table is generated too. |
| `docs/` | `docs/TROUBLESHOOTING.md`, the module-card contract (`docs/modules.md`), `docs/devices/` for models with notes but no tested list, and the [workflows](#workflows). |
| `launcher/` | The reference launcher, package `com.gathin.tvhome`: plain Java, AndroidX RecyclerView and DynamicAnimation, `minSdk` 26, no `INTERNET` permission. |
| `themes/` | One folder per theme: `config.json`, a required `screenshot.jpg`, optional wallpapers. Keys are documented in `themes/README.md`. |
| `addons/` | Optional module-card apps, each its own Gradle project, never built into the launcher. |
| `tools/` | The ADB client (`tv.py`) and the scripts, listed in the README's Tools table. Shell scripts have PowerShell twins. Each script's usage is in its first lines, and the larger ones have a guide beside them (`tools/*.md`): read it before running the script. |
| `tests/` | Python `unittest` suite: list format, README tables, press counts, themes, add-ons, the Home guard and the tools against fake TVs. |
| `.github/workflows/` | `ci.yml` (tests, launcher build, APK size gate), `addons.yml`, `maintainer-bot.yml`. |
| `.github/maintainer/MAINTAINER.md` | The review policy the maintainer and the PR bot apply. |
| `.github/pull_request_template.md` | The PR checklist. Fill it in. |

## Where a change goes

The core stays small on purpose. Pick the smallest home that fits (CONTRIBUTING, "Where does my idea go?"):

| The change | Where it goes |
|---|---|
| A TV list, a fix, a translation, docs, a tool fix | A pull request here |
| A small launcher switch that is off by default | A pull request here: see [`docs/workflows/launcher-switch.md`](docs/workflows/launcher-switch.md) |
| A new look: colours, wallpaper, dock, columns, that existing config keys can carry | `themes/<name>/` |
| A widget or tile that can be its own small app | `addons/<name>/`, as a module card that never touches `launcher/` |
| A whole new launcher, a heavy restyle, a port to another platform, a lower `minSdk`, new permissions or network access, Fire TV Home takeover, a changed default for every TV | Your own repository, plus one row in `SHOWCASE.md` |
| Safety rules in `PROMPT.md`, the Home-safety logic in the scripts, protected packages, CI and workflows, the APK size gate | Open an issue for the maintainer; do not change it in a PR |

Every PR is reviewed by the maintainer and by a bot that apply `.github/maintainer/MAINTAINER.md`: read its
verdicts before a non-trivial PR. Changes to the Home or launch path (Home key, boot, return to Home, the
install scripts) are usually held for a run on a real TV.

## Workflows

Step-by-step methods in `docs/workflows/`. Read the one that fits before you start; each opens with when to
use it and ends with the check to run.

- [`tv-list.md`](docs/workflows/tv-list.md): add a tested TV's debloat list with its `tv.json`, or update one.
- [`launcher-switch.md`](docs/workflows/launcher-switch.md): add an off-by-default launcher switch, within the size gate.
- [`translation.md`](docs/workflows/translation.md): add a launcher language, or fill strings missing from one.
- [`theme.md`](docs/workflows/theme.md): add a theme from existing keys, with its screenshot from a real TV.
- [`addon.md`](docs/workflows/addon.md): add a module-card app that never touches `launcher/`.
- [`showcase.md`](docs/workflows/showcase.md): list a project that lives in its own repository.
- [`press.md`](docs/workflows/press.md): add an article and update the four press counts.
- [`docs-notes.md`](docs/workflows/docs-notes.md): add a troubleshooting entry or a `PROMPT.md` fix, keeping pinned facts.
- [`perf-ab.md`](docs/workflows/perf-ab.md): an optional check of a launcher change for performance regressions with
  `tools/bench.py`, and measure an improvement or a failed check with traces, a report and flame charts.

## Before opening a PR

| Check | Command or rule |
|---|---|
| Tests, as CI runs them | `python3 -m unittest discover -s tests -v` after `python3 -m venv tools/venv` and `tools/venv/bin/pip install -r tools/requirements.txt`. On `main`, CI sets `CLEANYOURTV_STRICT=1`, and its "README tables" job checks that the TV tables and counts are what `tools/gen_tables.py` writes. On Windows the suites that drive `tools/*.sh` report as skipped: they need a POSIX shell, and CI runs them. |
| Launcher, if you touched `launcher/` | JDK 17, Android SDK platform 34, build-tools 34.0.0. In `launcher/`: `./gradlew --no-daemon testReleaseUnitTest lintRelease assembleRelease`. Lint must stay at zero errors. |
| APK size gate | `launcher/app/build/outputs/apk/release/app-release.apk` must be under 589,824 B (576 KB). Linux: `stat -c %s`, macOS: `stat -f %z`. |
| README rows | A new list adds only its folder, with a `tv.json`: the TV tables, the brand line and the counts are generated after merging, so leave them alone. A theme has its row in `themes/README.md`; an add-on in `addons/README.md`. |
| One topic per PR | A TV list never rides inside a launcher feature PR. A device's list plus small fixes for that same device can be one PR. |
| Tested on | Say which TV, Android version and firmware you tested on, in the PR template's "Tested on" line. |
| Real TV | Launcher changes are verified on a real TV, not only the emulator: `tools/tv.py --screenshot shots/after.png DPAD_DOWN`. D-pad from every edge (Up, Down, Left, Right). A button handled by an accessibility helper needs a press on the real remote, and a force-stop of the launcher switches the helpers off (`docs/TROUBLESHOOTING.md`). |
| Screenshots | Every visual change has a screenshot from the TV in the PR description. |
| Performance | A performance claim shows `tools/bench.py` before and after, plus trace numbers. For any other launcher change the regression check in [`docs/workflows/perf-ab.md`](docs/workflows/perf-ab.md) is optional but welcome. |
| Stray files | No files that nothing links to: screenshots belong in the PR description, not the tree. |

## Hard rules

Never do these in a PR:

- **No prebuilt APKs or binaries** in the repo, not even as CI artifacts. People build from source.
- **No uninstalling.** No `pm uninstall` in a list or a script. `pm hide` or `pm block` only with the matching
  undo written in the list header.
- **No network in the launcher:** no `INTERNET` permission, no network features, no analytics or telemetry, no
  background services or polling, no blur or video. It targets TVs with 1 GB of RAM.
- **No new launcher dependencies** without discussing them in an issue first.
- **No changes to the safety rules** in `PROMPT.md`, the Home guard and other safety checks in
  `tools/disable.sh`, `tools/check.py` and `launcher/build-and-install.sh`, or the list of packages the TV
  needs, without the maintainer.
- **No changing a default for every TV.** New behaviour is off until a theme or `config.xml` turns it on.
- **Firmware updaters are better left enabled** (commented out in the list); a list that disables one says
  why in its header.
- **No Private DNS or AdGuard setup, and no install or restore scripts, inside a list folder.** The README
  already has the optional DNS extra; a note that Private DNS works on that TV is fine.
- **No secrets or identifiers** in commits, lists or screenshots: no IP addresses, Wi-Fi names, account emails,
  serial numbers, Android IDs, MAC addresses or ADB keys. Use `<tv ip>` or `192.168.1.50` in docs.
- **Don't edit `.github/workflows/`** or the APK size gate in a normal PR. A PR whose main content is a new CI
  workflow that builds or publishes releases is closed.
- **No root, no getting around app-store protection, no private builds** others cannot reproduce from this repo.
- **No extras added to the PROMPT flow** beyond debloating and the launcher.
- **Don't relax `minSdk`** (26) or the launcher's permissions; those are forks for `SHOWCASE.md`.
- **Blur or crop** Wi-Fi names, people's names and cities (the top bar, a theme's `greeting_name`) before a
  screenshot goes anywhere public. No TV Settings screenshots that show accounts, emails or device names.
  From `tools/tv.py --probe`, `getprop` and backups, quote only the model, Android version and firmware build.
- **Security problems are reported privately**, as [SECURITY.md](SECURITY.md) describes, never in a public issue.

## Commit and PR style

- Titles are `area: what changed`, in plain words, with the issue in parentheses when there is one:
  `examples: add Epson LS800 laser projector (Askey HA90, Amlogic), Android TV 14`,
  `docs: an HDMI row opens any app that claims the channel MIME type (#805)`,
  `Launcher: show_inputs_pill, to hide the Inputs pill`. The area is the folder or file the change is in:
  `examples`, `Launcher`, `docs`, `tools`, `tests`, `themes`, `addons`, `SHOWCASE`, `README`, `Press`.
- The body is one or two sentences on what changed and the TV it was tried on, then the template's checklist.
  `Fixes #N` or `Closes #N` when it resolves an issue.
- One topic per PR. Small is fine. Be kind in reviews.
- Contributions are released under the repo's MIT licence.

## Where to ask

Questions go to [GitHub Discussions](https://github.com/Gathin23/cleanyourtv/discussions); bugs, TV lists and
ideas that need a launcher change or a new dependency go to an issue first, with the templates in
`.github/ISSUE_TEMPLATE/`.

## Handover: this checkout's TV (Sony BRAVIA BF1), local customisations

Local work for one person's TV, not upstream material: the launcher changes below are a heavy restyle with some
changed defaults, which the rules above route to a fork (`SHOWCASE.md`), so do not open a PR from this tree as is.
The new look is switched on by the `aerial-glass` theme only; every new key is off by default.

### The TV

- Sony BRAVIA BF1, Google TV 12 (SDK 31), firmware `STT2.230811.001`. 4K panel, UI at 1920x1080, density 2.0.
- Address and restore commands: `backup/RESTORE.md` (gitignored). Home is `com.gathin.tvhome`; the stock
  `launcherx`, `setupwraith` and `dreamx` (Google's ambient screensaver) are disabled.
- Settings changed by hand: `secure screensaver_components` and `screensaver_default_component` =
  `com.gathin.tvhome/.ClockDream`. With `dreamx` disabled, the old value made the screensaver fail to bind and the TV
  dropped to standby after 10 minutes. `global stay_on_while_plugged_in` set back to `0` (a "Stay awake" switched on
  later stops any screensaver). `screen_off_timeout` is 10 minutes.
- Netflix (`com.netflix.ninja`) is hidden on Home but enabled. Once opened (from TV Settings), its own sign-in flow
  kept bringing it to the front; it was force-stopped. Disabling it was offered; the person has not answered.

### How to work on it (Windows, PowerShell 5.1)

- `tools\tv.py`: `$env:TV_HOST='<tv ip>'`, run with `tools\venv\Scripts\python.exe -W ignore`. PowerShell 5.1 drops
  inner quotes from native arguments, so commands with quotes or pipes go through `--batch` from a file:
  `cmd /c "tools\venv\Scripts\python.exe -W ignore tools\tv.py --batch < work\q.txt"`.
- Build and install: `launcher\gradlew.bat --stop`, then `launcher\build-and-install.ps1 -TvHost <tv ip>` (about
  90 s; Home already belongs to the launcher, nothing gets disabled). Without the `--stop` a leftover daemon can lock
  `minifyReleaseWithR8\classes.dex` and the build fails in seconds. Run long jobs in the background: the shell
  connector used here stalls under Gradle load.
- Theme: `tools\theme.ps1 -TvHost <tv ip> themes\aerial-glass` (about 2 minutes, re-uploads every wallpaper). Its
  exit code 1 comes from Python warnings on stderr; success is the line "applied". Code-only changes need no push.
- Checking: `tools\tv.py --screenshot shots\N.png`; `uiautomator dump` for exact bounds of tiles, names and the
  clock; `work\ascii.ps1` (coarse ASCII preview) and `work\tiles.ps1` (per-tile colours) measure screenshots. The
  person reviews with phone photos next to the samples; that is the real visual check.
- Screensaver without waiting: `am start -n com.android.systemui/.Somnambulator`, then
  `dumpsys dreams | grep mCurrentDreamName`; wake with `input keyevent KEYCODE_WAKEUP`.

### The design and where it lives

Target: the photos in `inspiration/`; `WhatsApp Image 2026-10-26 at 14.46.00.jpeg` (snowy peaks, 11:05) is the core
look. The table is the state the person last approved.

| Theme key (aerial-glass) | Code | What it does |
|---|---|---|
| `clock_large`, `show_greeting: false` | `HomeClockStyle` (new), `MainActivity.applyLargeClock` | Outfit ExtraLight 126sp squeezed to 0.75 width (reads tall), the date under it in the locale's order and sentence case, no text shadows (their blur was clipped into a visible box), top buttons level with the clock's top |
| `screensaver_home` | `ClockScene`, `ClockDream`, `dream_clock.xml` | The screensaver is Home without tiles and buttons: same clock in the same place, the picture Home is showing (Home's dim and colour depth), full brightness, crossfaded rotation written back to Home's rotation state |
| `square_tiles`, `columns: 10`, `focus_zoom: 120` | `AppAdapter` (sizes, 22% corners, tile = 81% of its slot), `MainActivity` (row placement) | Near-square tiles; the row sits low (tiles end near 89% of the height) and starts under the clock. At most 10 show and the rest scroll in from the right (grid clip bounds); fewer than 10 are centred (an offset decoration). `Config` accepts up to 10 columns |
| `clear_glass` | `AppAdapter.smokyGlass`, `glassTile`, `whiteMark(keepColour)` | One cold glass for every tile (`COLD_GLASS`: navy top to steel blue bottom, about 85% opaque) to match the wallpapers; per-app colours were dropped at the person's request (red and pink tiles). Logos in their own colours with plate, haze and drop shadow removed (adaptive foreground masked to the icon's rounded square), 78% of the tile height, unfocused ones 25% dimmer (`RESTING_LOGO`) |
| `white_marks` (off) | `AppAdapter.whiteMark`, `brandGlass` | Earlier variant, white logos on brand-coloured glass; kept, unused |
| `glass_style: rim` | `GlassView` | Focus: a crisp white rim, 1dp at about 65% white, no halo |
| `always_show_names: false` | `AppAdapter`, `item_app.xml` | Only the focused tile shows its name, under it: Outfit Light 13sp, no shadow. With `dock_title` off the focused name now shows in a single row (it used to stay hidden) |
| (square tiles) | `TileShadow` (new) | A soft shadow drawn only outside each tile. Android's elevation shadow showed through the glass as a dark square, so the tiles' outline alpha is 0 (it still clips the corners) |
| `wallpapers/` | `themes/aerial-glass/wallpapers`, `CREDITS.md` | 12 cold blue-hour landscapes from Wikimedia Commons (CC licences, credited), cropped to 1920x1080 and graded cooler and about 10% darker by `work\make_walls.ps1`, from candidates `work\fetch_walls.ps1` measured. Upscaled sources need `TileFlipXY` in the scaler, or their edges come out black (looked like bars). The previous Unsplash set is in `work\old-wallpapers` |

| `notifications_button` | `GuardRevival` (now also lists, cancels and clears notifications), `MainActivity.addBell`, `ic_bell*.xml` | A bell left of Wi-Fi, with a dot while anything waits; it keeps the pill tint so it inverts on focus. It opens the panel below on its notifications. Google TV's own panel belonged to the disabled `launcherx`. Needs `cmd notification allow_listener com.gathin.tvhome/com.gathin.tvhome.GuardRevival` (granted; Android 12 records it in `dumpsys notification`, not `enabled_notification_listeners`) |
| `settings_key_panel` | `QuickSettingsActivity` (rewritten), `activity_panel.xml`, `panel_drawer.xml`, `HomeKeyService` | The remote's gear (`KEYCODE_SETTINGS`, otherwise routed to a MediaTek agent that opened launcherx's panel) opens a drawer shaped like Google TV's: date and time with a TV-settings gear, tiles (Screensaver, Wi-Fi, Bluetooth, Picture, System update, Launcher), notification cards that turn light on focus, Clear all. Installed with `-HomeKeyService -RestoreKeyServices`; re-pass both on every install. Only a real press proves the gear (injected keys bypass the helper) |

Tile shadow (`TileShadow`): light at rest, it follows the zoom and lift and deepens to its focused strength; the
Holder invalidates the item on each animation frame. Unfocused logos are 25% dimmer.

### To do next (checkpoint 2026-10-27, 01:24)

1. **Bluetooth tile shows "feature not available".** `Settings.ACTION_BLUETOOTH_SETTINGS` does not resolve on this
   Sony. Find what Google TV's settings use for Bluetooth / "Remotes & accessories" (`cmd package query-activities`
   on `com.android.tv.settings`, e.g. an accessories activity) and open that from the tile instead.
2. **Weather widget beside the clock, as in the core sample** (current temperature with an icon, place, condition,
   high/low, and the small advice pill), with the clock made a little smaller to make room. The launcher has no
   `INTERNET` permission by design, so weather needs either an add-on module card app (`addons/`, its own network
   permission) or a fork-level permission change: decide with the person first.
3. Confirm with the person that the gear button opens the new drawer (real press) and that the last build installed
   (it finished while the connector was down).
4. Netflix: disable or leave as it is (asked, not answered).

Also changed: `WallpaperPlayer`'s crossfade (the incoming picture was added behind the opaque current one, so no fade
showed and the change landed as a cut); `styles.xml` raises the app's shadow alphas (the tiles no longer use them,
other elevated views do); `config.xml` has `advanced_effects` on (first session). Ideas not yet asked for: a soft
shade behind the clock digits on very light skies; three thin line-art logos (Media Player, Timers & Clock, one
more) read fainter than the rest.

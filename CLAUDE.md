# Kids MDM Browser (WebView) — Project Context for Claude Code

## What this is

A locked-down Android browser for a kid's phone, package `com.kidsmdm.browser`, built as a thin
Kotlin/Jetpack Compose wrapper around `android.webkit.WebView` — **not** a Chromium engine fork.
Part of the same parental-control ecosystem as:

- [kid-phone-server](https://github.com/siesta5787/kid-phone-server) — self-hosted admin server
- [kids-launcher-mdm](https://github.com/siesta5787/kids-launcher-mdm) — Device Owner launcher/MDM agent
- [kids-mdm-im](https://github.com/siesta5787/kids-mdm-im) — Molly/Signal fork

### Why WebView, not a Chromium fork

An earlier attempt ([siesta5787/android-titanium-browser](https://github.com/siesta5787/android-titanium-browser),
now abandoned, local copy at `../browser/` — **don't resurrect it, don't reuse its build
infrastructure**) forked a full Chromium/Vanadium-based browser. After extensive self-hosted CI
work, its build kept failing at a mystery ~600s cutoff that was never fully root-caused (see that
repo's own `CLAUDE.md` for the full saga — Windows sleep settings, WSL2 networking, and CPU
scheduling priority were all tried; none fully explained it). Separately, the actual maintenance
burden of owning a Chromium fork turned out far larger than the project needs.

Pivoted to wrapping `android.webkit.WebView` instead. Primary target is **GrapheneOS**, whose
system WebView provider is Vanadium itself — this app inherits Vanadium's hardening and patch
cadence *for free* from the OS, without building or maintaining any browser engine. Full design
rationale in the approved plan this was built from (ask if you need the historical plan text).

## Frozen external contracts — do not change without also updating the other repos

- **Package name `com.kidsmdm.browser`** — hardcoded in `kids-launcher-mdm`'s `AppEnforcer.kt`
  (`BROWSER_PACKAGE_NAME`) and its manifest's `com.kidsmdm.browser.ACCESS_JOURNAL` permission.
- **Journal contract** (`journal/JournalDatabase.kt`, `journal/JournalProvider.kt`): table
  `journal_entry(_id, url, title, timestamp, created_at)`, exported signature-permission-gated
  provider at authority `com.kidsmdm.browser.journal`, URI `entries/<sinceId>` → rows with
  `_id > sinceId`, oldest-first, capped at 200. Consumed as-is by `kids-launcher-mdm`'s
  `BrowserHistorySync.kt` — don't touch that file to make this one easier, change this one to
  match it instead.
- **Signature-permission blocker**: `ACCESS_JOURNAL` only grants to a caller signed with this
  app's *exact* certificate. Currently blocked the same way as `kids-mdm-im`'s conversation
  journal — all three apps need the same production signing key before this actually works
  end-to-end on a real enrolled device (debug-keystore testing works fine standalone, see below).
- **MDM policy bundle** already pushed by `kids-launcher-mdm`'s `AppEnforcer.applyBrowserPolicy()`
  via `setApplicationRestrictions` — `DnsOverHttpsMode`, `IncognitoModeAvailability`,
  `BrowserGuestModeEnabled`, `DeveloperToolsAvailability`, `ExtensionInstallBlocklist`,
  `ProxySettings`. Most are moot by construction (this app never builds incognito/extensions/
  dev-tools UI in the first place). `DnsOverHttpsMode` is the one that matters and needs the
  on-device verification spike described in the plan — not yet done as of this writing.
- **kid-phone-server**'s `/api/devices/browser-history` endpoint already exists, matches the
  upload shape the launcher already sends — no changes needed there.

## Non-obvious gotcha: testing on the real enrolled device vs. an emulator

**A freshly-`adb install`ed debug build of this app would not launch at all on the real
Device-Owner-enrolled test device** (`0A031JECB05961`, running `kids-launcher-mdm.debug` as
Device Owner) - `am start`/`monkey` both failed with "Activity class does not exist" /
`START_CLASS_NOT_FOUND`, despite `dumpsys package` showing the activity correctly registered,
`aapt dump badging` confirming the compiled manifest was correct, and the package showing
`suspended=false hidden=false`. Lock Task mode was also confirmed inactive
(`mLockTaskModeState=NONE`). Root cause not fully pinned down - some Device-Owner-driven
restriction is blocking launch of a non-allowlisted sideloaded app through a path that doesn't
show up in the usual suspend/hide/lock-task checks. **Installing and launching the exact same APK
on a plain AVD emulator (`test_avd`) worked immediately, first try** - use the emulator for
day-to-day feature development/iteration, not the live enrolled device, until this is understood.
If you need to test against real Device-Owner policy enforcement specifically, that's a distinct,
deliberate test - don't expect ordinary `adb install && am start` iteration to work there.

Verifying the journal write path can't use `adb shell content query` directly either, even on a
clean emulator - that runs as the `shell` UID, which correctly gets rejected with a
`SecurityException` (`requires com.kidsmdm.browser.ACCESS_JOURNAL`) since shell isn't signed with
this app's certificate. That rejection is itself a *correct* signal, not a bug. To actually
inspect recorded rows during development, pull the raw SQLite file instead:
`adb shell run-as com.kidsmdm.browser.debug cat databases/kids_history_journal.db > local.db`,
then read it locally (a plain SQLite reader, or in a pinch even `grep -a` for known substrings
works well enough to confirm a row landed). `content query` via `run-as` doesn't work either - the
`content` shell command itself requires `ACCESS_CONTENT_PROVIDERS_EXTERNALLY`, which only
shell/system hold, regardless of which UID `run-as` switches to.

## Reference-only, not forked

`anthonycr/Lightning-Browser` (MPL-2.0) and `plateaukao/einkbro` (GPLv3+) may be read for
implementation patterns (WebView state save/restore, `DownloadListener`, permission plumbing,
fullscreen video) - do not copy code verbatim, this is a from-scratch build with no upstream
tracking relationship to either.

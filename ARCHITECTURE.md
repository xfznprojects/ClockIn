# Architecture

ClockIn is a single-activity Jetpack Compose app with a deliberately small, framework-light
architecture: **Composable screens → ViewModels → Repository → Room DAO**, with two platform
services (`NfcManager`, `ScanClock`) injected by hand through constructors. The app is small enough
that a DI framework would add more ceremony than value.

## Layers

```
ui/screens/*          Compose screens + ViewModels (created via viewModel() factories)
data/repository/      ScanRepository — the only way the UI touches persistence
data/local/           Room: ScanRecord entity, ScanRecordDao, AppDatabase, PreferencesManager
nfc/                  NfcManager — Reader Mode wrapper, NDEF parse/write
util/                 ScanClock, DateTimeUtils, CardNumberUtils, Constants, SoundManager
```

## Data model

One table, `scan_records`:

| Column | Type | Purpose |
| --- | --- | --- |
| `id` | Long, PK autoincrement | identity |
| `scan_date` | String `yyyy-MM-dd` | calendar day (always ASCII, locale-independent) |
| `card_number` | String | badge number, digits only, normalized before insert |
| `scan_time` | String `HH:mm:ss` | wall-clock time of the scan |
| `timestamp` | Long, epoch ms | canonical ordering key |

Two invariants keep the UI honest:

1. **`timestamp` always encodes the scan moment, not the insert moment.** Live scans use
   `ScanClock`'s validated time; manual/backfilled entries derive the timestamp from the entered
   date + time (`DateTimeUtils.parseDateTimeToTimestamp`). Every list query orders by `timestamp`,
   so backdated records sort correctly everywhere, including exports.
2. **Storage strings are strict and locale-independent.** All parsing/formatting goes through
   `java.time` with explicit locales and `ResolverStyle.STRICT` — a device set to a non-Gregorian
   calendar or non-ASCII digit locale cannot corrupt stored data, two-digit years are rejected, and
   trailing garbage fails parsing instead of silently succeeding.

## NFC

**Reading** uses `NfcAdapter.enableReaderMode` with `FLAG_READER_NFC_A | FLAG_READER_NFC_B |
FLAG_READER_NO_PLATFORM_SOUNDS` — reader mode (rather than foreground dispatch) allows continuous
scanning without activity relaunches and lets the app play its own confirmation tone. Tag callbacks
arrive on a binder thread; all state mutation is marshalled back to the main thread.

**Badges** are NDEF text records of the form `Badge <n>`. `CardNumberUtils` owns the format:
`normalize()` strips the prefix, `isValidCardNumber()` enforces digits-only, `nfcPayload()` renders
the canonical payload. The writer validates before burning a tag, detects existing content and asks
before overwriting (new tag discoveries are ignored while that decision is pending), and falls back
to `NdefFormatable` for factory-blank tags.

The manifest registers **only** `NDEF_DISCOVERED` for `text/plain`, so a written badge launches the
app, but the app does not hijack every NFC tag on the phone. The launch intent is processed only on
fresh starts (`savedInstanceState == null`), so rotation doesn't re-trigger navigation.

## The clock-trust problem

Kiosk phones are often old devices with dead RTC batteries and no SIM/network — their clock can
reset to 1970 (or drift wildly) after every power loss. Attendance data with wrong dates is worse
than no data, so `ScanClock` gates every scan:

1. If the system clock is plausible (after a built-in floor date), use it — and remember the pair
   *(wall time, elapsedRealtime)* as a reference.
2. Otherwise, estimate: last known-good wall time + elapsed uptime since then. References are
   discarded across reboots (elapsed-time regression) and after 7 days.
3. Otherwise, ask the operator to confirm today's date, and advance that date automatically as whole
   days of uptime pass, so an unattended kiosk rolls over midnight correctly.
4. If nothing is trustworthy, the scan is **rejected with a clear message** rather than recorded with
   a garbage timestamp. Estimated times are labelled in the scan confirmation.

The scan debounce also uses `elapsedRealtime` rather than wall time, so clock adjustments can't
swallow or duplicate scans.

## Scan pipeline

```
tag → NfcManager (parse NDEF text)
    → ScanViewModel.onTagScanned
        1. failure-lock check (an unacknowledged error blocks new scans — and is re-checked
           inside the coroutine before insert and before showing success, to close races)
        2. normalize + digits-only validation
        3. debounce (same badge within 1.5 s, elapsedRealtime-based)
        4. ScanClock.getScanTime() — reject if untrusted
        5. optional duplicate check (per day / per hour, DAO count query)
        6. insert + green indicator + beep; the delayed reset is a tracked Job cancelled by
           newer scans so confirmations aren't cut short
```

## Admin & export

- Admin is PIN-gated (`Constants.ADMIN_PIN`, compared in `AppNavigation`); the unlock lasts for the
  process lifetime by design — it's a kiosk convenience gate, not a security boundary. Anyone with
  the APK can read the PIN; treat physical access to the phone as the actual perimeter.
- Add/edit dialogs share the same validation (strict date, `HH:mm:ss` time, digits-only badge) and
  both derive `timestamp` from the entered values.
- Search escapes SQL `LIKE` wildcards (`%`, `_`, `\`) and renders at most 100 results inside the
  scrolling admin screen.
- Export writes the full history to a CSV in app-private cache (`cacheDir/exports/`) and shares it
  via `FileProvider` + `EXTRA_STREAM` — the email body carries only a short per-day summary, so large
  databases can't blow past the Android binder transaction limit.

## UI state

ViewModels are created with `viewModel()` + factories, so screen state (scan session count, failure
locks, calendar position) survives configuration changes and `viewModelScope` is cancelled properly.
"Today" and NFC-enabled status are refreshed on every `ON_RESUME`, so the home screen never shows a
stale date or a stale NFC warning. Settings persist through `SharedPreferences` exposed as
`StateFlow`.

## Known limitations

- **Single shared PIN**, compiled into the app — fine for a kiosk in a clubhouse, not for adversarial
  environments. Rotate it by editing `Constants.kt` and rebuilding.
- **No schema migrations yet** — the schema is at version 1; add a Room migration before changing it.
- **One device, one database** — there is no sync or multi-station merge; export CSV per device.
- **Badge numbers are the identity** — the app tracks badges, not people; keep the badge → person
  mapping wherever you manage members.

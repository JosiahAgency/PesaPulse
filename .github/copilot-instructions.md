# Copilot Instructions for PesaPulse (Cashia)

Cashia (PesaPulse) is an Android app (Kotlin + Jetpack Compose) that reads M-PESA SMS
messages, parses them into structured transactions, and provides dashboards, calendar
views, analytics, and upcoming-payment reminders — all processed locally on
device.

## Build / test / lint commands

Use the Gradle wrapper; JDK 17 is required (matches CI).

```bash
# Unit tests (fast, no device/emulator needed)
./gradlew :app:testDebugUnitTest

# Run a single unit test class
./gradlew :app:testDebugUnitTest --tests "com.example.cashia.utility.ParserQualityTest"

# Run a single test method
./gradlew :app:testDebugUnitTest --tests "com.example.cashia.utility.ParserQualityTest.someTestMethod"

# Build and lint/resource checks
./gradlew :app:assembleDebug :app:lintDebug

# Compile instrumentation tests (no device/emulator needed)
./gradlew :app:assembleDebugAndroidTest

# Run instrumentation tests (requires a connected device/emulator)
./gradlew :app:connectedDebugAndroidTest
```

CI (`.github/workflows/quality-gates.yml`) runs two jobs:
1. `testDebugUnitTest` + `assembleDebug` + `lintDebug` + `assembleDebugAndroidTest`.
2. A dedicated performance job running only
   `com.example.cashia.utility.ParserPerformanceEvaluatorTest`.

### Git hooks

Enable local hooks with `./scripts/setup-git-hooks.sh` (or `.ps1` on Windows) — this sets
`core.hooksPath=.githooks`.
- `pre-commit` runs `ParserQualityTest` only (fast gate).
- `pre-push` runs the full unit test suite + `assembleDebug`.
When touching the SMS parser, expect these hooks to run automatically; run the same
commands manually if hooks aren't enabled.

## Architecture

- **Package/namespace mismatch**: Gradle `namespace`/`applicationId` is `com.pesapulse`,
  but all Kotlin source lives under `com.example.cashia`. Keep using
  `com.example.cashia.*` for new files — do not "fix" this to match the namespace.
- **Layout** (`app/src/main/java/com/example/cashia/`):
  - `models/` — Room entities, ViewModels, and WorkManager `Worker`s live together here
    (e.g. `MpesaTransactions.kt`, `DashboardViewModel.kt`, `DailyDigestWorker.kt`).
  - `daos/` — Room DAOs, one per entity.
  - `utility/` — parsing pipeline, schedulers, notification/biometric helpers, SMS repo.
  - `ui/navigation/`, `ui/screens/<feature>/`, `ui/reusedElements/` — Compose UI, grouped
    by feature screen (dashboard, analytics, calendar, category, etc.).
  - `widget/` — Jetpack Glance home-screen widget.
  - `AppDatabase.kt`, `MainActivity.kt`, `PesaPulseApplication.kt` at the package root.
- **SMS ingestion & parsing pipeline** (core data flow, spans several files):
  1. `utility/SmsRepository.fetchMpesaMessages` reads new SMS from the `content://sms/inbox`
     provider (only `address = "MPESA"`, filtered by `lastSmsId` via `SyncState`).
  2. `MpesaParserPipeline.kt` normalizes message bodies (`normalizeSmsBody`) and computes a
     stable `contentHash` for dedup/grouping.
  3. `models/MpesaTransactions.kt` (`parseMpesaMessage`) does the actual regex-based
     extraction, producing a `ParseResult` (full match / low-confidence partial fields /
     unmatched).
  4. Full matches become `MpesaTransactions` rows; everything else is stored in
     `UnmatchedSms` for later review/reprocessing.
  5. `utility/ParseLearningService.kt` implements the "learning loop": bump
     `PARSER_VERSION` in `MpesaParserPipeline.kt` after improving regexes, then call
     `reprocessOutdated` to re-run the new parser over previously-unmatched/low-confidence
     rows, promoting successes into real transactions. `clusterByStructure` groups
     unmatched messages by structural "skeleton" to surface new patterns worth a regex.
  6. User corrections (manual re-categorization/edits) are captured via
     `UserCorrectionDao`/`SystemCategoryOverrideDao` and feed back into categorization.
- **Room migrations**: `AppDatabase.kt` maintains explicit numbered `Migration` objects
  (e.g. `MIGRATION_1_2`, `MIGRATION_2_3`). Any entity schema change requires bumping the
  DB version and adding a corresponding migration — do not rely on destructive fallback.
- **ViewModels** follow one-per-feature-screen convention (`DashboardViewModel`,
  `AnalyticsViewModel`, `CalendarViewModel`, `UpcomingViewModel`,
  `CategoryViewModel`, `SettingsViewModel`, `SummaryReportViewModel`,
  `TransactionsViewModel`) and are wired directly into their matching screen under
  `ui/screens/<feature>/`.
- **Background work** (WorkManager) drives recurring notifications:
  `DailyDigestWorker`, `MonthlySummaryWorker`, `UpcomingReminderWorker`, scheduled via
  `utility/NotificationScheduler`. `BootReceiver` reschedules work after device reboot.
- **App lock**: biometric/PIN gate is enforced in `MainActivity` before Compose content is
  shown (`BiometricAuthenticator`, `PesaTimeConfiguration`), not via navigation guards.
- **Widget**: `widget/CashiaWidget.kt` (Glance) reads state through
  `WidgetUpdateManager`/`WidgetPrivacyReceiver`, which respects the app's privacy setting
  (hides balances when locked/private mode is on).

## Key conventions

- SMS parsing/matching logic is regex-driven and versioned (`PARSER_VERSION`,
  `CONFIDENCE_FULL`, `LOW_CONFIDENCE_THRESHOLD` in `MpesaParserPipeline.kt`). When
  changing regexes, also consider whether `ParserQualityTest` / `ParserPerformanceEvaluatorTest`
  fixtures need updates, since these are the CI quality/performance gates.
- Dependency versions are centralized in the Gradle version catalog
  (`gradle/libs.versions.toml`) and referenced via `libs.*` in `build.gradle.kts` — add new
  dependencies there rather than hardcoding versions inline.
- `build.gradle.kts` (root) force-pins a couple of transitive build-tooling dependencies
  (bouncycastle, jdom2) to patched versions for known CVEs — don't remove these forces
  without confirming AGP has bumped them upstream.

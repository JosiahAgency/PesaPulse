# Cashia (PesaPulse) 💸📊

<p align="center">
  <img src="app/src/main/pesapulse_logo-playstore.png" alt="PesaPulse logo" width="140" />
</p>

Cashia is an Android personal finance app that reads M-PESA SMS messages, parses them into
structured transactions, and turns them into live insights and reminders.

## ✨ What the app currently does

- 📥 **SMS sync & parsing**: reads M-PESA messages and classifies transaction types.
- 🧾 **Transaction history**: browse all records, inspect details, and review unmatched SMS.
- 🏠 **Dashboard**: balance and period summaries, due-soon context, and quick financial status.
- 📆 **Calendar view**: spending and activity over time with week/day context.
- 📈 **Insights (Analytics)**: trends, category breakdowns, weekday patterns, and merchant analysis.
- ⏰ **Upcoming payments**: full CRUD for future expenses/income/savings with recurrence and status
  tracking.
- 🏷️ **Category management**: search names and custom keywords, filter your categories and the
  built-in library, and expand subcategory groups. Create categories with optional auto-match
  keywords, personalise built-in names/icons, and auto-sort payments in Other without changing
  already-categorised payments.
  Auto-sort shows a dismissible completion message with the number of transactions updated,
  including zero when nothing changed.
- 🛡️ **Security & privacy**: optional biometric/PIN app lock and privacy-aware home screen widget
  state.
- 🔔 **Smart reminders**: daily digest, upcoming reminders, and monthly summary notifications via
  WorkManager.

## 🧭 Main navigation

Bottom navigation includes:

1. **Home**
2. **History**
3. **Calendar**
4. **Insights**

The floating navigation dock keeps all four labels visible, highlights the active tab,
and restores each tab's saved state. It is hidden on secondary pages such as Settings
and Categories.

Additional routes:

- ⚙️ Settings
- 🏷️ Categories
- ⏳ Upcoming Payments
- 📩 Review Unmatched SMS
- 📄 Summary Report details

## 🧱 Tech stack

- **Language/UI**: Kotlin, Jetpack Compose, Material 3
- **Architecture**: ViewModel + repository-style data flow
- **Persistence**: Room
- **Background work**: WorkManager
- **Auth/Security**: Android Biometric + device credential flow
- **Widget**: Jetpack Glance App Widget
- **Services**: Firebase (Google services/auth wiring present)

## 🔐 Permissions used

```xml

<uses-permission android:name="android.permission.READ_SMS" /><uses-permission
android:name="android.permission.RECEIVE_SMS" /><uses-permission
android:name="android.permission.POST_NOTIFICATIONS" /><uses-permission
android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

SMS processing is local to the device.

## Financial data and payment reliability

Unmatched SMS corrections require a positive amount, recipient, date and direction.
Time, fees, balance and M-PESA reference may be left blank: details show **Unknown**
and CSV exports leave those cells empty. Date-only records sort at the beginning of
their day; that ordering timestamp is not a reported payment time. Spending totals
include the principal and known fees, and may understate spending when fees are missing.
SMS formats missing a payment date or amount go to review rather than using delivery
dates, due dates or zero amounts as financial facts.
Version 14 preserves existing records and returns old placeholder-based manual entries
to SMS review rather than continuing to count invented values.

Upcoming payments are **never automatically marked paid**. Foreground sync, background
SMS ingestion, parser reprocessing and manual corrections create persisted suggestions.
Existing transactions are also reconciled against pending payments. Suggestions require
matching direction, recipient evidence, an amount within 5%, and a date within seven days.
Confirm or dismiss them on Upcoming Payments; dismissed pairs stay dismissed.
Savings items require manual completion because their wallet direction is unspecified.

Completing or skipping a pending payment is atomic and repeat-safe. Monthly and yearly
recurrences retain the original scheduled day (January 31 → February 28/29 → March 31).
The next occurrence is created even if overdue; missed occurrences are not silently skipped.
Existing recurring items use their stored due date as the initial anchor.

Home, notification settings, Insights and target reminders share one budget per calendar
month. Legacy global targets migrate into the current month, with an existing monthly
spending target taking precedence. Targets do not silently carry into future months.
Settings changes are observed through a retained preference listener.

Daily/monthly notification taps open the requested report on cold or warm launch, after
app authentication when enabled. Startup preserves pending reminder work instead of
postponing it every time the app opens.

## 🧪 Testing

The project includes local unit tests and instrumentation tests, including parser
quality/performance coverage and DAO equivalence checks.

Run locally:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug :app:lintDebug
./gradlew :app:assembleDebugAndroidTest
```

`assembleDebugAndroidTest` compiles the instrumentation tests; running them with
`connectedDebugAndroidTest` requires a connected device or emulator.

CI quality workflow:

- `.github/workflows/quality-gates.yml`
- Runs unit tests, builds the debug app and instrumentation tests, and fails on lint errors.

## 🚀 Setup

1. Clone:

```bash
git clone https://github.com/JosiahAgency/PesaPulse.git
```

1. Open in Android Studio.
2. Build/run on device or emulator (minSdk 26).

## 🧩 Project layout (high level)

```text
app/src/main/java/com/example/cashia/
├── models/           # entities, workers, and viewmodels
├── daos/             # Room DAOs
├── utility/          # parser, schedulers, helpers
├── ui/               # navigation, screens, reusable components
├── widget/           # Glance widget
├── AppDatabase.kt
├── MainActivity.kt
└── PesaPulseApplication.kt
```

## ⚠️ Disclaimer

Cashia/PesaPulse is an independent app and is not affiliated with Safaricom or M-PESA.

## 📄 Licence

MIT Licence

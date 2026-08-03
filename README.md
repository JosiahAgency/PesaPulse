# Cashia (PesaPulse) 💸📊

<p align="center">
  <img src="app/src/main/pesapulse_logo-playstore.png" alt="PesaPulse logo" width="140" />
</p>

Cashia is an Android personal finance app that reads M-PESA SMS messages, parses them into structured transactions, and turns them into live insights, forecasts, and reminders.

## ✨ What the app currently does

- 📥 **SMS sync & parsing**: reads M-PESA messages and classifies transaction types.
- 🧾 **Transaction history**: browse all records, inspect details, and review unmatched SMS.
- 🏠 **Dashboard**: balance and period summaries, due-soon context, and quick financial status.
- 📆 **Calendar view**: spending and activity over time with week/day context.
- 📈 **Insights (Analytics)**: trends, category breakdowns, weekday patterns, and merchant analysis.
- 🔮 **Forecasts (Projections)**: projected balance, upcoming impact, and due-soon grouping.
- ⏰ **Upcoming payments**: full CRUD for future expenses/income/savings with recurrence and status tracking.
- 🏷️ **Category management**: manage custom categories and apply categorization controls.
- 🛡️ **Security & privacy**: optional biometric/PIN app lock and privacy-aware home screen widget state.
- 🔔 **Smart reminders**: daily digest, upcoming reminders, and monthly summary notifications via WorkManager.

## 🧭 Main navigation

Bottom navigation includes:

1. **Home**
2. **History**
3. **Calendar**
4. **Insights**
5. **Forecast**

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
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

SMS processing is local to the device.

## 🧪 Testing

The project includes local unit tests and instrumentation tests, including parser quality/performance coverage and DAO equivalence checks.

Run locally:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebugAndroidTest
```

CI quality workflow:

- `.github/workflows/quality-gates.yml`

## 🚀 Setup

1. Clone:

```bash
git clone https://github.com/JosiahAgency/Cashia.git
```

2. Open in Android Studio.
3. Build/run on device or emulator (minSdk 26).

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

## 📄 License

MIT License

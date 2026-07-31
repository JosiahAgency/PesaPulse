# PesaPulse

PesaPulse is an Android personal finance application that automatically reads and parses M-PESA SMS messages to help users track income, expenses, spending habits, and financial trends in real time.

The application transforms raw transaction SMS messages into structured financial records, providing actionable insights, projections, and analytics without requiring manual data entry.

---

## Features

### Automatic M-PESA SMS Sync
- Reads incoming M-PESA SMS messages
- Parses transactions automatically
- Detects:
  - Money sent
  - Money received
  - Paybill payments
  - Buy goods transactions
  - Airtime purchases
  - Withdrawals
  - Reversals
  - Fuliza transactions
  - Balance inquiries

### Smart Transaction Categorization
Automatically categorizes transactions into groups such as:
- Food
- Transport
- Shopping
- Bills
- Salary
- Airtime
- Loans
- Entertainment
- Savings
- Other

### Financial Insights
Provides useful analytics including:
- Daily spending trends
- Monthly summaries
- Spending distribution
- Income vs expense analysis
- Largest expense categories
- Transaction frequency analysis

### Financial Projections
Uses historical transaction patterns to:
- Estimate future spending
- Predict upcoming cash flow
- Forecast balance trends
- Identify overspending behavior

### Unmatched SMS Review System
- Detects unsupported or unknown SMS formats
- Allows manual review and classification
- Supports future parser learning improvements

### Offline First
- Local Room database storage
- Works without internet connection
- Fast local analytics and querying

---

## Tech Stack

### Android
- Kotlin
- Jetpack Compose
- Coroutines
- Android Content Resolver

### Persistence
- Room Database

### Architecture
- Repository Pattern
- Local-first data layer
- Structured parser pipeline

### SMS Processing
- Regex-based transaction extraction
- Pattern grouping
- SMS synchronization system

---

## How It Works

1. User grants SMS permission
2. App scans M-PESA inbox messages
3. SMS messages are parsed into structured transactions
4. Transactions are stored locally
5. Analytics and projections are generated from transaction history

---

## Permissions

PesaPulse requires the following permission:

```xml
<uses-permission android:name="android.permission.READ_SMS"/>
```

SMS messages are processed locally on-device and are not shared externally.

---

## Project Structure

```text
models/
├── MpesaTransactions.kt
├── UnmatchedSms.kt

utility/
├── SmsRepository.kt
├── Parser utilities

database/
├── Room entities
├── DAOs
├── Sync state management
```

---

## Future Improvements

- Machine learning transaction categorization
- Cloud backup & sync
- Budget goals
- Recurring payment detection
- Multi-account support
- Data export (CSV/PDF)
- AI-powered financial recommendations
- Fraud/anomaly detection

---

## Installation

1. Clone the repository

```bash
git clone https://github.com/yourusername/pesapulse.git
```

2. Open in Android Studio

3. Build and run on an Android device

---

## Commit/Push Quality Gates

This project now includes automated **test + performance evaluators** to catch regressions before and after code is pushed.

### Running tests locally

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebugAndroidTest
```

### 1. Local Git hooks (pre-commit and pre-push)

Hooks are stored in `.githooks/`:
- `pre-commit`: runs parser quality tests
- `pre-push`: runs unit tests, parser performance evaluator, and debug assemble

Enable them once per clone:

```bash
git config core.hooksPath .githooks
```

### 2. GitHub Actions on push/PR

Workflow: `.github/workflows/quality-gates.yml`

Runs on every push and pull request:
- `:app:testDebugUnitTest`
- `:app:assembleDebug`
- Performance evaluator test (`ParserPerformanceEvaluatorTest`)

---

## Disclaimer

PesaPulse is not affiliated with Safaricom or M-PESA.

This application processes SMS data locally for personal finance tracking purposes only.

---

## License

MIT License

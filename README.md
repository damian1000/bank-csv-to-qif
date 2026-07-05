# Bank CSV → QIF Converter

[![CI](https://github.com/damian1000/bank-csv-to-qif/actions/workflows/ci.yml/badge.svg)](https://github.com/damian1000/bank-csv-to-qif/actions/workflows/ci.yml)
[![CodeQL](https://github.com/damian1000/bank-csv-to-qif/actions/workflows/codeql.yml/badge.svg)](https://github.com/damian1000/bank-csv-to-qif/actions/workflows/codeql.yml)
[![codecov](https://codecov.io/gh/damian1000/bank-csv-to-qif/graph/badge.svg)](https://codecov.io/gh/damian1000/bank-csv-to-qif)
[![Release](https://img.shields.io/github/v/release/damian1000/bank-csv-to-qif)](https://github.com/damian1000/bank-csv-to-qif/releases)

Converts bank CSV statement exports into [QIF (Quicken Interchange Format)](https://en.wikipedia.org/wiki/Quicken_Interchange_Format), the format used by Quicken, MoneyDance, GnuCash, KMyMoney, Microsoft Money, and a long tail of legacy personal-finance tools that long predate Open Banking.

Built because every bank exports a _slightly_ different CSV layout — different column orders, different date formats, different ways of expressing money in versus money out — and every finance tool wants QIF. This bridges the gap with a per-bank parser, a canonical QIF writer, and a tiny CLI.

## Supported banks

| Bank           | CLI name       | Notes                                                                                                                                                                                                                                                     |
| -------------- | -------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Kiwibank (NZ)  | `kiwibank`     | Date `dd/MM/yyyy`, distinct Other Party (→ payee) and Particulars (→ memo) columns, separate Money In / Money Out columns. Output as `!Type:Bank`.                                                                                                        |
| Santander (UK) | `santander`    | Date `dd/MM/yyyy`, `£` amounts in quoted fields, strips `PURCHASE DOMESTIC ` / `APPLE PAY ` / `RECURRENT TRANSACTION ` / `CARD PAYMENT TO ` / `PURCHASE - INTERNATIONAL ` prefixes from the payee. Skips `INITIAL BALANCE` rows. Output as `!Type:CCard`. |
| Crypto.com     | `cryptodotcom` | ISO timestamp (`yyyy-MM-dd HH:mm:ss`), signed native-currency amount, payee is `description_currency`. Output as `!Type:CCard`.                                                                                                                           |

## Use it from the CLI

### Download a release (no build required)

Grab the latest tarball or zip from [GitHub Releases](https://github.com/damian1000/bank-csv-to-qif/releases), extract it, and run:

```bash
tar -xf bank-csv-to-qif-1.0.0.tar
./bank-csv-to-qif-1.0.0/bin/bank-csv-to-qif kiwibank statement.csv statement.qif
```

Each release ships a `SHA256SUMS.txt` next to the archives. Requires JDK 25 on `PATH`.

### Build it locally

```bash
./gradlew installDist
./build/install/bank-csv-to-qif/bin/bank-csv-to-qif kiwibank statement.csv statement.qif
```

Exit codes follow the BSD `sysexits.h` convention: `64` for bad usage, `66` for an unreadable input, `1` if no transactions were parseable (so the output file isn't created).

## Use it as a library

```kotlin
val transactions = Files.newBufferedReader(Paths.get("santander-2024-01.csv"))
    .use { SantanderReader().parse(it) }

Files.newBufferedWriter(Paths.get("santander-2024-01.qif")).use {
    QifWriter(QifType.CREDIT_CARD).write(transactions, it)
}
```

The intermediate type — `List<Transaction>` — uses `LocalDate` for dates and `BigDecimal` for amounts (signed, positive for inflows). The original ad-hoc scripts these are descended from used `Double` for amounts, which is a real bug for any reasonable definition of "bank statement"; this version doesn't.

## Adding a new bank

Implement `BankCsvReader`:

```kotlin
class MyBankReader : BankCsvReader {
    override fun parse(input: Reader): List<Transaction> {
        // Use commons-csv to parse; return a Transaction per non-header,
        // non-balance row. Return null from your row-mapper to skip rows
        // that aren't real transactions (headers, info lines, etc.).
    }
}
```

The pattern in the existing readers:

- Use `CSVFormat.DEFAULT` from commons-csv for the parse (handles quoted fields with embedded commas correctly).
- Try to parse a date from the date column; if it fails, skip the row — that's how all three readers reject the header.
- Express the amount as a signed `BigDecimal` (positive for inflows, negative for outflows). The QIF writer doesn't add any sign fixups.
- Strip bank-specific noise from the payee in the reader, not the writer. That keeps `QifWriter` agnostic to where the data came from.

## Design notes

- **Readers are stateless.** A single `KiwibankReader` instance is safe to reuse across multiple files and threads.
- **QIF format choice (`!Type:Bank` vs `!Type:CCard`)** is per-bank, not per-row. Most banks export from a single account, so this is the right granularity.
- **No streaming.** Both reader and writer materialise the full transaction list. For bank statements (typically thousands of rows max), this is fine and avoids the complexity of streaming through commons-csv's iterator semantics.
- **No automatic categorisation, splits, or cleared-status.** QIF supports those; this writer intentionally doesn't. Categorisation belongs in the finance tool that imports the output, not here.

## Stack

- Kotlin 2.3.21 (JVM target 25)
- Java 25 toolchain
- Apache Commons CSV 1.14.1 (the one runtime dependency)
- JUnit Jupiter 6.1
- Gradle 9.5.1

## License

Apache 2.0 — see [LICENSE](LICENSE).

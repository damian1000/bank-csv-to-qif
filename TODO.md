# TODO

## Next (Highest Leverage)

- **Tests for `Main.kt`.** The CLI currently has no tests, which is the biggest gap in the Codecov report. Cover: bad bank name → exit 64, unreadable input → exit 66, valid run writes the right QIF and reports the right count. Use a temp directory for input/output paths.
- **More tests around malformed input.** Each reader currently has a happy-path test; add cases for truncated rows (`row.size() < expected`), unparseable amounts, and dates in the wrong format. These are the "real CSV in the wild" failure modes.

## Bug Fixes / Hardening

- The `KiwibankReader` uses column 3 for both `payee` and `memo` because the original Java did, but Kiwibank's actual export has different columns for `Other Party` and `Particulars`. Re-check a real recent export and consider promoting one of them to memo.
- `SantanderReader.cleanPayee` only strips fixed-position prefixes; if Santander rewords boilerplate (which they have done before), the prefix list goes stale silently. Consider parameterising the prefix list via a small config file or extracting it to a public constant so users can extend it without recompiling.
- `CryptoDotComReader` payee is `"{description}_{currency}"`; the underscore is arbitrary and looks ugly in Quicken. Reconsider — maybe `"description (currency)"` or just description.
- `QifWriter` doesn't escape characters that have special meaning in QIF (a literal `^` in a memo would break the parser). Add escaping or document the limitation.

## Banks To Add

- **HSBC UK** — common UK current/credit-card export format.
- **Monzo / Starling** — modern UK challenger banks, well-documented CSV exports.
- **Wise** (formerly TransferWise) — multi-currency, has its own column layout.
- **Schwab / Fidelity** — US brokerage CSV format, would extend the use case beyond personal banking.
- **ANZ NZ, Westpac NZ** — pair with Kiwibank for NZ market coverage.

## Other Format Outputs

- **OFX writer.** Many tools accept OFX as well as QIF. Same `Transaction` model could emit either.
- **CSV passthrough writer** with a canonical column order, useful for users who don't want QIF but want their bank-specific CSV normalised.

## Distribution / Release

- Publish a release binary via `./gradlew installDist` packaged as a tarball, triggered by a release tag in GitHub Actions. Lets non-Gradle users grab a `bin/bank-csv-to-qif` directly.
- Consider publishing the library jar to Maven Central (the `io.github.damian1000` group ID was chosen precisely so this is straightforward when the time comes).
- Optional: a Homebrew tap or Scoop manifest for one-line install.

## Optional Features

- **Add `KiwibankOfxFixer`** as a separate `bank-csv-to-qif ofx-fix <file.ofx>` subcommand. The original script fixes Kiwibank's broken `<MEMO>` placement so OFX-aware tools can use the file.
- **Date-range filtering** on the CLI: `--from 2024-01-01 --to 2024-03-31`.
- **Category guessing** based on payee — a simple rules file mapping merchant patterns to categories, written into the QIF `L` field.
- **Verbose mode** (`-v`) that prints each transaction as it's parsed, useful for debugging unfamiliar exports.

## Cleanup

- The three readers share an identical `parseAmount` helper (strip quotes, `£`, commas) and `tryParseDate` pattern. Extract a small base class or extension helpers — but only if a fourth reader needs them. Premature extraction is the bigger risk right now.
- `Bank` enum lives inside `Main.kt`; might be cleaner as its own file if more entries get added.

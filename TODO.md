# TODO

## Roadmap (prioritized)

External review framing: "useful real product, publish a release". This repo is small but actually serves a real user need — that's its strength. Don't try to turn it into something it isn't.

### P1 — publish a release

The single highest-leverage move here:

- Cut a GitHub release with a tagged version (e.g. `v1.0.0`).
- Trigger `./gradlew installDist` in a GitHub Actions workflow on tag push; attach the resulting tarball as a release artifact.
- README installation section: "download the latest release, unzip, run `bin/bank-csv-to-qif kiwibank input.csv output.qif`".

Goes from "demo on GitHub" to "real product users can grab" without changing a line of source.

### P2 — one bank or one polish item

Pick one — adding a new bank is more useful than half-fixing an old one:

- Add **HSBC UK** or **Monzo / Starling** support (most common UK formats not yet covered).
- Or: add OFX writer alongside QIF, which broadens usefulness without adding a bank.

### P3 — stretch (only if there's user demand)

The "banks to add" / "other format outputs" lists below are all reasonable but speculative. Don't build them unless someone asks.

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

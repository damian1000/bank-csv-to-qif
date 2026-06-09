# TODO

- Add HSBC UK support.
- Add Monzo / Starling support.
- Add OFX writer alongside QIF.
- Re-check Kiwibank's `Other Party` vs `Particulars` columns; `KiwibankReader` currently uses column 3 for both payee and memo.
- Publish library jar to Maven Central.
- Add `KiwibankOfxFixer` as a `bank-csv-to-qif ofx-fix <file.ofx>` subcommand.
- Add Wise CSV support.
- Add Schwab / Fidelity CSV support.
- Add ANZ NZ and Westpac NZ CSV support.

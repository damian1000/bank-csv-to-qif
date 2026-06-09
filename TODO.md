# TODO

- Add HSBC UK support.
- Add Monzo / Starling support.
- Add OFX writer alongside QIF.
- Re-check Kiwibank's `Other Party` vs `Particulars` columns; `KiwibankReader` currently uses column 3 for both payee and memo.
- Parameterise `SantanderReader.cleanPayee` prefix list (config file or public constant) so it can be extended without recompiling.
- Extract a `parseAmount` / `tryParseDate` helper once a 4th reader lands.
- Publish library jar to Maven Central.
- Add a Homebrew tap or Scoop manifest.
- Add `KiwibankOfxFixer` as a `bank-csv-to-qif ofx-fix <file.ofx>` subcommand.
- Add `--from` / `--to` date-range filtering to the CLI.
- Add category guessing from payee via a rules file, written into the QIF `L` field.
- Add a `-v` verbose mode that prints each parsed transaction.
- Add Wise CSV support.
- Add Schwab / Fidelity CSV support.
- Add ANZ NZ and Westpac NZ CSV support.
- Add a CSV passthrough writer with canonical column order.

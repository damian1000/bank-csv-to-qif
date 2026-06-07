package io.github.damian1000.csv2qif

import java.io.Reader

/**
 * Parses a bank's CSV statement into a stream of [Transaction]s.
 *
 * Implementations are responsible for:
 *  - knowing the bank's column layout and date format,
 *  - stripping bank-specific noise from the payee field (e.g. "PURCHASE DOMESTIC " prefixes),
 *  - producing the correct sign on the amount (negative = outflow).
 *
 * Implementations should be stateless and safe to reuse across multiple files.
 */
interface BankCsvReader {
    fun parse(input: Reader): List<Transaction>
}

package io.github.damian1000.csv2qif.readers

import io.github.damian1000.csv2qif.BankCsvReader
import io.github.damian1000.csv2qif.Transaction
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import java.io.Reader
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Reader for Kiwibank's CSV export.
 *
 * Column layout (zero-indexed):
 *  - 2: date (dd/MM/yyyy)
 *  - 3: memo / payee (Kiwibank combines these — the same field is used for both)
 *  - 4: amount in (blank if outflow)
 *  - 5: amount out (blank if inflow)
 *
 * Lines that don't have a parseable date in column 2 are skipped silently:
 * Kiwibank exports include a few header rows before the transactions, and this
 * is the simplest robust way to skip them.
 */
class KiwibankReader : BankCsvReader {
    private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    override fun parse(input: Reader): List<Transaction> {
        val format =
            CSVFormat.DEFAULT
                .builder()
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .get()
        return format.parse(input).mapNotNull { row -> parseRow(row) }
    }

    private fun parseRow(row: CSVRecord): Transaction? {
        if (row.size() < 5) return null
        val date = tryParseDate(row.get(COL_DATE)) ?: return null
        val description = row.get(COL_DESCRIPTION)
        val moneyIn = parseAmount(row.get(COL_MONEY_IN))
        val moneyOut = if (row.size() > COL_MONEY_OUT) parseAmount(row.get(COL_MONEY_OUT)) else null
        val amount = signedAmount(moneyIn, moneyOut) ?: return null
        return Transaction(date = date, payee = description, memo = description, amount = amount)
    }

    private fun tryParseDate(raw: String): LocalDate? =
        try {
            LocalDate.parse(raw, dateFormat)
        } catch (_: Exception) {
            null
        }

    private fun parseAmount(raw: String): BigDecimal? {
        val cleaned = raw.trim()
        if (cleaned.isEmpty()) return null
        return BigDecimal(cleaned)
    }

    /**
     * Returns null for info/balance rows (no amount in either column); these
     * are skipped rather than treated as zero transactions.
     */
    private fun signedAmount(
        moneyIn: BigDecimal?,
        moneyOut: BigDecimal?,
    ): BigDecimal? =
        when {
            moneyOut != null -> moneyOut.negate()
            moneyIn != null -> moneyIn
            else -> null
        }

    companion object {
        private const val COL_DATE = 2
        private const val COL_DESCRIPTION = 3
        private const val COL_MONEY_IN = 4
        private const val COL_MONEY_OUT = 5
    }
}

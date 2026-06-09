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
 * Reader for Crypto.com's CSV export.
 *
 * Column layout (zero-indexed):
 *  - 0: timestamp (the first 10 chars are an ISO-8601 date: yyyy-MM-dd)
 *  - 1: transaction description
 *  - 2: secondary description (concatenated to col 1 to form the payee)
 *  - 7: amount, signed (already negative for outflows)
 *
 * The first row in a Crypto.com export is a header line whose description
 * column contains "Transaction Description"; it's skipped.
 */
class CryptoDotComReader : BankCsvReader {
    private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

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
        if (row.size() < 8) return null
        val description = row.get(COL_DESCRIPTION_PRIMARY)
        if (description.contains("Transaction Description")) return null
        val secondary = row.get(COL_DESCRIPTION_SECONDARY)
        val payee = if (secondary.isBlank()) description else "$description ($secondary)"
        val date = tryParseDate(row.get(COL_DATE).take(10)) ?: return null
        val amount =
            parseAmount(row.get(COL_AMOUNT))
                ?: throw IllegalArgumentException("Missing amount in row $payee")
        return Transaction(date = date, payee = payee, memo = payee, amount = amount)
    }

    private fun tryParseDate(raw: String): LocalDate? =
        try {
            LocalDate.parse(raw, dateFormat)
        } catch (_: Exception) {
            null
        }

    private fun parseAmount(raw: String): BigDecimal? {
        val cleaned =
            raw
                .replace("\"", "")
                .replace("£", "")
                .replace(",", "")
                .trim()
        if (cleaned.isEmpty()) return null
        return BigDecimal(cleaned)
    }

    companion object {
        private const val COL_DATE = 0
        private const val COL_DESCRIPTION_PRIMARY = 1
        private const val COL_DESCRIPTION_SECONDARY = 2
        private const val COL_AMOUNT = 7
    }
}

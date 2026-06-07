package io.github.damian1000.csv2qif

import java.io.Writer
import java.time.format.DateTimeFormatter

/**
 * QIF account type — written as the file's `!Type:` header. Pick the one
 * matching the kind of account whose transactions you're exporting: Quicken
 * and similar tools route transactions differently depending on this header.
 */
enum class QifType(val header: String) {
    BANK("!Type:Bank"),
    CASH("!Type:Cash"),
    CREDIT_CARD("!Type:CCard"),
    INVESTMENT("!Type:Invst"),
    OTHER_ASSET("!Type:Oth A"),
    OTHER_LIABILITY("!Type:Oth L"),
}

/**
 * Writes transactions in QIF format. QIF fields used here:
 *  - `D` date (dd/MM/yyyy)
 *  - `M` memo
 *  - `P` payee
 *  - `T` amount (negative for outflow)
 *  - `^` end of transaction
 *
 * Output ends each line with `\n`; no `\r\n` regardless of platform, matching
 * the original QIF spec.
 */
class QifWriter(private val type: QifType = QifType.CREDIT_CARD) {

    private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun write(transactions: List<Transaction>, output: Writer) {
        output.write(type.header)
        output.write("\n")
        for (txn in transactions) {
            output.write("D${dateFormat.format(txn.date)}\n")
            output.write("M${txn.memo}\n")
            output.write("P${txn.payee}\n")
            output.write("T${txn.amount.toPlainString()}\n")
            output.write("^\n")
        }
    }
}

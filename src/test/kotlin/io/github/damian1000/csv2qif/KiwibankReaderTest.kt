package io.github.damian1000.csv2qif

import io.github.damian1000.csv2qif.readers.KiwibankReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.InputStreamReader
import java.math.BigDecimal
import java.time.LocalDate

class KiwibankReaderTest {
    @Test
    fun `parses sample CSV preserving sign convention and skipping balance rows`() {
        val transactions = readFixture("kiwibank-sample.csv")
        assertEquals(4, transactions.size, "header and balance rows should be skipped")

        assertEquals(
            Transaction(
                date = LocalDate.of(2024, 1, 2),
                payee = "Grocery Store",
                memo = "Grocery Store",
                amount = BigDecimal("-45.20"),
            ),
            transactions[0],
        )
        assertEquals(
            Transaction(
                date = LocalDate.of(2024, 1, 3),
                payee = "Salary Deposit",
                memo = "Salary Deposit",
                amount = BigDecimal("3500.00"),
            ),
            transactions[1],
        )
        assertEquals(BigDecimal("-4.50"), transactions[2].amount)
        assertEquals(BigDecimal("12.50"), transactions[3].amount)
    }

    private fun readFixture(name: String): List<Transaction> {
        val stream = checkNotNull(javaClass.classLoader.getResourceAsStream(name)) { "missing $name" }
        return InputStreamReader(stream).use { KiwibankReader().parse(it) }
    }
}

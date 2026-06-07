package io.github.damian1000.csv2qif

import io.github.damian1000.csv2qif.readers.CryptoDotComReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.InputStreamReader
import java.math.BigDecimal
import java.time.LocalDate

class CryptoDotComReaderTest {

    @Test
    fun `parses sample CSV using native GBP amount, takes ISO date prefix from timestamp`() {
        val transactions = readFixture("cryptodotcom-sample.csv")
        assertEquals(4, transactions.size, "header row should be skipped")

        assertEquals(
            Transaction(
                date = LocalDate.of(2024, 1, 2),
                payee = "Crypto Earn Deposit_USDC",
                memo = "Crypto Earn Deposit_USDC",
                amount = BigDecimal("-78.50"),
            ),
            transactions[0],
        )

        assertEquals(BigDecimal("0.04"), transactions[1].amount, "positive amounts pass through unchanged")
        assertEquals(BigDecimal("0.03"), transactions[2].amount)
        assertEquals(BigDecimal("78.50"), transactions[3].amount)
    }

    private fun readFixture(name: String): List<Transaction> {
        val stream = checkNotNull(javaClass.classLoader.getResourceAsStream(name)) { "missing $name" }
        return InputStreamReader(stream).use { CryptoDotComReader().parse(it) }
    }
}

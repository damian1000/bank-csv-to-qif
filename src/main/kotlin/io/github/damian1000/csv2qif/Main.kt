package io.github.damian1000.csv2qif

import io.github.damian1000.csv2qif.readers.CryptoDotComReader
import io.github.damian1000.csv2qif.readers.KiwibankReader
import io.github.damian1000.csv2qif.readers.SantanderReader
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size != 3) {
        printUsage()
        exitProcess(64)
    }
    val (bankName, inputPath, outputPath) = args
    val bank = Bank.byName(bankName)
    if (bank == null) {
        System.err.println("Unknown bank: '$bankName'.")
        printUsage()
        exitProcess(64)
    }

    val input = Paths.get(inputPath)
    if (!Files.isReadable(input)) {
        System.err.println("Cannot read input file: $inputPath")
        exitProcess(66)
    }

    val transactions = Files.newBufferedReader(input).use { bank.reader().parse(it) }
    if (transactions.isEmpty()) {
        System.err.println("No transactions parsed from $inputPath; nothing written.")
        exitProcess(1)
    }

    val output = Paths.get(outputPath)
    Files.newBufferedWriter(output).use { QifWriter(bank.qifType).write(transactions, it) }
    println("Wrote ${transactions.size} transactions to $outputPath")
}

private fun printUsage() {
    System.err.println("Usage: bank-csv-to-qif <bank> <input.csv> <output.qif>")
    System.err.println("Banks: ${Bank.entries.joinToString(", ") { it.cliName }}")
}

private enum class Bank(val cliName: String, val qifType: QifType) {
    KIWIBANK("kiwibank", QifType.BANK),
    SANTANDER("santander", QifType.CREDIT_CARD),
    CRYPTO_DOT_COM("cryptodotcom", QifType.CREDIT_CARD);

    fun reader(): BankCsvReader = when (this) {
        KIWIBANK -> KiwibankReader()
        SANTANDER -> SantanderReader()
        CRYPTO_DOT_COM -> CryptoDotComReader()
    }

    companion object {
        fun byName(name: String): Bank? = entries.firstOrNull { it.cliName.equals(name, ignoreCase = true) }
    }
}

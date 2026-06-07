package io.github.damian1000.csv2qif

import io.github.damian1000.csv2qif.readers.CryptoDotComReader
import io.github.damian1000.csv2qif.readers.KiwibankReader
import io.github.damian1000.csv2qif.readers.SantanderReader
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    exitProcess(run(args, System.out, System.err))
}

/**
 * Pure-function entry point: returns the exit code rather than calling
 * `exitProcess`, so it can be exercised from tests without killing the JVM.
 * Exit codes follow the BSD `sysexits.h` convention: 64 = usage error,
 * 66 = input file missing/unreadable, 1 = ran successfully but produced no
 * transactions, 0 = success.
 */
fun run(args: Array<String>, out: PrintStream, err: PrintStream): Int {
    if (args.size != 3) {
        printUsage(err)
        return 64
    }
    val (bankName, inputPath, outputPath) = args
    val bank = Bank.byName(bankName)
    if (bank == null) {
        err.println("Unknown bank: '$bankName'.")
        printUsage(err)
        return 64
    }

    val input = Paths.get(inputPath)
    if (!Files.isReadable(input)) {
        err.println("Cannot read input file: $inputPath")
        return 66
    }

    val transactions = Files.newBufferedReader(input).use { bank.reader().parse(it) }
    if (transactions.isEmpty()) {
        err.println("No transactions parsed from $inputPath; nothing written.")
        return 1
    }

    val output = Paths.get(outputPath)
    Files.newBufferedWriter(output).use { QifWriter(bank.qifType).write(transactions, it) }
    out.println("Wrote ${transactions.size} transactions to $outputPath")
    return 0
}

private fun printUsage(err: PrintStream) {
    err.println("Usage: bank-csv-to-qif <bank> <input.csv> <output.qif>")
    err.println("Banks: ${Bank.entries.joinToString(", ") { it.cliName }}")
}

internal enum class Bank(val cliName: String, val qifType: QifType) {
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

package io.github.damian1000.csv2qif

import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeParseException
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
fun run(
    args: Array<String>,
    out: PrintStream,
    err: PrintStream,
): Int {
    val parsed =
        parseArgs(args) ?: run {
            printUsage(err)
            return 64
        }

    val bank = Bank.byName(parsed.bankName)
    if (bank == null) {
        err.println("Unknown bank: '${parsed.bankName}'.")
        printUsage(err)
        return 64
    }

    val input = Paths.get(parsed.inputPath)
    if (!Files.isReadable(input)) {
        err.println("Cannot read input file: ${parsed.inputPath}")
        return 66
    }

    val allTransactions = Files.newBufferedReader(input).use { bank.reader().parse(it) }
    val transactions =
        allTransactions.filter { txn ->
            (parsed.from == null || !txn.date.isBefore(parsed.from)) &&
                (parsed.to == null || !txn.date.isAfter(parsed.to))
        }

    if (parsed.verbose) {
        val skipped = allTransactions.size - transactions.size
        transactions.forEach { err.println("  parsed: ${it.date} ${it.amount.toPlainString()} | ${it.payee}") }
        if (skipped > 0) err.println("  ($skipped transaction(s) outside --from/--to range)")
    }

    if (transactions.isEmpty()) {
        err.println("No transactions parsed from ${parsed.inputPath}; nothing written.")
        return 1
    }

    val output = Paths.get(parsed.outputPath)
    Files.newBufferedWriter(output).use { QifWriter(bank.qifType).write(transactions, it) }
    out.println("Wrote ${transactions.size} transactions to ${parsed.outputPath}")
    return 0
}

internal data class ParsedArgs(
    val bankName: String,
    val inputPath: String,
    val outputPath: String,
    val from: LocalDate?,
    val to: LocalDate?,
    val verbose: Boolean,
)

internal fun parseArgs(args: Array<String>): ParsedArgs? {
    var from: LocalDate? = null
    var to: LocalDate? = null
    var verbose = false
    val positional = mutableListOf<String>()

    var i = 0
    while (i < args.size) {
        when (val arg = args[i]) {
            "-v", "--verbose" -> verbose = true
            "--from" -> {
                if (i + 1 >= args.size) return null
                from = parseIsoDate(args[i + 1]) ?: return null
                i++
            }
            "--to" -> {
                if (i + 1 >= args.size) return null
                to = parseIsoDate(args[i + 1]) ?: return null
                i++
            }
            else -> positional.add(arg)
        }
        i++
    }

    if (positional.size != 3) return null
    return ParsedArgs(positional[0], positional[1], positional[2], from, to, verbose)
}

private fun parseIsoDate(s: String): LocalDate? =
    try {
        LocalDate.parse(s)
    } catch (_: DateTimeParseException) {
        null
    }

private fun printUsage(err: PrintStream) {
    err.println("Usage: bank-csv-to-qif [options] <bank> <input.csv> <output.qif>")
    err.println("Options:")
    err.println("  --from YYYY-MM-DD   only include transactions on or after this date")
    err.println("  --to YYYY-MM-DD     only include transactions on or before this date")
    err.println("  -v, --verbose       print each parsed transaction (to stderr)")
    err.println("Banks: ${Bank.entries.joinToString(", ") { it.cliName }}")
}

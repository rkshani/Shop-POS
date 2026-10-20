package com.dukan.pos.core.export

/** RFC-4180 quoting. Shop names contain commas; Urdu names contain everything. */
fun csvRow(values: List<String?>): String =
    values.joinToString(",") { raw ->
        val value = raw.orEmpty()
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

fun buildCsv(header: List<String>, rows: List<List<String?>>): String =
    buildString {
        // Excel on Windows opens UTF-8 CSV as mojibake without a BOM, and the
        // shopkeeper's accountant is on Excel.
        append('\uFEFF')
        appendLine(csvRow(header))
        rows.forEach { appendLine(csvRow(it)) }
    }

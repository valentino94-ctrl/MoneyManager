package com.v94studio.moneymanager.data

internal fun csvRow(vararg values: Any?): String {
    return values.joinToString(separator = ",") { value ->
        val text = value?.toString().orEmpty()
        if (text.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"${text.replace("\"", "\"\"")}\""
        } else {
            text
        }
    }
}

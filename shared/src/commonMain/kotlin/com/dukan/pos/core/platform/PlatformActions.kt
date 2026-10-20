package com.dukan.pos.core.platform

/**
 * The three things this app needs from the outside world: write a file the
 * shopkeeper can keep, read one back, and open WhatsApp.
 */
interface PlatformActions {
    /** Hands [content] to the platform's share/save sheet. Returns false if the user cancelled. */
    suspend fun saveTextFile(fileName: String, mimeType: String, content: String): Boolean

    /** Opens the platform file picker and returns the file's text, or null if cancelled. */
    suspend fun readTextFile(): String?

    /** Opens a URL (used for wa.me deep links). Returns false when nothing can handle it. */
    fun openUrl(url: String): Boolean
}

/**
 * wa.me needs a bare international number. Pakistani numbers get typed as
 * 03001234567, +923001234567 or 0092… — all three must end up the same.
 */
fun normalizePhoneForWhatsApp(raw: String, defaultCountryCode: String = "92"): String? {
    val digits = raw.filter { it.isDigit() }
    if (digits.length < 7) return null
    return when {
        digits.startsWith("00") -> digits.drop(2)
        digits.startsWith(defaultCountryCode) && digits.length >= 11 -> digits
        digits.startsWith("0") -> defaultCountryCode + digits.drop(1)
        else -> defaultCountryCode + digits
    }
}

fun whatsAppUrl(phone: String, message: String): String =
    "https://wa.me/$phone?text=${message.urlEncode()}"

private fun String.urlEncode(): String = buildString {
    for (byte in this@urlEncode.encodeToByteArray()) {
        val value = byte.toInt() and 0xFF
        val char = value.toChar()
        if (char.isLetterOrDigit() && value < 128 || char in "-_.~") {
            append(char)
        } else {
            append('%')
            append(value.toString(16).uppercase().padStart(2, '0'))
        }
    }
}

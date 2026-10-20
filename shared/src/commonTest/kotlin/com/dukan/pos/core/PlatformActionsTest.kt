package com.dukan.pos.core

import com.dukan.pos.core.export.buildCsv
import com.dukan.pos.core.export.csvRow
import com.dukan.pos.core.i18n.Strings
import com.dukan.pos.core.i18n.fmt
import com.dukan.pos.core.platform.normalizePhoneForWhatsApp
import com.dukan.pos.core.platform.whatsAppUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhoneNormalisationTest {

    @Test
    fun all_the_ways_a_pakistani_number_gets_typed_normalise_to_one_form() {
        val expected = "923001234567"
        assertEquals(expected, normalizePhoneForWhatsApp("03001234567"))
        assertEquals(expected, normalizePhoneForWhatsApp("+92 300 1234567"))
        assertEquals(expected, normalizePhoneForWhatsApp("0092-300-1234567"))
        assertEquals(expected, normalizePhoneForWhatsApp("923001234567"))
    }

    @Test
    fun junk_input_yields_no_number() {
        assertNull(normalizePhoneForWhatsApp(""))
        assertNull(normalizePhoneForWhatsApp("abc"))
        assertNull(normalizePhoneForWhatsApp("123"))
    }

    @Test
    fun urdu_reminder_text_survives_url_encoding() {
        val url = whatsAppUrl("923001234567", "السلام علیکم، باقی: Rs 4,250")
        assertTrue(url.startsWith("https://wa.me/923001234567?text="))
        assertTrue(url.none { it == ' ' })
        assertTrue(url.contains("%"))
    }
}

class CsvTest {

    @Test
    fun commas_quotes_and_newlines_are_escaped() {
        assertEquals("\"Chawal, 5kg\"", csvRow(listOf("Chawal, 5kg")))
        assertEquals("\"He said \"\"cheap\"\"\"", csvRow(listOf("He said \"cheap\"")))
        assertEquals("plain", csvRow(listOf("plain")))
        assertEquals("a,,b", csvRow(listOf("a", null, "b")))
    }

    @Test
    fun csv_starts_with_a_bom_so_excel_reads_urdu() {
        val csv = buildCsv(listOf("Name"), listOf(listOf("چاول")))
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("چاول"))
    }
}

class StringsTest {

    @Test
    fun urdu_overrides_do_not_drop_any_key() {
        // The data-class default means a missing Urdu key falls back to English
        // rather than crashing — this asserts the important ones are translated.
        assertTrue(Strings.Urdu.navKhata != Strings.English.navKhata)
        assertTrue(Strings.Urdu.exportBackup != Strings.English.exportBackup)
        assertTrue(Strings.Urdu.returnItems != Strings.English.returnItems)
    }

    @Test
    fun positional_arguments_fill_in_order() {
        assertEquals(
            "Hello Ali, you owe Rs 500",
            "Hello %1\$s, you owe %2\$s".fmt("Ali", "Rs 500"),
        )
        assertEquals("3 sales", "%s sales".fmt("3"))
    }
}

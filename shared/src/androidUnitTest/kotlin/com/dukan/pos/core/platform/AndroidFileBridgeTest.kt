package com.dukan.pos.core.platform

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidFileBridgeTest {

    private val results = mutableListOf<String>()

    private fun attach() {
        AndroidFileBridge.createDocument = { _, _ -> }
        AndroidFileBridge.openDocument = {}
    }

    @AfterTest
    fun tearDown() {
        AndroidFileBridge.detach(isChangingConfigurations = false)
        results.clear()
    }

    @Test
    fun rotationKeepsTheWaitingCallbackForTheNewActivity() {
        attach()
        AndroidFileBridge.requestCreate("backup.json", "application/json") { results += "delivered:$it" }

        AndroidFileBridge.detach(isChangingConfigurations = true) // old Activity destroyed by rotation
        assertTrue(results.isEmpty())

        attach() // recreated Activity registers again and receives the result
        AndroidFileBridge.deliverCreateResult(null)
        assertEquals(listOf("delivered:null"), results)
    }

    @Test
    fun finishingTheActivityCancelsSoNothingWaitsForever() {
        attach()
        AndroidFileBridge.requestOpen { results += "open:$it" }

        AndroidFileBridge.detach(isChangingConfigurations = false)

        assertEquals(listOf("open:null"), results)
    }

    @Test
    fun aNewRequestReleasesTheOneItReplaces() {
        attach()
        AndroidFileBridge.requestCreate("a.json", "application/json") { results += "first:$it" }
        AndroidFileBridge.requestCreate("b.json", "application/json") { results += "second:$it" }

        assertEquals(listOf("first:null"), results)
    }

    @Test
    fun requestWithoutAnActivityIsRefused() {
        assertFalse(AndroidFileBridge.requestCreate("a.json", "application/json") { results += "x" })
        assertFalse(AndroidFileBridge.requestOpen { results += "x" })
        assertTrue(results.isEmpty())
    }

    @Test
    fun resultIsDeliveredOnlyOnce() {
        attach()
        AndroidFileBridge.requestOpen { results += "open:$it" }

        AndroidFileBridge.deliverOpenResult(null)
        AndroidFileBridge.deliverOpenResult(null)

        assertEquals(1, results.size)
    }
}

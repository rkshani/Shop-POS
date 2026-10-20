package com.dukan.pos.domain

import com.dukan.pos.data.local.dao.DatabaseSnapshot
import com.dukan.pos.data.local.entity.*
import com.dukan.pos.data.repository.validateRestoreSnapshot
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RestoreValidationTest {
    private val settings = ShopSettingsEntity(
        shopName = "Test", ownerName = "Owner", phone = "", address = "",
        currencySymbol = "Rs", language = "en", theme = "SYSTEM",
        allowNegativeStock = false, receiptFooter = "", invoicePrefix = "INV",
    )

    @Test fun emptyDatabaseWithSettingsIsValid() {
        validateRestoreSnapshot(DatabaseSnapshot(settings = listOf(settings)))
    }

    @Test fun missingSettingsRejected() {
        assertFailsWith<IllegalArgumentException> { validateRestoreSnapshot(DatabaseSnapshot()) }
    }

    @Test fun duplicateCustomerIdsRejected() {
        val customer = CustomerEntity(id = 10, name = "A", createdAtMillis = 0)
        assertFailsWith<IllegalArgumentException> {
            validateRestoreSnapshot(DatabaseSnapshot(settings = listOf(settings), customers = listOf(customer, customer)))
        }
    }

    @Test fun negativeCustomerBalanceRejected() {
        val customer = CustomerEntity(id = 10, name = "A", balancePaisa = -1, createdAtMillis = 0)
        assertFailsWith<IllegalArgumentException> {
            validateRestoreSnapshot(DatabaseSnapshot(settings = listOf(settings), customers = listOf(customer)))
        }
    }

    @Test fun orphanSaleItemRejected() {
        val item = SaleItemEntity(id = 1, saleId = 999, productId = 999,
            productName = "Missing", unit = "pc", qty = 1.0, unitPricePaisa = 100,
            unitCostPaisa = 50)
        assertFailsWith<IllegalArgumentException> {
            validateRestoreSnapshot(DatabaseSnapshot(settings = listOf(settings), saleItems = listOf(item)))
        }
    }
}

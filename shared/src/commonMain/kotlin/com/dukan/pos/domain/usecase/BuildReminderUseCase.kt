package com.dukan.pos.domain.usecase

import com.dukan.pos.core.platform.PlatformActions
import com.dukan.pos.core.platform.normalizePhoneForWhatsApp
import com.dukan.pos.core.platform.whatsAppUrl
import com.dukan.pos.domain.model.Customer
import com.dukan.pos.domain.repository.SettingsRepository

/**
 * The manual job every kiryana owner already does on WhatsApp each week,
 * turned into one tap. The message text comes from the UI so it is translated;
 * this use case only handles the number and the deep link.
 */
class SendUdhaarReminderUseCase(
    private val actions: PlatformActions,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(customer: Customer, message: String): Result {
        val phone = customer.phone?.let { normalizePhoneForWhatsApp(it) }
            ?: return Result.NoPhone

        settingsRepository.get() // ensures settings exist before the shop name is used upstream
        return if (actions.openUrl(whatsAppUrl(phone, message))) Result.Opened else Result.NoApp
    }

    enum class Result { Opened, NoPhone, NoApp }
}

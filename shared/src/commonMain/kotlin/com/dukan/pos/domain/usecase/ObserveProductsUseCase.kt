package com.dukan.pos.domain.usecase

import com.dukan.pos.domain.model.Product
import com.dukan.pos.domain.repository.ProductRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlin.time.Duration.Companion.milliseconds

class ObserveProductsUseCase(
    private val repository: ProductRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
    operator fun invoke(
        query: Flow<String>,
        categoryId: Flow<Long?>,
    ): Flow<List<Product>> =
        combine(query.debounce(200.milliseconds), categoryId) { q, c -> q to c }
            .flatMapLatest { (q, c) -> repository.observeProducts(q, c) }
}

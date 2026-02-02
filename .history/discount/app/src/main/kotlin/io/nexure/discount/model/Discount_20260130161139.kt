package io.nexure.discount.model

import kotlinx.serialization.Serializable

/**
 * Discount data class representing a discount applied to a product
 * @param discountId Unique identifier for this discount (used for idempotency)
 * @param percent Discount percentage (0-100, exclusive of 0)
 */
@Serializable
data class Discount(
    val discountId: String,
    val percent: Double
) {
    init {
        require(percent > 0 && percent <= 100) {
            "Discount percent must be between 0 (exclusive) and 100 (inclusive), got: $percent"
        }
    }
}

/**
 * Request model for applying a discount to a product
 */
@Serializable
data class ApplyDiscountRequest(
    val discountId: String,
    val percent: Double
)
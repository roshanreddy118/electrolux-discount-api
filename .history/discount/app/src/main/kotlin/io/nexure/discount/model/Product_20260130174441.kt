package io.nexure.discount.model

import kotlinx.serialization.Serializable

/**
 * Product data class representing a product in our catalog
 * @param id Unique identifier for the product
 * @param name Human-readable product name
 * @param basePrice Price before any discounts or VAT are applied
 * @param country Country where this product is sold (affects VAT calculation)
 * @param discounts List of applied discounts to this product
 */
@Serializable
data class Product(
    val id: String,
    val name: String,
    val basePrice: Double,
    val country: String,
    val discounts: List<Discount> = emptyList()
)

/**
 * ProductResponse includes the calculated final price for API responses
 * This separates the stored data model from the API response model
 */
@Serializable
data class ProductResponse(
    val id: String,
    val name: String,
    val basePrice: Double,
    val country: String,
    val discounts: List<Discount>,
    val finalPrice: Double
)

/**
 * Response model for discount application
 */
@Serializable
data class ApplyDiscountResponse(
    val message: String,
    val product: ProductResponse
)
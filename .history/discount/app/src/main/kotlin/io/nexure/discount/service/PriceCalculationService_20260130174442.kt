package io.nexure.discount.service

import io.nexure.discount.model.Discount
import io.nexure.discount.model.Product
import io.nexure.discount.model.ProductResponse

/**
 * Service for calculating final product prices including discounts and VAT
 * Implements the formula: finalPrice = basePrice × (1 - totalDiscount%) × (1 + VAT%)
 */
object PriceCalculationService {
    
    /**
     * Calculate the final price for a product including all discounts and VAT
     * @param product The product to calculate price for
     * @return Final price after applying discounts and VAT
     */
    fun calculateFinalPrice(product: Product): Double {
        val vatRate = VatService.getVatRate(product.country)
        val totalDiscountPercent = calculateTotalDiscountPercent(product.discounts)
        
        // Formula: finalPrice = basePrice × (1 - totalDiscount%) × (1 + VAT%)
        return product.basePrice * (1.0 - totalDiscountPercent / 100.0) * (1.0 + vatRate)
    }
    
    /**
     * Convert a Product to ProductResponse with calculated final price
     * This is used for API responses
     * @param product The product to convert
     * @return ProductResponse with final price calculated
     */
    fun toProductResponse(product: Product): ProductResponse {
        val finalPrice = calculateFinalPrice(product)
        return ProductResponse(
            id = product.id,
            name = product.name,
            basePrice = product.basePrice,
            country = product.country,
            discounts = product.discounts,
            finalPrice = finalPrice
        )
    }
    
    /**
     * Calculate total discount percentage from a list of discounts
     * Uses compound discount calculation: 1 - (1-d1/100) * (1-d2/100) * ... * (1-dn/100)
     * @param discounts List of discounts to calculate total from
     * @return Total discount percentage (0-100)
     */
    private fun calculateTotalDiscountPercent(discounts: List<Discount>): Double {
        if (discounts.isEmpty()) return 0.0
        
        // Calculate compound discount: multiply all (1 - discount%) factors
        val remainingPriceFactor = discounts.fold(1.0) { acc, discount ->
            acc * (1.0 - discount.percent / 100.0)
        }
        
        // Total discount = 1 - remaining price factor
        return (1.0 - remainingPriceFactor) * 100.0
    }
}
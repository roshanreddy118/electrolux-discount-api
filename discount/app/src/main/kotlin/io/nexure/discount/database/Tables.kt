package io.nexure.discount.database

import org.jetbrains.exposed.sql.Table

/**
 * Products table definition using Exposed ORM
 * Stores the core product information
 */
object Products : Table("products") {
    val id = varchar("id", 50) // Primary key
    val name = varchar("name", 200) // Product name
    val basePrice = decimal("base_price", 10, 2) // Price with 2 decimal places
    val country = varchar("country", 50) // Country for VAT calculation
    
    override val primaryKey = PrimaryKey(id)
}

/**
 * Product discounts table definition using Exposed ORM
 * Stores individual discounts applied to products
 * Uses composite unique constraint to prevent duplicate discount applications
 */
object ProductDiscounts : Table("product_discounts") {
    val id = long("id").autoIncrement() // Auto-generated primary key
    val productId = varchar("product_id", 50) references Products.id // Foreign key to products
    val discountId = varchar("discount_id", 50) // Discount identifier for idempotency
    val percent = decimal("percent", 5, 2) // Discount percentage with 2 decimal places
    
    override val primaryKey = PrimaryKey(id)
    
    // Unique constraint to ensure same discount cannot be applied twice to same product
    // This is our main concurrency control mechanism at database level
    init {
        uniqueIndex(productId, discountId)
    }
}
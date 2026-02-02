package io.nexure.discount.repository

import io.nexure.discount.database.ProductDiscounts
import io.nexure.discount.database.Products
import io.nexure.discount.model.Discount
import io.nexure.discount.model.Product
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal

/**
 * Repository for managing product data with PostgreSQL backend
 * Implements concurrency-safe operations for discount application
 */
class ProductRepository {
    
    /**
     * Find all products for a specific country
     * @param country Country to filter products by
     * @return List of products in the specified country with their discounts
     */
    fun findByCountry(country: String): List<Product> = transaction {
        // Join products with their discounts in a single query for efficiency
        val productDiscountPairs = (Products leftJoin ProductDiscounts)
            .selectAll()
            .where { Products.country eq country }
            .orderBy(Products.id)
        
        // Group results by product to build the discount lists
        val productsWithDiscounts = productDiscountPairs
            .groupBy { it[Products.id] }
            .map { (productId, rows) ->
                val firstRow = rows.first()
                val discounts = rows.mapNotNull { row ->
                    row.getOrNull(ProductDiscounts.discountId)?.let { discountId ->
                        Discount(
                            discountId = discountId,
                            percent = row[ProductDiscounts.percent].toDouble()
                        )
                    }
                }
                
                Product(
                    id = productId,
                    name = firstRow[Products.name],
                    basePrice = firstRow[Products.basePrice].toDouble(),
                    country = firstRow[Products.country],
                    discounts = discounts
                )
            }
        
        productsWithDiscounts
    }
    
    /**
     * Find a single product by its ID
     * @param productId The product ID to search for
     * @return Product if found, null otherwise
     */
    fun findById(productId: String): Product? = transaction {
        val productDiscountPairs = (Products leftJoin ProductDiscounts)
            .selectAll()
            .where { Products.id eq productId }
        
        val rows = productDiscountPairs.toList()
        if (rows.isEmpty()) return@transaction null
        
        val firstRow = rows.first()
        val discounts = rows.mapNotNull { row ->
            row.getOrNull(ProductDiscounts.discountId)?.let { discountId ->
                Discount(
                    discountId = discountId,
                    percent = row[ProductDiscounts.percent].toDouble()
                )
            }
        }
        
        Product(
            id = firstRow[Products.id],
            name = firstRow[Products.name],
            basePrice = firstRow[Products.basePrice].toDouble(),
            country = firstRow[Products.country],
            discounts = discounts
        )
    }
    
    /**
     * Apply a discount to a product with idempotency and concurrency safety
     * Uses database unique constraint to prevent duplicate discount applications
     * @param productId The product to apply discount to
     * @param discount The discount to apply
     * @return true if discount was applied, false if it already existed
     * @throws IllegalArgumentException if product doesn't exist
     * @throws RuntimeException for other database errors
     */
    fun applyDiscount(productId: String, discount: Discount): Boolean = transaction {
        // First verify the product exists
        val productExists = Products.selectAll().where { Products.id eq productId }.count() > 0
        if (!productExists) {
            throw IllegalArgumentException("Product with ID '$productId' not found")
        }
        
        try {
            // Attempt to insert the discount
            // The unique constraint on (product_id, discount_id) will prevent duplicates
            ProductDiscounts.insert {
                it[this.productId] = productId
                it[this.discountId] = discount.discountId
                it[this.percent] = BigDecimal.valueOf(discount.percent)
            }
            true // Successfully inserted new discount
        } catch (e: ExposedSQLException) {
            // Check if this is a unique constraint violation (discount already exists)
            // Support both PostgreSQL and H2 error messages
            val errorMessage = e.message?.lowercase() ?: ""
            if (errorMessage.contains("duplicate key value violates unique constraint") ||
                errorMessage.contains("unique constraint") ||
                errorMessage.contains("unique index or primary key violation")) {
                false // Discount already exists - idempotent behavior
            } else {
                throw RuntimeException("Database error while applying discount: ${e.message}", e)
            }
        }
    }
    
    /**
     * Create a new product in the database
     * @param product The product to create
     */
    fun create(product: Product): Unit = transaction {
        Products.insert {
            it[id] = product.id
            it[name] = product.name
            it[basePrice] = BigDecimal.valueOf(product.basePrice)
            it[country] = product.country
        }
        
        // Insert any existing discounts
        product.discounts.forEach { discount ->
            ProductDiscounts.insert {
                it[productId] = product.id
                it[discountId] = discount.discountId
                it[percent] = BigDecimal.valueOf(discount.percent)
            }
        }
    }
    
    /**
     * Get all products (for testing/admin purposes)
     * @return List of all products with their discounts
     */
    fun findAll(): List<Product> = transaction {
        val productDiscountPairs = (Products leftJoin ProductDiscounts)
            .selectAll()
            .orderBy(Products.id)
        
        val productsWithDiscounts = productDiscountPairs
            .groupBy { it[Products.id] }
            .map { (productId, rows) ->
                val firstRow = rows.first()
                val discounts = rows.mapNotNull { row ->
                    row.getOrNull(ProductDiscounts.discountId)?.let { discountId ->
                        Discount(
                            discountId = discountId,
                            percent = row[ProductDiscounts.percent].toDouble()
                        )
                    }
                }
                
                Product(
                    id = productId,
                    name = firstRow[Products.name],
                    basePrice = firstRow[Products.basePrice].toDouble(),
                    country = firstRow[Products.country],
                    discounts = discounts
                )
            }
        
        productsWithDiscounts
    }
}
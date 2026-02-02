package io.nexure.discount.service

import io.nexure.discount.model.Product
import io.nexure.discount.repository.ProductRepository
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Service for seeding initial data into the database
 * Creates sample products for testing the API
 */
object DataSeedingService {
    
    /**
     * Initialize the database with sample products for testing
     * This method is safe to call multiple times (won't create duplicates)
     */
    fun seedDatabase(productRepository: ProductRepository) {
        try {
            transaction {
                // Check if data already exists to avoid duplicates
                val existingProducts = productRepository.findAll()
                if (existingProducts.isNotEmpty()) {
                    println("Database already contains ${existingProducts.size} products. Skipping seed data.")
                    return@transaction
                }
                
                // Create sample products as shown in README examples
                val sampleProducts = listOf(
                    // Sweden products (25% VAT)
                    Product(
                        id = "laptop-se",
                        name = "Gaming Laptop", 
                        basePrice = 1000.0,
                        country = "Sweden"
                    ),
                    Product(
                        id = "phone-se",
                        name = "Smartphone",
                        basePrice = 800.0, 
                        country = "Sweden"
                    ),
                    Product(
                        id = "headphones-se",
                        name = "Wireless Headphones",
                        basePrice = 200.0,
                        country = "Sweden"
                    ),
                    
                    // Germany products (19% VAT)
                    Product(
                        id = "laptop-de", 
                        name = "Gaming Laptop",
                        basePrice = 1200.0,
                        country = "Germany"
                    ),
                    Product(
                        id = "phone-de",
                        name = "Smartphone", 
                        basePrice = 900.0,
                        country = "Germany"
                    ),
                    Product(
                        id = "tablet-de",
                        name = "Tablet Pro",
                        basePrice = 600.0,
                        country = "Germany"
                    ),
                    
                    // France products (20% VAT)
                    Product(
                        id = "laptop-fr",
                        name = "Gaming Laptop",
                        basePrice = 1100.0,
                        country = "France" 
                    ),
                    Product(
                        id = "phone-fr",
                        name = "Smartphone",
                        basePrice = 850.0,
                        country = "France"
                    ),
                    Product(
                        id = "watch-fr",
                        name = "Smart Watch",
                        basePrice = 300.0,
                        country = "France"
                    )
                )
                
                // Insert sample products
                sampleProducts.forEach { product ->
                    try {
                        productRepository.create(product)
                        println("Created sample product: ${product.id} - ${product.name} (${product.country})")
                    } catch (e: Exception) {
                        println("Failed to create product ${product.id}: ${e.message}")
                    }
                }
                
                println("✅ Database seeded with ${sampleProducts.size} sample products")
                
                // Print summary by country for verification
                val productsByCountry = sampleProducts.groupBy { it.country }
                productsByCountry.forEach { (country, products) ->
                    val vatRate = VatService.getVatRate(country)
                    println("📍 $country (${(vatRate * 100).toInt()}% VAT): ${products.size} products")
                }
            }
        } catch (e: Exception) {
            println("❌ Failed to seed database: ${e.message}")
            e.printStackTrace()
        }
    }
}
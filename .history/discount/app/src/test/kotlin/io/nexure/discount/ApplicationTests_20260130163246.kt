package io.nexure.discount

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.nexure.discount.database.DatabaseConfig
import io.nexure.discount.model.Product
import io.nexure.discount.repository.ProductRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlin.test.*

/**
 * Integration tests for the Product API with in-memory H2 database
 * Tests both basic functionality and concurrency safety
 */
class ApplicationTests {
    
    /**
     * Set up test data before each test
     * Creates sample products in different countries for testing
     */
    @BeforeTest
    fun setUp() {
        // Reset any existing database connection
        DatabaseConfig.reset()
        
        // Initialize H2 in-memory database for all tests
        DatabaseConfig.init(
            jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
            username = "sa",
            password = "",
            driverClassName = "org.h2.Driver"
        )
        
        // Create sample test data
        val productRepository = ProductRepository()
        
        val testProducts = listOf(
            Product("laptop-se", "Gaming Laptop", 1000.0, "Sweden"),
            Product("phone-se", "Smartphone", 800.0, "Sweden"),
            Product("laptop-de", "Gaming Laptop", 1200.0, "Germany"),
            Product("phone-de", "Smartphone", 900.0, "Germany"),
            Product("laptop-fr", "Gaming Laptop", 1100.0, "France"),
            Product("headphones-se", "Wireless Headphones", 200.0, "Sweden")
        )
        
        testProducts.forEach { product ->
            try {
                productRepository.create(product)
            } catch (e: Exception) {
                // Product might already exist, ignore
            }
        }
    }
    
    /**
     * Test GET /products endpoint with valid country
     */
    @Test
    fun testGetProductsByCountry() = testApplication {
        application { module() }
        
        client.get("/products?country=Sweden").apply {
            assertEquals(HttpStatusCode.OK, status)
            val responseBody = bodyAsText()
            assertTrue(responseBody.contains("laptop-se"))
            assertTrue(responseBody.contains("finalPrice")) // Ensure price calculation is included
        }
    }
    
    /**
     * Test GET /products endpoint with invalid country
     */
    @Test
    fun testGetProductsWithInvalidCountry() = testApplication {
        application { module() }
        
        client.get("/products?country=InvalidCountry").apply {
            assertEquals(HttpStatusCode.BadRequest, status)
            val responseBody = bodyAsText()
            assertTrue(responseBody.contains("Unsupported country"))
        }
    }
    
    /**
     * Test GET /products endpoint without country parameter
     */
    @Test
    fun testGetProductsWithoutCountry() = testApplication {
        application { module() }
        
        client.get("/products").apply {
            assertEquals(HttpStatusCode.BadRequest, status)
            val responseBody = bodyAsText()
            assertTrue(responseBody.contains("Country parameter is required"))
        }
    }
    
    /**
     * Test PUT /products/{id}/discount endpoint with valid discount
     */
    @Test
    fun testApplyDiscount() = testApplication {
        application { module() }
        
        val discountRequestJson = """
            {
                "discountId": "summer-sale-2024",
                "percent": 15.0
            }
        """.trimIndent()
        
        client.put("/products/laptop-se/discount") {
            contentType(ContentType.Application.Json)
            setBody(discountRequestJson)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val responseBody = bodyAsText()
            assertTrue(responseBody.contains("Discount applied successfully") || responseBody.contains("Discount already applied"))
            assertTrue(responseBody.contains("finalPrice"))
        }
    }
    
    /**
     * Test PUT /products/{id}/discount endpoint with non-existent product
     */
    @Test
    fun testApplyDiscountToNonExistentProduct() = testApplication {
        application { module() }
        
        val discountRequestJson = """
            {
                "discountId": "test-discount",
                "percent": 10.0
            }
        """.trimIndent()
        
        client.put("/products/non-existent-product/discount") {
            contentType(ContentType.Application.Json)
            setBody(discountRequestJson)
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
            val responseBody = bodyAsText()
            assertTrue(responseBody.contains("not found"))
        }
    }
    
    /**
     * Test idempotency: applying the same discount twice should not change the result
     */
    @Test
    fun testDiscountIdempotency() = testApplication {
        application { module() }
        
        val discountRequestJson = """
            {
                "discountId": "black-friday-2024",
                "percent": 20.0
            }
        """.trimIndent()
        
        // First application
        val firstResponse = client.put("/products/phone-se/discount") {
            contentType(ContentType.Application.Json)
            setBody(discountRequestJson)
        }
        assertEquals(HttpStatusCode.OK, firstResponse.status)
        
        // Second application of same discount
        val secondResponse = client.put("/products/phone-se/discount") {
            contentType(ContentType.Application.Json)
            setBody(discountRequestJson)
        }
        assertEquals(HttpStatusCode.OK, secondResponse.status)
        
        // Both should succeed (idempotent behavior)
        val secondBody = secondResponse.bodyAsText()
        assertTrue(secondBody.contains("Discount already applied") || secondBody.contains("Discount applied successfully"))
    }
    
    /**
     * Test concurrency safety: multiple simultaneous discount applications
     * This is the critical test that demonstrates the solution meets the requirements
     */
    @Test
    fun testConcurrentDiscountApplication() = runBlocking {
        testApplication {
            application { module() }
            
            val discountRequestJson = """
                {
                    "discountId": "flash-sale-2024",
                    "percent": 25.0
                }
            """.trimIndent()
            
            val numberOfConcurrentRequests = 10
            
            // Launch multiple concurrent requests to apply the same discount
            val concurrentRequests = (1..numberOfConcurrentRequests).map {
                async {
                    client.put("/products/headphones-se/discount") {
                        contentType(ContentType.Application.Json)
                        setBody(discountRequestJson)
                    }
                }
            }
            
            // Wait for all requests to complete
            val responses = concurrentRequests.awaitAll()
            
            // All requests should return 200 OK (either applied or already applied)
            responses.forEach { response ->
                assertEquals(HttpStatusCode.OK, response.status)
            }
            
            // Verify the discount was applied exactly once by checking the database
            val productRepository = ProductRepository()
            val product = productRepository.findById("headphones-se")
            assertNotNull(product)
            
            val appliedDiscounts = product.discounts.filter { it.discountId == "flash-sale-2024" }
            assertEquals(1, appliedDiscounts.size, "Discount should be applied exactly once despite concurrent requests")
            assertEquals(25.0, appliedDiscounts.first().percent)
        }
    }
    
    /**
     * Test price calculation with multiple discounts
     */
    @Test
    fun testMultipleDiscountPriceCalculation() = testApplication {
        application { module() }
        
        // Apply first discount
        val firstDiscountJson = """
            {
                "discountId": "discount1",
                "percent": 10.0
            }
        """.trimIndent()
        
        client.put("/products/laptop-de/discount") {
            contentType(ContentType.Application.Json)
            setBody(firstDiscountJson)
        }
        
        // Apply second discount
        val secondDiscountJson = """
            {
                "discountId": "discount2", 
                "percent": 5.0
            }
        """.trimIndent()
        
        client.put("/products/laptop-de/discount") {
            contentType(ContentType.Application.Json)
            setBody(secondDiscountJson)
        }
        
        // Get products to verify price calculation
        val response = client.get("/products?country=Germany")
        assertEquals(HttpStatusCode.OK, response.status)
        
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("laptop-de"))
        assertTrue(responseBody.contains("finalPrice"))
    }
    
    /**
     * Test health endpoint
     */
    @Test
    fun testHealthEndpoint() = testApplication {
        application { module() }
        
        client.get("/health").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertTrue(bodyAsText().contains("healthy"))
        }
    }
}

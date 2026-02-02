package io.nexure.discount

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*
import org.junit.jupiter.api.BeforeEach
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import io.nexure.discount.database.Products
import io.nexure.discount.database.ProductDiscounts
import java.math.BigDecimal
import kotlinx.serialization.json.*

class ApplicationTests {

    @BeforeEach
    fun setUp() {
        // Initialize H2 database for testing with PostgreSQL compatibility
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", driver = "org.h2.Driver")
        
        // Create tables
        transaction {
            SchemaUtils.drop(ProductDiscounts, Products)
            SchemaUtils.create(Products, ProductDiscounts)
            
            // Insert test data using supported countries from VatService
            Products.insert {
                it[Products.id] = "test-product-sweden"
                it[Products.name] = "Test Product Sweden"
                it[Products.basePrice] = BigDecimal("100.00")
                it[Products.country] = "Sweden" // Use supported country
            }
            
            Products.insert {
                it[Products.id] = "test-product-germany"
                it[Products.name] = "Test Product Germany"
                it[Products.basePrice] = BigDecimal("150.00")
                it[Products.country] = "Germany" // Use supported country
            }
        }
    }

    @Test
    fun testHealthEndpoint() = testApplication {
        application {
            module()
        }
        client.get("/health").apply {
            assertEquals(HttpStatusCode.OK, status)
            val response = Json.parseToJsonElement(bodyAsText()).jsonObject
            assertEquals("healthy", response["status"]?.jsonPrimitive?.content)
        }
    }

    @Test
    fun testGetProductsWithValidCountry() = testApplication {
        application {
            module()
        }
        client.get("/products?country=Sweden").apply {
            assertEquals(HttpStatusCode.OK, status)
            
            val response = Json.parseToJsonElement(bodyAsText()).jsonArray
            assertTrue(response.size > 0, "Should return at least one product")
            
            val product = response[0].jsonObject
            assertEquals("test-product-sweden", product["id"]?.jsonPrimitive?.content)
            assertEquals("Test Product Sweden", product["name"]?.jsonPrimitive?.content)
            assertEquals(100.0, product["basePrice"]?.jsonPrimitive?.double)
            assertEquals("Sweden", product["country"]?.jsonPrimitive?.content)
            assertNotNull(product["finalPrice"]?.jsonPrimitive?.double, "Should have calculated final price")
        }
    }

    @Test
    fun testGetProductsWithoutCountry() = testApplication {
        application {
            module()
        }
        client.get("/products").apply {
            assertEquals(HttpStatusCode.BadRequest, status)
            val response = Json.parseToJsonElement(bodyAsText()).jsonObject
            assertTrue(response["error"]?.jsonPrimitive?.content?.contains("Country parameter is required") == true)
        }
    }

    @Test 
    fun testApplyDiscount() = testApplication {
        application {
            module()
        }
        
        val discountRequest = """
            {
                "discountId": "SUMMER_SALE",
                "percent": 10.0
            }
        """.trimIndent()
        
        client.put("/products/test-product-sweden/discount") {
            contentType(ContentType.Application.Json)
            setBody(discountRequest)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            
            val response = Json.parseToJsonElement(bodyAsText()).jsonObject
            assertEquals("Discount applied successfully", response["message"]?.jsonPrimitive?.content)
            assertNotNull(response["product"], "Should include updated product")
        }
    }

    @Test
    fun testApplyDiscountToNonExistentProduct() = testApplication {
        application {
            module()
        }
        
        val discountRequest = """
            {
                "discountId": "SUMMER_SALE",
                "percent": 10.0
            }
        """.trimIndent()
        
        client.put("/products/non-existent-product/discount") {
            contentType(ContentType.Application.Json)
            setBody(discountRequest)
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }
}

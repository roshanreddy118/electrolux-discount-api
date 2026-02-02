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
        // Initialize H2 database for testing
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", driver = "org.h2.Driver")
        
        // Create tables
        transaction {
            SchemaUtils.drop(ProductDiscounts, Products)
            SchemaUtils.create(Products, ProductDiscounts)
            
            // Insert test data
            Products.insert {
                it[Products.id] = "test-product-1"
                it[Products.name] = "Test Product"
                it[Products.basePrice] = BigDecimal("100.00")
                it[Products.country] = "US"
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
    fun testGetProductsWithCountry() = testApplication {
        application {
            module()
        }
        client.get("/products?country=US").apply {
            assertEquals(HttpStatusCode.OK, status)
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
    fun testGetProductsWithInvalidCountry() = testApplication {
        application {
            module()
        }
        client.get("/products?country=INVALID").apply {
            assertEquals(HttpStatusCode.BadRequest, status)
            val response = Json.parseToJsonElement(bodyAsText()).jsonObject
            assertTrue(response["error"]?.jsonPrimitive?.content?.contains("Unsupported country") == true)
        }
    }
}

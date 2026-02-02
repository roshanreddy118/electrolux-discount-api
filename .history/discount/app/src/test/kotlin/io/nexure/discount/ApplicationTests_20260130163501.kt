package io.nexure.discount

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*
import org.junit.jupiter.api.BeforeEach
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.serialization.json.*

class ApplicationTests {

    @BeforeEach
    fun setUp() {
        // Initialize H2 database for testing
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        
        // Create tables
        transaction {
            SchemaUtils.create(Products, DiscountApplications)
            
            // Insert test data
            Products.insert {
                it[id] = 1
                it[name] = "Test Product"
                it[price] = 100.0
                it[country] = "US"
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
            assertEquals("OK", bodyAsText())
        }
    }

    @Test
    fun testGetProductsWithoutCountry() = testApplication {
        application {
            module()
        }
        client.get("/products").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }
}

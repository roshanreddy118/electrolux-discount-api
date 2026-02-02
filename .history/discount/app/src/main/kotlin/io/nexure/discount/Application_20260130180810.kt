package io.nexure.discount

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.nexure.discount.database.DatabaseConfig
import io.nexure.discount.model.ApplyDiscountRequest
import io.nexure.discount.model.Discount
import io.nexure.discount.model.ApplyDiscountResponse
import io.nexure.discount.repository.ProductRepository
import io.nexure.discount.service.DataSeedingService
import io.nexure.discount.service.PriceCalculationService
import io.nexure.discount.service.VatService
import kotlinx.serialization.json.Json

const val DISCOUNT_ENDPOINT = "/discount"

fun main() {
    embeddedServer(
        factory = Netty,
        port = 8082,
        host = "0.0.0.0",
        module = Application::module,
    ).start(true)
}

fun Application.module() {
    // Initialize database connection only if not already initialized (for tests)
    try {
        DatabaseConfig.init()
    } catch (e: Exception) {
        // Database might already be initialized by tests, ignore
        environment.log.info("Database already initialized or initialization failed: ${e.message}")
    }
    
    // Don't seed database if products already exist (prevents test interference)
    val productRepository = ProductRepository()
    try {
        val existingProducts = productRepository.findAll()
        if (existingProducts.isEmpty()) {
            DataSeedingService.seedDatabase(productRepository)
        }
    } catch (e: Exception) {
        environment.log.warn("Failed to check or seed database: ${e.message}")
    }
    
    // Configure JSON serialization for API responses
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    
    // Configure error handling with proper HTTP status codes
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to cause.message))
        }
        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Internal server error"))
        }
    }
    
    routing()
}

fun Application.routing() {
    val productRepository = ProductRepository()
    
    routing {
        /**
         * GET /products?country={country}
         * Returns all products for the given country with calculated final prices
         */
        get("/products") {
            val country = call.request.queryParameters["country"]
            
            if (country.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest, 
                    mapOf("error" to "Country parameter is required")
                )
                return@get
            }
            
            // Validate that the country is supported for VAT calculation
            if (!VatService.isCountrySupported(country)) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "error" to "Unsupported country: $country",
                        "supportedCountries" to VatService.getSupportedCountries()
                    )
                )
                return@get
            }
            
            try {
                val products = productRepository.findByCountry(country)
                val productResponses = products.map { product ->
                    PriceCalculationService.toProductResponse(product)
                }
                
                call.respond(HttpStatusCode.OK, productResponses)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError, 
                    mapOf("error" to "Failed to retrieve products: ${e.message}")
                )
            }
        }
        
        /**
         * PUT /products/{id}/discount
         * Applies a discount to a product with idempotency and concurrency safety
         */
        put("/products/{id}/discount") {
            val productId = call.parameters["id"]
            
            if (productId.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest, 
                    mapOf("error" to "Product ID is required")
                )
                return@put
            }
            
            try {
                val discountRequest = call.receive<ApplyDiscountRequest>()
                
                // Validate discount request
                if (discountRequest.discountId.isBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest, 
                        mapOf("error" to "Discount ID cannot be empty")
                    )
                    return@put
                }
                
                if (discountRequest.percent <= 0 || discountRequest.percent > 100) {
                    call.respond(
                        HttpStatusCode.BadRequest, 
                        mapOf("error" to "Discount percent must be between 0 (exclusive) and 100 (inclusive)")
                    )
                    return@put
                }
                
                val discount = Discount(
                    discountId = discountRequest.discountId,
                    percent = discountRequest.percent
                )
                
                // Apply discount with concurrency safety
                val discountApplied = productRepository.applyDiscount(productId, discount)
                
                if (discountApplied) {
                    // Fetch updated product to return with new final price
                    val updatedProduct = productRepository.findById(productId)
                    if (updatedProduct != null) {
                        val productResponse = PriceCalculationService.toProductResponse(updatedProduct)
                        call.respond(
                            HttpStatusCode.OK, 
                            ApplyDiscountResponse(
                                message = "Discount applied successfully",
                                product = productResponse
                            )
                        )
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Product not found"))
                    }
                } else {
                    // Discount already exists - idempotent response
                    val existingProduct = productRepository.findById(productId)
                    if (existingProduct != null) {
                        val productResponse = PriceCalculationService.toProductResponse(existingProduct)
                        call.respond(
                            HttpStatusCode.OK, 
                            ApplyDiscountResponse(
                                message = "Discount already applied",
                                product = productResponse
                            )
                        )
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Product not found"))
                    }
                }
                
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to e.message))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError, 
                    mapOf("error" to "Failed to apply discount: ${e.message}")
                )
            }
        }
        
        /**
         * GET /health
         * Health check endpoint for monitoring
         */
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "healthy"))
        }
    }
}

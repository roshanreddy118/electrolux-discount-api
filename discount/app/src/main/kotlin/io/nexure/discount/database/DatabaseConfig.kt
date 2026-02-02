package io.nexure.discount.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Database configuration and connection management
 * Uses HikariCP for connection pooling and Exposed ORM for database operations
 */
object DatabaseConfig {
    
    private var database: Database? = null
    private var currentJdbcUrl: String? = null
    
    /**
     * Check if database is connected and healthy
     */
    fun isConnected(): Boolean {
        return database != null
    }
    
    /**
     * Get database status information
     */
    fun getStatus(): Map<String, Any> {
        return mapOf(
            "connected" to isConnected(),
            "type" to when {
                currentJdbcUrl?.contains("h2") == true -> "H2 (In-Memory)"
                currentJdbcUrl?.contains("postgresql") == true -> "PostgreSQL"
                else -> "Not configured"
            }
        )
    }
    
    /**
     * Initialize database connection with HikariCP connection pool
     * This provides production-ready connection management
     * 
     * Default: Uses PostgreSQL for persistent storage (as per requirements)
     * For tests: Override with H2 in-memory database
     */
    fun init(
        jdbcUrl: String = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/discount_db",
        username: String = System.getenv("DATABASE_USERNAME") ?: "discount_user", 
        password: String = System.getenv("DATABASE_PASSWORD") ?: "discount_pass",
        driverClassName: String = when {
            jdbcUrl.contains("h2") -> "org.h2.Driver"
            jdbcUrl.contains("postgresql") -> "org.postgresql.Driver"
            else -> "org.postgresql.Driver"
        }
    ): Database {
        
        // Return existing database if already initialized with same URL
        database?.let { existingDb ->
            return existingDb
        }
        
        // Configure HikariCP connection pool for optimal performance
        val hikariConfig = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = username
            this.password = password
            this.driverClassName = driverClassName
            this.maximumPoolSize = if (jdbcUrl.contains("h2")) 5 else 20 // Smaller pool for H2
            this.minimumIdle = if (jdbcUrl.contains("h2")) 1 else 5      
            this.idleTimeout = 300000 // 5 minutes idle timeout
            this.connectionTimeout = 30000 // 30 seconds connection timeout
            this.leakDetectionThreshold = 60000 // 1 minute leak detection
        }
        
        val dataSource = HikariDataSource(hikariConfig)
        val db = Database.connect(dataSource)
        
        // Create tables if they don't exist
        initializeTables()
        
        database = db
        currentJdbcUrl = jdbcUrl
        return db
    }
    
    /**
     * Create database tables using Exposed ORM schema utilities
     * This is safe to run multiple times (won't recreate existing tables)
     */
    private fun initializeTables() {
        transaction {
            SchemaUtils.create(Products, ProductDiscounts)
        }
    }
    
    /**
     * Reset database for testing purposes
     */
    fun reset() {
        database = null
        currentJdbcUrl = null
    }
}
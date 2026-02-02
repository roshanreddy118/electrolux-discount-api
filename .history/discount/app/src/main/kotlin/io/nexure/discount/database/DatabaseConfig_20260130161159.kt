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
    
    /**
     * Initialize database connection with HikariCP connection pool
     * This provides production-ready connection management
     */
    fun init(
        jdbcUrl: String = "jdbc:postgresql://localhost:5432/discount_db",
        username: String = "discount_user", 
        password: String = "discount_pass",
        driverClassName: String = "org.postgresql.Driver"
    ): Database {
        // Configure HikariCP connection pool for optimal performance
        val hikariConfig = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = username
            this.password = password
            this.driverClassName = driverClassName
            this.maximumPoolSize = 20 // Max connections in pool
            this.minimumIdle = 5      // Min idle connections
            this.idleTimeout = 300000 // 5 minutes idle timeout
            this.connectionTimeout = 30000 // 30 seconds connection timeout
            this.leakDetectionThreshold = 60000 // 1 minute leak detection
        }
        
        val dataSource = HikariDataSource(hikariConfig)
        val database = Database.connect(dataSource)
        
        // Create tables if they don't exist
        initializeTables()
        
        return database
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
}
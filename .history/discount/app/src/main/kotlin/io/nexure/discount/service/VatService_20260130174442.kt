package io.nexure.discount.service

/**
 * Service for calculating VAT rates based on country
 * Contains the business logic for country-specific tax calculations
 */
object VatService {
    
    // VAT rates as specified in the requirements
    private val vatRates = mapOf(
        "Sweden" to 0.25,   // 25%
        "Germany" to 0.19,  // 19%
        "France" to 0.20    // 20%
    )
    
    /**
     * Get VAT rate for a specific country
     * @param country The country name (case-sensitive)
     * @return VAT rate as decimal (e.g., 0.25 for 25%)
     * @throws IllegalArgumentException if country is not supported
     */
    fun getVatRate(country: String): Double {
        return vatRates[country] 
            ?: throw IllegalArgumentException("Unsupported country: $country. Supported countries: ${vatRates.keys}")
    }
    
    /**
     * Check if a country is supported for VAT calculation
     * @param country The country name to check
     * @return true if country is supported, false otherwise
     */
    fun isCountrySupported(country: String): Boolean {
        return vatRates.containsKey(country)
    }
    
    /**
     * Get list of all supported countries
     * @return Set of supported country names
     */
    fun getSupportedCountries(): Set<String> {
        return vatRates.keys
    }
}
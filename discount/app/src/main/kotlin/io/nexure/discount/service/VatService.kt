package io.nexure.discount.service

/**
 * Service for calculating VAT rates based on country
 * Contains the business logic for country-specific tax calculations
 */
object VatService {
    
    // VAT rates with lowercase keys for case-insensitive lookup
    private val vatRates = mapOf(
        "sweden" to 0.25,   // 25%
        "germany" to 0.19,  // 19%
        "france" to 0.20    // 20%
    )
    
    /**
     * Get VAT rate for a specific country (case-insensitive)
     * @param country The country name (case-insensitive, will be normalized)
     * @return VAT rate as decimal (e.g., 0.25 for 25%)
     * @throws IllegalArgumentException if country is not supported
     */
    fun getVatRate(country: String): Double {
        val normalizedCountry = country.trim().lowercase()
        return vatRates[normalizedCountry] 
            ?: throw IllegalArgumentException("Unsupported country: $country. Supported countries: Sweden, Germany, France")
    }
    
    /**
     * Check if a country is supported for VAT calculation (case-insensitive)
     * @param country The country name to check
     * @return true if country is supported, false otherwise
     */
    fun isCountrySupported(country: String): Boolean {
        val normalizedCountry = country.trim().lowercase()
        return vatRates.containsKey(normalizedCountry)
    }
    
    /**
     * Get list of all supported countries
     * @return Set of supported country names (properly capitalized)
     */
    fun getSupportedCountries(): Set<String> {
        return setOf("Sweden", "Germany", "France")
    }
}
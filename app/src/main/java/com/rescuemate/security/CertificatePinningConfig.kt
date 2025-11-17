package com.rescuemate.security

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Certificate Pinning Configuration
 * Protects against Man-in-the-Middle attacks by pinning SSL certificates
 */
object CertificatePinningConfig {
    
    /**
     * Certificate pins for backend services
     * NOTE: These should be updated with actual certificate pins for production
     */
    private val certificatePinner = CertificatePinner.Builder()
        // Example pins - replace with actual production certificates
        // Format: "sha256/<base64-encoded-sha256-hash>"
        
        // Twilio API pins
        .add("api.twilio.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        
        // ElevenLabs API pins  
        .add("api.elevenlabs.io", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        
        // Backend emergency API pins (replace with your backend domain)
        // .add("your-backend.com", "sha256/...")
        
        .build()
    
    /**
     * Create OkHttpClient with certificate pinning
     */
    fun createSecureHttpClient(enablePinning: Boolean = true): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
        
        // Only enable certificate pinning in production builds
        if (enablePinning) {
            builder.certificatePinner(certificatePinner)
        }
        
        return builder.build()
    }
    
    /**
     * Get certificate pins for a specific host
     */
    fun getPinsForHost(hostname: String): List<String> {
        return try {
            certificatePinner.findMatchingPins(hostname).map { it.toString() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Instructions for getting certificate pins:
     * 
     * 1. Using OpenSSL:
     *    ```
     *    openssl s_client -servername api.twilio.com -connect api.twilio.com:443 | \
     *    openssl x509 -pubkey -noout | \
     *    openssl pkey -pubin -outform der | \
     *    openssl dgst -sha256 -binary | \
     *    openssl enc -base64
     *    ```
     *
     * 2. Using OkHttp's built-in tool:
     *    Run the app once without pinning, then check the error message
     *    which will contain the actual certificate hashes
     *
     * 3. Recommended: Pin multiple certificates (current + backup)
     *    to prevent app breakage during certificate rotation
     */
}


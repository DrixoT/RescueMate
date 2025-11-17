package com.rescuemate.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ValidationUtils
 */
class ValidationUtilsTest {
    
    @Test
    fun testValidPhoneNumber() {
        val result = ValidationUtils.validatePhoneNumber("+1234567890")
        assertTrue(result.isValid)
    }
    
    @Test
    fun testInvalidPhoneNumber() {
        val result = ValidationUtils.validatePhoneNumber("123")
        assertFalse(result.isValid)
    }
    
    @Test
    fun testValidEmail() {
        val result = ValidationUtils.validateEmail("test@example.com")
        assertTrue(result.isValid)
    }
    
    @Test
    fun testInvalidEmail() {
        val result = ValidationUtils.validateEmail("invalid-email")
        assertFalse(result.isValid)
    }
    
    @Test
    fun testNormalizePhoneNumber() {
        val normalized = ValidationUtils.normalizePhoneNumber("+1 (234) 567-8900")
        assertEquals("+12345678900", normalized)
    }
    
    @Test
    fun testValidBloodType() {
        val result = ValidationUtils.validateBloodType("O+")
        assertTrue(result.isValid)
    }
    
    @Test
    fun testInvalidBloodType() {
        val result = ValidationUtils.validateBloodType("Z+")
        assertFalse(result.isValid)
    }
    
    @Test
    fun testValidHeartRate() {
        val result = ValidationUtils.validateHeartRate(75)
        assertTrue(result.isValid)
    }
    
    @Test
    fun testInvalidHeartRateTooLow() {
        val result = ValidationUtils.validateHeartRate(20)
        assertFalse(result.isValid)
    }
    
    @Test
    fun testInvalidHeartRateTooHigh() {
        val result = ValidationUtils.validateHeartRate(300)
        assertFalse(result.isValid)
    }
    
    @Test
    fun testValidBloodPressure() {
        val result = ValidationUtils.validateBloodPressure("120/80")
        assertTrue(result.isValid)
    }
    
    @Test
    fun testInvalidBloodPressureFormat() {
        val result = ValidationUtils.validateBloodPressure("120-80")
        assertFalse(result.isValid)
    }
    
    @Test
    fun testValidateAll_AllValid() {
        val results = ValidationUtils.validateAll(
            ValidationUtils.ValidationResult.success(),
            ValidationUtils.ValidationResult.success()
        )
        assertTrue(results.isValid)
    }
    
    @Test
    fun testValidateAll_OneInvalid() {
        val results = ValidationUtils.validateAll(
            ValidationUtils.ValidationResult.success(),
            ValidationUtils.ValidationResult.error("Error")
        )
        assertFalse(results.isValid)
    }
}


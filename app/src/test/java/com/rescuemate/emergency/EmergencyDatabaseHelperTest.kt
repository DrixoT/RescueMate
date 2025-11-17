package com.rescuemate.emergency

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rescuemate.emergency.data.EmergencyContact
import com.rescuemate.emergency.data.MedicalInfo
import com.rescuemate.emergency.data.database.EmergencyDatabaseHelper
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Unit tests for EmergencyDatabaseHelper
 * NOTE: These are instrumented tests that require Android framework
 */
@RunWith(AndroidJUnit4::class)
class EmergencyDatabaseHelperTest {
    
    private lateinit var database: EmergencyDatabaseHelper
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = EmergencyDatabaseHelper(context)
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun testInsertContact() {
        val contact = EmergencyContact(
            name = "John Doe",
            phoneNumber = "+1234567890",
            relationship = "Friend",
            priority = 1
        )
        
        val result = database.insertContact(contact)
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun testGetAllContacts() {
        // Insert test contact
        val contact = EmergencyContact(
            name = "Jane Doe",
            phoneNumber = "+1987654321",
            relationship = "Family",
            priority = 1
        )
        database.insertContact(contact)
        
        // Retrieve contacts
        val result = database.getAllContacts()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isNotEmpty() == true)
    }
    
    @Test
    fun testDeleteContact() {
        val contact = EmergencyContact(
            id = "test-contact-id",
            name = "Test User",
            phoneNumber = "+1111111111",
            relationship = "Test",
            priority = 1
        )
        database.insertContact(contact)
        
        val deleteResult = database.deleteContact(contact.id)
        assertTrue(deleteResult.isSuccess)
    }
    
    @Test
    fun testInsertMedicalInfo() {
        val medicalInfo = MedicalInfo(
            userId = "test-user-id",
            bloodType = "O+",
            baselineHeartRate = 70
        )
        
        val result = database.insertOrUpdateMedicalInfo(medicalInfo)
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun testGetMedicalInfo() {
        val userId = "test-user-id-2"
        val medicalInfo = MedicalInfo(
            userId = userId,
            bloodType = "A+",
            baselineHeartRate = 65
        )
        database.insertOrUpdateMedicalInfo(medicalInfo)
        
        val result = database.getMedicalInfo(userId)
        assertTrue(result.isSuccess)
        assertEquals("A+", result.getOrNull()?.bloodType)
    }
    
    @Test
    fun testContactPagination() {
        // Insert multiple contacts
        repeat(25) { index ->
            val contact = EmergencyContact(
                name = "Contact $index",
                phoneNumber = "+123456789$index",
                relationship = "Friend",
                priority = index
            )
            database.insertContact(contact)
        }
        
        // Test pagination
        val page1 = database.getAllContacts(limit = 10, offset = 0)
        val page2 = database.getAllContacts(limit = 10, offset = 10)
        
        assertTrue(page1.isSuccess)
        assertTrue(page2.isSuccess)
        assertEquals(10, page1.getOrNull()?.size)
        assertEquals(10, page2.getOrNull()?.size)
    }
    
    @Test
    fun testGetContactCount() {
        val result = database.getContactCount()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!! >= 0)
    }
}


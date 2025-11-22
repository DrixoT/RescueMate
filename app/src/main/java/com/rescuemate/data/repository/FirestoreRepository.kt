package com.rescuemate.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.rescuemate.emergency.data.EmergencyContact
import com.rescuemate.emergency.data.MedicalInfo
import com.rescuemate.emergency.data.Medication
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val userId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")

    // ================== User Profile ==================

    suspend fun saveUserProfile(name: String, dateOfBirth: String, gender: String, phone: String) {
        val userMap = hashMapOf(
            "name" to name,
            "dateOfBirth" to dateOfBirth,
            "gender" to gender,
            "phone" to phone,
            "updatedAt" to System.currentTimeMillis()
        )
        
        try {
            db.collection("users").document(userId)
                .set(userMap, SetOptions.merge())
                .await()
            Log.d("FirestoreRepo", "User profile saved to Firestore")
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error saving user profile", e)
            throw e
        }
    }

    suspend fun getUserProfile(): Map<String, Any>? {
        return try {
            val snapshot = db.collection("users").document(userId).get().await()
            snapshot.data
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error fetching user profile", e)
            null
        }
    }

    // ================== Medical Info ==================

    suspend fun saveMedicalInfo(info: MedicalInfo) {
        val medicalMap = hashMapOf(
            "bloodType" to (info.bloodType ?: ""),
            "knownConditions" to info.knownConditions,
            "allergies" to info.allergies,
            "medications" to info.currentMedications.map { mapMedication(it) },
            "baselineHeartRate" to info.baselineHeartRate,
            "emergencyNotes" to (info.emergencyNotes ?: ""),
            "updatedAt" to System.currentTimeMillis()
        )

        try {
            db.collection("users").document(userId)
                .collection("medical").document("info")
                .set(medicalMap, SetOptions.merge())
                .await()
            Log.d("FirestoreRepo", "Medical info saved to Firestore")
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error saving medical info", e)
            throw e
        }
    }

    suspend fun getMedicalInfo(): MedicalInfo? {
        return try {
            val snapshot = db.collection("users").document(userId)
                .collection("medical").document("info")
                .get().await()

            if (snapshot.exists()) {
                val data = snapshot.data ?: return null
                
                val medicationsList = (data["medications"] as? List<Map<String, Any>>)?.map { 
                    Medication(
                        name = it["name"] as? String ?: "",
                        dosage = it["dosage"] as? String ?: "",
                        frequency = it["frequency"] as? String ?: "",
                        affectsHeartRate = it["affectsHeartRate"] as? Boolean ?: false,
                        criticalMedication = it["criticalMedication"] as? Boolean ?: false
                    )
                } ?: emptyList()

                MedicalInfo(
                    userId = userId,
                    bloodType = data["bloodType"] as? String,
                    knownConditions = (data["knownConditions"] as? List<String>) ?: emptyList(),
                    allergies = (data["allergies"] as? List<String>) ?: emptyList(),
                    currentMedications = medicationsList,
                    baselineHeartRate = (data["baselineHeartRate"] as? Long)?.toInt() ?: 70,
                    emergencyNotes = data["emergencyNotes"] as? String
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error fetching medical info", e)
            null
        }
    }

    private fun mapMedication(med: Medication): Map<String, Any> {
        return mapOf(
            "name" to med.name,
            "dosage" to med.dosage,
            "frequency" to med.frequency,
            "affectsHeartRate" to med.affectsHeartRate,
            "criticalMedication" to med.criticalMedication
        )
    }

    // ================== Contacts ==================

    suspend fun saveContact(contact: EmergencyContact) {
        val contactMap = hashMapOf(
            "id" to contact.id,
            "name" to contact.name,
            "phoneNumber" to contact.phoneNumber,
            "relationship" to contact.relationship,
            "priority" to contact.priority,
            "isPrimaryContact" to contact.isPrimaryContact,
            "email" to contact.email
        )

        try {
            db.collection("users").document(userId)
                .collection("contacts").document(contact.id)
                .set(contactMap)
                .await()
            Log.d("FirestoreRepo", "Contact saved to Firestore")
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error saving contact", e)
            throw e
        }
    }

    suspend fun getContacts(): List<EmergencyContact> {
        return try {
            val snapshot = db.collection("users").document(userId)
                .collection("contacts")
                .get().await()

            snapshot.documents.mapNotNull { doc ->
                EmergencyContact(
                    id = doc.getString("id") ?: doc.id,
                    name = doc.getString("name") ?: "",
                    phoneNumber = doc.getString("phoneNumber") ?: "",
                    relationship = doc.getString("relationship") ?: "",
                    priority = (doc.getLong("priority") ?: 1).toInt(),
                    isPrimaryContact = doc.getBoolean("isPrimaryContact") ?: false,
                    email = doc.getString("email")
                )
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error fetching contacts", e)
            emptyList()
        }
    }
    
    suspend fun deleteContact(contactId: String) {
        try {
            db.collection("users").document(userId)
                .collection("contacts").document(contactId)
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error deleting contact", e)
            throw e
        }
    }
}


package com.rescuemate.data.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.rescuemate.emergency.data.EmergencyContact
import com.rescuemate.emergency.data.InteractionLog
import com.rescuemate.emergency.data.MedicalInfo
import com.rescuemate.emergency.data.Medication
import kotlinx.coroutines.tasks.await

class FirestoreRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    // Explicitly initialize with the correct bucket URL to avoid "Object does not exist" 404 errors
    // if google-services.json has a different default bucket
    private val storage = try {
        FirebaseStorage.getInstance("gs://rescuemate-c98a3.firebasestorage.app")
    } catch (e: Exception) {
        Log.w(TAG, "Could not get storage instance with specific bucket, falling back to default", e)
        FirebaseStorage.getInstance()
    }

    companion object {
        private const val TAG = "FirestoreRepository"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_INTERACTION_LOGS = "logs"
        private const val COLLECTION_CONTACTS = "contacts"
        private const val COLLECTION_MEDICAL_INFO = "medical_info"
    }

    // ==================== User Profile ====================

    suspend fun saveUserProfile(name: String, dateOfBirth: String, gender: String, phone: String) {
        val userId = auth.currentUser?.uid ?: return
        val userMap = mapOf(
            "name" to name,
            "dateOfBirth" to dateOfBirth,
            "gender" to gender,
            "phone" to phone,
            "updatedAt" to System.currentTimeMillis()
        )

        try {
            db.collection(COLLECTION_USERS).document(userId)
                .set(userMap, SetOptions.merge())
                .await()
            Log.d(TAG, "User profile saved to Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user profile", e)
            throw e
        }
    }

    suspend fun getUserProfile(): Map<String, Any>? {
        val userId = auth.currentUser?.uid ?: return null
        return try {
            val document = db.collection(COLLECTION_USERS).document(userId).get().await()
            if (document.exists()) {
                document.data
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user profile", e)
            null
        }
    }

    suspend fun uploadProfilePhoto(uri: Uri): String {
        val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
        val ref = storage.reference.child("profile_photos/$userId.jpg")
        
        return try {
            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            
            // Update user profile with photo URL
            db.collection(COLLECTION_USERS).document(userId)
                .update("photoUrl", downloadUrl)
                .await()
                
            downloadUrl
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading profile photo", e)
            throw e
        }
    }
    
    suspend fun updateUserProfilePhoto(url: String) {
        val userId = auth.currentUser?.uid ?: return
        try {
            db.collection(COLLECTION_USERS).document(userId)
                .update("photoUrl", url)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile photo URL", e)
            throw e
        }
    }

    // ==================== Medical Info ====================

    suspend fun saveMedicalInfo(medicalInfo: MedicalInfo) {
        val userId = auth.currentUser?.uid ?: return
        try {
            // Convert MedicalInfo to Map manually or rely on Firestore mapping if no-arg constructor exists.
            // Since we don't control MedicalInfo easily right now, manual mapping is safer or just save the object if it's a data class (Firestore supports it usually).
            // However, nested objects like List<Medication> need to be handled.
            // Let's trust Firestore's POJO mapper for now, but fall back if needed.
            
            db.collection(COLLECTION_USERS).document(userId)
                .collection(COLLECTION_MEDICAL_INFO).document("default")
                .set(medicalInfo)
                .await()
            Log.d(TAG, "Medical info saved to Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving medical info", e)
            throw e
        }
    }

    suspend fun getMedicalInfo(): MedicalInfo? {
        val userId = auth.currentUser?.uid ?: return null
        return try {
            val document = db.collection(COLLECTION_USERS).document(userId)
                .collection(COLLECTION_MEDICAL_INFO).document("default")
                .get()
                .await()
            
            if (document.exists()) {
                // Manual mapping might be safer if automatic fails, but let's try automatic first
                // If MedicalInfo doesn't have no-arg constructor, this might fail.
                // To be safe, let's construct it manually if we can.
                
                val data = document.data ?: return null
                
                val knownConditions = (data["knownConditions"] as? List<String>) ?: emptyList()
                val allergies = (data["allergies"] as? List<String>) ?: emptyList()
                val bloodType = data["bloodType"] as? String
                val dob = data["dateOfBirth"] as? String ?: ""
                
                val medsListRaw = data["currentMedications"] as? List<Map<String, Any>> ?: emptyList()
                val medications = medsListRaw.map { 
                    Medication(
                        name = it["name"] as? String ?: "",
                        dosage = it["dosage"] as? String ?: "",
                        frequency = it["frequency"] as? String ?: ""
                    )
                }

                MedicalInfo(
                    userId = userId,
                    dateOfBirth = dob,
                    bloodType = bloodType,
                    knownConditions = knownConditions,
                    currentMedications = medications,
                    allergies = allergies
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching medical info", e)
            null
        }
    }

    // ==================== Emergency Contacts ====================

    suspend fun saveContact(contact: EmergencyContact) {
        val userId = auth.currentUser?.uid ?: return
        try {
            db.collection(COLLECTION_USERS).document(userId)
                .collection(COLLECTION_CONTACTS).document(contact.id)
                .set(contact)
                .await()
            Log.d(TAG, "Contact saved to Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving contact", e)
            throw e
        }
    }
    
    suspend fun deleteContact(contactId: String) {
        val userId = auth.currentUser?.uid ?: return
        try {
            db.collection(COLLECTION_USERS).document(userId)
                .collection(COLLECTION_CONTACTS).document(contactId)
                .delete()
                .await()
            Log.d(TAG, "Contact deleted from Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting contact", e)
            throw e
        }
    }

    suspend fun getContacts(): List<EmergencyContact> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = db.collection(COLLECTION_USERS).document(userId)
                .collection(COLLECTION_CONTACTS)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                // Manual mapping for safety
                try {
                    EmergencyContact(
                        id = doc.getString("id") ?: doc.id,
                        name = doc.getString("name") ?: "",
                        phoneNumber = doc.getString("phoneNumber") ?: "",
                        relationship = doc.getString("relationship") ?: "",
                        isPrimaryContact = doc.getBoolean("primaryContact") ?: false
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching contacts", e)
            emptyList()
        }
    }

    // ==================== Interaction Logs ====================

    fun saveInteractionLog(log: InteractionLog) {
        val userId = auth.currentUser?.uid ?: return
        // Fire and forget mostly, or we can make it suspend if caller handles it
        // The existing usage in InteractionLogManager calls it without suspend in a coroutine scope, but checking the signature there...
        // It was `firestoreRepository?.saveInteractionLog(log)`
        
        try {
            db.collection(COLLECTION_USERS).document(userId)
                .collection(COLLECTION_INTERACTION_LOGS).document(log.id)
                .set(log)
                .addOnSuccessListener { 
                    Log.d(TAG, "Interaction log saved to Firestore: ${log.id}") 
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error saving interaction log", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating save interaction log", e)
        }
    }

    suspend fun getInteractionLogs(userId: String): Result<List<InteractionLog>> {
        return try {
            val snapshot = db.collection(COLLECTION_USERS).document(userId)
                .collection(COLLECTION_INTERACTION_LOGS)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val logs = snapshot.documents.mapNotNull { doc ->
                try {
                    InteractionLog(
                        id = doc.getString("id") ?: doc.id,
                        userId = doc.getString("userId") ?: userId,
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        summary = doc.getString("summary") ?: "",
                        transcript = doc.getString("transcript") ?: "",
                        type = doc.getString("type") ?: "UNKNOWN"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing interaction log: ${doc.id}", e)
                    null
                }
            }
            Result.success(logs)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching interaction logs", e)
            Result.failure(e)
        }
    }
}

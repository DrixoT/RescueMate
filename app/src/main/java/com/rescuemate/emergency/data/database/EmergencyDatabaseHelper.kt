package com.rescuemate.emergency.data.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.rescuemate.emergency.EmergencyConstants
import com.rescuemate.emergency.data.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Emergency Database Helper
 * Handles all emergency-related data persistence
 */
class EmergencyDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    EmergencyConstants.DATABASE_NAME,
    null,
    EmergencyConstants.DATABASE_VERSION
) {

    companion object {
        private const val TAG = "EmergencyDatabaseHelper"
        
        // Emergency Contacts Table
        private const val TABLE_CONTACTS = "emergency_contacts"
        private const val COL_CONTACT_ID = "id"
        private const val COL_CONTACT_NAME = "name"
        private const val COL_CONTACT_PHONE = "phone_number"
        private const val COL_CONTACT_RELATIONSHIP = "relationship"
        private const val COL_CONTACT_PRIORITY = "priority"
        private const val COL_CONTACT_VERIFIED = "is_verified"
        private const val COL_CONTACT_EMAIL = "email"
        private const val COL_CONTACT_PRIMARY = "is_primary"
        private const val COL_CONTACT_NOTIFICATION_PREF = "notification_preference"
        private const val COL_CONTACT_MEDICAL_KNOWLEDGE = "medical_knowledge"
        private const val COL_CONTACT_LAST_CONTACTED = "last_contacted_timestamp"

        // Medical Info Table
        private const val TABLE_MEDICAL_INFO = "medical_info"
        private const val COL_MEDICAL_USER_ID = "user_id"
        private const val COL_MEDICAL_DOB = "date_of_birth"
        private const val COL_MEDICAL_BLOOD_TYPE = "blood_type"
        private const val COL_MEDICAL_CONDITIONS = "known_conditions"
        private const val COL_MEDICAL_MEDICATIONS = "current_medications"
        private const val COL_MEDICAL_ALLERGIES = "allergies"
        private const val COL_MEDICAL_BASELINE_HR = "baseline_heart_rate"
        private const val COL_MEDICAL_BASELINE_BP = "baseline_blood_pressure"
        private const val COL_MEDICAL_NOTES = "emergency_notes"
        private const val COL_MEDICAL_HOSPITAL = "preferred_hospital"
        private const val COL_MEDICAL_DOCTOR = "doctor_name"
        private const val COL_MEDICAL_DOCTOR_PHONE = "doctor_phone"

        // Emergency Events Table
        private const val TABLE_EVENTS = "emergency_events"
        private const val COL_EVENT_ID = "id"
        private const val COL_EVENT_USER_ID = "user_id"
        private const val COL_EVENT_TYPE = "emergency_type"
        private const val COL_EVENT_STATUS = "status"
        private const val COL_EVENT_PHASE = "current_phase"
        private const val COL_EVENT_TRIGGERED = "triggered_timestamp"
        private const val COL_EVENT_RESOLVED = "resolved_timestamp"
        private const val COL_EVENT_HEALTH_DATA = "health_data_json"
        private const val COL_EVENT_LOCATION_DATA = "location_data_json"
        private const val COL_EVENT_USER_RESPONDED = "user_responded"
        private const val COL_EVENT_USER_CANCELLED = "user_cancelled"
        private const val COL_EVENT_BACKEND_NOTIFIED = "backend_notified"
        private const val COL_EVENT_TWILIO_SID = "twilio_call_sid"

        // Contact Attempts Table
        private const val TABLE_ATTEMPTS = "contact_attempts"
        private const val COL_ATTEMPT_ID = "id"
        private const val COL_ATTEMPT_EVENT_ID = "emergency_event_id"
        private const val COL_ATTEMPT_CONTACT_ID = "contact_id"
        private const val COL_ATTEMPT_TYPE = "attempt_type"
        private const val COL_ATTEMPT_TIMESTAMP = "timestamp"
        private const val COL_ATTEMPT_SUCCESS = "success"
        private const val COL_ATTEMPT_FAILURE_REASON = "failure_reason"
        private const val COL_ATTEMPT_CALL_SID = "call_sid"

        // Contact Responses Table
        private const val TABLE_RESPONSES = "contact_responses"
        private const val COL_RESPONSE_ID = "id"
        private const val COL_RESPONSE_EVENT_ID = "emergency_event_id"
        private const val COL_RESPONSE_CONTACT_ID = "contact_id"
        private const val COL_RESPONSE_TYPE = "response_type"
        private const val COL_RESPONSE_TIMESTAMP = "timestamp"
        private const val COL_RESPONSE_NOTES = "notes"
        private const val COL_RESPONSE_SOURCE = "response_source"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create Emergency Contacts Table
        db.execSQL("""
            CREATE TABLE $TABLE_CONTACTS (
                $COL_CONTACT_ID TEXT PRIMARY KEY,
                $COL_CONTACT_NAME TEXT NOT NULL,
                $COL_CONTACT_PHONE TEXT NOT NULL,
                $COL_CONTACT_RELATIONSHIP TEXT,
                $COL_CONTACT_PRIORITY INTEGER DEFAULT 1,
                $COL_CONTACT_VERIFIED INTEGER DEFAULT 0,
                $COL_CONTACT_EMAIL TEXT,
                $COL_CONTACT_PRIMARY INTEGER DEFAULT 0,
                $COL_CONTACT_NOTIFICATION_PREF TEXT DEFAULT 'ALL',
                $COL_CONTACT_MEDICAL_KNOWLEDGE TEXT DEFAULT 'BASIC',
                $COL_CONTACT_LAST_CONTACTED INTEGER DEFAULT 0
            )
        """)

        // Create Medical Info Table
        db.execSQL("""
            CREATE TABLE $TABLE_MEDICAL_INFO (
                $COL_MEDICAL_USER_ID TEXT PRIMARY KEY,
                $COL_MEDICAL_DOB TEXT,
                $COL_MEDICAL_BLOOD_TYPE TEXT,
                $COL_MEDICAL_CONDITIONS TEXT,
                $COL_MEDICAL_MEDICATIONS TEXT,
                $COL_MEDICAL_ALLERGIES TEXT,
                $COL_MEDICAL_BASELINE_HR INTEGER DEFAULT 70,
                $COL_MEDICAL_BASELINE_BP TEXT,
                $COL_MEDICAL_NOTES TEXT,
                $COL_MEDICAL_HOSPITAL TEXT,
                $COL_MEDICAL_DOCTOR TEXT,
                $COL_MEDICAL_DOCTOR_PHONE TEXT
            )
        """)

        // Create Emergency Events Table
        db.execSQL("""
            CREATE TABLE $TABLE_EVENTS (
                $COL_EVENT_ID TEXT PRIMARY KEY,
                $COL_EVENT_USER_ID TEXT NOT NULL,
                $COL_EVENT_TYPE TEXT NOT NULL,
                $COL_EVENT_STATUS TEXT NOT NULL,
                $COL_EVENT_PHASE INTEGER DEFAULT 1,
                $COL_EVENT_TRIGGERED INTEGER NOT NULL,
                $COL_EVENT_RESOLVED INTEGER,
                $COL_EVENT_HEALTH_DATA TEXT,
                $COL_EVENT_LOCATION_DATA TEXT,
                $COL_EVENT_USER_RESPONDED INTEGER DEFAULT 0,
                $COL_EVENT_USER_CANCELLED INTEGER DEFAULT 0,
                $COL_EVENT_BACKEND_NOTIFIED INTEGER DEFAULT 0,
                $COL_EVENT_TWILIO_SID TEXT
            )
        """)

        // Create Contact Attempts Table
        db.execSQL("""
            CREATE TABLE $TABLE_ATTEMPTS (
                $COL_ATTEMPT_ID TEXT PRIMARY KEY,
                $COL_ATTEMPT_EVENT_ID TEXT NOT NULL,
                $COL_ATTEMPT_CONTACT_ID TEXT NOT NULL,
                $COL_ATTEMPT_TYPE TEXT NOT NULL,
                $COL_ATTEMPT_TIMESTAMP INTEGER NOT NULL,
                $COL_ATTEMPT_SUCCESS INTEGER DEFAULT 0,
                $COL_ATTEMPT_FAILURE_REASON TEXT,
                $COL_ATTEMPT_CALL_SID TEXT,
                FOREIGN KEY($COL_ATTEMPT_EVENT_ID) REFERENCES $TABLE_EVENTS($COL_EVENT_ID)
            )
        """)

        // Create Contact Responses Table
        db.execSQL("""
            CREATE TABLE $TABLE_RESPONSES (
                $COL_RESPONSE_ID TEXT PRIMARY KEY,
                $COL_RESPONSE_EVENT_ID TEXT NOT NULL,
                $COL_RESPONSE_CONTACT_ID TEXT NOT NULL,
                $COL_RESPONSE_TYPE TEXT NOT NULL,
                $COL_RESPONSE_TIMESTAMP INTEGER NOT NULL,
                $COL_RESPONSE_NOTES TEXT,
                $COL_RESPONSE_SOURCE TEXT DEFAULT 'PHONE_CALL',
                FOREIGN KEY($COL_RESPONSE_EVENT_ID) REFERENCES $TABLE_EVENTS($COL_EVENT_ID)
            )
        """)

        // Create Indexes
        db.execSQL("CREATE INDEX idx_events_user_id ON $TABLE_EVENTS($COL_EVENT_USER_ID)")
        db.execSQL("CREATE INDEX idx_events_status ON $TABLE_EVENTS($COL_EVENT_STATUS)")
        db.execSQL("CREATE INDEX idx_attempts_event_id ON $TABLE_ATTEMPTS($COL_ATTEMPT_EVENT_ID)")
        db.execSQL("CREATE INDEX idx_responses_event_id ON $TABLE_RESPONSES($COL_RESPONSE_EVENT_ID)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Implement proper migration strategy instead of dropping tables
        Log.d(TAG, "Upgrading database from version $oldVersion to $newVersion")
        
        try {
            when {
                oldVersion < 2 -> {
                    // Future migration for version 2
                    // Example: db.execSQL("ALTER TABLE $TABLE_CONTACTS ADD COLUMN new_column TEXT")
                }
                // Add more migration cases as needed
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during database migration", e)
            // As a last resort, recreate tables (data loss)
            Log.w(TAG, "Migration failed, recreating database (data will be lost)")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_RESPONSES")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_ATTEMPTS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_EVENTS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_MEDICAL_INFO")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_CONTACTS")
            onCreate(db)
        }
    }

    // Emergency Contact Operations
    fun insertContact(contact: EmergencyContact): Result<Long> {
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_CONTACT_ID, contact.id)
                put(COL_CONTACT_NAME, contact.name)
                put(COL_CONTACT_PHONE, contact.phoneNumber)
                put(COL_CONTACT_RELATIONSHIP, contact.relationship)
                put(COL_CONTACT_PRIORITY, contact.priority)
                put(COL_CONTACT_VERIFIED, if (contact.isVerified) 1 else 0)
                put(COL_CONTACT_EMAIL, contact.email)
                put(COL_CONTACT_PRIMARY, if (contact.isPrimaryContact) 1 else 0)
                put(COL_CONTACT_NOTIFICATION_PREF, contact.notificationPreference.name)
                put(COL_CONTACT_MEDICAL_KNOWLEDGE, contact.medicalKnowledge.name)
                put(COL_CONTACT_LAST_CONTACTED, contact.lastContactedTimestamp)
            }
            val result = db.insertWithOnConflict(TABLE_CONTACTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            if (result == -1L) {
                Result.failure(Exception("Failed to insert contact"))
            } else {
                Result.success(result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting contact: ${contact.name}", e)
            Result.failure(e)
        }
    }

    fun getAllContacts(limit: Int? = null, offset: Int = 0): Result<List<EmergencyContact>> {
        return try {
            val contacts = mutableListOf<EmergencyContact>()
            val db = readableDatabase
            val limitStr = if (limit != null) "$offset,$limit" else null
            val cursor = db.query(
                TABLE_CONTACTS,
                null,
                null,
                null,
                null,
                null,
                "$COL_CONTACT_PRIORITY ASC",
                limitStr
            )

            cursor.use {
                while (it.moveToNext()) {
                    try {
                        contacts.add(
                            EmergencyContact(
                                id = it.getString(it.getColumnIndexOrThrow(COL_CONTACT_ID)),
                                name = it.getString(it.getColumnIndexOrThrow(COL_CONTACT_NAME)),
                                phoneNumber = it.getString(it.getColumnIndexOrThrow(COL_CONTACT_PHONE)),
                                relationship = it.getString(it.getColumnIndexOrThrow(COL_CONTACT_RELATIONSHIP)),
                                priority = it.getInt(it.getColumnIndexOrThrow(COL_CONTACT_PRIORITY)),
                                isVerified = it.getInt(it.getColumnIndexOrThrow(COL_CONTACT_VERIFIED)) == 1,
                                email = it.getString(it.getColumnIndexOrThrow(COL_CONTACT_EMAIL)),
                                isPrimaryContact = it.getInt(it.getColumnIndexOrThrow(COL_CONTACT_PRIMARY)) == 1,
                                notificationPreference = EmergencyContact.NotificationPreference.valueOf(
                                    it.getString(it.getColumnIndexOrThrow(COL_CONTACT_NOTIFICATION_PREF))
                                ),
                                medicalKnowledge = EmergencyContact.MedicalKnowledge.valueOf(
                                    it.getString(it.getColumnIndexOrThrow(COL_CONTACT_MEDICAL_KNOWLEDGE))
                                ),
                                lastContactedTimestamp = it.getLong(it.getColumnIndexOrThrow(COL_CONTACT_LAST_CONTACTED))
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing contact from cursor", e)
                        // Continue with other contacts
                    }
                }
            }
            Result.success(contacts)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all contacts", e)
            Result.failure(e)
        }
    }

    fun deleteContact(contactId: String): Result<Int> {
        return try {
            val db = writableDatabase
            val result = db.delete(TABLE_CONTACTS, "$COL_CONTACT_ID = ?", arrayOf(contactId))
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting contact: $contactId", e)
            Result.failure(e)
        }
    }

    // Medical Info Operations
    fun insertOrUpdateMedicalInfo(medicalInfo: MedicalInfo): Result<Long> {
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_MEDICAL_USER_ID, medicalInfo.userId)
                put(COL_MEDICAL_DOB, medicalInfo.dateOfBirth)
                put(COL_MEDICAL_BLOOD_TYPE, medicalInfo.bloodType)
                put(COL_MEDICAL_CONDITIONS, JSONArray(medicalInfo.knownConditions).toString())
                put(COL_MEDICAL_MEDICATIONS, serializeMedications(medicalInfo.currentMedications))
                put(COL_MEDICAL_ALLERGIES, JSONArray(medicalInfo.allergies).toString())
                put(COL_MEDICAL_BASELINE_HR, medicalInfo.baselineHeartRate)
                put(COL_MEDICAL_BASELINE_BP, medicalInfo.baselineBloodPressure)
                put(COL_MEDICAL_NOTES, medicalInfo.emergencyNotes)
                put(COL_MEDICAL_HOSPITAL, medicalInfo.preferredHospital)
                put(COL_MEDICAL_DOCTOR, medicalInfo.doctorName)
                put(COL_MEDICAL_DOCTOR_PHONE, medicalInfo.doctorPhone)
            }
            val result = db.insertWithOnConflict(TABLE_MEDICAL_INFO, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            if (result == -1L) {
                Result.failure(Exception("Failed to insert/update medical info"))
            } else {
                Result.success(result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting/updating medical info", e)
            Result.failure(e)
        }
    }

    fun getMedicalInfo(userId: String): Result<MedicalInfo?> {
        return try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_MEDICAL_INFO,
                null,
                "$COL_MEDICAL_USER_ID = ?",
                arrayOf(userId),
                null,
                null,
                null
            )

            val result = cursor.use {
                if (it.moveToFirst()) {
                    MedicalInfo(
                        userId = it.getString(it.getColumnIndexOrThrow(COL_MEDICAL_USER_ID)),
                        dateOfBirth = it.getString(it.getColumnIndexOrThrow(COL_MEDICAL_DOB)),
                        bloodType = it.getString(it.getColumnIndexOrThrow(COL_MEDICAL_BLOOD_TYPE)),
                        knownConditions = parseJsonArray(it.getString(it.getColumnIndexOrThrow(COL_MEDICAL_CONDITIONS))),
                        currentMedications = deserializeMedications(it.getString(it.getColumnIndexOrThrow(COL_MEDICAL_MEDICATIONS))),
                        allergies = parseJsonArray(it.getString(it.getColumnIndexOrThrow(COL_MEDICAL_ALLERGIES))),
                        baselineHeartRate = it.getInt(it.getColumnIndexOrThrow(COL_MEDICAL_BASELINE_HR)),
                        baselineBloodPressure = it.getString(it.getColumnIndexOrThrow(COL_MEDICAL_BASELINE_BP)),
                        emergencyNotes = it.getString(it.getColumnIndexOrThrow(COL_MEDICAL_NOTES)),
                        preferredHospital = it.getString(it.getColumnIndexOrThrow(COL_MEDICAL_HOSPITAL)),
                        doctorName = it.getString(it.getColumnIndexOrThrow(COL_MEDICAL_DOCTOR)),
                        doctorPhone = it.getString(it.getColumnIndexOrThrow(COL_MEDICAL_DOCTOR_PHONE))
                    )
                } else null
            }
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting medical info for user: $userId", e)
            Result.failure(e)
        }
    }

    // Emergency Event Operations
    fun insertEmergencyEvent(event: EmergencyEvent): Result<Long> {
        return try {
            val db = writableDatabase
            db.beginTransaction()
            try {
                val values = ContentValues().apply {
                    put(COL_EVENT_ID, event.id)
                    put(COL_EVENT_USER_ID, event.userId)
                    put(COL_EVENT_TYPE, event.emergencyType.name)
                    put(COL_EVENT_STATUS, event.status.name)
                    put(COL_EVENT_PHASE, event.currentPhase)
                    put(COL_EVENT_TRIGGERED, event.triggeredTimestamp)
                    put(COL_EVENT_RESOLVED, event.resolvedTimestamp)
                    put(COL_EVENT_HEALTH_DATA, serializeHealthData(event.healthData))
                    put(COL_EVENT_LOCATION_DATA, serializeLocationData(event.locationData))
                    put(COL_EVENT_USER_RESPONDED, if (event.userResponded) 1 else 0)
                    put(COL_EVENT_USER_CANCELLED, if (event.userCancelled) 1 else 0)
                    put(COL_EVENT_BACKEND_NOTIFIED, if (event.backendNotified) 1 else 0)
                    put(COL_EVENT_TWILIO_SID, event.twilioCallSid)
                }
                val result = db.insertWithOnConflict(TABLE_EVENTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
                db.setTransactionSuccessful()
                if (result == -1L) {
                    Result.failure(Exception("Failed to insert emergency event"))
                } else {
                    Result.success(result)
                }
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting emergency event: ${event.id}", e)
            Result.failure(e)
        }
    }

    fun getActiveEmergencyEvent(userId: String): Result<EmergencyEvent?> {
        return try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_EVENTS,
                null,
                "$COL_EVENT_USER_ID = ? AND $COL_EVENT_RESOLVED IS NULL",
                arrayOf(userId),
                null,
                null,
                "$COL_EVENT_TRIGGERED DESC",
                "1"
            )

            val result = cursor.use {
                if (it.moveToFirst()) parseEmergencyEventFromCursor(it) else null
            }
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting active emergency event for user: $userId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get emergency events with pagination
     */
    fun getEmergencyEvents(userId: String, limit: Int = 20, offset: Int = 0): Result<List<EmergencyEvent>> {
        return try {
            val events = mutableListOf<EmergencyEvent>()
            val db = readableDatabase
            val cursor = db.query(
                TABLE_EVENTS,
                null,
                "$COL_EVENT_USER_ID = ?",
                arrayOf(userId),
                null,
                null,
                "$COL_EVENT_TRIGGERED DESC",
                "$offset,$limit"
            )
            
            cursor.use {
                while (it.moveToNext()) {
                    try {
                        events.add(parseEmergencyEventFromCursor(it))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing emergency event", e)
                        // Continue with other events
                    }
                }
            }
            Result.success(events)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting emergency events", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get total count of contacts
     */
    fun getContactCount(): Result<Int> {
        return try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_CONTACTS", null)
            val count = cursor.use {
                if (it.moveToFirst()) it.getInt(0) else 0
            }
            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting contact count", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get total count of emergency events for user
     */
    fun getEventCount(userId: String): Result<Int> {
        return try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM $TABLE_EVENTS WHERE $COL_EVENT_USER_ID = ?",
                arrayOf(userId)
            )
            val count = cursor.use {
                if (it.moveToFirst()) it.getInt(0) else 0
            }
            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting event count", e)
            Result.failure(e)
        }
    }

    fun updateEventStatus(eventId: String, status: EmergencyConstants.EmergencyStatus): Result<Int> {
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_EVENT_STATUS, status.name)
                if (status == EmergencyConstants.EmergencyStatus.CANCELLED_BY_USER ||
                    status == EmergencyConstants.EmergencyStatus.RESOLVED_BY_CONTACT) {
                    put(COL_EVENT_RESOLVED, System.currentTimeMillis())
                }
            }
            val result = db.update(TABLE_EVENTS, values, "$COL_EVENT_ID = ?", arrayOf(eventId))
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating event status: $eventId", e)
            Result.failure(e)
        }
    }

    // Helper Methods
    private fun serializeMedications(medications: List<Medication>): String {
        val jsonArray = JSONArray()
        medications.forEach { med ->
            jsonArray.put(JSONObject().apply {
                put("name", med.name)
                put("dosage", med.dosage)
                put("frequency", med.frequency)
                put("affectsHeartRate", med.affectsHeartRate)
                put("criticalMedication", med.criticalMedication)
            })
        }
        return jsonArray.toString()
    }

    private fun deserializeMedications(json: String?): List<Medication> {
        if (json.isNullOrEmpty()) return emptyList()
        val medications = mutableListOf<Medication>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                medications.add(
                    Medication(
                        name = obj.getString("name"),
                        dosage = obj.getString("dosage"),
                        frequency = obj.getString("frequency"),
                        affectsHeartRate = obj.optBoolean("affectsHeartRate", false),
                        criticalMedication = obj.optBoolean("criticalMedication", false)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return medications
    }

    private fun parseJsonArray(json: String?): List<String> {
        if (json.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun serializeHealthData(healthData: HealthData): String {
        return JSONObject().apply {
            put("currentHeartRate", healthData.currentHeartRate)
            put("normalHeartRate", healthData.normalHeartRate)
            put("riskScore", healthData.riskScore)
            put("alertReason", healthData.alertReason)
        }.toString()
    }

    private fun serializeLocationData(locationData: LocationData): String {
        return JSONObject().apply {
            put("latitude", locationData.latitude)
            put("longitude", locationData.longitude)
            put("address", locationData.address)
            put("accuracy", locationData.accuracy)
        }.toString()
    }

    private fun parseEmergencyEventFromCursor(cursor: android.database.Cursor): EmergencyEvent {
        try {
            Log.d(TAG, "📖 Parsing emergency event from database cursor...")
            
            val eventId = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_ID))
            val userId = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_USER_ID))
            Log.d(TAG, "   Event ID: $eventId, User ID: $userId")
            
            // Parse health data from JSON with null safety
            val healthDataJson = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(COL_EVENT_HEALTH_DATA))
            val healthData = try {
                deserializeHealthData(healthDataJson)
            } catch (e: Exception) {
                Log.w(TAG, "   Failed to parse health data, using defaults", e)
                HealthData(currentHeartRate = 0, normalHeartRate = 70, alertReason = "Parse error")
            }
            
            // Parse location data from JSON with null safety
            val locationDataJson = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(COL_EVENT_LOCATION_DATA))
            val locationData = try {
                deserializeLocationData(locationDataJson)
            } catch (e: Exception) {
                Log.w(TAG, "   Failed to parse location data, using defaults", e)
                LocationData(latitude = 0.0, longitude = 0.0, accuracy = 0f, address = "Unknown")
            }
            
            // Get user medical info with error handling
            val medicalInfo = try {
                getMedicalInfo(userId).getOrNull() ?: run {
                    Log.d(TAG, "   No medical info found for user, creating default")
                    MedicalInfo(userId = userId)
                }
            } catch (e: Exception) {
                Log.w(TAG, "   Error retrieving medical info, using default", e)
                MedicalInfo(userId = userId)
            }
            
            // Get emergency contacts with error handling
            val contacts = try {
                getAllContacts().getOrNull() ?: emptyList()
            } catch (e: Exception) {
                Log.w(TAG, "   Error retrieving contacts, using empty list", e)
                emptyList()
            }
            
            // Parse timestamps safely
            val triggeredTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_EVENT_TRIGGERED))
            val resolvedTimestamp = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(COL_EVENT_RESOLVED))
            
            // Parse boolean flags safely
            val userResponded = cursor.getInt(cursor.getColumnIndexOrThrow(COL_EVENT_USER_RESPONDED)) == 1
            val userCancelled = cursor.getInt(cursor.getColumnIndexOrThrow(COL_EVENT_USER_CANCELLED)) == 1
            val backendNotified = cursor.getInt(cursor.getColumnIndexOrThrow(COL_EVENT_BACKEND_NOTIFIED)) == 1
            
            // Create UserInfo with null safety
            val userInfo = UserInfo(
                userId = userId,
                name = medicalInfo.doctorName ?: "User",
                age = 0, // Would calculate from DOB if stored
                phoneNumber = "",
                medicalInfo = medicalInfo
            )
            
            // Parse emergency type and status with fallback
            val emergencyType = try {
                EmergencyConstants.EmergencyType.valueOf(
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_TYPE))
                )
            } catch (e: Exception) {
                Log.w(TAG, "   Invalid emergency type, using MANUAL_TRIGGER", e)
                EmergencyConstants.EmergencyType.MANUAL_TRIGGER
            }
            
            val status = try {
                EmergencyConstants.EmergencyStatus.valueOf(
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_STATUS))
                )
            } catch (e: Exception) {
                Log.w(TAG, "   Invalid status, using INITIATED", e)
                EmergencyConstants.EmergencyStatus.INITIATED
            }
            
            val event = EmergencyEvent(
                id = eventId,
                userId = userId,
                emergencyType = emergencyType,
                status = status,
                currentPhase = cursor.getInt(cursor.getColumnIndexOrThrow(COL_EVENT_PHASE)),
                triggeredTimestamp = triggeredTimestamp,
                resolvedTimestamp = resolvedTimestamp,
                healthData = healthData,
                locationData = locationData,
                userInfo = userInfo,
                emergencyContacts = contacts,
                userResponded = userResponded,
                userCancelled = userCancelled,
                backendNotified = backendNotified,
                twilioCallSid = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(COL_EVENT_TWILIO_SID))
            )
            
            Log.d(TAG, "Successfully parsed emergency event: ${event.id}")
            return event
            
        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL: Failed to parse emergency event from cursor", e)
            Log.e(TAG, "   Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "   Exception message: ${e.message}")
            
            // Instead of throwing, return a minimal valid event to prevent crashes
            // This allows the app to continue functioning even with corrupted data
            val fallbackId = try {
                cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_ID))
            } catch (ex: Exception) {
                "corrupted_event_${System.currentTimeMillis()}"
            }
            
            val fallbackUserId = try {
                cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_USER_ID))
            } catch (ex: Exception) {
                "unknown_user"
            }
            
            Log.w(TAG, "Returning fallback emergency event to prevent crash")
            return EmergencyEvent(
                id = fallbackId,
                userId = fallbackUserId,
                emergencyType = EmergencyConstants.EmergencyType.MANUAL_TRIGGER,
                status = EmergencyConstants.EmergencyStatus.RESOLVED_BY_CONTACT,
                currentPhase = 0,
                triggeredTimestamp = System.currentTimeMillis(),
                resolvedTimestamp = System.currentTimeMillis(),
                healthData = HealthData(currentHeartRate = 0, normalHeartRate = 70, alertReason = "Data corrupted"),
                locationData = LocationData(latitude = 0.0, longitude = 0.0, accuracy = 0f, address = "Unknown"),
                userInfo = UserInfo(
                    userId = fallbackUserId,
                    name = "Unknown",
                    age = 0,
                    phoneNumber = "",
                    medicalInfo = MedicalInfo(userId = fallbackUserId)
                ),
                emergencyContacts = emptyList(),
                userResponded = false,
                userCancelled = false,
                backendNotified = false,
                twilioCallSid = null
            )
        }
    }
    
    private fun deserializeHealthData(json: String?): HealthData {
        if (json.isNullOrEmpty()) {
            return HealthData(currentHeartRate = 0, normalHeartRate = 70, alertReason = "No data")
        }
        return try {
            val obj = JSONObject(json)
            HealthData(
                currentHeartRate = obj.optInt("currentHeartRate", 0),
                normalHeartRate = obj.optInt("normalHeartRate", 70),
                riskScore = obj.optDouble("riskScore", 0.0).toFloat(),
                alertReason = obj.optString("alertReason", "")
            )
        } catch (e: Exception) {
            Log.e("EmergencyDatabaseHelper", "Error deserializing health data", e)
            HealthData(currentHeartRate = 0, normalHeartRate = 70, alertReason = "Parse error")
        }
    }
    
    private fun deserializeLocationData(json: String?): LocationData {
        if (json.isNullOrEmpty()) {
            return LocationData(latitude = 0.0, longitude = 0.0, accuracy = 0f, address = "Unknown")
        }
        return try {
            val obj = JSONObject(json)
            LocationData(
                latitude = obj.optDouble("latitude", 0.0),
                longitude = obj.optDouble("longitude", 0.0),
                address = obj.optString("address", "Unknown"),
                accuracy = obj.optDouble("accuracy", 0.0).toFloat()
            )
        } catch (e: Exception) {
            Log.e("EmergencyDatabaseHelper", "Error deserializing location data", e)
            LocationData(latitude = 0.0, longitude = 0.0, accuracy = 0f, address = "Parse error")
        }
    }
    
    private fun android.database.Cursor.getLongOrNull(columnIndex: Int): Long? {
        return if (isNull(columnIndex)) null else getLong(columnIndex)
    }
    
    private fun android.database.Cursor.getStringOrNull(columnIndex: Int): String? {
        return if (isNull(columnIndex)) null else getString(columnIndex)
    }
}


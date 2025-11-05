package com.rescuemate.emergency.data.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
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
        // Handle database upgrades
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RESPONSES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ATTEMPTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EVENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MEDICAL_INFO")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CONTACTS")
        onCreate(db)
    }

    // Emergency Contact Operations
    fun insertContact(contact: EmergencyContact): Long {
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
        return db.insertWithOnConflict(TABLE_CONTACTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAllContacts(): List<EmergencyContact> {
        val contacts = mutableListOf<EmergencyContact>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_CONTACTS,
            null,
            null,
            null,
            null,
            null,
            "$COL_CONTACT_PRIORITY ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
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
            }
        }
        return contacts
    }

    fun deleteContact(contactId: String): Int {
        val db = writableDatabase
        return db.delete(TABLE_CONTACTS, "$COL_CONTACT_ID = ?", arrayOf(contactId))
    }

    // Medical Info Operations
    fun insertOrUpdateMedicalInfo(medicalInfo: MedicalInfo): Long {
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
        return db.insertWithOnConflict(TABLE_MEDICAL_INFO, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getMedicalInfo(userId: String): MedicalInfo? {
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

        return cursor.use {
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
    }

    // Emergency Event Operations
    fun insertEmergencyEvent(event: EmergencyEvent): Long {
        val db = writableDatabase
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
        return db.insertWithOnConflict(TABLE_EVENTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getActiveEmergencyEvent(userId: String): EmergencyEvent? {
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

        return cursor.use {
            if (it.moveToFirst()) parseEmergencyEventFromCursor(it) else null
        }
    }

    fun updateEventStatus(eventId: String, status: EmergencyConstants.EmergencyStatus) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_EVENT_STATUS, status.name)
            if (status == EmergencyConstants.EmergencyStatus.CANCELLED_BY_USER ||
                status == EmergencyConstants.EmergencyStatus.RESOLVED_BY_CONTACT) {
                put(COL_EVENT_RESOLVED, System.currentTimeMillis())
            }
        }
        db.update(TABLE_EVENTS, values, "$COL_EVENT_ID = ?", arrayOf(eventId))
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
        // Simplified parsing - would need full implementation
        return EmergencyEvent(
            id = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_ID)),
            userId = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_USER_ID)),
            emergencyType = EmergencyConstants.EmergencyType.valueOf(
                cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_TYPE))
            ),
            status = EmergencyConstants.EmergencyStatus.valueOf(
                cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_STATUS))
            ),
            currentPhase = cursor.getInt(cursor.getColumnIndexOrThrow(COL_EVENT_PHASE)),
            triggeredTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_EVENT_TRIGGERED)),
            healthData = HealthData(currentHeartRate = 0, normalHeartRate = 70, alertReason = ""),
            locationData = LocationData(latitude = 0.0, longitude = 0.0, accuracy = 0f),
            userInfo = UserInfo(userId = "", name = "", age = 0, phoneNumber = "", medicalInfo = MedicalInfo(userId = "")),
            emergencyContacts = emptyList()
        )
    }
}


/**
 * Emergency Database Model
 */

const mongoose = require('mongoose');

const contactAttemptSchema = new mongoose.Schema({
    contactPhone: { type: String, required: true },
    contactName: String,
    attemptType: {
        type: String,
        enum: ['VOICE', 'SMS', 'EMAIL', 'PUSH'],
        required: true
    },
    timestamp: { type: Date, default: Date.now },
    success: { type: Boolean, default: false },
    callSid: String,
    messageSid: String,
    failureReason: String
});

const responseSchema = new mongoose.Schema({
    contactPhone: { type: String, required: true },
    response: {
        type: String,
        enum: ['USER_FINE', 'CHECKING_ON_USER', 'NEED_HELP', 'NO_RESPONSE'],
        required: true
    },
    timestamp: { type: Date, default: Date.now },
    source: {
        type: String,
        enum: ['PHONE_CALL', 'SMS_REPLY', 'WEB_INTERFACE', 'APP'],
        default: 'PHONE_CALL'
    },
    notes: String
});

const emergencySchema = new mongoose.Schema({
    userId: {
        type: String,
        required: true,
        index: true
    },
    emergencyType: {
        type: String,
        enum: [
            'CARDIAC_ALERT',
            'MEDICAL_EMERGENCY',
            'UNRESPONSIVE',
            'MANUAL_TRIGGER',
            'SCHEDULED_CHECKIN_MISSED',
            'FALL_DETECTED',
            'ABNORMAL_VITALS'
        ],
        required: true
    },
    status: {
        type: String,
        enum: [
            'INITIATED',
            'ACTIVE',
            'PHASE_1',
            'PHASE_2',
            'PHASE_3',
            'RESOLVED',
            'CANCELLED',
            'FAILED'
        ],
        default: 'INITIATED',
        index: true
    },
    phase: {
        type: Number,
        min: 1,
        max: 3,
        default: 1
    },

    // Health Data
    healthData: {
        currentHeartRate: Number,
        normalHeartRate: Number,
        heartRateTrend: [Number],
        riskScore: Number,
        alertReason: String,
        activityLevel: String
    },

    // Location Data
    location: {
        latitude: { type: Number, required: true },
        longitude: { type: Number, required: true },
        address: String,
        accuracy: Number,
        googleMapsLink: String
    },

    // User Information
    userInfo: {
        name: { type: String, required: true },
        age: Number,
        phoneNumber: String,
        medicalInfo: {
            bloodType: String,
            knownConditions: [String],
            currentMedications: [String],
            allergies: [String],
            baselineHeartRate: Number,
            emergencyNotes: String
        }
    },

    // Contact Tracking
    contactAttempts: [contactAttemptSchema],
    responses: [responseSchema],

    // Timestamps
    triggeredAt: {
        type: Date,
        default: Date.now,
        required: true
    },
    phase1StartTime: Date,
    phase2StartTime: Date,
    phase3StartTime: Date,
    resolvedAt: Date,

    // Resolution Info
    resolvedBy: {
        type: String,
        enum: [
            'USER_CANCELLATION',
            'CONTACT_CONFIRMATION',
            'CONTACT_VOICE_RESPONSE',
            'CONTACT_SMS_RESPONSE',
            'SYSTEM_TIMEOUT',
            'MANUAL_INTERVENTION'
        ]
    },
    cancelReason: String,

    // Metadata
    deviceInfo: String,
    appVersion: String,
    batteryLevel: Number
}, {
    timestamps: true // Adds createdAt and updatedAt
});

// Indexes for performance
emergencySchema.index({ userId: 1, triggeredAt: -1 });
emergencySchema.index({ status: 1, triggeredAt: -1 });
emergencySchema.index({ 'location.latitude': 1, 'location.longitude': 1 });

// Virtual for duration
emergencySchema.virtual('durationMinutes').get(function() {
    if (!this.resolvedAt) {
        return Math.round((Date.now() - this.triggeredAt) / 60000);
    }
    return Math.round((this.resolvedAt - this.triggeredAt) / 60000);
});

// Method to check if emergency is active
emergencySchema.methods.isActive = function() {
    return ['INITIATED', 'ACTIVE', 'PHASE_1', 'PHASE_2', 'PHASE_3'].includes(this.status);
};

// Method to get successful responses
emergencySchema.methods.getSuccessfulResponses = function() {
    return this.responses.filter(r =>
        r.response === 'USER_FINE' || r.response === 'CHECKING_ON_USER'
    );
};

// Static method to get active emergencies
emergencySchema.statics.findActiveEmergencies = function() {
    return this.find({
        status: { $in: ['INITIATED', 'ACTIVE', 'PHASE_1', 'PHASE_2', 'PHASE_3'] }
    }).sort({ triggeredAt: -1 });
};

// Static method to get user's emergency history
emergencySchema.statics.findUserHistory = function(userId, limit = 10) {
    return this.find({ userId })
        .sort({ triggeredAt: -1 })
        .limit(limit);
};

const Emergency = mongoose.model('Emergency', emergencySchema);

module.exports = Emergency;


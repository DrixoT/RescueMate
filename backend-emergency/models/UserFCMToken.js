/**
 * User FCM Token Model
 * Stores FCM tokens for logged-in users to enable push notifications
 */

const mongoose = require('mongoose');
const logger = require('../utils/logger');

const userFCMTokenSchema = new mongoose.Schema({
    userId: {
        type: String,
        required: true,
        index: true
    },
    fcmToken: {
        type: String,
        required: true,
        unique: true,
        index: true
    },
    phoneNumber: {
        type: String,
        index: true
    },
    email: {
        type: String,
        lowercase: true,
        index: true
    },
    deviceId: {
        type: String
    },
    lastActive: {
        type: Date,
        default: Date.now,
        index: true
    }
}, {
    timestamps: true // Adds createdAt and updatedAt
});

// Compound index for efficient lookups
userFCMTokenSchema.index({ phoneNumber: 1, email: 1 });
userFCMTokenSchema.index({ userId: 1, lastActive: -1 });

// Static method to find tokens by phone or email
userFCMTokenSchema.statics.findByContact = async function(phoneNumber, email) {
    const normalizedPhone = phoneNumber ? normalizePhoneNumber(phoneNumber) : null;
    const normalizedEmail = email ? email.toLowerCase() : null;
    
    logger.debug('Finding FCM tokens by contact', {
        inputPhone: phoneNumber,
        normalizedPhone,
        inputEmail: email,
        normalizedEmail
    });
    
    if (!normalizedPhone && !normalizedEmail) {
        logger.warn('No phone or email provided for FCM token lookup');
        return [];
    }
    
    const query = {
        $or: []
    };
    
    if (normalizedPhone) {
        query.$or.push({ phoneNumber: normalizedPhone });
    }
    
    if (normalizedEmail) {
        query.$or.push({ email: normalizedEmail });
    }
    
    logger.debug('FCM token query', {
        query: JSON.stringify(query)
    });
    
    const results = await this.find(query);
    
    logger.debug('FCM token query results', {
        phone: normalizedPhone,
        email: normalizedEmail,
        count: results.length,
        results: results.map(r => ({
            userId: r.userId,
            phone: r.phoneNumber,
            email: r.email,
            lastActive: r.lastActive
        }))
    });
    
    return results;
};

// Normalize phone number to E.164 format
// This must match the Android normalization in FCMRepository.kt
function normalizePhoneNumber(phone) {
    if (!phone) return null;
    
    // Remove all non-digits first
    let cleaned = phone.replace(/\D/g, '');
    
    if (cleaned.length === 0) {
        return null;
    }
    
    // Add +1 for US numbers if not present
    if (cleaned.length === 10) {
        // 10-digit US number: add +1 prefix
        return '+1' + cleaned;
    } else if (cleaned.startsWith('1') && cleaned.length === 11) {
        // 11-digit number starting with 1: add + prefix
        return '+' + cleaned;
    } else if (phone.startsWith('+')) {
        // Already has + prefix: clean and return with +
        // This handles cases like "+1 (555) 123-4567" -> "+15551234567"
        return '+' + cleaned;
    } else {
        // Other format: add + prefix
        return '+' + cleaned;
    }
}

const UserFCMToken = mongoose.model('UserFCMToken', userFCMTokenSchema);

module.exports = UserFCMToken;

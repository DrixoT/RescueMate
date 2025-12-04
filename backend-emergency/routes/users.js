/**
 * User API Routes
 * Handles user-related endpoints including FCM token registration
 */

const express = require('express');
const router = express.Router();
const UserFCMToken = require('../models/UserFCMToken');
const logger = require('../utils/logger');

/**
 * POST /api/users/fcm-token
 * Register or update FCM token for logged-in user
 */
router.post('/fcm-token', async (req, res) => {
    try {
        const {
            fcmToken,
            userId,
            email,
            phoneNumber,
            deviceId
        } = req.body;

        // Validate required fields
        if (!fcmToken || !userId) {
            return res.status(400).json({
                success: false,
                error: 'FCM token and user ID are required'
            });
        }

        // Normalize phone number and email
        // Handle empty string as null for consistency
        const normalizedPhone = phoneNumber && phoneNumber.trim() !== '' 
            ? normalizePhoneNumber(phoneNumber) 
            : null;
        const normalizedEmail = email && email.trim() !== '' 
            ? email.toLowerCase() 
            : null;

        logger.info('FCM token registration request', {
            userId,
            email: normalizedEmail,
            phone: normalizedPhone,
            hasToken: !!fcmToken,
            tokenPreview: fcmToken ? fcmToken.substring(0, 20) + '...' : null
        });

        // Check if token already exists
        let tokenRecord = await UserFCMToken.findOne({ fcmToken });

        if (tokenRecord) {
            // Update existing token
            tokenRecord.userId = userId;
            tokenRecord.phoneNumber = normalizedPhone;
            tokenRecord.email = normalizedEmail;
            tokenRecord.deviceId = deviceId;
            tokenRecord.lastActive = new Date();
            await tokenRecord.save();

            logger.info('FCM token updated', {
                userId,
                fcmToken: fcmToken.substring(0, 20) + '...'
            });
        } else {
            // Create new token record
            tokenRecord = new UserFCMToken({
                fcmToken,
                userId,
                phoneNumber: normalizedPhone,
                email: normalizedEmail,
                deviceId,
                lastActive: new Date()
            });
            await tokenRecord.save();

            logger.info('FCM token registered', {
                userId,
                fcmToken: fcmToken.substring(0, 20) + '...'
            });
        }

        // Remove old tokens for this user (keep only the latest 5 per user)
        const userTokens = await UserFCMToken.find({ userId })
            .sort({ lastActive: -1 });
        
        if (userTokens.length > 5) {
            const tokensToRemove = userTokens.slice(5);
            await UserFCMToken.deleteMany({
                _id: { $in: tokensToRemove.map(t => t._id) }
            });
            logger.info(`Removed ${tokensToRemove.length} old FCM tokens for user ${userId}`);
        }

        res.json({
            success: true,
            message: 'FCM token registered successfully'
        });

    } catch (error) {
        logger.error('FCM token registration error:', error);
        res.status(500).json({
            success: false,
            error: error.message || 'Internal server error'
        });
    }
});

/**
 * GET /api/users/fcm-token/:userId
 * Get FCM tokens for a specific user (debug endpoint)
 */
router.get('/fcm-token/:userId', async (req, res) => {
    try {
        const { userId } = req.params;
        
        const tokens = await UserFCMToken.find({ userId })
            .sort({ lastActive: -1 });
        
        logger.info('FCM tokens retrieved for user', {
            userId,
            count: tokens.length
        });
        
        res.json({
            success: true,
            userId,
            count: tokens.length,
            tokens: tokens.map(t => ({
                fcmToken: t.fcmToken.substring(0, 20) + '...',
                phoneNumber: t.phoneNumber,
                email: t.email,
                deviceId: t.deviceId,
                lastActive: t.lastActive,
                createdAt: t.createdAt
            }))
        });
    } catch (error) {
        logger.error('Error retrieving FCM tokens', error);
        res.status(500).json({
            success: false,
            error: error.message || 'Internal server error'
        });
    }
});

/**
 * GET /api/users/fcm-token/check?phone=...&email=...
 * Check if FCM tokens exist for phone/email (debug endpoint)
 */
router.get('/fcm-token/check', async (req, res) => {
    try {
        const { phone, email } = req.query;
        
        if (!phone && !email) {
            return res.status(400).json({
                success: false,
                error: 'Phone or email query parameter is required'
            });
        }
        
        const normalizedPhone = phone ? normalizePhoneNumber(phone) : null;
        const normalizedEmail = email ? email.toLowerCase() : null;
        
        logger.info('Checking FCM tokens', {
            inputPhone: phone,
            normalizedPhone,
            inputEmail: email,
            normalizedEmail
        });
        
        const tokens = await UserFCMToken.findByContact(normalizedPhone, normalizedEmail);
        
        logger.info('FCM token check results', {
            phone: normalizedPhone,
            email: normalizedEmail,
            count: tokens.length
        });
        
        res.json({
            success: true,
            query: {
                phone: normalizedPhone,
                email: normalizedEmail
            },
            count: tokens.length,
            tokens: tokens.map(t => ({
                userId: t.userId,
                fcmToken: t.fcmToken.substring(0, 20) + '...',
                phoneNumber: t.phoneNumber,
                email: t.email,
                deviceId: t.deviceId,
                lastActive: t.lastActive
            }))
        });
    } catch (error) {
        logger.error('Error checking FCM tokens', error);
        res.status(500).json({
            success: false,
            error: error.message || 'Internal server error'
        });
    }
});

/**
 * DELETE /api/users/fcm-token
 * Remove FCM token (e.g., on logout)
 */
router.delete('/fcm-token', async (req, res) => {
    try {
        const { fcmToken, userId } = req.body;

        if (!fcmToken && !userId) {
            return res.status(400).json({
                success: false,
                error: 'FCM token or user ID is required'
            });
        }

        const query = {};
        if (fcmToken) query.fcmToken = fcmToken;
        if (userId) query.userId = userId;

        await UserFCMToken.deleteMany(query);

        logger.info('FCM token(s) removed', { fcmToken, userId });

        res.json({
            success: true,
            message: 'FCM token removed successfully'
        });

    } catch (error) {
        logger.error('FCM token removal error:', error);
        res.status(500).json({
            success: false,
            error: error.message || 'Internal server error'
        });
    }
});

/**
 * Normalize phone number to E.164 format
 * This must match the Android normalization in FCMRepository.kt and UserFCMToken.js
 */
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

module.exports = router;

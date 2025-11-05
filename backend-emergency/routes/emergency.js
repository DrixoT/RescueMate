/**
 * Emergency API Routes
 * Handles emergency contact alerts and emergency workflow
 */

const express = require('express');
const router = express.Router();
const twilioService = require('../services/twilioService');
const Emergency = require('../models/Emergency');
const logger = require('../utils/logger');

/**
 * POST /api/emergency/contact-alert
 * Send emergency alert to all emergency contacts
 */
router.post('/contact-alert', async (req, res) => {
    try {
        const {
            userId,
            emergencyType,
            healthData,
            location,
            userInfo,
            timestamp
        } = req.body;

        // Validate required fields
        if (!userId || !emergencyType || !location || !userInfo) {
            return res.status(400).json({
                success: false,
                error: 'Missing required fields'
            });
        }

        // Create emergency record
        const emergency = new Emergency({
            userId,
            emergencyType,
            status: 'ACTIVE',
            phase: 2,
            healthData,
            location,
            userInfo,
            triggeredAt: new Date(timestamp),
            contactAttempts: [],
            responses: []
        });

        await emergency.save();

        logger.info(`Emergency alert created: ${emergency._id}`, {
            userId,
            emergencyType
        });

        res.json({
            success: true,
            message: 'Emergency alert created successfully',
            emergencyId: emergency._id.toString()
        });

    } catch (error) {
        logger.error('Contact alert error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

/**
 * POST /api/emergency/contact-call
 * Initiate Twilio call to emergency contact
 */
router.post('/contact-call', async (req, res) => {
    try {
        const {
            userId,
            emergencyId,
            contactPhone,
            contactName,
            messageType,
            healthSummary,
            locationLink,
            emergencyDetails
        } = req.body;

        // Validate
        if (!emergencyId || !contactPhone || !messageType) {
            return res.status(400).json({
                success: false,
                error: 'Missing required fields'
            });
        }

        // Find emergency record
        const emergency = await Emergency.findById(emergencyId);
        if (!emergency) {
            return res.status(404).json({
                success: false,
                error: 'Emergency not found'
            });
        }

        let result;

        if (messageType === 'voice') {
            // Initiate voice call via Twilio
            result = await twilioService.makeEmergencyCall(
                contactPhone,
                contactName,
                emergency.userInfo.name,
                healthSummary,
                locationLink,
                emergencyDetails,
                emergencyId
            );
        } else if (messageType === 'sms') {
            // Send SMS via Twilio
            result = await twilioService.sendEmergencySMS(
                contactPhone,
                contactName,
                emergency.userInfo.name,
                healthSummary,
                locationLink,
                emergencyId
            );
        } else {
            return res.status(400).json({
                success: false,
                error: 'Invalid message type'
            });
        }

        // Record contact attempt
        emergency.contactAttempts.push({
            contactPhone,
            contactName,
            attemptType: messageType.toUpperCase(),
            timestamp: new Date(),
            success: result.success,
            callSid: result.callSid || result.messageSid,
            failureReason: result.error
        });

        await emergency.save();

        logger.info(`Contact ${messageType} initiated`, {
            emergencyId,
            contactPhone,
            success: result.success
        });

        res.json({
            success: result.success,
            message: result.success ? `${messageType} sent successfully` : result.error,
            callSid: result.callSid || result.messageSid,
            emergencyId: emergencyId
        });

    } catch (error) {
        logger.error('Contact call error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

/**
 * POST /api/emergency/contact-response
 * Record emergency contact response (user safe/not safe)
 */
router.post('/contact-response', async (req, res) => {
    try {
        const {
            emergencyId,
            contactPhone,
            response,
            timestamp,
            notes
        } = req.body;

        // Validate
        if (!emergencyId || !contactPhone || !response) {
            return res.status(400).json({
                success: false,
                error: 'Missing required fields'
            });
        }

        // Find emergency
        const emergency = await Emergency.findById(emergencyId);
        if (!emergency) {
            return res.status(404).json({
                success: false,
                error: 'Emergency not found'
            });
        }

        // Record response
        emergency.responses.push({
            contactPhone,
            response: response.toUpperCase(),
            timestamp: new Date(timestamp),
            notes,
            source: 'PHONE_CALL'
        });

        // Update emergency status if user is confirmed safe
        if (response.toLowerCase() === 'user_fine') {
            emergency.status = 'RESOLVED';
            emergency.resolvedAt = new Date();
            emergency.resolvedBy = 'CONTACT_CONFIRMATION';

            // Notify all other contacts of resolution
            await twilioService.notifyContactsOfResolution(
                emergency.contactAttempts.map(a => a.contactPhone),
                emergency.userInfo.name,
                contactPhone
            );
        }

        await emergency.save();

        logger.info(`Contact response recorded`, {
            emergencyId,
            contactPhone,
            response
        });

        res.json({
            success: true,
            message: 'Response recorded successfully',
            emergencyStatus: emergency.status
        });

    } catch (error) {
        logger.error('Contact response error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

/**
 * GET /api/emergency/:emergencyId/status
 * Get emergency status
 */
router.get('/:emergencyId/status', async (req, res) => {
    try {
        const { emergencyId } = req.params;

        const emergency = await Emergency.findById(emergencyId);
        if (!emergency) {
            return res.status(404).json({
                success: false,
                error: 'Emergency not found'
            });
        }

        res.json({
            success: true,
            emergency: {
                id: emergency._id,
                userId: emergency.userId,
                status: emergency.status,
                phase: emergency.phase,
                emergencyType: emergency.emergencyType,
                triggeredAt: emergency.triggeredAt,
                resolvedAt: emergency.resolvedAt,
                contactAttempts: emergency.contactAttempts.length,
                responses: emergency.responses.length
            }
        });

    } catch (error) {
        logger.error('Status check error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

/**
 * GET /api/emergency/call-status/:callSid
 * Get Twilio call status
 */
router.get('/call-status/:callSid', async (req, res) => {
    try {
        const { callSid } = req.params;

        const status = await twilioService.getCallStatus(callSid);

        res.json({
            success: true,
            callSid,
            status: status.status,
            duration: status.duration,
            errorCode: status.errorCode,
            errorMessage: status.errorMessage
        });

    } catch (error) {
        logger.error('Call status error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

/**
 * POST /api/emergency/cancel
 * Cancel an active emergency
 */
router.post('/cancel', async (req, res) => {
    try {
        const { emergencyId, reason } = req.body;

        if (!emergencyId) {
            return res.status(400).json({
                success: false,
                error: 'Emergency ID required'
            });
        }

        const emergency = await Emergency.findById(emergencyId);
        if (!emergency) {
            return res.status(404).json({
                success: false,
                error: 'Emergency not found'
            });
        }

        emergency.status = 'CANCELLED';
        emergency.resolvedAt = new Date();
        emergency.resolvedBy = 'USER_CANCELLATION';
        emergency.cancelReason = reason || 'User cancelled';

        await emergency.save();

        // Notify contacts of cancellation
        await twilioService.notifyContactsOfCancellation(
            emergency.contactAttempts.map(a => a.contactPhone),
            emergency.userInfo.name
        );

        logger.info(`Emergency cancelled: ${emergencyId}`);

        res.json({
            success: true,
            message: 'Emergency cancelled successfully'
        });

    } catch (error) {
        logger.error('Cancel emergency error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

/**
 * RESERVED FOR FUTURE - Phase 3
 * POST /api/emergency/services
 * Call emergency services (911) via Twilio
 * WARNING: This will incur $75+ charges
 */
router.post('/services', async (req, res) => {
    res.status(501).json({
        success: false,
        error: 'Emergency services integration reserved for Phase 3',
        message: 'This feature is not yet implemented. Phase 2 (emergency contacts) must be tested and validated first.',
        estimatedCost: '$75+ per emergency services call',
        requiredSetup: [
            'E911 address registration with Twilio',
            'Emergency services compliance documentation',
            'Legal liability coverage',
            'Production testing with test emergency numbers'
        ]
    });
});

module.exports = router;


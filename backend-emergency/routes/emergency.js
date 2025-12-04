/**
 * Emergency API Routes
 * Handles emergency contact alerts and emergency workflow
 */

const express = require('express');
const router = express.Router();
const twilioService = require('../services/twilioService');
const elevenLabsService = require('../services/elevenLabsService');
const fcmService = require('../services/fcmService');
const Emergency = require('../models/Emergency');
const UserFCMToken = require('../models/UserFCMToken');
const logger = require('../utils/logger');

/**
 * GET /api/emergency/conversation-token
 * Get conversation token for ElevenLabs private agent
 * Required for starting conversations with private agents from mobile app
 */
router.get('/conversation-token', async (req, res) => {
    try {
        const { agentId } = req.query;

        if (!agentId) {
            return res.status(400).json({
                success: false,
                error: 'agentId query parameter is required'
            });
        }

        logger.info(`Conversation token requested for agent: ${agentId}`);

        const result = await elevenLabsService.getConversationToken(agentId);

        if (!result.success) {
            logger.error(`Failed to get conversation token: ${result.error}`);
            return res.status(500).json({
                success: false,
                error: result.error
            });
        }

        res.json({
            success: true,
            token: result.token
        });

    } catch (error) {
        logger.error('Conversation token endpoint error:', error);
        res.status(500).json({
            success: false,
            error: error.message || 'Internal server error'
        });
    }
});

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
            timestamp,
            emergencyContacts
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

        // Send FCM notifications to logged-in contacts
        if (emergencyContacts && Array.isArray(emergencyContacts) && emergencyContacts.length > 0) {
            try {
                logger.info('Processing FCM notifications for emergency contacts', {
                    totalContacts: emergencyContacts.length,
                    contacts: emergencyContacts.map(c => ({
                        name: c.name,
                        phone: c.phoneNumber || c.phone,
                        email: c.email
                    }))
                });
                
                const fcmTokens = [];
                
                // Query for FCM tokens matching emergency contacts
                for (const contact of emergencyContacts) {
                    // Handle both phoneNumber and phone fields, filter out empty strings
                    const phoneNumber = (contact.phoneNumber || contact.phone || '').trim();
                    const email = (contact.email || '').trim();
                    
                    logger.info('Searching for FCM tokens', {
                        contactName: contact.name,
                        phoneNumber: phoneNumber || '(empty)',
                        email: email || '(empty)'
                    });
                    
                    // Only search if we have at least phone or email (non-empty)
                    if (phoneNumber || email) {
                        const tokens = await UserFCMToken.findByContact(
                            phoneNumber || null, 
                            email || null
                        );
                        logger.info('FCM tokens found', {
                            contactName: contact.name,
                            tokensFound: tokens.length,
                            tokens: tokens.map(t => ({
                                userId: t.userId,
                                phone: t.phoneNumber,
                                email: t.email,
                                lastActive: t.lastActive
                            }))
                        });
                        
                        tokens.forEach(token => {
                            if (!fcmTokens.includes(token.fcmToken)) {
                                fcmTokens.push(token.fcmToken);
                            }
                        });
                    } else {
                        logger.warn('Contact has no phone or email for FCM matching', {
                            contactName: contact.name
                        });
                    }
                }

                logger.info('FCM notification summary', {
                    totalTokens: fcmTokens.length,
                    tokenPreview: fcmTokens.length > 0 ? fcmTokens.map(t => t.substring(0, 20) + '...') : []
                });

                // Send FCM notifications
                if (fcmTokens.length > 0) {
                    const emergencyData = {
                        emergencyId: emergency._id.toString(),
                        userId,
                        userName: userInfo.name || 'User',
                        emergencyType,
                        alertReason: healthData?.alertReason || 'Emergency protocol initiated',
                        location: location.googleMapsLink || location.address || 'Location unavailable',
                        timestamp: timestamp || new Date().toISOString()
                    };

                    logger.info('Sending FCM notifications', {
                        emergencyId: emergency._id,
                        totalTokens: fcmTokens.length,
                        emergencyData
                    });

                    const fcmResult = await fcmService.sendBulkEmergencyNotifications(fcmTokens, emergencyData);
                    
                    logger.info('FCM notifications sent', {
                        emergencyId: emergency._id,
                        totalTokens: fcmTokens.length,
                        success: fcmResult.success,
                        failed: fcmResult.failed,
                        results: fcmResult.results.map(r => ({
                            success: r.success,
                            error: r.error,
                            shouldRemoveToken: r.shouldRemoveToken
                        }))
                    });

                    // Remove invalid tokens
                    fcmResult.results.forEach(result => {
                        if (result.shouldRemoveToken) {
                            UserFCMToken.deleteOne({ fcmToken: result.token }).catch(err => {
                                logger.error('Failed to remove invalid FCM token', err);
                            });
                        }
                    });
                } else {
                    logger.warn('No FCM tokens found for any emergency contacts', {
                        emergencyId: emergency._id,
                        contactsChecked: emergencyContacts.length
                    });
                }
            } catch (fcmError) {
                // Don't fail the entire request if FCM fails
                logger.error('Error sending FCM notifications', {
                    error: fcmError.message,
                    stack: fcmError.stack,
                    emergencyId: emergency._id
                });
            }
        } else {
            logger.warn('No emergency contacts provided for FCM notifications', {
                emergencyId: emergency._id,
                hasContacts: !!emergencyContacts,
                isArray: Array.isArray(emergencyContacts)
            });
        }

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
            // Initiate voice call via Twilio with ElevenLabs integration
            result = await twilioService.makeEmergencyCall(
                contactPhone,
                contactName,
                emergency.userInfo.name,
                healthSummary,
                locationLink,
                emergencyDetails,
                emergencyId,
                emergency.userInfo.age,
                emergency.userInfo.medicalInfo || {}
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


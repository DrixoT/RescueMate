/**
 * Webhook Routes
 * Handle Twilio callbacks for voice calls and SMS
 */

const express = require('express');
const router = express.Router();
const twilioService = require('../services/twilioService');
const Emergency = require('../models/Emergency');
const logger = require('../utils/logger');
const twilio = require('twilio');

/**
 * GET /api/webhooks/emergency-voice
 * Generate TwiML for emergency voice call (Twilio TTS fallback)
 */
router.get('/emergency-voice', async (req, res) => {
    try {
        const {
            emergencyId,
            userName,
            contactName,
            healthSummary,
            locationLink
        } = req.query;

        const twiml = twilioService.generateEmergencyVoiceTwiML(
            userName,
            contactName,
            healthSummary,
            locationLink
        );

        res.type('text/xml');
        res.send(twiml);

    } catch (error) {
        logger.error('Emergency voice webhook error:', error);
        res.type('text/xml');
        res.send('<Response><Say>Error generating emergency message.</Say></Response>');
    }
});

/**
 * GET /api/webhooks/emergency-voice-elevenlabs
 * Generate TwiML for emergency voice call using ElevenLabs audio
 */
router.get('/emergency-voice-elevenlabs', async (req, res) => {
    try {
        const {
            emergencyId,
            audioUrl,
            userName,
            contactName
        } = req.query;

        const twimlResponse = new twilio.twiml.VoiceResponse();

        // Play ElevenLabs generated audio
        if (audioUrl) {
            // Build full URL for audio file
            const baseUrl = process.env.EMERGENCY_WEBHOOK_URL?.replace('/api/webhooks', '') || 
                          `http://${req.get('host')}`;
            const fullAudioUrl = `${baseUrl}${audioUrl}`;

            twimlResponse.play(fullAudioUrl);
        } else {
            // Fallback to text-to-speech if audio URL not provided
            twimlResponse.say({
                voice: 'Polly.Matthew',
                language: 'en-US'
            }, `Emergency Alert from RescueMate. ${userName} needs immediate assistance.`);
        }

        // Add response gathering
        twimlResponse.gather({
            numDigits: 1,
            action: `/api/webhooks/emergency-response?emergencyId=${encodeURIComponent(emergencyId)}`,
            method: 'POST',
            timeout: 10
        }).say({
            voice: 'Polly.Matthew',
            language: 'en-US'
        }, `Press 1 if ${userName} is safe. Press 2 if you need help. Press 9 to repeat this message.`);

        twimlResponse.say({
            voice: 'Polly.Matthew',
            language: 'en-US'
        }, 'No response received. This message will repeat.');

        twimlResponse.redirect(`/api/webhooks/emergency-voice-elevenlabs?${new URLSearchParams(req.query).toString()}`);

        res.type('text/xml');
        res.send(twimlResponse.toString());

    } catch (error) {
        logger.error('ElevenLabs emergency voice webhook error:', error);
        const twimlResponse = new twilio.twiml.VoiceResponse();
        twimlResponse.say('Error generating emergency message.');
        res.type('text/xml');
        res.send(twimlResponse.toString());
    }
});

/**
 * POST /api/webhooks/emergency-response
 * Handle user response from voice call (1=safe, 2=help)
 */
router.post('/emergency-response', async (req, res) => {
    try {
        const { Digits, CallSid } = req.body;
        const emergencyId = req.query.emergencyId;

        const twimlResponse = new twilio.twiml.VoiceResponse();

        if (Digits === '1') {
            // User confirmed safe
            twimlResponse.say({
                voice: 'Polly.Matthew',
                language: 'en-US'
            }, 'Thank you for confirming safety. The emergency alert has been cancelled. All contacts will be notified.');

            // Update emergency status
            if (emergencyId) {
                await Emergency.findByIdAndUpdate(emergencyId, {
                    status: 'RESOLVED',
                    resolvedAt: new Date(),
                    resolvedBy: 'CONTACT_VOICE_RESPONSE'
                });
            }

            logger.info(`Emergency resolved via voice response`, { emergencyId, CallSid });

        } else if (Digits === '2') {
            // Need additional help
            twimlResponse.say({
                voice: 'Polly.Matthew',
                language: 'en-US'
            }, 'Help request received. Emergency contacts and services are being notified. Please call 911 immediately if life-threatening.');

            logger.info(`Additional help requested`, { emergencyId, CallSid });

        } else if (Digits === '9') {
            // Repeat message
            twimlResponse.redirect('/api/webhooks/emergency-voice?' + new URLSearchParams(req.query).toString());

        } else {
            twimlResponse.say({
                voice: 'Polly.Matthew',
                language: 'en-US'
            }, 'Invalid selection. Please try again.');
            twimlResponse.redirect('/api/webhooks/emergency-voice?' + new URLSearchParams(req.query).toString());
        }

        res.type('text/xml');
        res.send(twimlResponse.toString());

    } catch (error) {
        logger.error('Emergency response webhook error:', error);
        const twimlResponse = new twilio.twiml.VoiceResponse();
        twimlResponse.say('Error processing your response.');
        res.type('text/xml');
        res.send(twimlResponse.toString());
    }
});

/**
 * POST /api/webhooks/call-status
 * Handle Twilio call status callbacks
 */
router.post('/call-status', async (req, res) => {
    try {
        const {
            CallSid,
            CallStatus,
            CallDuration,
            From,
            To
        } = req.body;

        logger.info(`Call status update`, {
            CallSid,
            CallStatus,
            CallDuration,
            From,
            To
        });

        // Update emergency record with call status
        // This would query by CallSid in production

        res.sendStatus(200);

    } catch (error) {
        logger.error('Call status webhook error:', error);
        res.sendStatus(500);
    }
});

/**
 * POST /api/webhooks/sms-status
 * Handle Twilio SMS status callbacks
 */
router.post('/sms-status', async (req, res) => {
    try {
        const {
            MessageSid,
            MessageStatus,
            From,
            To,
            ErrorCode,
            ErrorMessage
        } = req.body;

        logger.info(`SMS status update`, {
            MessageSid,
            MessageStatus,
            From,
            To,
            ErrorCode,
            ErrorMessage
        });

        res.sendStatus(200);

    } catch (error) {
        logger.error('SMS status webhook error:', error);
        res.sendStatus(500);
    }
});

/**
 * POST /api/webhooks/sms-reply
 * Handle incoming SMS replies from emergency contacts
 */
router.post('/sms-reply', async (req, res) => {
    try {
        const {
            From,
            Body,
            MessageSid
        } = req.body;

        logger.info(`SMS reply received`, {
            From,
            Body: Body.substring(0, 50) // Log first 50 chars
        });

        const twimlResponse = new twilio.twiml.MessagingResponse();

        // Parse response
        const bodyLower = Body.toLowerCase();

        if (bodyLower.includes('safe')) {
            // Extract emergency ID if present
            const emergencyIdMatch = Body.match(/safe\s+([a-f0-9]{8})/i);

            if (emergencyIdMatch) {
                const emergencyId = emergencyIdMatch[1];

                // Update emergency status
                const emergency = await Emergency.findOne({
                    _id: { $regex: new RegExp(`^${emergencyId}`) },
                    status: 'ACTIVE'
                });

                if (emergency) {
                    emergency.status = 'RESOLVED';
                    emergency.resolvedAt = new Date();
                    emergency.resolvedBy = 'CONTACT_SMS_RESPONSE';
                    emergency.responses.push({
                        contactPhone: From,
                        response: 'USER_FINE',
                        timestamp: new Date(),
                        source: 'SMS_REPLY',
                        notes: Body
                    });
                    await emergency.save();

                    twimlResponse.message('✅ Thank you! Emergency cancelled. User confirmed safe.');

                    // Notify other contacts
                    await twilioService.notifyContactsOfResolution(
                        emergency.contactAttempts.map(a => a.contactPhone).filter(p => p !== From),
                        emergency.userInfo.name,
                        From
                    );
                } else {
                    twimlResponse.message('⚠️ Emergency not found or already resolved.');
                }
            } else {
                twimlResponse.message('Please include the emergency ID (e.g., "SAFE 12345678")');
            }

        } else if (bodyLower.includes('help')) {
            twimlResponse.message('🚨 Additional help request recorded. Please call 911 if life-threatening emergency.');

        } else {
            twimlResponse.message('Reply "SAFE [emergency ID]" if user is safe, or "HELP [emergency ID]" if assistance needed.');
        }

        res.type('text/xml');
        res.send(twimlResponse.toString());

    } catch (error) {
        logger.error('SMS reply webhook error:', error);
        const twimlResponse = new twilio.twiml.MessagingResponse();
        twimlResponse.message('Error processing your response. Please try again.');
        res.type('text/xml');
        res.send(twimlResponse.toString());
    }
});

module.exports = router;


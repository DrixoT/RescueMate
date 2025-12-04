/**
 * Twilio Service
 * Handles emergency contact voice calls and SMS via Twilio API
 * Phase 1: Emergency contacts only (NOT emergency services)
 */

const twilio = require('twilio');
const logger = require('../utils/logger');
const elevenLabsService = require('./elevenLabsService');

// Initialize Twilio client
const client = twilio(
    process.env.TWILIO_ACCOUNT_SID,
    process.env.TWILIO_AUTH_TOKEN
);

const TWILIO_PHONE = process.env.TWILIO_PHONE_NUMBER;
const WEBHOOK_BASE_URL = process.env.EMERGENCY_WEBHOOK_URL || 'http://localhost:3000/api/webhooks';

/**
 * Make emergency voice call to contact using ElevenLabs voice
 */
async function makeEmergencyCall(
    contactPhone,
    contactName,
    userName,
    healthSummary,
    locationLink,
    emergencyDetails,
    emergencyId,
    userAge = null,
    medicalInfo = {}
) {
    try {
        // Parse emergency condition from healthSummary or emergencyDetails
        const condition = emergencyDetails?.emergencyType || 
                         healthSummary?.match(/experiencing[^.]*/i)?.[0] || 
                         'an emergency situation';

        // Extract alert reason from emergencyDetails or healthSummary for pre-reported illness check
        let alertReason = null;
        if (emergencyDetails && typeof emergencyDetails === 'string') {
            // Parse from emergencyDetails string (format: "Alert Reason: ...")
            const alertMatch = emergencyDetails.match(/Alert Reason:\s*([^\n]+)/i);
            if (alertMatch) {
                alertReason = alertMatch[1].trim();
            }
        }
        if (!alertReason && healthSummary) {
            // Try to extract from healthSummary
            const summaryMatch = healthSummary.match(/experiencing[^.]*/i);
            if (summaryMatch) {
                alertReason = summaryMatch[0].replace(/experiencing\s*/i, '').trim();
            }
        }

        // Generate ElevenLabs voice message
        const voiceResult = await elevenLabsService.generateEmergencyCallVoice({
            userName,
            age: userAge || 0,
            condition,
            location: locationLink || 'Location unavailable',
            medicalInfo,
            contactName,
            alertReason,
            healthSummary
        });

        let twimlUrl;

        if (voiceResult.success && voiceResult.audioUrl) {
            // Use ElevenLabs generated audio
            // Build TwiML URL that will play the audio file
            twimlUrl = `${WEBHOOK_BASE_URL}/emergency-voice-elevenlabs?` +
                `emergencyId=${encodeURIComponent(emergencyId)}` +
                `&audioUrl=${encodeURIComponent(voiceResult.audioUrl)}` +
                `&userName=${encodeURIComponent(userName)}` +
                `&contactName=${encodeURIComponent(contactName)}`;
            
            logger.info(`Using ElevenLabs voice for emergency call`, {
                emergencyId,
                audioUrl: voiceResult.audioUrl
            });
        } else {
            // Fallback to Twilio TTS if ElevenLabs fails
            logger.warn('ElevenLabs voice generation failed, using Twilio TTS fallback', {
                error: voiceResult.error
            });
            
            twimlUrl = `${WEBHOOK_BASE_URL}/emergency-voice?` +
                `emergencyId=${encodeURIComponent(emergencyId)}` +
                `&userName=${encodeURIComponent(userName)}` +
                `&contactName=${encodeURIComponent(contactName)}` +
                `&healthSummary=${encodeURIComponent(healthSummary)}` +
                `&locationLink=${encodeURIComponent(locationLink)}`;
        }

        // Initiate call
        const call = await client.calls.create({
            to: contactPhone,
            from: TWILIO_PHONE,
            url: twimlUrl,
            method: 'GET',
            timeout: 45,
            record: true, // Record for compliance
            statusCallback: `${WEBHOOK_BASE_URL}/call-status`,
            statusCallbackMethod: 'POST',
            statusCallbackEvent: ['initiated', 'ringing', 'answered', 'completed']
        });

        logger.info(`Emergency call initiated`, {
            callSid: call.sid,
            contactPhone,
            emergencyId,
            usingElevenLabs: voiceResult.success
        });

        return {
            success: true,
            callSid: call.sid,
            status: call.status,
            audioUrl: voiceResult.audioUrl || null
        };

    } catch (error) {
        logger.error('Emergency call error:', error);
        return {
            success: false,
            error: error.message
        };
    }
}

/**
 * Send emergency SMS to contact
 */
async function sendEmergencySMS(
    contactPhone,
    contactName,
    userName,
    healthSummary,
    locationLink,
    emergencyId
) {
    try {
        const message = buildEmergencySMSMessage(
            userName,
            healthSummary,
            locationLink,
            emergencyId
        );

        const sms = await client.messages.create({
            to: contactPhone,
            from: TWILIO_PHONE,
            body: message,
            statusCallback: `${WEBHOOK_BASE_URL}/sms-status`
        });

        logger.info(`Emergency SMS sent`, {
            messageSid: sms.sid,
            contactPhone,
            emergencyId
        });

        return {
            success: true,
            messageSid: sms.sid,
            status: sms.status
        };

    } catch (error) {
        logger.error('Emergency SMS error:', error);
        return {
            success: false,
            error: error.message
        };
    }
}

/**
 * Build emergency SMS message
 */
function buildEmergencySMSMessage(userName, healthSummary, locationLink, emergencyId) {
    return `🚨 EMERGENCY ALERT - RescueMate 🚨\n\n` +
        `${healthSummary}\n\n` +
        `📍 Location: ${locationLink}\n\n` +
        `⚠️ URGENT: Please check on ${userName} immediately!\n\n` +
        `If ${userName} is SAFE, reply "SAFE ${emergencyId.substring(0, 8)}" to cancel this alert.\n\n` +
        `If you need help, reply "HELP ${emergencyId.substring(0, 8)}"`;
}

/**
 * Generate TwiML voice message
 */
function generateEmergencyVoiceTwiML(userName, contactName, healthSummary, locationLink) {
    const voiceScript = `
        <Response>
            <Say voice="Polly.Matthew" language="en-US">
                Emergency Alert from RescueMate.
                This is an urgent message for ${contactName}.

                ${userName} needs immediate assistance.

                ${healthSummary}

                Location information has been sent via text message to this number.
                Google Maps link: ${locationLink}

                Please check on ${userName} immediately.

                If ${userName} is safe, press 1 to cancel this alert.
                If you need additional help, press 2.
                To repeat this message, press 9.

                This is an automated emergency notification from RescueMate.
                Please respond immediately.
            </Say>

            <Gather numDigits="1" action="/api/webhooks/emergency-response" method="POST" timeout="10">
                <Say voice="Polly.Matthew" language="en-US">
                    Press 1 if ${userName} is safe.
                    Press 2 if you need help.
                    Press 9 to repeat this message.
                </Say>
            </Gather>

            <Say voice="Polly.Matthew" language="en-US">
                No response received. This message will repeat.
            </Say>

            <Redirect>/api/webhooks/emergency-voice</Redirect>
        </Response>
    `;

    return voiceScript;
}

/**
 * Get call status from Twilio
 */
async function getCallStatus(callSid) {
    try {
        const call = await client.calls(callSid).fetch();

        return {
            status: call.status,
            duration: call.duration,
            startTime: call.startTime,
            endTime: call.endTime,
            direction: call.direction,
            answeredBy: call.answeredBy,
            errorCode: call.errorCode,
            errorMessage: call.errorMessage
        };

    } catch (error) {
        logger.error('Get call status error:', error);
        throw error;
    }
}

/**
 * Get message status from Twilio
 */
async function getMessageStatus(messageSid) {
    try {
        const message = await client.messages(messageSid).fetch();

        return {
            status: message.status,
            dateSent: message.dateSent,
            errorCode: message.errorCode,
            errorMessage: message.errorMessage
        };

    } catch (error) {
        logger.error('Get message status error:', error);
        throw error;
    }
}

/**
 * Notify contacts of emergency resolution
 */
async function notifyContactsOfResolution(contactPhones, userName, confirmingContact) {
    const promises = contactPhones.map(async (phone) => {
        try {
            const message = `✅ EMERGENCY RESOLVED - RescueMate\n\n` +
                `${userName} has been confirmed SAFE.\n\n` +
                `Confirmed by: ${confirmingContact}\n` +
                `Time: ${new Date().toLocaleString()}\n\n` +
                `Thank you for your quick response!`;

            await client.messages.create({
                to: phone,
                from: TWILIO_PHONE,
                body: message
            });

            logger.info(`Resolution notification sent to ${phone}`);

        } catch (error) {
            logger.error(`Failed to notify ${phone} of resolution:`, error);
        }
    });

    await Promise.allSettled(promises);
}

/**
 * Notify contacts of emergency cancellation
 */
async function notifyContactsOfCancellation(contactPhones, userName) {
    const promises = contactPhones.map(async (phone) => {
        try {
            const message = `⚠️ EMERGENCY CANCELLED - RescueMate\n\n` +
                `${userName} has cancelled the emergency alert.\n` +
                `They are SAFE.\n\n` +
                `Time: ${new Date().toLocaleString()}\n\n` +
                `No further action needed.`;

            await client.messages.create({
                to: phone,
                from: TWILIO_PHONE,
                body: message
            });

            logger.info(`Cancellation notification sent to ${phone}`);

        } catch (error) {
            logger.error(`Failed to notify ${phone} of cancellation:`, error);
        }
    });

    await Promise.allSettled(promises);
}

/**
 * Send health update to contacts during ongoing emergency
 */
async function sendHealthUpdate(contactPhones, userName, healthUpdate) {
    const promises = contactPhones.map(async (phone) => {
        try {
            const message = `📊 HEALTH UPDATE - RescueMate\n\n` +
                `${userName}\n` +
                `${healthUpdate}\n\n` +
                `Time: ${new Date().toLocaleString()}\n\n` +
                `Continue monitoring.`;

            await client.messages.create({
                to: phone,
                from: TWILIO_PHONE,
                body: message
            });

        } catch (error) {
            logger.error(`Failed to send health update to ${phone}:`, error);
        }
    });

    await Promise.allSettled(promises);
}

/**
 * Validate phone number format
 */
function validatePhoneNumber(phoneNumber) {
    // Basic E.164 format validation
    const phoneRegex = /^\+[1-9]\d{1,14}$/;
    return phoneRegex.test(phoneNumber);
}

/**
 * Format phone number to E.164
 */
function formatPhoneNumber(phoneNumber) {
    // Remove all non-digits
    let cleaned = phoneNumber.replace(/\D/g, '');

    // Add +1 for US numbers if not present
    if (cleaned.length === 10) {
        cleaned = '1' + cleaned;
    }

    return '+' + cleaned;
}

module.exports = {
    makeEmergencyCall,
    sendEmergencySMS,
    generateEmergencyVoiceTwiML,
    getCallStatus,
    getMessageStatus,
    notifyContactsOfResolution,
    notifyContactsOfCancellation,
    sendHealthUpdate,
    validatePhoneNumber,
    formatPhoneNumber
};


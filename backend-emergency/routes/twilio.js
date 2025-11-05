/**
 * Twilio Routes (placeholder for additional Twilio endpoints)
 */

const express = require('express');
const router = express.Router();

/**
 * GET /api/twilio/test
 * Test Twilio configuration
 */
router.get('/test', async (req, res) => {
    try {
        const accountSid = process.env.TWILIO_ACCOUNT_SID;
        const phoneNumber = process.env.TWILIO_PHONE_NUMBER;

        if (!accountSid || !phoneNumber) {
            return res.json({
                success: false,
                configured: false,
                message: 'Twilio not configured. Please set TWILIO_ACCOUNT_SID and TWILIO_PHONE_NUMBER in .env'
            });
        }

        res.json({
            success: true,
            configured: true,
            phoneNumber: phoneNumber,
            message: 'Twilio is configured'
        });

    } catch (error) {
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

module.exports = router;


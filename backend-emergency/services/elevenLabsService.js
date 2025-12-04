/**
 * ElevenLabs Voice Service
 * Generates emergency voice messages using ElevenLabs Text-to-Speech API
 */

const axios = require('axios');
const logger = require('../utils/logger');
const fs = require('fs');
const path = require('path');

const ELEVEN_API_KEY = process.env.ELEVEN_API_KEY;
const ELEVEN_VOICE_ID = process.env.ELEVEN_VOICE_ID || '21m00Tcm4TlvDq8ikWAM'; // Default: Rachel
const ELEVEN_API_URL = 'https://api.elevenlabs.io/v1';

/**
 * Generate text-to-speech audio from text
 * @param {string} text - Text to convert to speech
 * @param {string} voiceId - ElevenLabs voice ID (optional, uses default if not provided)
 * @returns {Promise<{success: boolean, audioUrl?: string, audioPath?: string, error?: string}>}
 */
async function textToSpeech(text, voiceId = ELEVEN_VOICE_ID) {
    try {
        if (!ELEVEN_API_KEY) {
            logger.warn('ElevenLabs API key not configured');
            return {
                success: false,
                error: 'ElevenLabs API key not configured'
            };
        }

        if (!text || text.trim().length === 0) {
            return {
                success: false,
                error: 'Text cannot be empty'
            };
        }

        const url = `${ELEVEN_API_URL}/text-to-speech/${voiceId}`;

        const response = await axios.post(
            url,
            {
                text: text,
                model_id: 'eleven_monolingual_v1',
                voice_settings: {
                    stability: 0.5,
                    similarity_boost: 0.75,
                    style: 0.0,
                    use_speaker_boost: true
                }
            },
            {
                headers: {
                    'Accept': 'audio/mpeg',
                    'xi-api-key': ELEVEN_API_KEY,
                    'Content-Type': 'application/json'
                },
                responseType: 'arraybuffer',
                timeout: 30000 // 30 second timeout
            }
        );

        // Save audio to temporary file
        const audioDir = path.join(__dirname, '../temp');
        if (!fs.existsSync(audioDir)) {
            fs.mkdirSync(audioDir, { recursive: true });
        }

        const timestamp = Date.now();
        const audioPath = path.join(audioDir, `emergency_voice_${timestamp}.mp3`);

        fs.writeFileSync(audioPath, response.data);

        logger.info(`ElevenLabs audio generated`, {
            voiceId,
            textLength: text.length,
            audioPath
        });

        // In production, upload to cloud storage (S3, etc.) and return public URL
        // For now, return local path - webhook will need to serve this file
        // TODO: Upload to cloud storage and return public URL
        const audioUrl = `/api/audio/emergency_voice_${timestamp}.mp3`;

        return {
            success: true,
            audioUrl,
            audioPath,
            voiceId
        };

    } catch (error) {
        logger.error('ElevenLabs text-to-speech error:', error);
        
        if (error.response) {
            return {
                success: false,
                error: `ElevenLabs API error: ${error.response.status} - ${error.response.statusText}`
            };
        }

        return {
            success: false,
            error: error.message || 'Unknown error generating voice'
        };
    }
}

/**
 * Generate emergency voice message script
 * @param {string} userName - Name of the person in emergency
 * @param {number} age - Age of the person
 * @param {string} condition - Emergency condition/type
 * @param {string} location - Location description or address
 * @param {object} medicalInfo - Medical information object
 * @param {string} alertReason - Pre-reported illness or alert reason (optional)
 * @returns {string} Formatted emergency message script
 */
function generateEmergencyMessage(userName, age, condition, location, medicalInfo = {}, alertReason = null) {
    const medicalDetails = [];
    
    if (medicalInfo.bloodType) {
        medicalDetails.push(`Blood type: ${medicalInfo.bloodType}`);
    }
    
    if (medicalInfo.knownConditions && medicalInfo.knownConditions.length > 0) {
        medicalDetails.push(`Known conditions: ${medicalInfo.knownConditions.join(', ')}`);
    }
    
    if (medicalInfo.allergies && medicalInfo.allergies.length > 0) {
        medicalDetails.push(`Allergies: ${medicalInfo.allergies.join(', ')}`);
    }
    
    if (medicalInfo.currentMedications && medicalInfo.currentMedications.length > 0) {
        const medNames = medicalInfo.currentMedications.map(m => 
            typeof m === 'string' ? m : m.name
        ).join(', ');
        medicalDetails.push(`Current medications: ${medNames}`);
    }

    // New message format: "User A Has initiated an SOS Protocol, Please reach out to them."
    let script = `${userName} has initiated an SOS Protocol. Please reach out to them immediately. `;
    
    // Check for pre-reported illness (from alertReason)
    // alertReason contains specific symptoms/conditions reported before emergency activation
    const hasPreReportedIllness = alertReason && 
        alertReason.trim().length > 0 &&
        !alertReason.toLowerCase().includes('manual emergency') &&
        !alertReason.toLowerCase().includes('triggered by user');
    
    if (hasPreReportedIllness) {
        // User reported illness before emergency protocol was activated
        script += `Before activating the emergency protocol, ${userName} reported: ${alertReason}. `;
    } else if (condition && condition !== 'an emergency situation' && condition !== 'Manual emergency triggered by user') {
        // Current emergency condition
        script += `${userName} is currently experiencing: ${condition}. `;
    }
    
    if (location && location !== 'Location unavailable') {
        script += `Current location: ${location}. `;
    }
    
    if (medicalDetails.length > 0) {
        script += `Medical information: ${medicalDetails.join('. ')}. `;
    }
    
    script += `This is an automated emergency notification from RescueMate. `;
    script += `Please check on ${userName} immediately. `;
    script += `If ${userName} is safe, please respond to cancel this alert.`;

    return script;
}

/**
 * Generate emergency voice message for Twilio call
 * @param {object} emergencyData - Emergency data object
 * @returns {Promise<{success: boolean, audioUrl?: string, script?: string, error?: string}>}
 */
async function generateEmergencyCallVoice(emergencyData) {
    try {
        const {
            userName,
            age,
            condition,
            location,
            medicalInfo = {},
            contactName,
            alertReason = null,
            healthSummary = null
        } = emergencyData;

        // Extract alert reason from healthSummary if not provided directly
        let extractedAlertReason = alertReason;
        if (!extractedAlertReason && healthSummary) {
            // Try to extract from healthSummary (format: "User is experiencing...")
            const match = healthSummary.match(/experiencing[^.]*/i);
            if (match) {
                extractedAlertReason = match[0].replace(/experiencing\s*/i, '').trim();
            }
        }

        // Generate script with alert reason for pre-reported illness check
        const script = generateEmergencyMessage(userName, age, condition, location, medicalInfo, extractedAlertReason);

        // Generate audio
        const result = await textToSpeech(script);

        if (!result.success) {
            return {
                success: false,
                script, // Return script even if audio generation fails
                error: result.error
            };
        }

        logger.info(`Emergency call voice generated`, {
            userName,
            contactName,
            audioUrl: result.audioUrl
        });

        return {
            success: true,
            audioUrl: result.audioUrl,
            audioPath: result.audioPath,
            script,
            voiceId: result.voiceId
        };

    } catch (error) {
        logger.error('Generate emergency call voice error:', error);
        return {
            success: false,
            error: error.message || 'Failed to generate emergency voice'
        };
    }
}

/**
 * Clean up old audio files (older than 1 hour)
 */
function cleanupOldAudioFiles() {
    try {
        const audioDir = path.join(__dirname, '../temp');
        if (!fs.existsSync(audioDir)) {
            return;
        }

        const files = fs.readdirSync(audioDir);
        const oneHourAgo = Date.now() - (60 * 60 * 1000);

        files.forEach(file => {
            if (file.startsWith('emergency_voice_')) {
                const filePath = path.join(audioDir, file);
                const stats = fs.statSync(filePath);
                
                if (stats.mtimeMs < oneHourAgo) {
                    fs.unlinkSync(filePath);
                    logger.debug(`Cleaned up old audio file: ${file}`);
                }
            }
        });

    } catch (error) {
        logger.error('Cleanup audio files error:', error);
    }
}

/**
 * Get available ElevenLabs voices
 * @returns {Promise<{success: boolean, voices?: Array, error?: string}>}
 */
async function getAvailableVoices() {
    try {
        if (!ELEVEN_API_KEY) {
            return {
                success: false,
                error: 'ElevenLabs API key not configured'
            };
        }

        const response = await axios.get(`${ELEVEN_API_URL}/voices`, {
            headers: {
                'xi-api-key': ELEVEN_API_KEY
            },
            timeout: 10000
        });

        return {
            success: true,
            voices: response.data.voices || []
        };

    } catch (error) {
        logger.error('Get available voices error:', error);
        return {
            success: false,
            error: error.message || 'Failed to fetch voices'
        };
    }
}

/**
 * Get conversation token for private ElevenLabs agent
 * This token is required to start conversations with private agents
 * @param {string} agentId - The ElevenLabs agent ID
 * @returns {Promise<{success: boolean, token?: string, error?: string}>}
 */
async function getConversationToken(agentId) {
    try {
        if (!ELEVEN_API_KEY) {
            logger.error('ElevenLabs API key not configured for conversation token');
            return {
                success: false,
                error: 'ElevenLabs API key not configured'
            };
        }

        if (!agentId) {
            return {
                success: false,
                error: 'Agent ID is required'
            };
        }

        logger.info(`Fetching conversation token for agent: ${agentId}`);

        const response = await axios.get(
            `${ELEVEN_API_URL}/convai/conversation/token?agent_id=${agentId}`,
            {
                headers: {
                    'xi-api-key': ELEVEN_API_KEY
                },
                timeout: 10000
            }
        );

        if (response.data && response.data.token) {
            logger.info('Conversation token fetched successfully');
            return {
                success: true,
                token: response.data.token
            };
        }

        return {
            success: false,
            error: 'No token in response'
        };

    } catch (error) {
        logger.error('Get conversation token error:', error);
        
        if (error.response) {
            return {
                success: false,
                error: `ElevenLabs API error: ${error.response.status} - ${JSON.stringify(error.response.data)}`
            };
        }

        return {
            success: false,
            error: error.message || 'Failed to fetch conversation token'
        };
    }
}

// Run cleanup on startup and every hour
cleanupOldAudioFiles();
setInterval(cleanupOldAudioFiles, 60 * 60 * 1000);

module.exports = {
    textToSpeech,
    generateEmergencyMessage,
    generateEmergencyCallVoice,
    getAvailableVoices,
    cleanupOldAudioFiles,
    getConversationToken
};


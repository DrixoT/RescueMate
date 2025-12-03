/**
 * RescueMate Firebase Cloud Functions
 * 
 * Cloud functions for secure server-side operations
 */

const functions = require('firebase-functions');
const admin = require('firebase-admin');
const axios = require('axios');

// Initialize Firebase Admin
admin.initializeApp();

/**
 * Get conversation token for ElevenLabs private agent
 * 
 * This function fetches a conversation token from ElevenLabs API
 * using the API key stored securely in Firebase config.
 * 
 * The API key is never exposed to the client - only the temporary token is returned.
 * 
 * Usage from Android:
 *   val functions = Firebase.functions
 *   val result = functions.getHttpsCallable("getConversationToken").call(data).await()
 * 
 * @param {Object} data - The request data containing agentId
 * @param {string} data.agentId - The ElevenLabs agent ID
 * @returns {Object} - Object containing the conversation token
 */
exports.getConversationToken = functions.https.onCall(async (data, context) => {
    try {
        const { agentId } = data;
        
        // Validate input
        if (!agentId) {
            throw new functions.https.HttpsError(
                'invalid-argument',
                'agentId is required'
            );
        }
        
        // Get API key from Firebase config
        // Set with: firebase functions:config:set elevenlabs.apikey="YOUR_KEY"
        const apiKey = functions.config().elevenlabs?.apikey;
        
        if (!apiKey) {
            console.error('ElevenLabs API key not configured in Firebase config');
            throw new functions.https.HttpsError(
                'failed-precondition',
                'ElevenLabs API key not configured. Run: firebase functions:config:set elevenlabs.apikey="YOUR_KEY"'
            );
        }
        
        console.log(`Fetching conversation token for agent: ${agentId}`);
        
        // Fetch conversation token from ElevenLabs API
        const response = await axios.get(
            `https://api.elevenlabs.io/v1/convai/conversation/token?agent_id=${agentId}`,
            {
                headers: {
                    'xi-api-key': apiKey
                },
                timeout: 10000
            }
        );
        
        if (response.data && response.data.token) {
            console.log('Conversation token fetched successfully');
            return {
                success: true,
                token: response.data.token
            };
        }
        
        throw new functions.https.HttpsError(
            'internal',
            'No token in ElevenLabs response'
        );
        
    } catch (error) {
        console.error('Error fetching conversation token:', error);
        
        // Handle axios errors
        if (error.response) {
            const status = error.response.status;
            const message = error.response.data?.detail?.message || error.response.statusText;
            
            if (status === 401) {
                throw new functions.https.HttpsError(
                    'unauthenticated',
                    `ElevenLabs authentication failed: ${message}`
                );
            } else if (status === 404) {
                throw new functions.https.HttpsError(
                    'not-found',
                    `Agent not found: ${message}`
                );
            }
            
            throw new functions.https.HttpsError(
                'internal',
                `ElevenLabs API error: ${status} - ${message}`
            );
        }
        
        // Re-throw if already an HttpsError
        if (error instanceof functions.https.HttpsError) {
            throw error;
        }
        
        throw new functions.https.HttpsError(
            'internal',
            error.message || 'Failed to fetch conversation token'
        );
    }
});

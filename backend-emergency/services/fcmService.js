/**
 * Firebase Cloud Messaging Service
 * Handles sending push notifications to logged-in users
 */

const admin = require('firebase-admin');
const logger = require('../utils/logger');

// Initialize Firebase Admin if not already initialized
if (!admin.apps.length) {
    try {
        // Try to initialize with service account from environment
        if (process.env.FIREBASE_SERVICE_ACCOUNT) {
            const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
            admin.initializeApp({
                credential: admin.credential.cert(serviceAccount)
            });
            logger.info('Firebase Admin initialized with service account');
        } else if (process.env.FIREBASE_PROJECT_ID) {
            // Initialize with default credentials (for Google Cloud environments)
            admin.initializeApp({
                projectId: process.env.FIREBASE_PROJECT_ID
            });
            logger.info('Firebase Admin initialized with project ID');
        } else {
            logger.warn('Firebase Admin not initialized - FCM notifications will not work', {
                hasServiceAccount: !!process.env.FIREBASE_SERVICE_ACCOUNT,
                hasProjectId: !!process.env.FIREBASE_PROJECT_ID
            });
        }
    } catch (error) {
        logger.error('Failed to initialize Firebase Admin:', {
            error: error.message,
            stack: error.stack
        });
    }
} else {
    logger.info('Firebase Admin already initialized');
}

/**
 * Send emergency notification to FCM token
 * @param {string} fcmToken - FCM registration token
 * @param {object} emergencyData - Emergency data
 * @returns {Promise<{success: boolean, messageId?: string, error?: string}>}
 */
async function sendEmergencyNotification(fcmToken, emergencyData) {
    try {
        if (!admin.apps.length) {
            logger.error('Cannot send FCM notification: Firebase Admin not initialized');
            return {
                success: false,
                error: 'Firebase Admin not initialized'
            };
        }

        const {
            emergencyId,
            userId,
            userName,
            emergencyType,
            alertReason,
            location,
            timestamp
        } = emergencyData;

        const message = {
            notification: {
                title: `Emergency Alert - ${userName}`,
                body: `${userName} has initiated an SOS Protocol. Tap to view details.`
            },
            data: {
                type: 'EMERGENCY_ALERT',
                emergencyId: emergencyId || '',
                userId: userId || '',
                userName: userName || '',
                emergencyType: emergencyType || '',
                alertReason: alertReason || '',
                location: location || '',
                timestamp: timestamp || new Date().toISOString()
            },
            token: fcmToken,
            android: {
                priority: 'high',
                notification: {
                    channelId: 'emergency_notifications',
                    sound: 'default',
                    priority: 'high',
                    visibility: 'public'
                }
            },
            apns: {
                payload: {
                    aps: {
                        sound: 'default',
                        badge: 1,
                        contentAvailable: true
                    }
                }
            }
        };

        logger.info('Sending FCM notification', {
            token: fcmToken.substring(0, 20) + '...',
            emergencyId,
            userName,
            emergencyType,
            alertReason
        });

        const response = await admin.messaging().send(message);

        logger.info('FCM notification sent successfully', {
            messageId: response,
            emergencyId,
            userName,
            token: fcmToken.substring(0, 20) + '...'
        });

        return {
            success: true,
            messageId: response
        };

    } catch (error) {
        logger.error('Error sending FCM notification:', {
            error: error.message,
            code: error.code,
            stack: error.stack,
            token: fcmToken ? fcmToken.substring(0, 20) + '...' : 'null',
            emergencyId
        });
        
        // Handle specific FCM errors
        if (error.code === 'messaging/invalid-registration-token' ||
            error.code === 'messaging/registration-token-not-registered') {
            // Token is invalid, should be removed from database
            logger.warn('FCM token is invalid, marking for removal', {
                token: fcmToken.substring(0, 20) + '...',
                errorCode: error.code
            });
            return {
                success: false,
                error: 'Invalid FCM token',
                shouldRemoveToken: true
            };
        }

        return {
            success: false,
            error: error.message || 'Failed to send notification'
        };
    }
}

/**
 * Send emergency notifications to multiple FCM tokens
 * @param {Array<string>} fcmTokens - Array of FCM tokens
 * @param {object} emergencyData - Emergency data
 * @returns {Promise<{success: number, failed: number, results: Array}>}
 */
async function sendBulkEmergencyNotifications(fcmTokens, emergencyData) {
    if (!fcmTokens || fcmTokens.length === 0) {
        return {
            success: 0,
            failed: 0,
            results: []
        };
    }

    const results = await Promise.allSettled(
        fcmTokens.map(token => sendEmergencyNotification(token, emergencyData))
    );

    const success = results.filter(r => r.status === 'fulfilled' && r.value.success).length;
    const failed = results.length - success;

    logger.info('Bulk FCM notifications sent', {
        total: fcmTokens.length,
        success,
        failed
    });

    return {
        success,
        failed,
        results: results.map((r, i) => ({
            token: fcmTokens[i],
            ...(r.status === 'fulfilled' ? r.value : { success: false, error: r.reason })
        }))
    };
}

module.exports = {
    sendEmergencyNotification,
    sendBulkEmergencyNotifications
};

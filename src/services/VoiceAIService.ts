/**
 * Voice AI Service for Emergency Calling
 * Uses backend proxy to securely handle ElevenLabs API calls
 *
 * SECURITY: API keys are stored on backend server, not in frontend
 * Users do NOT need to provide their own API keys
 */

interface EmergencyCallParams {
  userName: string;
  age: number;
  gender: string;
  condition: string;
  location: {
    latitude: number;
    longitude: number;
    address?: string;
  };
  timestamp: Date;
  emergencyContactName?: string;
  emergencyContactNumber?: string;
  medicalInfo?: {
    allergies?: string;
    medications?: string;
    conditions?: string;
  };
}

interface CallStatus {
  isActive: boolean;
  stage: 'idle' | 'initiating' | 'generating' | 'playing' | 'completed' | 'failed';
  message: string;
  progress: number; // 0-100
}

export class VoiceAIService {
  private backendUrl: string;
  private voiceId = "JBFqnCBsd6RMkjVDRZzb"; // ElevenLabs default voice
  private callStatus: CallStatus = {
    isActive: false,
    stage: 'idle',
    message: '',
    progress: 0
  };
  private statusCallback?: (status: CallStatus) => void;
  private audioElement?: HTMLAudioElement;

  constructor(backendUrl: string = 'http://localhost:3000') {
    this.backendUrl = backendUrl;
  }

  setStatusCallback(callback: (status: CallStatus) => void) {
    this.statusCallback = callback;
  }

  private updateStatus(stage: CallStatus['stage'], message: string, progress: number = 0) {
    this.callStatus = {
      isActive: stage !== 'idle' && stage !== 'completed' && stage !== 'failed',
      stage,
      message,
      progress
    };
    this.statusCallback?.(this.callStatus);
  }

  private async reverseGeocode(latitude: number, longitude: number): Promise<string | null> {
    try {
      const response = await fetch(`${this.backendUrl}/api/location/reverse-geocode`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ latitude, longitude })
      });

      if (response.ok) {
        const data = await response.json();
        return data.formattedAddress;
      }
    } catch (error) {
      console.error('Reverse geocoding failed:', error);
    }
    return null;
  }

  private formatLocation(location: EmergencyCallParams['location']): string {
    if (location.address) {
      return location.address;
    }
    return `GPS coordinates: Latitude ${location.latitude.toFixed(6)}, Longitude ${location.longitude.toFixed(6)}`;
  }

  private generateEmergencyScript(params: EmergencyCallParams): string {
    const timeElapsed = Math.floor((new Date().getTime() - params.timestamp.getTime()) / 1000 / 60);
    const locationStr = this.formatLocation(params.location);

    let script = `
Emergency Alert from RescueMate.

This is an automated emergency notification for ${params.userName}.

An emergency has been detected. ${params.userName}, a ${params.age}-year-old ${params.gender}, is experiencing: ${params.condition}.

This incident was detected ${timeElapsed} minute${timeElapsed !== 1 ? 's' : ''} ago.

Current location: ${locationStr}.
`;

    // Add medical information if available
    if (params.medicalInfo) {
      if (params.medicalInfo.allergies) {
        script += `\nIMPORTANT: Known allergies - ${params.medicalInfo.allergies}.`;
      }
      if (params.medicalInfo.medications) {
        script += `\nCurrent medications: ${params.medicalInfo.medications}.`;
      }
      if (params.medicalInfo.conditions) {
        script += `\nMedical conditions: ${params.medicalInfo.conditions}.`;
      }
    }

    script += `

Please check on ${params.userName} immediately. Emergency services have been alerted and are being dispatched to this location.

If you need additional assistance, please call 9-1-1 directly.

This is an automated emergency notification from RescueMate Emergency Response System.
`;

    return script.trim();
  }

  async initiateEmergencyCall(params: EmergencyCallParams): Promise<void> {
    try {
      this.updateStatus('initiating', 'Initiating emergency voice call...', 10);

      // Generate the emergency message
      const script = this.generateEmergencyScript(params);
      console.log(' Emergency Script Generated:', script);

      this.updateStatus('generating', 'Generating voice message...', 30);

      // Call ElevenLabs API
      const response = await fetch(
        `https://api.elevenlabs.io/v1/text-to-speech/${this.voiceId}`,
        {
          method: 'POST',
          headers: {
            'Accept': 'audio/mpeg',
            'Content-Type': 'application/json',
            'xi-api-key': this.apiKey
          },
          body: JSON.stringify({
            text: script,
            model_id: 'eleven_multilingual_v2',
            voice_settings: {
              stability: 0.5,
              similarity_boost: 0.75
            }
          })
        }
      );

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(`ElevenLabs API Error: ${response.status} - ${JSON.stringify(errorData)}`);
      }

      this.updateStatus('playing', 'Playing emergency message...', 60);

      // Convert response to blob and play
      const audioBlob = await response.blob();
      const audioUrl = URL.createObjectURL(audioBlob);

      // Create and play audio element
      this.audioElement = new Audio(audioUrl);

      await new Promise<void>((resolve, reject) => {
        if (!this.audioElement) {
          reject(new Error('Audio element not created'));
          return;
        }

        this.audioElement.onended = () => {
          this.updateStatus('completed', 'Emergency call completed successfully', 100);
          URL.revokeObjectURL(audioUrl);
          resolve();
        };

        this.audioElement.onerror = (error) => {
          console.error('Audio playback error:', error);
          reject(new Error('Failed to play audio'));
        };

        this.audioElement.play().catch(reject);
      });
      const response = await fetch(`${this.backendUrl}/api/voice/emergency-call`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-user-id': localStorage.getItem('rescuemate_user_id') || 'anonymous'
        },
        body: JSON.stringify({
          text: message,
          voiceId: this.voiceId
        })
      });
        {
          method: 'POST',
          headers: {
            'Accept': 'audio/mpeg',
            'Content-Type': 'application/json',
            'xi-api-key': this.apiKey
          },
          body: JSON.stringify({
            text: message,
            model_id: 'eleven_multilingual_v2',
            voice_settings: {
              stability: 0.5,
              similarity_boost: 0.75
            }
          })
        }
      );

      if (!response.ok) {
        throw new Error(`Failed to generate follow-up: ${response.status}`);
      }

      const audioBlob = await response.blob();
      const audioUrl = URL.createObjectURL(audioBlob);
      const audio = new Audio(audioUrl);

      await new Promise<void>((resolve, reject) => {
        audio.onended = () => {
          URL.revokeObjectURL(audioUrl);
          resolve();
        };
        audio.onerror = reject;
        audio.play().catch(reject);
      });

    } catch (error) {
      console.error('Follow-up announcement failed:', error);
      throw error;
    }
  }

  private logEmergencyCall(params: EmergencyCallParams, status: 'success' | 'failed') {
    const callLog = {
      timestamp: new Date().toISOString(),
      userName: params.userName,
      status,
      location: params.location,
      condition: params.condition
    };

    // Store in localStorage
    const logs = JSON.parse(localStorage.getItem('rescuemate_voice_call_logs') || '[]');
    logs.push(callLog);

  getCallStatus(): CallStatus {

      const response = await fetch(`${this.backendUrl}/api/voice/test`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ userName })
      });

      if (!response.ok) {
        throw new Error('Test request failed');
      }

      const audioBlob = await response.blob();
      const audioUrl = URL.createObjectURL(audioBlob);
      const audio = new Audio(audioUrl);

      await new Promise<void>((resolve, reject) => {
        audio.onended = () => {
          URL.revokeObjectURL(audioUrl);
          this.updateStatus('completed', 'Voice AI test completed successfully', 100);
          resolve();
        };
        audio.onerror = reject;
        audio.play().catch(reject);
      });


  endCall() {
    if (this.audioElement) {
      this.audioElement.pause();
      this.audioElement = undefined;
    }
    this.updateStatus('idle', 'Call ended', 0);
  }

  // Test the voice AI with a sample message
  async testVoiceAI(userName: string): Promise<void> {
    const testMessage = `Hello, this is a test of the RescueMate Voice AI system for ${userName}. If you can hear this message clearly, the Voice AI is working correctly.`;

    try {
export function initializeVoiceAI(backendUrl?: string): VoiceAIService {
  const url = backendUrl || process.env.REACT_APP_BACKEND_URL || 'http://localhost:3000';
  voiceAIInstance = new VoiceAIService(url);
      throw error;
    }
  }
}

// Singleton instance
let voiceAIInstance: VoiceAIService | null = null;

export function getVoiceAIService(): VoiceAIService | null {
  return voiceAIInstance;
}

export function initializeVoiceAI(apiKey: string): VoiceAIService {
  if (!apiKey || apiKey.trim() === '') {
    throw new Error('ElevenLabs API key is required');
  }
  voiceAIInstance = new VoiceAIService(apiKey);
  return voiceAIInstance;
}

export function isVoiceAIInitialized(): boolean {
  return voiceAIInstance !== null;
}


import { motion } from "motion/react";
import { Users, Settings, MapPin, Wifi, Shield, AlertCircle, PhoneCall } from "lucide-react";
import { Button } from "./ui/button";
import { Badge } from "./ui/badge";
import { useState, useEffect } from "react";
import { getVoiceAIService, isVoiceAIInitialized, initializeVoiceAI } from "../services/VoiceAIService";

interface HomeDashboardProps {
  onNavigate: (screen: string) => void;
}

export function HomeDashboard({ onNavigate }: HomeDashboardProps) {
  const [sosActive, setSOSActive] = useState(false);
  const [voiceCallStatus, setVoiceCallStatus] = useState<string>('');
  const [sosCountdown, setSOSCountdown] = useState<number | null>(null);

  const handleSOSPress = () => {
    if (sosActive) {
      // Cancel SOS
      setSOSActive(false);
      setVoiceCallStatus('');
      setSOSCountdown(null);
      return;
    }

    // Start 3-second countdown before activating
    let count = 3;


    const interval = setInterval(() => {
      count--;
      if (count > 0) {
        setSOSCountdown(count);
      } else {
        clearInterval(interval);
        activateSOS();
      }
    }, 1000);
  };

  const activateSOS = async () => {
    setSOSActive(true);

    try {
      // Get user profile
      const userProfile = JSON.parse(localStorage.getItem('rescuemate_user_profile') || '{}');
      const contacts = JSON.parse(localStorage.getItem('rescuemate_contacts') || '[]');

      // Get current location
      const position = await new Promise<GeolocationPosition>((resolve, reject) => {
        navigator.geolocation.getCurrentPosition(resolve, reject, {
          enableHighAccuracy: true,
          timeout: 10000,
          maximumAge: 0
        });
      });

      const location = {
        latitude: position.coords.latitude,
        longitude: position.coords.longitude
      };

      console.log('🚨 SOS ACTIVATED');
      console.log('📍 Location:', location);
      console.log('📞 Contacts to notify:', contacts.length);

      // Check if Voice AI is enabled
      const voiceAIEnabled = localStorage.getItem('rescuemate_voiceai_enabled') === 'true';

      if (voiceAIEnabled && isVoiceAIInitialized()) {
        setVoiceCallStatus('Initiating Voice AI call...');

        const voiceAI = getVoiceAIService();
        if (voiceAI) {
          // Set up status callback
          voiceAI.setStatusCallback((status) => {
            setVoiceCallStatus(status.message);
          });

          // Make emergency call
          await voiceAI.initiateEmergencyCall({
            userName: userProfile.name || 'Unknown User',
            age: parseInt(userProfile.age) || 0,
            gender: userProfile.gender || 'Unknown',
            condition: 'Emergency SOS button activated - immediate assistance required',
            location: location,
            timestamp: new Date(),
            medicalInfo: {
              allergies: userProfile.allergies,
              medications: userProfile.currentMedication,
              conditions: userProfile.medicalHistory
            }
          });

          console.log('✅ Voice AI call completed');

        {voiceCallStatus && sosActive && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className="mb-4 px-4 py-2 bg-[#2d1420] border border-[#5A1E3C] rounded-lg flex items-center gap-2"
          >
            <PhoneCall className="w-4 h-4 text-[#E91E63] animate-pulse" />
            <p className="text-xs text-[#e8dff5]">{voiceCallStatus}</p>
          </motion.div>
        )}
        }
      } else {
        setVoiceCallStatus('Voice AI not enabled. Configure in Settings.');
      }
        console.log('🚨 SOS ACTIVATED - Emergency alerts sent to:', contacts);
      // In production, this would:
      // 1. Send SMS/push notifications to emergency contacts
      // 2. Call emergency services if configured
      // 3. Start continuous location sharing
      // 4. Record audio/video if enabled

    } catch (error) {
      console.error('SOS activation error:', error);
      setVoiceCallStatus(`Error: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
    }, 1000);
  };

  return (
    <div className="relative min-h-screen flex flex-col px-6 py-8 overflow-hidden">
      {/* Background gradient */}
      <div className="absolute inset-0 cosmic-gradient" />
      
      {/* Header */}
      <div className="relative z-10 flex justify-between items-start mb-12">
        <div>
          <h2 className="mb-1">RescueMate</h2>
          <p className="uppercase tracking-[0.2em] text-[10px] text-[#a89bb5]">
            Protection Active
          </p>
        </div>
        <Button
          variant="ghost"
          size="icon"
          onClick={() => onNavigate('settings')}
          className="text-[#e8dff5] hover:bg-[#2d1420]"
        >
          <Settings className="w-5 h-5" />
        </Button>
      </div>
      
      {/* Status Indicators */}
      <div className="relative z-10 flex gap-3 mb-12">
        <Badge variant="secondary" className="bg-[#2d1420] text-[#e8dff5] border-[#5A1E3C] px-3 py-1.5">
          <MapPin className="w-3 h-3 mr-1.5" />
          Location: Active
        </Badge>
        <Badge variant="secondary" className="bg-[#2d1420] text-[#e8dff5] border-[#5A1E3C] px-3 py-1.5">
          <Wifi className="w-3 h-3 mr-1.5" />
          Network: Secure
        </Badge>
      </div>
      
      {/* Main SOS Button */}
      <div className="relative z-10 flex-1 flex items-center justify-center flex-col">
        {sosActive && (
          <motion.div
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            className="mb-4 px-4 py-2 bg-[#E91E63] rounded-full flex items-center gap-2"
          >
            <AlertCircle className="w-4 h-4" />
            <p className="text-sm font-semibold">SOS ACTIVE - Help is on the way</p>
          </motion.div>
        )}

        <motion.div
          className="relative"
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.95 }}
        >
          {/* Outer glow ring */}
          <motion.div
            className="absolute inset-0 rounded-full"
            animate={{
              scale: sosActive ? [1, 1.3, 1] : [1, 1.2, 1],
              opacity: sosActive ? [0.7, 0.3, 0.7] : [0.5, 0.2, 0.5],
            }}
            transition={{
              duration: sosActive ? 1 : 2,
              repeat: Infinity,
              ease: "easeInOut",
            }}
            style={{
              background: `radial-gradient(circle, rgba(233, 30, 99, ${sosActive ? '0.6' : '0.4'}) 0%, rgba(233, 30, 99, 0) 70%)`,
            }}
          />
          
          {/* Middle glow ring */}
          <motion.div
            className="absolute inset-0 rounded-full"
            animate={{
              scale: sosActive ? [1, 1.2, 1] : [1, 1.15, 1],
              opacity: sosActive ? [0.8, 0.4, 0.8] : [0.6, 0.3, 0.6],
            }}
            transition={{
              duration: sosActive ? 1 : 2,
              repeat: Infinity,
              ease: "easeInOut",
              delay: 0.3,
            }}
            style={{
              background: `radial-gradient(circle, rgba(233, 30, 99, ${sosActive ? '0.7' : '0.5'}) 0%, rgba(233, 30, 99, 0) 60%)`,
            }}
          />
          
          {/* SOS Button */}
          <Button
            onClick={handleSOSPress}
            className={`relative w-56 h-56 rounded-full ${
              sosActive
                ? 'bg-gradient-to-br from-[#FF1744] to-[#F50057] hover:from-[#FF5252] hover:to-[#FF1744]'
                : 'bg-gradient-to-br from-[#E91E63] to-[#C2185B] hover:from-[#FF1744] hover:to-[#E91E63]'
            } text-white shadow-2xl ${sosActive ? 'animate-pulse' : 'animate-pulse-glow'}`}
            aria-label={sosActive ? "Cancel Emergency Alert" : "Activate Emergency Alert"}
          >
            <div className="flex flex-col items-center gap-2">
              <Shield style={{ width: '100px', height: '100px' }} strokeWidth={2.5} />
              {sosCountdown !== null && (
                <motion.div
                  initial={{ scale: 0 }}
                  animate={{ scale: 1 }}
                  className="text-4xl font-bold"
                >
                  {sosCountdown}
                </motion.div>
              )}
              {sosActive && <span className="text-sm font-semibold">TAP TO CANCEL</span>}
            </div>
          </Button>
        </motion.div>
      </div>
      
      {/* Quick Action Buttons */}
      <div className="relative z-10 grid grid-cols-2 gap-4 mt-12">
        <Button
          variant="outline"
          onClick={() => onNavigate('contacts')}
          className="h-16 bg-[#1a0f23] border-[#5A1E3C] hover:bg-[#2d1420] text-[#e8dff5] flex flex-col items-center gap-1.5"
        >
          <Users className="w-5 h-5" />
          <span className="text-xs">Contacts</span>
        </Button>
        <Button
          variant="outline"
          onClick={() => onNavigate('location')}
          className="h-16 bg-[#1a0f23] border-[#5A1E3C] hover:bg-[#2d1420] text-[#e8dff5] flex flex-col items-center gap-1.5"
        >
          <MapPin className="w-5 h-5" />
          <span className="text-xs">Live Location</span>
        </Button>
      </div>
      
      {/* Safety Tip */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.5 }}
        className="relative z-10 mt-8 p-4 bg-[#2d1420]/50 rounded-lg border border-[#5A1E3C]"
      >
        <p className="uppercase tracking-[0.2em] text-[10px] text-[#a89bb5] mb-2">
          Today's Safety Tip
        </p>
        <p className="text-sm text-[#e8dff5]">
          Keep your emergency contacts updated and ensure location services are enabled for faster response times.
        </p>
      </motion.div>
    </div>
  );
}

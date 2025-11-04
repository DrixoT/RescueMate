import { motion } from "motion/react";
import { Users, Settings, MapPin, Wifi, Shield } from "lucide-react";
import { Button } from "./ui/button";
import { Badge } from "./ui/badge";

interface HomeDashboardProps {
  onNavigate: (screen: string) => void;
}

export function HomeDashboard({ onNavigate }: HomeDashboardProps) {
  const handleSOSPress = () => {
    // Simulate SOS activation
    alert("🚨 SOS ACTIVATED\n\nEmergency services and your contacts have been notified.\nYour location is being shared.");
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
      <div className="relative z-10 flex-1 flex items-center justify-center">
        <motion.div
          className="relative"
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.95 }}
        >
          {/* Outer glow ring */}
          <motion.div
            className="absolute inset-0 rounded-full"
            animate={{
              scale: [1, 1.2, 1],
              opacity: [0.5, 0.2, 0.5],
            }}
            transition={{
              duration: 2,
              repeat: Infinity,
              ease: "easeInOut",
            }}
            style={{
              background: 'radial-gradient(circle, rgba(233, 30, 99, 0.4) 0%, rgba(233, 30, 99, 0) 70%)',
            }}
          />
          
          {/* Middle glow ring */}
          <motion.div
            className="absolute inset-0 rounded-full"
            animate={{
              scale: [1, 1.15, 1],
              opacity: [0.6, 0.3, 0.6],
            }}
            transition={{
              duration: 2,
              repeat: Infinity,
              ease: "easeInOut",
              delay: 0.3,
            }}
            style={{
              background: 'radial-gradient(circle, rgba(233, 30, 99, 0.5) 0%, rgba(233, 30, 99, 0) 60%)',
            }}
          />
          
          {/* SOS Button */}
          <Button
            onClick={handleSOSPress}
            className="relative w-56 h-56 rounded-full bg-gradient-to-br from-[#E91E63] to-[#C2185B] hover:from-[#FF1744] hover:to-[#E91E63] text-white shadow-2xl animate-pulse-glow"
          >
            <Shield style={{ width: '125px', height: '125px' }} strokeWidth={2.5} />
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

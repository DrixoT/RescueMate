import { motion } from "motion/react";
import { ArrowLeft, Bell, MapPin, Palette, Shield, Info, ChevronRight, Mic } from "lucide-react";
import { Button } from "./ui/button";
import { Switch } from "./ui/switch";
import { Card } from "./ui/card";
import { Separator } from "./ui/separator";
import { useState } from "react";

interface SettingsScreenProps {
  onBack: () => void;
}

export function SettingsScreen({ onBack }: SettingsScreenProps) {
  const [autoSendAlert, setAutoSendAlert] = useState(true);
  const [locationTracking, setLocationTracking] = useState(true);
  const [soundAlerts, setSoundAlerts] = useState(true);

  return (
    <div className="relative min-h-screen flex flex-col px-6 py-8 overflow-hidden">
      {/* Background gradient */}
      <div className="absolute inset-0 cosmic-gradient" />
      
      {/* Header */}
      <div className="relative z-10 flex items-center mb-8">
        <Button
          variant="ghost"
          size="icon"
          onClick={onBack}
          className="text-[#e8dff5] hover:bg-[#2d1420] mr-3"
        >
          <ArrowLeft className="w-5 h-5" />
        </Button>
        <div>
          <h2 className="mb-0.5">Settings</h2>
          <p className="uppercase tracking-[0.2em] text-[10px] text-[#a89bb5]">
            Configure Your Protection
          </p>
        </div>
      </div>
      
      {/* Settings Sections */}
      <div className="relative z-10 space-y-6">
        {/* Emergency Settings */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4 }}
        >
          <div className="mb-3">
            <p className="uppercase tracking-[0.2em] text-[10px] text-[#a89bb5]">
              Emergency Settings
            </p>
          </div>
          
          <Card className="p-0 bg-[#1a0f23] border-[#5A1E3C] overflow-hidden">
            {/* Auto-Send Alert */}
            <div className="flex items-center justify-between p-4">
              <div className="flex items-center gap-3 flex-1">
                <div className="w-10 h-10 rounded-full bg-[#2d1420] flex items-center justify-center">
                  <Bell className="w-5 h-5 text-[#E91E63]" />
                </div>
                <div className="flex-1">
                  <h4 className="text-[#e8dff5] mb-1">Auto-Send Alert</h4>
                  <p className="text-xs text-[#a89bb5]">
                    Automatically notify contacts on SOS
                  </p>
                </div>
              </div>
              <Switch
                checked={autoSendAlert}
                onCheckedChange={setAutoSendAlert}
                className="data-[state=checked]:bg-[#E91E63]"
              />
            </div>
            
            <Separator className="bg-[#5A1E3C]" />
            
            {/* Location Tracking */}
            <div className="flex items-center justify-between p-4">
              <div className="flex items-center gap-3 flex-1">
                <div className="w-10 h-10 rounded-full bg-[#2d1420] flex items-center justify-center">
                  <MapPin className="w-5 h-5 text-[#E91E63]" />
                </div>
                <div className="flex-1">
                  <h4 className="text-[#e8dff5] mb-1">Location Tracking</h4>
                  <p className="text-xs text-[#a89bb5]">
                    Share real-time location during emergency
                  </p>
                </div>
              </div>
              <Switch
                checked={locationTracking}
                onCheckedChange={setLocationTracking}
                className="data-[state=checked]:bg-[#E91E63]"
              />
            </div>
            
            <Separator className="bg-[#5A1E3C]" />
            
            {/* Sound Alerts */}
            <div className="flex items-center justify-between p-4">
              <div className="flex items-center gap-3 flex-1">
                <div className="w-10 h-10 rounded-full bg-[#2d1420] flex items-center justify-center">
                  <Shield className="w-5 h-5 text-[#E91E63]" />
                </div>
                <div className="flex-1">
                  <h4 className="text-[#e8dff5] mb-1">Sound Alerts</h4>
                  <p className="text-xs text-[#a89bb5]">
                    Play alert sound when SOS is activated
                  </p>
                </div>
              </div>
              <Switch
                checked={soundAlerts}
                onCheckedChange={setSoundAlerts}
                className="data-[state=checked]:bg-[#E91E63]"
              />
            </div>
          </Card>
        </motion.div>
        
        {/* AI & Automation */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, delay: 0.1 }}
        >
          <div className="mb-3">
            <p className="uppercase tracking-[0.2em] text-[10px] text-[#a89bb5]">
              AI & Automation
            </p>
          </div>
          
          <Card className="p-0 bg-[#1a0f23] border-[#5A1E3C] overflow-hidden">
            <button className="w-full flex items-center justify-between p-4 hover:bg-[#2d1420] transition-colors">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-[#2d1420] flex items-center justify-center">
                  <Mic className="w-5 h-5 text-[#E91E63]" />
                </div>
                <div className="text-left">
                  <h4 className="text-[#e8dff5] mb-1">Setup Voice AI</h4>
                  <p className="text-xs text-[#a89bb5]">
                    Voice-activated emergency assistance
                  </p>
                </div>
              </div>
              <ChevronRight className="w-5 h-5 text-[#a89bb5]" />
            </button>
          </Card>
        </motion.div>
        
        {/* Appearance */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, delay: 0.15 }}
        >
          <div className="mb-3">
            <p className="uppercase tracking-[0.2em] text-[10px] text-[#a89bb5]">
              Appearance
            </p>
          </div>
          
          <Card className="p-0 bg-[#1a0f23] border-[#5A1E3C] overflow-hidden">
            <button className="w-full flex items-center justify-between p-4 hover:bg-[#2d1420] transition-colors">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-[#2d1420] flex items-center justify-center">
                  <Palette className="w-5 h-5 text-[#E91E63]" />
                </div>
                <div className="text-left">
                  <h4 className="text-[#e8dff5] mb-1">Theme</h4>
                  <p className="text-xs text-[#a89bb5]">
                    Cosmic Dark (Default)
                  </p>
                </div>
              </div>
              <ChevronRight className="w-5 h-5 text-[#a89bb5]" />
            </button>
          </Card>
        </motion.div>
        
        {/* About */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, delay: 0.2 }}
        >
          <div className="mb-3">
            <p className="uppercase tracking-[0.2em] text-[10px] text-[#a89bb5]">
              About
            </p>
          </div>
          
          <Card className="p-0 bg-[#1a0f23] border-[#5A1E3C] overflow-hidden">
            <button className="w-full flex items-center justify-between p-4 hover:bg-[#2d1420] transition-colors">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-[#2d1420] flex items-center justify-center">
                  <Info className="w-5 h-5 text-[#E91E63]" />
                </div>
                <div className="text-left">
                  <h4 className="text-[#e8dff5] mb-1">App Information</h4>
                  <p className="text-xs text-[#a89bb5]">
                    Version 1.0.0
                  </p>
                </div>
              </div>
              <ChevronRight className="w-5 h-5 text-[#a89bb5]" />
            </button>
          </Card>
        </motion.div>
      </div>
      
      {/* Privacy Notice */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.5 }}
        className="relative z-10 mt-auto pt-8"
      >
        <div className="p-4 bg-[#2d1420]/50 rounded-lg border border-[#5A1E3C]">
          <p className="uppercase tracking-[0.2em] text-[10px] text-[#a89bb5] mb-2">
            Privacy & Security
          </p>
          <p className="text-xs text-[#e8dff5] leading-relaxed">
            Your location and personal data are encrypted end-to-end. Information is only shared with your designated emergency contacts when you activate SOS.
          </p>
        </div>
      </motion.div>
    </div>
  );
}

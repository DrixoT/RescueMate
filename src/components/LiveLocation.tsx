import { motion } from "motion/react";
import { ArrowLeft, MapPin, Navigation, Share2, Clock, AlertCircle } from "lucide-react";
import { Button } from "./ui/button";
import { Badge } from "./ui/badge";
import { useState, useEffect } from "react";

interface LiveLocationProps {
  onBack: () => void;
}

interface LocationData {
  latitude: number;
  longitude: number;
  accuracy: number;
  timestamp: number;
}

export function LiveLocation({ onBack }: LiveLocationProps) {
  const [isSharing, setIsSharing] = useState(false);
  const [location, setLocation] = useState<LocationData | null>(null);
  const [error, setError] = useState<string>("");
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null);

  useEffect(() => {
    if (isSharing) {
      // Start watching location
      if ("geolocation" in navigator) {
        const watchId = navigator.geolocation.watchPosition(
          (position) => {
            setLocation({
              latitude: position.coords.latitude,
              longitude: position.coords.longitude,
              accuracy: position.coords.accuracy,
              timestamp: position.timestamp,
            });
            setLastUpdate(new Date());
            setError("");
          },
          (err) => {
            setError(err.message);
            console.error("Geolocation error:", err);
          },
          {
            enableHighAccuracy: true,
            timeout: 5000,
            maximumAge: 0,
          }
        );

        return () => navigator.geolocation.clearWatch(watchId);
      } else {
        setError("Geolocation is not supported by your browser");
      }
    }
  }, [isSharing]);

  const handleShareLocation = () => {
    if (!isSharing && !("geolocation" in navigator)) {
      setError("Geolocation is not supported by your browser");
      return;
    }
    setIsSharing(!isSharing);
    if (!isSharing) {
      // Request initial location
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setLocation({
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
            accuracy: position.coords.accuracy,
            timestamp: position.timestamp,
          });
          setLastUpdate(new Date());
        },
        (err) => {
          setError(err.message);
        }
      );
    }
  };

  const formatCoordinate = (coord: number, isLat: boolean) => {
    const direction = isLat ? (coord >= 0 ? 'N' : 'S') : (coord >= 0 ? 'E' : 'W');
    return `${Math.abs(coord).toFixed(4)}° ${direction}`;
  };

  const getAccuracyLevel = (accuracy: number) => {
    if (accuracy < 10) return 'Excellent';
    if (accuracy < 50) return 'High';
    if (accuracy < 100) return 'Medium';
    return 'Low';
  };

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
        <div className="flex-1">
          <h2 className="mb-0.5">Live Location</h2>
          <p className="uppercase tracking-[0.2em] text-[10px] text-[#a89bb5]">
            Real-Time Tracking
          </p>
        </div>
      </div>
      
      {/* Status Badge */}
      <div className="relative z-10 mb-6">
        {isSharing ? (
          <Badge className="bg-[#E91E63] text-white px-3 py-1.5">
            <motion.div
              animate={{ scale: [1, 1.2, 1] }}
              transition={{ duration: 1.5, repeat: Infinity }}
              className="w-2 h-2 bg-white rounded-full mr-2"
            />
            Location Sharing Active
          </Badge>
        ) : (
          <Badge variant="secondary" className="bg-[#2d1420] text-[#a89bb5] border-[#5A1E3C] px-3 py-1.5">
            Location Sharing Inactive
          </Badge>
        )}
      </div>
      
      {/* Map Container */}
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.4 }}
        className="relative z-10 flex-1 rounded-2xl overflow-hidden border-2 border-[#5A1E3C] mb-6"
        style={{
          minHeight: '400px',
        }}
      >
        {/* Mock Map Background */}
        <div className="absolute inset-0 bg-[#0a0510]">
          {/* Grid pattern for map aesthetic */}
          <div className="absolute inset-0" style={{
            backgroundImage: 'linear-gradient(rgba(90, 30, 60, 0.1) 1px, transparent 1px), linear-gradient(90deg, rgba(90, 30, 60, 0.1) 1px, transparent 1px)',
            backgroundSize: '50px 50px',
          }} />
        </div>
        
        {/* Center Marker with Glow */}
        <div className="absolute inset-0 flex items-center justify-center">
          <motion.div
            animate={{
              scale: [1, 1.1, 1],
            }}
            transition={{
              duration: 2,
              repeat: Infinity,
              ease: "easeInOut",
            }}
            className="relative"
          >
            {/* Outer glow */}
            <div className="absolute inset-0 rounded-full cosmic-glow-strong blur-2xl" />
            
            {/* Marker */}
            <div className="relative w-20 h-20 rounded-full bg-gradient-to-br from-[#E91E63] to-[#C2185B] flex items-center justify-center border-4 border-[#0a0510]">
              <Navigation className="w-10 h-10 text-white" />
            </div>
          </motion.div>
        </div>
        
        {/* Coordinates Overlay */}
        <div className="absolute top-4 left-4 right-4">
          <div className="bg-[#1a0f23]/90 backdrop-blur-sm rounded-lg p-3 border border-[#5A1E3C]">
            {error ? (
              <div className="flex items-start gap-2">
                <AlertCircle className="w-4 h-4 text-[#E91E63] mt-0.5 flex-shrink-0" />
                <div className="flex-1 min-w-0">
                  <p className="uppercase tracking-[0.15em] text-[10px] text-[#a89bb5] mb-1">
                    Location Error
                  </p>
                  <p className="text-xs text-[#e8dff5]">
                    {error}
                  </p>
                </div>
              </div>
            ) : location ? (
              <div className="flex items-start gap-2">
                <MapPin className="w-4 h-4 text-[#E91E63] mt-0.5 flex-shrink-0" />
                <div className="flex-1 min-w-0">
                  <p className="uppercase tracking-[0.15em] text-[10px] text-[#a89bb5] mb-1">
                    Current Location
                  </p>
                  <p className="text-sm text-[#e8dff5]">
                    {formatCoordinate(location.latitude, true)}, {formatCoordinate(location.longitude, false)}
                  </p>
                  <p className="text-xs text-[#a89bb5] mt-1">
                    Accuracy: {getAccuracyLevel(location.accuracy)} ({Math.round(location.accuracy)}m)
                  </p>
                </div>
              </div>
            ) : (
              <div className="flex items-start gap-2">
                <MapPin className="w-4 h-4 text-[#a89bb5] mt-0.5 flex-shrink-0" />
                <div className="flex-1 min-w-0">
                  <p className="uppercase tracking-[0.15em] text-[10px] text-[#a89bb5] mb-1">
                    Location
                  </p>
                  <p className="text-sm text-[#a89bb5]">
                    Waiting for location data...
                  </p>
                </div>
              </div>
            )}
          </div>
        </div>
        
        {/* Last Updated */}
        {isSharing && lastUpdate && (
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="absolute bottom-4 left-4 right-4"
          >
            <div className="bg-[#1a0f23]/90 backdrop-blur-sm rounded-lg p-3 border border-[#5A1E3C]">
              <div className="flex items-center gap-2">
                <Clock className="w-3.5 h-3.5 text-[#a89bb5]" />
                <p className="text-xs text-[#a89bb5]">
                  Last updated: {lastUpdate.toLocaleTimeString()} • Accuracy: {location ? getAccuracyLevel(location.accuracy) : 'Unknown'}
                </p>
              </div>
            </div>
          </motion.div>
        )}
      </motion.div>
      
      {/* Share Location Button */}
      <div className="relative z-10 space-y-3">
        <Button
          onClick={handleShareLocation}
          className={`w-full h-14 rounded-full transition-all duration-300 ${
            isSharing
              ? 'bg-[#2d1420] hover:bg-[#1a0f23] text-[#e8dff5] border border-[#5A1E3C]'
              : 'bg-[#E91E63] hover:bg-[#C2185B] text-white cosmic-glow'
          }`}
        >
          <Share2 className="w-5 h-5 mr-2" />
          {isSharing ? 'Stop Sharing Location' : 'Share My Location'}
        </Button>
        
        {/* Info */}
        <p className="text-center text-xs text-[#a89bb5]">
          {isSharing
            ? 'Your location is being shared with emergency contacts'
            : 'Share your real-time location with trusted contacts'}
        </p>
      </div>
    </div>
  );
}

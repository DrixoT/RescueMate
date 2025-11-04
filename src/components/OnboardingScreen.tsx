import { motion } from "motion/react";
import { Button } from "./ui/button";
import { Shield } from "lucide-react";

interface OnboardingScreenProps {
  onStart: () => void;
}

export function OnboardingScreen({ onStart }: OnboardingScreenProps) {
  return (
    <div className="relative min-h-screen flex flex-col items-center justify-between px-6 py-12 overflow-hidden">
      {/* Cosmic Background */}
      <div 
        className="absolute inset-0 opacity-20"
        style={{
          backgroundImage: `url('https://images.unsplash.com/photo-1615392030676-6c532fe0c302?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxkYXJrJTIwcHVycGxlJTIwY29zbW9zJTIwc3RhcnN8ZW58MXx8fHwxNzYyMjExNTcxfDA&ixlib=rb-4.1.0&q=80&w=1080&utm_source=figma&utm_medium=referral')`,
          backgroundSize: 'cover',
          backgroundPosition: 'center',
        }}
      />
      
      {/* Gradient Overlay */}
      <div className="absolute inset-0 cosmic-gradient opacity-80" />
      
      {/* Content */}
      <div className="relative z-10 flex flex-col items-center justify-center flex-1 max-w-md">
        {/* Logo Icon */}
        <motion.div
          initial={{ scale: 0, rotate: -180 }}
          animate={{ scale: 1, rotate: 0 }}
          transition={{ duration: 0.8, ease: "easeOut" }}
          className="mb-8"
        >
          <div className="relative">
            <Shield className="w-24 h-24 text-[#E91E63]" strokeWidth={1.5} />
            <div className="absolute inset-0 cosmic-glow-strong blur-xl" />
          </div>
        </motion.div>
        
        {/* App Name */}
        <motion.h1
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.3 }}
          className="mb-3 tracking-tight text-center"
        >
          RescueMate
        </motion.h1>
        
        {/* Label */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 0.6, delay: 0.5 }}
          className="mb-12"
        >
          <p className="uppercase tracking-[0.2em] text-[10px] text-[#a89bb5] text-center">
            Emergency Response System
          </p>
        </motion.div>
        
        {/* Reassurance Message */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.7 }}
          className="mb-16 text-center space-y-4"
        >
          <p className="text-[#e8dff5] leading-relaxed">
            You're never alone
          </p>
          <p className="text-[#a89bb5] text-sm leading-relaxed px-4">
            Your safety network is always one tap away. We're here to protect you, guide you, and keep you connected.
          </p>
        </motion.div>
      </div>
      
      {/* CTA Button */}
      <motion.div
        initial={{ opacity: 0, y: 30 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6, delay: 0.9 }}
        className="relative z-10 w-full max-w-md"
      >
        <Button
          onClick={onStart}
          className="w-full h-14 bg-[#E91E63] hover:bg-[#C2185B] text-white rounded-full cosmic-glow transition-all duration-300"
        >
          Get Started
        </Button>
      </motion.div>
    </div>
  );
}

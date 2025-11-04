import { motion } from "motion/react";
import { Shield, Mail, Phone, Chrome, Apple } from "lucide-react";
import { Button } from "./ui/button";
import { Separator } from "./ui/separator";

interface SignInScreenProps {
  onSignIn: () => void;
  onSignUp: () => void;
}

export function SignInScreen({ onSignIn, onSignUp }: SignInScreenProps) {
  return (
    <div className="relative min-h-screen flex flex-col px-6 py-12 overflow-hidden">
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
      <div className="relative z-10 flex flex-col justify-center flex-1 max-w-md mx-auto w-full">
        {/* Logo */}
        <motion.div
          initial={{ scale: 0, rotate: -180 }}
          animate={{ scale: 1, rotate: 0 }}
          transition={{ duration: 0.6 }}
          className="flex flex-col items-center mb-12"
        >
          <div className="relative mb-4">
            <Shield className="w-16 h-16 text-[#E91E63]" strokeWidth={1.5} />
            <div className="absolute inset-0 cosmic-glow blur-xl" />
          </div>
          <h1 className="mb-2">Welcome Back</h1>
          <p className="uppercase tracking-[0.2em] text-[10px] text-[#a89bb5]">
            Sign in to continue
          </p>
        </motion.div>
        
        {/* Sign In Options */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.2 }}
          className="space-y-3 mb-6"
        >
          {/* Google Sign In */}
          <Button
            onClick={onSignIn}
            variant="outline"
            className="w-full h-14 bg-[#1a0f23] border-[#5A1E3C] hover:bg-[#2d1420] text-[#e8dff5]"
          >
            <Chrome className="w-5 h-5 mr-3" />
            Continue with Google
          </Button>
          
          {/* Apple Sign In */}
          <Button
            onClick={onSignIn}
            variant="outline"
            className="w-full h-14 bg-[#1a0f23] border-[#5A1E3C] hover:bg-[#2d1420] text-[#e8dff5]"
          >
            <Apple className="w-5 h-5 mr-3" />
            Continue with Apple
          </Button>
          
          {/* Phone Sign In */}
          <Button
            onClick={onSignIn}
            variant="outline"
            className="w-full h-14 bg-[#1a0f23] border-[#5A1E3C] hover:bg-[#2d1420] text-[#e8dff5]"
          >
            <Phone className="w-5 h-5 mr-3" />
            Continue with Phone
          </Button>
          
          {/* Email Sign In */}
          <Button
            onClick={onSignIn}
            variant="outline"
            className="w-full h-14 bg-[#1a0f23] border-[#5A1E3C] hover:bg-[#2d1420] text-[#e8dff5]"
          >
            <Mail className="w-5 h-5 mr-3" />
            Continue with Email
          </Button>
        </motion.div>
        
        {/* Divider */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 0.5, delay: 0.4 }}
          className="flex items-center gap-4 my-6"
        >
          <Separator className="flex-1 bg-[#5A1E3C]" />
          <p className="uppercase tracking-[0.2em] text-[10px] text-[#a89bb5]">
            New User?
          </p>
          <Separator className="flex-1 bg-[#5A1E3C]" />
        </motion.div>
        
        {/* Sign Up Button */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.5 }}
        >
          <Button
            onClick={onSignUp}
            className="w-full h-14 bg-[#E91E63] hover:bg-[#C2185B] text-white rounded-full cosmic-glow"
          >
            Create New Account
          </Button>
        </motion.div>
        
        {/* Privacy Note */}
        <motion.p
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.7 }}
          className="text-center text-xs text-[#a89bb5] mt-8 leading-relaxed"
        >
          By continuing, you agree to our Terms of Service and Privacy Policy
        </motion.p>
      </div>
    </div>
  );
}

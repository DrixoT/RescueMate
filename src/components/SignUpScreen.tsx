import { motion } from "motion/react";
import { ArrowLeft, Shield } from "lucide-react";
import { Button } from "./ui/button";
import { Input } from "./ui/input";
import { Label } from "./ui/label";
import { Textarea } from "./ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "./ui/select";
import { useState } from "react";

interface SignUpScreenProps {
  onBack: () => void;
  onComplete: () => void;
}

export function SignUpScreen({ onBack, onComplete }: SignUpScreenProps) {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formData, setFormData] = useState({
    name: "",
    age: "",
    gender: "",
    phone: "",
    medicalHistory: "",
    currentMedication: "",
    allergies: "",
  });

  const validateForm = () => {
    const newErrors: Record<string, string> = {};

    if (!formData.name.trim()) {
      newErrors.name = "Name is required";
    }

    if (!formData.age) {
      newErrors.age = "Age is required";
    } else if (parseInt(formData.age) < 1 || parseInt(formData.age) > 120) {
      newErrors.age = "Please enter a valid age";
    }

    if (!formData.gender) {
      newErrors.gender = "Gender is required";
    }

    if (!formData.phone.trim()) {
      newErrors.phone = "Phone number is required";
    } else if (!/^[\d\s\-\+\(\)]+$/.test(formData.phone)) {
      newErrors.phone = "Please enter a valid phone number";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    setIsSubmitting(true);

    // Simulate API call
    setTimeout(() => {
      // Save user profile to localStorage
      localStorage.setItem('rescuemate_user_profile', JSON.stringify(formData));
      setIsSubmitting(false);
      onComplete();
    }, 500);
  };

  return (
    <div className="relative min-h-screen flex flex-col overflow-hidden">
      {/* Background gradient */}
      <div className="absolute inset-0 cosmic-gradient" />
      
      {/* Header */}
      <div className="relative z-10 flex items-center px-6 py-8">
        <Button
          variant="ghost"
          size="icon"
          onClick={onBack}
          className="text-[#e8dff5] hover:bg-[#2d1420] mr-3"
        >
          <ArrowLeft className="w-5 h-5" />
        </Button>
        <div className="flex-1">
          <h2 className="mb-0.5">Create Account</h2>
          <p className="uppercase tracking-[0.2em] text-[10px] text-[#a89bb5]">
            Your Safety Profile
          </p>
        </div>
        <Shield className="w-6 h-6 text-[#E91E63]" strokeWidth={1.5} />
      </div>
      
      {/* Form */}
      <div className="relative z-10 flex-1 px-6 pb-8 overflow-y-auto">
        <form onSubmit={handleSubmit} className="space-y-6 max-w-md mx-auto">
          {/* Personal Information Section */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4 }}
            className="space-y-4"
          >
            <div>
              <p className="uppercase tracking-[0.2em] text-[10px] text-[#a89bb5] mb-4">
                Personal Information
              </p>
            </div>
            
            {/* Name */}
            <div className="space-y-2">
              <Label htmlFor="name" className="text-[#e8dff5]">
                Full Name *
              </Label>
              <Input
                id="name"
                type="text"
                required
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                className="bg-[#1a0f23] border-[#5A1E3C] text-[#e8dff5] focus:border-[#E91E63] h-12"
                placeholder="Enter your full name"
              />
            </div>
            
            {/* Age and Gender */}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="age" className="text-[#e8dff5]">
                  Age *
                </Label>
                <Input
                  id="age"
                  type="number"
                  required
                  value={formData.age}
                  onChange={(e) => setFormData({ ...formData, age: e.target.value })}
                  className="bg-[#1a0f23] border-[#5A1E3C] text-[#e8dff5] focus:border-[#E91E63] h-12"
                  placeholder="25"
                  min="1"
                  max="120"
                />
              </div>
              
              <div className="space-y-2">
                <Label htmlFor="gender" className="text-[#e8dff5]">
                  Gender *
                </Label>
                <Select
                  value={formData.gender}
                  onValueChange={(value) => setFormData({ ...formData, gender: value })}
                >
                  <SelectTrigger className="bg-[#1a0f23] border-[#5A1E3C] text-[#e8dff5] focus:border-[#E91E63] h-12">
                    <SelectValue placeholder="Select" />
                  </SelectTrigger>
                  <SelectContent className="bg-[#1a0f23] border-[#5A1E3C]">
                    <SelectItem value="male" className="text-[#e8dff5]">Male</SelectItem>
                    <SelectItem value="female" className="text-[#e8dff5]">Female</SelectItem>
                    <SelectItem value="non-binary" className="text-[#e8dff5]">Non-binary</SelectItem>
                    <SelectItem value="prefer-not-to-say" className="text-[#e8dff5]">Prefer not to say</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
            
            {/* Phone Number */}
            <div className="space-y-2">
              <Label htmlFor="phone" className="text-[#e8dff5]">
                Phone Number *
              </Label>
              <Input
                id="phone"
                type="tel"
                required
                value={formData.phone}
                onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                className="bg-[#1a0f23] border-[#5A1E3C] text-[#e8dff5] focus:border-[#E91E63] h-12"
                placeholder="+1 (555) 000-0000"
              />
            </div>
          </motion.div>
          
          {/* Medical Information Section */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, delay: 0.1 }}
            className="space-y-4"
          >
            <div>
              <p className="uppercase tracking-[0.2em] text-[10px] text-[#a89bb5] mb-1">
                Medical Information
              </p>
              <p className="text-xs text-[#a89bb5]">
                This information will be shared with emergency responders
              </p>
            </div>
            
            {/* Medical History */}
            <div className="space-y-2">
              <Label htmlFor="medical-history" className="text-[#e8dff5]">
                Medical History
              </Label>
              <Textarea
                id="medical-history"
                value={formData.medicalHistory}
                onChange={(e) => setFormData({ ...formData, medicalHistory: e.target.value })}
                className="bg-[#1a0f23] border-[#5A1E3C] text-[#e8dff5] focus:border-[#E91E63] min-h-[100px] resize-none"
                placeholder="Any chronic conditions, past surgeries, or relevant medical history..."
              />
            </div>
            
            {/* Current Medication */}
            <div className="space-y-2">
              <Label htmlFor="medication" className="text-[#e8dff5]">
                Current Medication
              </Label>
              <Textarea
                id="medication"
                value={formData.currentMedication}
                onChange={(e) => setFormData({ ...formData, currentMedication: e.target.value })}
                className="bg-[#1a0f23] border-[#5A1E3C] text-[#e8dff5] focus:border-[#E91E63] min-h-[80px] resize-none"
                placeholder="List any medications you're currently taking..."
              />
            </div>
            
            {/* Allergies */}
            <div className="space-y-2">
              <Label htmlFor="allergies" className="text-[#e8dff5]">
                Allergies
              </Label>
              <Textarea
                id="allergies"
                value={formData.allergies}
                onChange={(e) => setFormData({ ...formData, allergies: e.target.value })}
                className="bg-[#1a0f23] border-[#5A1E3C] text-[#e8dff5] focus:border-[#E91E63] min-h-[80px] resize-none"
                placeholder="Food, medication, or environmental allergies..."
              />
            </div>
          </motion.div>
          
          {/* Submit Button */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, delay: 0.2 }}
            className="pt-4"
          >
            <Button
              type="submit"
              className="w-full h-14 bg-[#E91E63] hover:bg-[#C2185B] text-white rounded-full cosmic-glow"
            >
              Complete Registration
            </Button>
          </motion.div>
        </form>
      </div>
    </div>
  );
}

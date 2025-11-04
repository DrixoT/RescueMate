import { motion } from "motion/react";
import { ArrowLeft, Star, User } from "lucide-react";
import { Button } from "./ui/button";
import { Input } from "./ui/input";
import { Label } from "./ui/label";
import { Switch } from "./ui/switch";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "./ui/select";
import { useState } from "react";

interface AddContactScreenProps {
  onBack: () => void;
  onSave: () => void;
}

export function AddContactScreen({ onBack, onSave }: AddContactScreenProps) {
  const [isPrimary, setIsPrimary] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formData, setFormData] = useState({
    name: "",
    relationship: "",
    phone: "",
    email: "",
  });

  const validateForm = () => {
    const newErrors: Record<string, string> = {};

    if (!formData.name.trim()) {
      newErrors.name = "Name is required";
    }

    if (!formData.relationship) {
      newErrors.relationship = "Relationship is required";
    }

    if (!formData.phone.trim()) {
      newErrors.phone = "Phone number is required";
    } else if (!/^[\d\s\-\+\(\)]+$/.test(formData.phone)) {
      newErrors.phone = "Please enter a valid phone number";
    }

    if (formData.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      newErrors.email = "Please enter a valid email address";
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
      // Save to localStorage
      const existingContacts = JSON.parse(localStorage.getItem('rescuemate_contacts') || '[]');
      const newContact = {
        id: Date.now(),
        name: formData.name,
        relationship: formData.relationship,
        phone: formData.phone,
        email: formData.email,
        isPrimary: isPrimary,
      };

      existingContacts.push(newContact);
      localStorage.setItem('rescuemate_contacts', JSON.stringify(existingContacts));

      setIsSubmitting(false);
      onSave();
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
          <h2 className="mb-0.5">Add Emergency Contact</h2>
          <p className="uppercase tracking-[0.2em] text-[10px] text-[#a89bb5]">
            Expand Your Safety Network
          </p>
        </div>
        <User className="w-6 h-6 text-[#E91E63]" strokeWidth={1.5} />
      </div>
      
      {/* Form */}
      <div className="relative z-10 flex-1 px-6 pb-8 overflow-y-auto">
        <form onSubmit={handleSubmit} className="space-y-6 max-w-md mx-auto">
          {/* Primary Contact Toggle */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4 }}
            className="p-4 bg-[#1a0f23] rounded-xl border border-[#5A1E3C]"
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3 flex-1">
                <Star className={`w-5 h-5 ${isPrimary ? 'text-[#E91E63] fill-[#E91E63]' : 'text-[#a89bb5]'}`} />
                <div>
                  <h4 className="text-[#e8dff5] mb-1">Primary Contact</h4>
                  <p className="text-xs text-[#a89bb5]">
                    Auto-notified during emergencies
                  </p>
                </div>
              </div>
              <Switch
                checked={isPrimary}
                onCheckedChange={setIsPrimary}
                className="data-[state=checked]:bg-[#E91E63]"
              />
            </div>
          </motion.div>
          
          {/* Contact Information */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, delay: 0.1 }}
            className="space-y-4"
          >
            <div>
              <p className="uppercase tracking-[0.2em] text-[10px] text-[#a89bb5] mb-4">
                Contact Details
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
                onChange={(e) => {
                  setFormData({ ...formData, name: e.target.value });
                  if (errors.name) setErrors({ ...errors, name: "" });
                }}
                className={`bg-[#1a0f23] border-[#5A1E3C] text-[#e8dff5] focus:border-[#E91E63] h-12 ${
                  errors.name ? 'border-[#E91E63]' : ''
                }`}
                placeholder="Enter contact name"
                aria-invalid={!!errors.name}
                aria-describedby={errors.name ? "name-error" : undefined}
              />
              {errors.name && (
                <p id="name-error" className="text-xs text-[#E91E63]">{errors.name}</p>
              )}
            </div>
            
            {/* Relationship */}
            <div className="space-y-2">
              <Label htmlFor="relationship" className="text-[#e8dff5]">
                Relationship *
              </Label>
              <Select
                value={formData.relationship}
                onValueChange={(value) => {
                  setFormData({ ...formData, relationship: value });
                  if (errors.relationship) setErrors({ ...errors, relationship: "" });
                }}
              >
                <SelectTrigger className={`bg-[#1a0f23] border-[#5A1E3C] text-[#e8dff5] focus:border-[#E91E63] h-12 ${
                  errors.relationship ? 'border-[#E91E63]' : ''
                }`}>
                  <SelectValue placeholder="Select relationship" />
                </SelectTrigger>
                <SelectContent className="bg-[#1a0f23] border-[#5A1E3C]">
                  <SelectItem value="family" className="text-[#e8dff5]">Family Member</SelectItem>
                  <SelectItem value="friend" className="text-[#e8dff5]">Friend</SelectItem>
                  <SelectItem value="spouse" className="text-[#e8dff5]">Spouse/Partner</SelectItem>
                  <SelectItem value="doctor" className="text-[#e8dff5]">Doctor</SelectItem>
                  <SelectItem value="colleague" className="text-[#e8dff5]">Colleague</SelectItem>
                  <SelectItem value="neighbor" className="text-[#e8dff5]">Neighbor</SelectItem>
                  <SelectItem value="other" className="text-[#e8dff5]">Other</SelectItem>
                </SelectContent>
              </Select>
              {errors.relationship && (
                <p id="relationship-error" className="text-xs text-[#E91E63]">{errors.relationship}</p>
              )}
            </div>
            
            {/* Phone Number */}
            <div className="space-y-2">
                onChange={(e) => {
                  setFormData({ ...formData, phone: e.target.value });
                  if (errors.phone) setErrors({ ...errors, phone: "" });
                }}
                className={`bg-[#1a0f23] border-[#5A1E3C] text-[#e8dff5] focus:border-[#E91E63] h-12 ${
                  errors.phone ? 'border-[#E91E63]' : ''
                }`}
              </Label>
                aria-invalid={!!errors.phone}
                aria-describedby={errors.phone ? "phone-error" : undefined}
              <Input
              {errors.phone && (
                <p id="phone-error" className="text-xs text-[#E91E63]">{errors.phone}</p>
              )}
                id="phone"
                type="tel"
                required
                value={formData.phone}
                onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                className="bg-[#1a0f23] border-[#5A1E3C] text-[#e8dff5] focus:border-[#E91E63] h-12"
                placeholder="+1 (555) 000-0000"
              />
            </div>
            
            {/* Email (Optional) */}
                onChange={(e) => {
                  setFormData({ ...formData, email: e.target.value });
                  if (errors.email) setErrors({ ...errors, email: "" });
                }}
                className={`bg-[#1a0f23] border-[#5A1E3C] text-[#e8dff5] focus:border-[#E91E63] h-12 ${
                  errors.email ? 'border-[#E91E63]' : ''
                }`}
                Email Address <span className="text-[#a89bb5] text-xs">(Optional)</span>
                aria-invalid={!!errors.email}
                aria-describedby={errors.email ? "email-error" : undefined}
              </Label>
              {errors.email && (
                <p id="email-error" className="text-xs text-[#E91E63]">{errors.email}</p>
              )}
              <Input
                id="email"
                type="email"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                className="bg-[#1a0f23] border-[#5A1E3C] text-[#e8dff5] focus:border-[#E91E63] h-12"
                placeholder="contact@example.com"
              />
            </div>
              disabled={isSubmitting}
          </motion.div>
          
          {/* Info Box */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
              disabled={isSubmitting}
            transition={{ delay: 0.3 }}
            className="p-4 bg-[#2d1420]/50 rounded-lg border border-[#5A1E3C]"
              {isSubmitting ? 'Saving...' : 'Save Contact'}
            <p className="uppercase tracking-[0.2em] text-[10px] text-[#a89bb5] mb-2">
              Important
            </p>
            <p className="text-xs text-[#e8dff5] leading-relaxed">
              This contact will receive your location and status updates when you activate SOS. Make sure they can be reached 24/7.
            </p>
          </motion.div>
          
          {/* Buttons */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, delay: 0.2 }}
            className="flex gap-3 pt-4"
          >
            <Button
              type="button"
              variant="outline"
              onClick={onBack}
              className="flex-1 h-14 bg-transparent border-[#5A1E3C] hover:bg-[#2d1420] text-[#e8dff5]"
            >
              Cancel
            </Button>
            <Button
              type="submit"
              className="flex-1 h-14 bg-[#E91E63] hover:bg-[#C2185B] text-white rounded-full cosmic-glow"
            >
              Save Contact
            </Button>
          </motion.div>
        </form>
      </div>
    </div>
  );
}

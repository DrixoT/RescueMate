import { motion } from "motion/react";
import { ArrowLeft, Phone, MessageSquare, Plus, Star } from "lucide-react";
import { Button } from "./ui/button";
import { Card } from "./ui/card";

interface EmergencyContactsProps {
  onBack: () => void;
  onAddContact: () => void;
}

const contacts = [
  { id: 1, name: "Sarah Mitchell", relationship: "Primary Contact", phone: "+1 (555) 0123", isPrimary: true },
  { id: 2, name: "Dr. James Chen", relationship: "Emergency Physician", phone: "+1 (555) 0456", isPrimary: false },
  { id: 3, name: "Alex Johnson", relationship: "Family", phone: "+1 (555) 0789", isPrimary: false },
  { id: 4, name: "911 Emergency", relationship: "Emergency Services", phone: "911", isPrimary: false },
];

export function EmergencyContacts({ onBack, onAddContact }: EmergencyContactsProps) {
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
          <h2 className="mb-0.5">Emergency Contacts</h2>
          <p className="uppercase tracking-[0.2em] text-[10px] text-[#a89bb5]">
            Your Safety Network
          </p>
        </div>
        <Button
          variant="ghost"
          size="icon"
          onClick={onAddContact}
          className="text-[#E91E63] hover:bg-[#2d1420]"
        >
          <Plus className="w-5 h-5" />
        </Button>
      </div>
      
      {/* Contacts List */}
      <div className="relative z-10 space-y-4">
        {contacts.map((contact, index) => (
          <motion.div
            key={contact.id}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, delay: index * 0.1 }}
          >
            <Card className="p-4 bg-[#1a0f23] border-[#5A1E3C] hover:bg-[#2d1420] transition-colors">
              <div className="flex items-center justify-between mb-3">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <h3 className="text-[#e8dff5]">{contact.name}</h3>
                    {contact.isPrimary && (
                      <Star className="w-3.5 h-3.5 text-[#E91E63] fill-[#E91E63]" />
                    )}
                  </div>
                  <p className="uppercase tracking-[0.15em] text-[10px] text-[#a89bb5]">
                    {contact.relationship}
                  </p>
                  <p className="text-sm text-[#e8dff5] mt-1 opacity-70">
                    {contact.phone}
                  </p>
                </div>
              </div>
              
              {/* Action Buttons */}
              <div className="flex gap-2">
                <Button
                  size="sm"
                  className="flex-1 bg-[#E91E63] hover:bg-[#C2185B] text-white h-9"
                >
                  <Phone className="w-3.5 h-3.5 mr-1.5" />
                  Call
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  className="flex-1 bg-transparent border-[#5A1E3C] hover:bg-[#2d1420] text-[#e8dff5] h-9"
                >
                  <MessageSquare className="w-3.5 h-3.5 mr-1.5" />
                  Message
                </Button>
              </div>
            </Card>
          </motion.div>
        ))}
      </div>
      
      {/* Info Note */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.5 }}
        className="relative z-10 mt-8 p-4 bg-[#2d1420]/50 rounded-lg border border-[#5A1E3C]"
      >
        <p className="uppercase tracking-[0.2em] text-[10px] text-[#a89bb5] mb-2">
          Auto-Alert
        </p>
        <p className="text-sm text-[#e8dff5]">
          When SOS is activated, all contacts marked with a star will be automatically notified with your location and status.
        </p>
      </motion.div>
    </div>
  );
}

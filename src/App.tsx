import { useState } from "react";
import { OnboardingScreen } from "./components/OnboardingScreen";
import { SignInScreen } from "./components/SignInScreen";
import { SignUpScreen } from "./components/SignUpScreen";
import { HomeDashboard } from "./components/HomeDashboard";
import { EmergencyContacts } from "./components/EmergencyContacts";
import { AddContactScreen } from "./components/AddContactScreen";
import { LiveLocation } from "./components/LiveLocation";
import { SettingsScreen } from "./components/SettingsScreen";

type Screen = 'onboarding' | 'signin' | 'signup' | 'home' | 'contacts' | 'addContact' | 'location' | 'settings';

export default function App() {
  const [currentScreen, setCurrentScreen] = useState<Screen>('onboarding');

  const renderScreen = () => {
    switch (currentScreen) {
      case 'onboarding':
        return <OnboardingScreen onStart={() => setCurrentScreen('signin')} />;
      case 'signin':
        return (
          <SignInScreen
            onSignIn={() => setCurrentScreen('home')}
            onSignUp={() => setCurrentScreen('signup')}
          />
        );
      case 'signup':
        return (
          <SignUpScreen
            onBack={() => setCurrentScreen('signin')}
            onComplete={() => setCurrentScreen('home')}
          />
        );
      case 'home':
        return <HomeDashboard onNavigate={(screen) => setCurrentScreen(screen as Screen)} />;
      case 'contacts':
        return (
          <EmergencyContacts
            onBack={() => setCurrentScreen('home')}
            onAddContact={() => setCurrentScreen('addContact')}
          />
        );
      case 'addContact':
        return (
          <AddContactScreen
            onBack={() => setCurrentScreen('contacts')}
            onSave={() => setCurrentScreen('contacts')}
          />
        );
      case 'location':
        return <LiveLocation onBack={() => setCurrentScreen('home')} />;
      case 'settings':
        return <SettingsScreen onBack={() => setCurrentScreen('home')} />;
      default:
        return <HomeDashboard onNavigate={(screen) => setCurrentScreen(screen as Screen)} />;
    }
  };

  return (
    <div className="min-h-screen bg-[#0a0510] text-[#e8dff5] overflow-x-hidden">
      {/* Mobile Container */}
      <div className="mx-auto max-w-md">
        {renderScreen()}
      </div>
    </div>
  );
}

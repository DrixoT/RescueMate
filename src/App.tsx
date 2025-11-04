import { useState, useEffect } from "react";
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
  const [currentScreen, setCurrentScreen] = useState<Screen>(() => {
    // Check if user has completed onboarding
    const hasCompletedOnboarding = localStorage.getItem('rescuemate_onboarding_complete');
    const isAuthenticated = localStorage.getItem('rescuemate_authenticated');

    if (hasCompletedOnboarding && isAuthenticated) {
      return 'home';
    } else if (hasCompletedOnboarding) {
      return 'signin';
    }
    return 'onboarding';
  });

  const renderScreen = () => {
    switch (currentScreen) {
      case 'onboarding':
        return <OnboardingScreen onStart={() => {
          localStorage.setItem('rescuemate_onboarding_complete', 'true');
          setCurrentScreen('signin');
        }} />;
      case 'signin':
        return (
          <SignInScreen
            onSignIn={() => {
              localStorage.setItem('rescuemate_authenticated', 'true');
              setCurrentScreen('home');
            }}
            onSignUp={() => setCurrentScreen('signup')}
          />
        );
      case 'signup':
        return (
          <SignUpScreen
            onBack={() => setCurrentScreen('signin')}
            onComplete={() => {
              localStorage.setItem('rescuemate_authenticated', 'true');
              setCurrentScreen('home');
            }}
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

package com.rescuemate.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rescuemate.data.UserPreferences
import com.rescuemate.ui.screens.OnboardingScreen
import com.rescuemate.ui.screens.SignInScreen
import com.rescuemate.ui.screens.SignUpScreen
import com.rescuemate.ui.screens.EmailLoginScreen
import com.rescuemate.ui.screens.HomeDashboard
import com.rescuemate.ui.screens.EmergencyContactsScreen
import com.rescuemate.ui.screens.AddContactScreen
import com.rescuemate.ui.screens.LiveLocationScreen
import com.rescuemate.ui.screens.SettingsScreen
import com.rescuemate.ui.screens.UserProfileScreen
import com.rescuemate.ui.screens.VoiceAISetupScreen
import com.rescuemate.ui.screens.PermissionRequestScreen
import com.rescuemate.ui.screens.BluetoothPairingScreen as BTPairingScreen

@Composable
fun RescueMateNavigation(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }
    
    // Determine start destination based on login status
    val startDestination = when {
        // If user is logged in, go to home
        userPrefs.isLoggedIn() -> Screen.Home.route
        // If onboarding is complete but not logged in, go to sign in
        userPrefs.isOnboardingComplete() -> Screen.SignIn.route
        // Otherwise, show onboarding
        else -> Screen.Onboarding.route
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onStart = {
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.SignIn.route) {
            SignInScreen(
                onSignIn = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SignIn.route) { inclusive = true }
                    }
                },
                onSignUp = {
                    navController.navigate(Screen.SignUp.route)
                },
                onEmailLogin = {
                    navController.navigate(Screen.EmailLogin.route)
                }
            )
        }
        
        composable(Screen.SignUp.route) {
            SignUpScreen(
                onBack = {
                    navController.popBackStack()
                },
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Home.route) {
            HomeDashboard(
                onNavigate = { screen ->
                    when (screen) {
                        "contacts" -> navController.navigate(Screen.Contacts.route)
                        "location" -> navController.navigate(Screen.Location.route)
                        "settings" -> navController.navigate(Screen.Settings.route)
                        "profile" -> navController.navigate(Screen.Profile.route)
                        "voiceAI" -> navController.navigate(Screen.VoiceAI.route)
                    }
                }
            )
        }
        
        composable(Screen.Contacts.route) {
            EmergencyContactsScreen(
                onBack = {
                    navController.popBackStack()
                },
                onAddContact = {
                    navController.navigate(Screen.AddContact.route)
                }
            )
        }
        
        composable(Screen.AddContact.route) {
            AddContactScreen(
                onBack = {
                    navController.popBackStack()
                },
                onSave = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Location.route) {
            LiveLocationScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                },
                onNavigateToBluetooth = {
                    navController.navigate(Screen.Bluetooth.route)
                },
                onNavigateToVoiceAI = {
                    navController.navigate(Screen.VoiceAI.route)
                },
                navController = navController
            )
        }
        
        composable(Screen.Bluetooth.route) {
            BTPairingScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.EmailLogin.route) {
            EmailLoginScreen(
                onBack = {
                    navController.popBackStack()
                },
                onLogin = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SignIn.route) { inclusive = true }
                    }
                },
                onSignUp = {
                    navController.navigate(Screen.SignUp.route)
                }
            )
        }

        composable(Screen.Profile.route) {
            UserProfileScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.VoiceAI.route) {
            VoiceAISetupScreen(
                onBack = {
                    navController.popBackStack()
                },
                onComplete = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.PermissionRequest.route) {
            PermissionRequestScreen(
                onPermissionsGranted = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.PermissionRequest.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

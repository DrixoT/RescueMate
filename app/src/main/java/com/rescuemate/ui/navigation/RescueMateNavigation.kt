package com.rescuemate.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rescuemate.data.UserPreferences
import com.rescuemate.ui.screens.*
import com.rescuemate.ui.screens.BluetoothPairingScreen as BTPairingScreen

@Composable
fun RescueMateNavigation(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }
    
    // Determine start destination based on login status with null safety
    val startDestination = try {
        val isLoggedIn = userPrefs.isLoggedIn()
        val isSetupComplete = userPrefs.isSetupComplete()
        val isOnboardingComplete = userPrefs.isOnboardingComplete()
        
        android.util.Log.d("RescueMateNavigation", "════════════════════════════════════════")
        android.util.Log.d("RescueMateNavigation", "Determining navigation start destination")
        android.util.Log.d("RescueMateNavigation", "   isLoggedIn: $isLoggedIn")
        android.util.Log.d("RescueMateNavigation", "   isSetupComplete: $isSetupComplete")
        android.util.Log.d("RescueMateNavigation", "   isOnboardingComplete: $isOnboardingComplete")
        
        val destination = when {
            // If user is logged in AND setup is complete, go to home
            isLoggedIn && isSetupComplete -> {
                android.util.Log.d("RescueMateNavigation", "User authenticated and setup complete -> Home")
                Screen.Home.route
            }
            // If onboarding is complete (regardless of login status), go to sign in
            isOnboardingComplete -> {
                android.util.Log.d("RescueMateNavigation", "Onboarding complete -> SignIn")
                Screen.SignIn.route
            }
            // Otherwise, show onboarding
            else -> {
                android.util.Log.d("RescueMateNavigation", "New user -> Onboarding")
                Screen.Onboarding.route
            }
        }
        
        android.util.Log.d("RescueMateNavigation", "Starting at: $destination")
        android.util.Log.d("RescueMateNavigation", "════════════════════════════════════════")
        destination
    } catch (e: Exception) {
        android.util.Log.e("RescueMateNavigation", "Error determining start destination, defaulting to Onboarding", e)
        Screen.Onboarding.route
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { 1000 },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -1000 },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -1000 },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { 1000 },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
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
                    try {
                        android.util.Log.d("RescueMateNavigation", "SignIn successful, determining next screen...")
                        val isSetupComplete = userPrefs.isSetupComplete()
                        val target = if (isSetupComplete) {
                            android.util.Log.d("RescueMateNavigation", "   Setup complete -> Home")
                            Screen.Home.route
                        } else {
                            android.util.Log.d("RescueMateNavigation", "   Setup incomplete -> SetupWizard")
                            Screen.SetupWizard.route
                        }
                        
                        navController.navigate(target) {
                            popUpTo(Screen.SignIn.route) { inclusive = true }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("RescueMateNavigation", "Navigation error after SignIn, defaulting to SetupWizard", e)
                        navController.navigate(Screen.SetupWizard.route) {
                            popUpTo(Screen.SignIn.route) { inclusive = true }
                        }
                    }
                },
                onSignUp = {
                    navController.navigate(Screen.SignUp.route)
                },
                onEmailLogin = {
                    navController.navigate(Screen.EmailLogin.route)
                },
                onPhoneLogin = {
                    navController.navigate(Screen.PhoneLogin.route)
                }
            )
        }
        
        composable(Screen.PhoneLogin.route) {
            PhoneLoginScreen(
                onBack = {
                    try {
                        navController.popBackStack()
                    } catch (e: Exception) {
                        android.util.Log.e("RescueMateNavigation", "Error navigating back from PhoneLogin", e)
                        navController.navigate(Screen.SignIn.route) {
                            popUpTo(Screen.PhoneLogin.route) { inclusive = true }
                        }
                    }
                },
                onLoginSuccess = {
                    try {
                        android.util.Log.d("RescueMateNavigation", "Phone login successful, determining next screen...")
                        val isSetupComplete = userPrefs.isSetupComplete()
                        val target = if (isSetupComplete) {
                            android.util.Log.d("RescueMateNavigation", "   Setup complete -> Home")
                            Screen.Home.route
                        } else {
                            android.util.Log.d("RescueMateNavigation", "   Setup incomplete -> SetupWizard")
                            Screen.SetupWizard.route
                        }
                        
                        navController.navigate(target) {
                            popUpTo(Screen.PhoneLogin.route) { inclusive = true }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("RescueMateNavigation", "Navigation error after PhoneLogin, defaulting to SetupWizard", e)
                        navController.navigate(Screen.SetupWizard.route) {
                            popUpTo(Screen.PhoneLogin.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        
        composable(Screen.SignUp.route) {
            SignUpScreen(
                onBack = {
                    navController.popBackStack()
                },
                onComplete = {
                    // New users always go to Setup Wizard
                    navController.navigate(Screen.SetupWizard.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Home.route) {
            HomeDashboard(
                onNavigate = { screen ->
                    when {
                        screen == "contacts" -> navController.navigate(Screen.Contacts.route)
                        screen == "location" -> navController.navigate(Screen.Location.route)
                        screen.startsWith("settings") -> navController.navigate(screen)
                        screen.startsWith("profile") -> navController.navigate(screen)
                        screen == "voiceAI" -> navController.navigate(Screen.VoiceAI.route)
                        screen == "logs" -> navController.navigate(Screen.Logs.route)
                        else -> android.util.Log.w("RescueMateNavigation", "Unknown navigation route: $screen")
                    }
                }
            )
        }
        
        // Modal-like transitions for secondary screens
        composable(
            route = Screen.Contacts.route,
            enterTransition = {
                slideInVertically(initialOffsetY = { 1000 }, animationSpec = tween(300)) + fadeIn()
            },
            exitTransition = {
                slideOutVertically(targetOffsetY = { -1000 }, animationSpec = tween(300)) + fadeOut()
            },
            popEnterTransition = {
                slideInVertically(initialOffsetY = { -1000 }, animationSpec = tween(300)) + fadeIn()
            },
            popExitTransition = {
                slideOutVertically(targetOffsetY = { 1000 }, animationSpec = tween(300)) + fadeOut()
            }
        ) {
            EmergencyContactsScreen(
                onBack = {
                    navController.popBackStack()
                },
                onAddContact = { name, phone ->
                    val route = if (name.isNullOrBlank() && phone.isNullOrBlank()) {
                        // Standard route for manual entry
                        "addContact"
                    } else {
                        // Route with pre-filled data
                        "addContact?name=${name ?: ""}&phone=${phone ?: ""}"
                    }
                    navController.navigate(route)
                }
            )
        }
        
        composable(
            route = Screen.AddContact.route,
            arguments = listOf(
                androidx.navigation.navArgument("name") { 
                    defaultValue = ""
                    nullable = true
                },
                androidx.navigation.navArgument("phone") { 
                    defaultValue = ""
                    nullable = true
                }
            ),
            enterTransition = {
                slideInVertically(initialOffsetY = { 1000 }, animationSpec = tween(300)) + fadeIn()
            },
            exitTransition = {
                slideOutVertically(targetOffsetY = { 1000 }, animationSpec = tween(300)) + fadeOut()
            }
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            
            AddContactScreen(
                onBack = {
                    navController.popBackStack()
                },
                onSave = {
                    navController.popBackStack()
                },
                initialName = name,
                initialPhone = phone
            )
        }
        
        composable(Screen.Location.route) {
            LiveLocationScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = "settings?mode={mode}",
            arguments = listOf(
                androidx.navigation.navArgument("mode") { 
                    defaultValue = "popup" 
                    nullable = true
                }
            ),
            enterTransition = {
                val mode = initialState.arguments?.getString("mode")
                if (mode == "swipe") {
                    slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(300))
                } else {
                    slideInVertically(initialOffsetY = { 1000 }, animationSpec = tween(300)) + fadeIn()
                }
            },
            exitTransition = {
                val mode = initialState.arguments?.getString("mode")
                if (mode == "swipe") {
                    slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(300))
                } else {
                    slideOutVertically(targetOffsetY = { 1000 }, animationSpec = tween(300)) + fadeOut()
                }
            }
        ) {
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
                    try {
                        navController.popBackStack()
                    } catch (e: Exception) {
                        android.util.Log.e("RescueMateNavigation", "Error navigating back from EmailLogin", e)
                        navController.navigate(Screen.SignIn.route) {
                            popUpTo(Screen.EmailLogin.route) { inclusive = true }
                        }
                    }
                },
                onLogin = {
                    try {
                        android.util.Log.d("RescueMateNavigation", "Email login successful, determining next screen...")
                        val isSetupComplete = userPrefs.isSetupComplete()
                        val target = if (isSetupComplete) {
                            android.util.Log.d("RescueMateNavigation", "   Setup complete -> Home")
                            Screen.Home.route
                        } else {
                            android.util.Log.d("RescueMateNavigation", "   Setup incomplete -> SetupWizard")
                            Screen.SetupWizard.route
                        }
                        
                        navController.navigate(target) {
                            popUpTo(Screen.EmailLogin.route) { inclusive = true }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("RescueMateNavigation", "Navigation error after EmailLogin, defaulting to SetupWizard", e)
                        navController.navigate(Screen.SetupWizard.route) {
                            popUpTo(Screen.EmailLogin.route) { inclusive = true }
                        }
                    }
                },
                onSignUp = {
                    navController.navigate(Screen.SignUp.route)
                }
            )
        }

        composable(
            route = "profile?mode={mode}",
            arguments = listOf(
                androidx.navigation.navArgument("mode") { 
                    defaultValue = "popup" 
                    nullable = true
                }
            ),
            enterTransition = {
                val mode = targetState.arguments?.getString("mode")
                if (mode == "swipe") {
                    // Slide in from LEFT (negative offset)
                    slideInHorizontally(initialOffsetX = { -1000 }, animationSpec = tween(300))
                } else {
                    // Pop up from bottom
                    slideInVertically(initialOffsetY = { 1000 }, animationSpec = tween(300)) + fadeIn()
                }
            },
            exitTransition = {
                val mode = initialState.arguments?.getString("mode")
                if (mode == "swipe") {
                    // Slide out to LEFT
                    slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(300))
                } else {
                    // Slide down
                    slideOutVertically(targetOffsetY = { 1000 }, animationSpec = tween(300)) + fadeOut()
                }
            }
        ) {
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

        composable(Screen.SetupWizard.route) {
            SetupWizardScreen(
                onComplete = {
                    try {
                        android.util.Log.d("RescueMateNavigation", "Setup wizard completed, navigating to Home...")
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.SetupWizard.route) { inclusive = true }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("RescueMateNavigation", "Navigation error after SetupWizard", e)
                        // Ensure we still navigate even if there's an error
                        try {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        } catch (e2: Exception) {
                            android.util.Log.e("RescueMateNavigation", "Critical: Cannot navigate to Home", e2)
                        }
                    }
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
        
        composable(Screen.Logs.route) {
            val userId = userPrefs.getUserId()
            InteractionLogsScreen(
                userId = userId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

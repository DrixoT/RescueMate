package com.rescuemate.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object SignIn : Screen("signin")
    object SignUp : Screen("signup")
    object EmailLogin : Screen("emailLogin")
    object PhoneLogin : Screen("phoneLogin")
    object Home : Screen("home")
    object Contacts : Screen("contacts")
    object AddContact : Screen("addContact?name={name}&phone={phone}")
    object Location : Screen("location")
    object Settings : Screen("settings")
    object Bluetooth : Screen("bluetooth")
    object Profile : Screen("profile")
    object VoiceAI : Screen("voiceAI")
    object PermissionRequest : Screen("permissionRequest")
    object SetupWizard : Screen("setupWizard")
    object Logs : Screen("logs")
    object EmergencyConfig : Screen("emergency_config")
    object EmergencyNotification : Screen("emergency_notification")
}


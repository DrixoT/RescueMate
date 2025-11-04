package com.rescuemate.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object SignIn : Screen("signin")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object Contacts : Screen("contacts")
    object AddContact : Screen("addContact")
    object Location : Screen("location")
    object Settings : Screen("settings")
    object Bluetooth : Screen("bluetooth")
}


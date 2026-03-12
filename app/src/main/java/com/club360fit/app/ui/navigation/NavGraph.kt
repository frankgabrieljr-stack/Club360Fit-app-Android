package com.club360fit.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.club360fit.app.data.SupabaseClient
import com.club360fit.app.ui.screens.welcome.WelcomeScreen
import com.club360fit.app.ui.screens.auth.AuthScreen
import com.club360fit.app.ui.screens.client.ClientHomeScreen
import com.club360fit.app.ui.screens.admin.AdminHomeScreen

/**
 * Root navigation graph. Welcome -> Auth (Sign In / Create Account) -> Client or Admin home.
 */
@Composable
fun Club360FitNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.WELCOME
    ) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onSignIn = { navController.navigate(Routes.SIGN_IN) },
                onCreateAccount = { navController.navigate(Routes.CREATE_ACCOUNT) }
            )
        }
        composable(Routes.SIGN_IN) {
            AuthScreen(
                isSignIn = true,
                onAuthSuccess = { isAdmin -> navController.navigate(if (isAdmin) Routes.ADMIN_HOME else Routes.CLIENT_HOME) { popUpTo(Routes.WELCOME) { inclusive = true } } },
                onNavigateToCreateAccount = { navController.navigate(Routes.CREATE_ACCOUNT) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.CREATE_ACCOUNT) {
            AuthScreen(
                isSignIn = false,
                onAuthSuccess = { isAdmin -> navController.navigate(if (isAdmin) Routes.ADMIN_HOME else Routes.CLIENT_HOME) { popUpTo(Routes.WELCOME) { inclusive = true } } },
                onNavigateToSignIn = { navController.navigate(Routes.SIGN_IN) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.CLIENT_HOME) {
            ClientHomeScreen(
                onSignOut = {
                    SupabaseClient.client.auth.signOut()
                    navController.navigate(Routes.WELCOME) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.ADMIN_HOME) {
            AdminHomeScreen(
                onSignOut = {
                    SupabaseClient.client.auth.signOut()
                    navController.navigate(Routes.WELCOME) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                }
            )
        }
    }
}

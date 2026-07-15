package com.thuo_ng.swift_chat_android.ui.auth

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.thuo_ng.swift_chat_android.ui.navigation.AuthGraph
import com.thuo_ng.swift_chat_android.ui.navigation.SignIn
import com.thuo_ng.swift_chat_android.ui.navigation.Signup

/**
 * AuthGraph chứa SignIn + Signup.
 *
 * AppViewModel observes the persisted auth state and owns root navigation.
 * This graph only handles navigation inside the auth flow.
 */
fun NavGraphBuilder.authGraph(rootNavController: NavController) {
    navigation<AuthGraph>(startDestination = SignIn) {
        composable<SignIn> {
            SignInScreen(
                onNavigateToSignUp = { rootNavController.navigate(Signup) }
            )
        }

        composable<Signup> {
            SignupScreen(
                onNavigateToSignIn = { rootNavController.popBackStack() }
            )
        }
    }
}

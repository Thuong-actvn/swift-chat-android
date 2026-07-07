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
 * AppNavGraph observes auth state and owns navigation to MainGraph.
 */
fun NavGraphBuilder.authGraph(rootNavController: NavController) {
    navigation<AuthGraph>(startDestination = SignIn) {
        composable<SignIn> {
            SignInScreen(
                onNavigateToSignUp = { rootNavController.navigate(Signup) },
                onEffect = { effect ->
                    when (effect) {
                        is AuthEffect.NavigateToMain -> Unit
                        else -> { /* Screen đã xử lý ShowError */ }
                    }
                }
            )
        }

        composable<Signup> {
            SignupScreen(
                onNavigateToSignIn = { rootNavController.popBackStack() },
                onEffect = { effect ->
                    when (effect) {
                        is AuthEffect.NavigateToMain -> Unit
                        else -> { /* Screen đã xử lý ShowError */ }
                    }
                }
            )
        }
    }
}

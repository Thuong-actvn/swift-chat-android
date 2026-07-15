package com.thuo_ng.swift_chat_android.ui.auth

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.thuo_ng.swift_chat_android.R
import com.thuo_ng.swift_chat_android.ui.components.*
import com.thuo_ng.swift_chat_android.ui.theme.SwiftChatTheme

/**
 * Root navigation is driven by AppViewModel's persisted auth state.
 * This screen only handles its UI, errors, and navigation within AuthGraph.
 */
@Composable
fun SignInScreen(
    onNavigateToSignUp: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Collect Effect — side-effect một lần
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AuthEffect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    SignInContent(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
        onNavigateToSignUp = onNavigateToSignUp,
        onGoogleSignInClick = { viewModel.handleIntent(AuthIntent.SubmitGoogleSignIn(context)) }
    )
}

/**
 * SignInContent is the stateless version of the Sign In screen.
 * It takes the UI state and event callbacks directly, making it easy to test
 * and preview without needing an actual ViewModel instance.
 */
@Composable
fun SignInContent(
    uiState: AuthUiState,
    onIntent: (AuthIntent) -> Unit,
    onNavigateToSignUp: () -> Unit,
    onGoogleSignInClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo
        Image(
            painter = painterResource(id = R.drawable.logo_app),
            contentDescription = "App Logo",
            modifier = Modifier.size(64.dp)
        )
        
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "SwiftChat",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Welcome Back",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Log in to continue your conversation.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Username field — với inline error
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Username",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            SwiftTextField(
                value = uiState.username,
                onValueChange = { onIntent(AuthIntent.UsernameChanged(it)) },
                placeholder = "Enter your username",
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.PersonOutline, contentDescription = null)
                },
                errorMessage = uiState.usernameError,
                onBlur = { onIntent(AuthIntent.ValidateUsername) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Password field — với inline error
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Password",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Forgot password?",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .clickable { /* TODO */ }
                )
            }
            SwiftPasswordTextField(
                value = uiState.password,
                onValueChange = { onIntent(AuthIntent.PasswordChanged(it)) },
                placeholder = "••••••••",
                isPasswordVisible = uiState.isPasswordVisible,
                onVisibilityToggle = { onIntent(AuthIntent.PasswordVisibilityToggled) },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Lock, contentDescription = null)
                },
                errorMessage = uiState.passwordError,
                onBlur = { onIntent(AuthIntent.ValidatePassword) }
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        SwiftPrimaryButton(
            text = "Sign In",
            onClick = { onIntent(AuthIntent.SubmitSignIn) },
            isLoading = uiState.isLoading,
            enabled = !uiState.isLoading && uiState.isSignInValid
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        DividerWithText("OR")
        
        Spacer(modifier = Modifier.height(32.dp))
        
        SwiftOutlinedButton(
            text = "Log in with Google",
            onClick = { onGoogleSignInClick() },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.icon_google),
                    contentDescription = "Google Logo",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
        )
        
        Spacer(modifier = Modifier.weight(1f, fill = false))
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Don't have an account? ",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
            Text(
                text = "Register now",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onNavigateToSignUp() }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignInScreenPreview() {
    SwiftChatTheme {
        // We use the stateless SignInContent here to avoid instantiating AuthViewModel during preview
        SignInContent(
            uiState = AuthUiState(),
            onIntent = {},
            onNavigateToSignUp = {},
            onGoogleSignInClick = {}
        )
    }
}

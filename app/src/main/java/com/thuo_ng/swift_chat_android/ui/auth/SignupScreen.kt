package com.thuo_ng.swift_chat_android.ui.auth

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * SignupScreen — tương tự SignInScreen, dùng Effect thay callback.
 */
@Composable
fun SignupScreen(
    onNavigateToSignIn: () -> Unit,
    onEffect: (AuthEffect) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Collect Effect
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AuthEffect.NavigateToMain -> onEffect(effect)
                is AuthEffect.NavigateToSignIn -> { /* không cần xử lý ở đây */ }
                is AuthEffect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    SignupContent(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
        onNavigateToSignIn = onNavigateToSignIn
    )
}

/**
 * SignupContent is the stateless version of the SignUp screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupContent(
    uiState: AuthUiState,
    onIntent: (AuthIntent) -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Sử dụng Scaffold nội bộ để quản lý TopAppBar và tránh việc bị đẩy xuống do padding trùng lặp
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SwiftChat",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally)
                            .padding(end = 48.dp) // Cân bằng với nút Back để text nằm giữa
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateToSignIn) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Logo
            Image(
                painter = painterResource(id = R.drawable.logo_app),
                contentDescription = "App Logo",
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Create New Account",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Join us to start chatting",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Username field
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
                    placeholder = "Choose a unique username",
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.PersonOutline, contentDescription = null)
                    },
                    errorMessage = uiState.usernameError,
                    onBlur = { onIntent(AuthIntent.ValidateUsername) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Email field
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Email",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SwiftTextField(
                    value = uiState.email,
                    onValueChange = { onIntent(AuthIntent.EmailChanged(it)) },
                    placeholder = "Enter your email address",
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Email, contentDescription = null)
                    },
                    errorMessage = uiState.emailError,
                    onBlur = { onIntent(AuthIntent.ValidateEmail) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Password field
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Password",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SwiftPasswordTextField(
                    value = uiState.password,
                    onValueChange = { onIntent(AuthIntent.PasswordChanged(it)) },
                    placeholder = "Create a strong password",
                    isPasswordVisible = uiState.isPasswordVisible,
                    onVisibilityToggle = { onIntent(AuthIntent.PasswordVisibilityToggled) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Lock, contentDescription = null)
                    },
                    errorMessage = uiState.passwordError,
                    onBlur = { onIntent(AuthIntent.ValidatePassword) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password field
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Confirm Password",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SwiftPasswordTextField(
                    value = uiState.confirmPassword,
                    onValueChange = { onIntent(AuthIntent.ConfirmPasswordChanged(it)) },
                    placeholder = "Re-enter your password",
                    isPasswordVisible = uiState.isConfirmPasswordVisible,
                    onVisibilityToggle = { onIntent(AuthIntent.ConfirmPasswordVisibilityToggled) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Lock, contentDescription = null)
                    },
                    errorMessage = uiState.confirmPasswordError,
                    onBlur = { onIntent(AuthIntent.ValidateConfirmPassword) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            SwiftPrimaryButton(
                text = "Sign Up",
                onClick = { onIntent(AuthIntent.SubmitSignUp) },
                isLoading = uiState.isLoading,
                enabled = !uiState.isLoading && uiState.isSignUpValid
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
               Text(
                    text = "Log In",
                     color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigateToSignIn() }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignupScreenPreview() {
    SwiftChatTheme {
        SignupContent(
            uiState = AuthUiState(),
            onIntent = {},
            onNavigateToSignIn = {}
        )
    }
}

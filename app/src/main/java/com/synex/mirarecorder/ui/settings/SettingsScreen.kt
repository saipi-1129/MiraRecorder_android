package com.synex.mirarecorder.ui.settings

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.synex.mirarecorder.di.AppModule
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("mirarecorder_prefs", android.content.Context.MODE_PRIVATE)
    }

    var serverUrl by remember {
        mutableStateOf(
            prefs.getString(AppModule.KEY_SERVER_URL, AppModule.DEFAULT_SERVER_URL)
                ?: AppModule.DEFAULT_SERVER_URL
        )
    }
    var connectionStatus by remember { mutableStateOf<ConnectionStatus>(ConnectionStatus.Idle) }
    var isUnlocked by remember {
        mutableStateOf(prefs.getBoolean(AppModule.KEY_UNLOCKED, false))
    }
    var password by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = "Server URL",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { newValue ->
                    serverUrl = newValue
                    prefs.edit().putString(AppModule.KEY_SERVER_URL, newValue).apply()
                    connectionStatus = ConnectionStatus.Idle
                },
                label = { Text("Server URL") },
                placeholder = { Text(AppModule.DEFAULT_SERVER_URL) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    connectionStatus = ConnectionStatus.Testing
                    scope.launch {
                        connectionStatus = testConnection(serverUrl)
                    }
                },
                enabled = connectionStatus != ConnectionStatus.Testing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Test Connection")
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (connectionStatus) {
                ConnectionStatus.Testing -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Testing connection...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                ConnectionStatus.Success -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF34C759),
                            modifier = Modifier.size(20.dp),
                        )
                        Text("Connected successfully", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is ConnectionStatus.Error -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF3B30),
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            (connectionStatus as ConnectionStatus.Error).message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFF3B30),
                        )
                    }
                }
                ConnectionStatus.Idle -> {}
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "All Users",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isUnlocked) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF34C759),
                        modifier = Modifier.size(20.dp),
                    )
                    Text("Unlocked", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        isUnlocked = false
                        prefs.edit().putBoolean(AppModule.KEY_UNLOCKED, false).apply()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Lock")
                }
            } else {
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = false
                    },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = passwordError,
                    supportingText = if (passwordError) {
                        { Text("Incorrect password") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (AppModule.verifyPassword(password)) {
                            isUnlocked = true
                            prefs.edit().putBoolean(AppModule.KEY_UNLOCKED, true).apply()
                            password = ""
                            passwordError = false
                        } else {
                            passwordError = true
                        }
                    },
                    enabled = password.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Unlock")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "MiraRecorder v1.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp),
            )
        }
    }
}

private sealed class ConnectionStatus {
    data object Idle : ConnectionStatus()
    data object Testing : ConnectionStatus()
    data object Success : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}

private suspend fun testConnection(baseUrl: String): ConnectionStatus {
    return withContext(Dispatchers.IO) {
        try {
            val url = if (baseUrl.endsWith("/")) "${baseUrl}api/targets" else "$baseUrl/api/targets"
            val client = OkHttpClient.Builder()
                .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                ConnectionStatus.Success
            } else {
                ConnectionStatus.Error("HTTP ${response.code}")
            }
        } catch (e: Exception) {
            ConnectionStatus.Error(e.message ?: "Connection failed")
        }
    }
}

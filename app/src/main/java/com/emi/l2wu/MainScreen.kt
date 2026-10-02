package com.emi.l2wu

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun MainScreen(
    innerPaddingValues: PaddingValues,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val roundedCornerShape = 8.dp
    var showDisclosureDialog by remember { mutableStateOf(false) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Notification permission is required!", Toast.LENGTH_LONG).show()
        }
    }

//    var isServiceStarted by remember { mutableStateOf(false) }
    val isServiceStarted by viewModel.isServiceStarted.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Screen Controller", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))

        Spacer(modifier = Modifier.height(24.dp))

        // Step 1: Notification Permission
        if (!hasNotificationPermission) {
            Button(
                onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                shape = RoundedCornerShape(roundedCornerShape),
                modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Grant Notification Permission")
            }
        } else {
            Text("✅ Notification Permission Granted", color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Step 2: Accessibility
        Button(
            onClick = {
//            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            showDisclosureDialog = true
        },
            shape = RoundedCornerShape(roundedCornerShape),
            modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Enable Accessibility Service")
        }

        if (showDisclosureDialog) {
            AccessibilityDisclosureDialog(
                onDismiss = { showDisclosureDialog = false },
                onAccept = {
                    showDisclosureDialog = false
                    openAccessibilitySettings(context)
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Step 3: Start Service
        Button(
            enabled = if (hasNotificationPermission && !isServiceStarted) true else false,
            onClick = {
                val intent = Intent(context, ScreenControlService::class.java)
                ContextCompat.startForegroundService(context, intent)
                viewModel.setServiceStarted(true)
            },
            shape = RoundedCornerShape(roundedCornerShape),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text("Start Service")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isServiceStarted) {
            Text("Service connected. You can close the application.", modifier = Modifier.padding(16.dp))
        }

        // Stop Service
        Button(
            enabled = if (isServiceStarted) true else false,
            onClick = {
                val intent = Intent(context, ScreenControlService::class.java)
                context.stopService(intent)
                viewModel.setServiceStarted(false)
            },
            shape = RoundedCornerShape(roundedCornerShape),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text("Stop Service")
        }

    }
}

@Composable
fun AccessibilityDisclosureDialog(
    onDismiss: () -> Unit,
    onAccept: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Important Notice: Accessibility Service Usage") },
        text = {
            Text(
                text = "Our app requires the Accessibility Service permission to function properly.\n\n" +
                        "• Why we use it: We use this service to open your screen when you lift up the phone and lock it by pressing the notification.\n\n" +
                        "• Data Privacy: This service is used strictly for the functionality mentioned above. We do not collect, store, or share any personal data or screen content.",
                modifier = Modifier
                    .heightIn(250.dp)
                    .verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = {
            // MUST be an affirmative action to accept and proceed to settings
            Button(onClick = onAccept) {
                Text(text = "Accept & Turn On")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(text = "No Thanks")
            }
        }
    )
}

private fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}
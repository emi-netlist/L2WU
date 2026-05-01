package com.emi.l2wu

import android.Manifest
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun MainScreen() {
    val context = LocalContext.current

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

    var isServiceStarted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Screen Controller (Android 16)", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))

        Spacer(modifier = Modifier.height(24.dp))

        // Step 1: Notification Permission
        if (!hasNotificationPermission) {
            Button(
                onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Grant Notification Permission")
            }
        } else {
            Text("✅ Notification Permission Granted", color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Step 2: Accessibility
        Button(onClick = {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        },
            modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Enable Accessibility (For Locking)")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Step 3: Start Service
        Button(
            enabled = if (hasNotificationPermission && !isServiceStarted) true else false,
            onClick = {
                val intent = Intent(context, ScreenControlService::class.java)
                ContextCompat.startForegroundService(context, intent)
                isServiceStarted = true
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text("Start Service")
        }
    }
}
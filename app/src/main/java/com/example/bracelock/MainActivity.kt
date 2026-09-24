package com.example.bracelock

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var dataManager: DataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataManager = DataManager(this)

        checkOverlayPermission()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BraceDashboard(
                        dataManager = dataManager,
                        onUpdateLockService = { startLockService() }
                    )
                }
            }
        }
    }

    private fun startLockService() {
        val intent = Intent(this, LockService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }
}

@Composable
fun BraceDashboard(dataManager: DataManager, onUpdateLockService: () -> Unit) {
    val context = LocalContext.current
    var isLocked by remember { mutableStateOf(dataManager.isLocked) }
    var notes by remember { mutableStateOf(dataManager.dailyNotes) }
    var wearTime by remember { mutableStateOf(dataManager.getElapsedTimeFormatted()) }

    var showOverrideDialog by remember { mutableStateOf(false) }
    var overrideReason by remember { mutableStateOf("") }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            dataManager.startWearing()
            isLocked = false
            wearTime = dataManager.getElapsedTimeFormatted()
            onUpdateLockService()
            Toast.makeText(context, "Foto geverifieerd! Toestel ontgrendeld.", Toast.LENGTH_LONG).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Cameratoestemming vereist", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Brace Tracker & Lock",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isLocked) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isLocked) "Status: VERGRENDELD" else "Status: BRACE ACTIEF",
                    color = if (isLocked) Color(0xFFC62828) else Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Huidige draagtijd: $wearTime")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val permission = Manifest.permission.CAMERA
                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                    cameraLauncher.launch(null)
                } else {
                    cameraPermissionLauncher.launch(permission)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("📸 Maak Foto (Ontgrendel)")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { showOverrideDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🚨 Nood-Override (Reden vereist)")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Dagelijkse Notities",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = notes,
            onValueChange = {
                notes = it
                dataManager.dailyNotes = it
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            placeholder = { Text("Typ hier bijzonderheden...") }
        )
    }

    if (showOverrideDialog) {
        AlertDialog(
            onDismissRequest = { showOverrideDialog = false },
            title = { Text("Spoed Override") },
            text = {
                Column {
                    Text("Voer een geldige reden in waarom de brace niet gedragen kan worden:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = overrideReason,
                        onValueChange = { overrideReason = it },
                        placeholder = { Text("Bijv. douchen of sporten...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (overrideReason.isNotBlank()) {
                            dataManager.logOverride(overrideReason)
                            isLocked = false
                            showOverrideDialog = false
                            onUpdateLockService()
                            Toast.makeText(context, "Override geregistreerd.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Bevestig & Ontgrendel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverrideDialog = false }) {
                    Text("Annuleren")
                }
            }
        )
    }
}

package com.example.bracelock

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
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
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var dataManager: DataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataManager = DataManager(this)

        checkOverlayPermission()
        scheduleDailyAlarm(this)

        setContent {
            MaterialTheme {
                MainScreen(dataManager) { startLockService() }
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
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleDailyAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return // Rechten ontbreken
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }
}

@Composable
fun MainScreen(dataManager: DataManager, onUpdateLockService: () -> Unit) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Dashboard", "Agenda", "Overzicht")

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        icon = {
                            when (index) {
                                0 -> Icon(Icons.Default.Home, contentDescription = null)
                                1 -> Icon(Icons.Default.DateRange, contentDescription = null)
                                2 -> Icon(Icons.Default.List, contentDescription = null)
                            }
                        },
                        label = { Text(title) },
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (selectedTabIndex) {
                0 -> DashboardTab(dataManager, onUpdateLockService)
                1 -> CalendarAndNotesTab(dataManager)
                2 -> HistoryTab(dataManager)
            }
        }
    }
}

@Composable
fun DashboardTab(dataManager: DataManager, onUpdateLockService: () -> Unit) {
    val context = LocalContext.current
    var isLocked by remember { mutableStateOf(dataManager.isLocked) }
    var wearTime by remember { mutableStateOf(dataManager.getElapsedTimeFormatted()) }
    var showOverrideDialog by remember { mutableStateOf(false) }
    var overrideReason by remember { mutableStateOf("") }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            dataManager.startWearing()
            isLocked = false
            wearTime = dataManager.getElapsedTimeFormatted()
            onUpdateLockService()
            Toast.makeText(context, "Verificatie gelukt!", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) cameraLauncher.launch(null)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Brace Tracker", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp, bottom = 16.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (isLocked) Color(0xFFFFEBEE) else Color(0xFFE8F5E9))) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (isLocked) "VERGRENDELD" else "BRACE ACTIEF", color = if (isLocked) Color(0xFFC62828) else Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Draagtijd: $wearTime", fontSize = 18.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.fillMaxWidth().height(60.dp)) {
            Text("📸 Maak Foto & Ontgrendel", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = { showOverrideDialog = true }, modifier = Modifier.fillMaxWidth().height(60.dp)) {
            Text("🚨 Nood-Override", fontSize = 16.sp)
        }
    }

    if (showOverrideDialog) {
        AlertDialog(
            onDismissRequest = { showOverrideDialog = false },
            title = { Text("Spoed Override") },
            text = {
                OutlinedTextField(value = overrideReason, onValueChange = { overrideReason = it }, placeholder = { Text("Reden (bijv. sporten)...") })
            },
            confirmButton = {
                Button(onClick = {
                    if (overrideReason.isNotBlank()) {
                        dataManager.logOverride(overrideReason)
                        isLocked = false
                        showOverrideDialog = false
                        onUpdateLockService()
                    }
                }) { Text("Ontgrendel") }
            },
            dismissButton = { TextButton(onClick = { showOverrideDialog = false }) { Text("Annuleren") } }
        )
    }
}

@Composable
fun CalendarAndNotesTab(dataManager: DataManager) {
    val context = LocalContext.current
    var newNote by remember { mutableStateOf("") }
    var notesList by remember { mutableStateOf(dataManager.getNotes()) }
    var calendarEvents by remember { mutableStateOf<List<String>>(emptyList()) }

    val calPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) calendarEvents = fetchTodayEvents(context)
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            calendarEvents = fetchTodayEvents(context)
        } else {
            calPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Agenda Vandaag", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Card(modifier = Modifier.fillMaxWidth().height(120.dp).padding(vertical = 8.dp)) {
            LazyColumn(modifier = Modifier.padding(12.dp)) {
                if (calendarEvents.isEmpty()) {
                    item { Text("Geen afspraken gevonden voor vandaag.", color = Color.Gray) }
                } else {
                    items(calendarEvents) { event -> Text("• $event", modifier = Modifier.padding(vertical = 4.dp)) }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Dagboek & Klachten", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = newNote, onValueChange = { newNote = it }, modifier = Modifier.weight(1f), placeholder = { Text("Nieuwe notitie...") })
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (newNote.isNotBlank()) {
                    dataManager.addNote(newNote)
                    notesList = dataManager.getNotes()
                    newNote = ""
                }
            }) { Icon(Icons.Default.Add, contentDescription = "Toevoegen") }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(notesList) { note ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(note, modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}

@Composable
fun HistoryTab(dataManager: DataManager) {
    val history = dataManager.getHistory()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Weekoverzicht & Logs", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))
        if (history.isEmpty()) {
            Text("Nog geen historie geregistreerd.", color = Color.Gray)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(history) { log ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(log, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }
    }
}

@SuppressLint("Range")
fun fetchTodayEvents(context: Context): List<String> {
    val events = mutableListOf<String>()
    val startOfDay = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }.timeInMillis
    val endOfDay = startOfDay + 86400000

    val projection = arrayOf(CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART)
    val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
    val selectionArgs = arrayOf(startOfDay.toString(), endOfDay.toString())

    try {
        val cursor = context.contentResolver.query(CalendarContract.Events.CONTENT_URI, projection, selection, selectionArgs, "${CalendarContract.Events.DTSTART} ASC")
        cursor?.use {
            while (it.moveToNext()) {
                val title = it.getString(it.getColumnIndex(CalendarContract.Events.TITLE))
                val time = it.getLong(it.getColumnIndex(CalendarContract.Events.DTSTART))
                val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(time))
                events.add("$timeString - $title")
            }
        }
    } catch (e: Exception) {
        events.add("Geen agenda-rechten of agenda leeg.")
    }
    return events
}

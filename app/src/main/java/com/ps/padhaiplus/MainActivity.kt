package com.ps.padhaiplus

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ps.padhaiplus.ui.theme.PadhaiPlusTheme
import kotlinx.coroutines.delay
import java.util.UUID

data class RoutineItem(
    val id: Int = (0..100000).random(),
    val subject: String = "",
    val minutes: Int = 30,
    val hour: Int = 8,
    val minute: Int = 0,
    val isEnabled: Boolean = true,
    val isCompleted: Boolean = false
)

data class StudentRecord(
    val name: String = "",
    val avatar: String = "🎓",
    val todayMinutes: Int = 0,
    val completionPercent: Int = 0,
    val isStudying: Boolean = false
)

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            PadhaiPlusTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PadhaiPlusApp()
                }
            }
        }
    }
}

@Composable
fun PadhaiPlusApp() {
    val context = LocalContext.current
    val db = remember {
        FirebaseFirestore.getInstance().apply {
            enableNetwork()
        }
    }
    val sharedPref = remember { context.getSharedPreferences("padhai_prefs", Context.MODE_PRIVATE) }

    // Login credentials & states (Simple Student Name / ID Entry)
    var studentName by remember { mutableStateOf("") }
    var activeDocId by remember { mutableStateOf("") }
    var isLoggedIn by remember { mutableStateOf(false) }
    var avatarPic by remember { mutableStateOf(sharedPref.getString("student_avatar", "🎓") ?: "🎓") }

    // Format & Navigation options
    var selectedSection by remember { mutableStateOf(0) }
    var is24HourFormat by remember { mutableStateOf(false) }

    // Routine inputs & storage
    var subjectInput by remember { mutableStateOf("") }
    var minutesInput by remember { mutableStateOf("30") }
    var hourInput by remember { mutableStateOf("8") }
    var minuteInput by remember { mutableStateOf("0") }
    var routines by remember { mutableStateOf(listOf<RoutineItem>()) }

    // Dialog states
    var editingRoutine by remember { mutableStateOf<RoutineItem?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }

    // Active Timer state, ML Kit Face Detection, & 60s Switch Grace Period
    var activeSubject by remember { mutableStateOf<String?>(null) }
    var secondsLeft by remember { mutableStateOf(0) }
    var totalSecondsPlanned by remember { mutableStateOf(0) }
    var isTimerPaused by remember { mutableStateOf(false) }
    var isFaceDetectionEnabled by remember { mutableStateOf(true) }
    var isFaceDetected by remember { mutableStateOf(true) }
    var backgroundGraceSeconds by remember { mutableStateOf(60) }
    var isAppInBackground by remember { mutableStateOf(false) }

    // Leaderboard state
    var leaderboard by remember { mutableStateOf(listOf<StudentRecord>()) }

    // Work Completion Percentage (%)
    val completedMins = routines.filter { it.isCompleted }.sumOf { it.minutes }
    val totalPlannedMins = routines.sumOf { it.minutes }.let { if (it == 0) 1 else it }
    val completionPercent = ((completedMins.toFloat() / totalPlannedMins) * 100).toInt().coerceIn(0, 100)

    // 1-MIN SWITCH WINDOW (Background lifecycle timer)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    isAppInBackground = true
                }
                Lifecycle.Event.ON_RESUME -> {
                    isAppInBackground = false
                    backgroundGraceSeconds = 60
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Single Active Session Enforcement Check
    LaunchedEffect(isLoggedIn, activeDocId) {
        if (isLoggedIn && activeDocId.isNotBlank()) {
            db.collection("students").document(activeDocId).get().addOnSuccessListener { document ->
                val activeSession = document.getString("active_session")
                val localSession = sharedPref.getString("session_id", "")
                if (activeSession != null && localSession != null && activeSession != localSession) {
                    isLoggedIn = false
                    Toast.makeText(context, "Logged out: Account in use on another device", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 60-second grace window countdown when backgrounded
    LaunchedEffect(isAppInBackground, activeSubject, backgroundGraceSeconds) {
        if (isAppInBackground && activeSubject != null && backgroundGraceSeconds > 0) {
            delay(1000L)
            backgroundGraceSeconds -= 1
        } else if (isAppInBackground && activeSubject != null && backgroundGraceSeconds == 0) {
            isTimerPaused = true
        }
    }

    // AUTO-RESUME timer when face is re-detected by ML Kit
    LaunchedEffect(isFaceDetected) {
        if (isFaceDetected && activeSubject != null && !isAppInBackground) {
            isTimerPaused = false
        }
    }

    // 1. Real-time Leaderboard Listener
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            db.collection("students").addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.map { doc ->
                        StudentRecord(
                            name = doc.getString("name") ?: "Student",
                            avatar = doc.getString("avatar") ?: "🎓",
                            todayMinutes = doc.getLong("todayMinutes")?.toInt() ?: 0,
                            completionPercent = doc.getLong("completionPercent")?.toInt() ?: 0,
                            isStudying = doc.getBoolean("isStudying") ?: false
                        )
                    }
                    leaderboard = list
                }
            }
        }
    }

    // 2. Countdown Timer Loop
    LaunchedEffect(activeSubject, secondsLeft, isTimerPaused, isFaceDetected, isFaceDetectionEnabled) {
        if (activeSubject != null && isFaceDetectionEnabled && !isFaceDetected) {
            isTimerPaused = true
        }

        if (activeSubject != null && secondsLeft > 0 && !isTimerPaused) {
            delay(1000L)
            secondsLeft -= 1
        } else if (activeSubject != null && secondsLeft == 0) {
            val earnedMins = totalSecondsPlanned / 60
            routines = routines.map { if (it.subject == activeSubject) it.copy(isCompleted = true) else it }
            val newCompletion = ((routines.filter { it.isCompleted }.sumOf { it.minutes }.toFloat() / routines.sumOf { it.minutes }.let { if (it == 0) 1 else it }) * 100).toInt()

            saveStudyProgress(db, activeDocId, studentName, avatarPic, earnedMins, newCompletion, isStudying = false, subject = "")
            activeSubject = null
            isTimerPaused = false
        }
    }

    // SIMPLE STUDENT NAME / ID LOGIN SCREEN (No email/password required)
    if (!isLoggedIn) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    "🎓 PadhaiPLUS+",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }

            Text("Student Schedule, Alarms & Focus Tracker", fontSize = 14.sp, color = Color.Gray)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = studentName,
                onValueChange = { studentName = it },
                label = { Text("Student Name / ID") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (studentName.isNotBlank()) {
                        val cleanName = studentName.trim()
                        val baseId = cleanName.lowercase().replace(Regex("[^a-z0-9_]"), "_").ifBlank { "student" }

                        db.collection("students").document(baseId).get()
                            .addOnSuccessListener { doc ->
                                if (doc.exists()) {
                                    studentName = doc.getString("name") ?: cleanName
                                    avatarPic = doc.getString("avatar") ?: "🎓"
                                    activeDocId = baseId
                                    isLoggedIn = true
                                    Toast.makeText(context, "Welcome back, $cleanName!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val updates = hashMapOf<String, Any>(
                                        "name" to cleanName,
                                        "avatar" to avatarPic,
                                        "isStudying" to false,
                                        "todayMinutes" to 0,
                                        "completionPercent" to 0
                                    )
                                    db.collection("students").document(baseId).set(updates, SetOptions.merge())
                                        .addOnSuccessListener {
                                            studentName = cleanName
                                            activeDocId = baseId
                                            isLoggedIn = true
                                            Toast.makeText(context, "Created new ID: $baseId", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Connection Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        Toast.makeText(context, "Please enter your Student Name / ID", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Enter with ID", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    // MAIN APP LAYOUT WITH 3 SECTIONS (NO LOGOUT BUTTON)
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    icon = { Text("📚", fontSize = 20.sp) },
                    label = { Text("Routines") }
                )
                NavigationBarItem(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    icon = { Text("🇳🇵", fontSize = 20.sp) },
                    label = { Text("Nepali Cal") }
                )
                NavigationBarItem(
                    selected = selectedSection == 2,
                    onClick = { selectedSection = 2 },
                    icon = { Text("🏆", fontSize = 20.sp) },
                    label = { Text("Leaderboard") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Header Row
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showProfileDialog = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(avatarPic, fontSize = 24.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(studentName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Tap for Profile ⚙️", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // ACTIVE TIMER CARD (WITH ML KIT & 60s SWITCH GRACE WINDOW)
            if (activeSubject != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTimerPaused || !isFaceDetected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            when {
                                !isFaceDetected && isFaceDetectionEnabled -> "⚠️ NO FACE DETECTED (Auto-Resumes when detected)"
                                isAppInBackground -> "⏱️ App Switch Grace Window: ${backgroundGraceSeconds}s left"
                                isTimerPaused -> "⏸️ TIMER PAUSED"
                                else -> "🟢 LIVE STUDYING: $activeSubject"
                            },
                            fontWeight = FontWeight.Bold
                        )
                        val mins = secondsLeft / 60
                        val secs = secondsLeft % 60
                        Text(String.format("%02d:%02d", mins, secs), fontSize = 36.sp, fontWeight = FontWeight.Bold)

                        // ML Kit Camera Preview & Presence Status
                        if (isFaceDetectionEnabled) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape)) {
                                    FaceDetectionCameraPreview(
                                        onFacePresenceChanged = { hasFace -> isFaceDetected = hasFace },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (isFaceDetected) "👤 ML Kit Face Detected (Active)" else "❌ Face Away! Auto-resumes on return",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFaceDetected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { isTimerPaused = !isTimerPaused }) {
                                Text(if (isTimerPaused) "Resume" else "Pause")
                            }
                            Button(
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                onClick = {
                                    val studiedSeconds = totalSecondsPlanned - secondsLeft
                                    val earnedMins = studiedSeconds / 60
                                    routines = routines.map { if (it.subject == activeSubject) it.copy(isCompleted = true) else it }
                                    val newCompletion = ((routines.filter { it.isCompleted }.sumOf { it.minutes }.toFloat() / routines.sumOf { it.minutes }.let { if (it == 0) 1 else it }) * 100).toInt()

                                    saveStudyProgress(db, activeDocId, studentName, avatarPic, earnedMins, newCompletion, isStudying = false, subject = "")
                                    activeSubject = null
                                    isTimerPaused = false
                                }
                            ) {
                                Text("Finish & Save")
                            }
                        }
                    }
                }
            }

            // SECTION CONTENT SWITCHER
            when (selectedSection) {
                0 -> RoutineAndAlarmsSection(
                    subjectInput = subjectInput,
                    onSubjectChange = { subjectInput = it },
                    minutesInput = minutesInput,
                    onMinutesChange = { minutesInput = it },
                    hourInput = hourInput,
                    onHourChange = { hourInput = it },
                    minuteInput = minuteInput,
                    onMinuteChange = { minuteInput = it },
                    routines = routines,
                    completionPercent = completionPercent,
                    is24HourFormat = is24HourFormat,
                    onToggle24HourFormat = { is24HourFormat = it },
                    activeSubject = activeSubject,
                    onAddRoutine = {
                        val mins = minutesInput.toIntOrNull() ?: 30
                        val hr = hourInput.toIntOrNull().let { if (it != null && it in 0..23) it else 8 }
                        val min = minuteInput.toIntOrNull().let { if (it != null && it in 0..59) it else 0 }
                        if (subjectInput.isNotBlank()) {
                            val newRoutine = RoutineItem(
                                subject = subjectInput.trim(),
                                minutes = mins,
                                hour = hr,
                                minute = min,
                                isEnabled = true
                            )
                            routines = routines + newRoutine
                            AlarmScheduler.scheduleAlarm(context, newRoutine.id, newRoutine.subject, newRoutine.hour, newRoutine.minute)
                            subjectInput = ""
                        }
                    },
                    onToggleAlarm = { item, enabled ->
                        routines = routines.map { if (it.id == item.id) it.copy(isEnabled = enabled) else it }
                        if (enabled) {
                            AlarmScheduler.scheduleAlarm(context, item.id, item.subject, item.hour, item.minute)
                        } else {
                            AlarmScheduler.cancelAlarm(context, item.id)
                        }
                    },
                    onStartRoutine = { item ->
                        activeSubject = item.subject
                        totalSecondsPlanned = item.minutes * 60
                        secondsLeft = item.minutes * 60
                        isTimerPaused = false
                        saveStudyProgress(db, activeDocId, studentName, avatarPic, 0, completionPercent, isStudying = true, subject = item.subject)
                    },
                    onEditRoutineAlarm = { item -> editingRoutine = item }
                )
                1 -> NepaliCalendarSection()
                2 -> LeaderboardSection(leaderboard = leaderboard)
            }
        }
    }

    // Profile Dialog
    if (showProfileDialog) {
        val totalStudied = leaderboard.find { it.name.lowercase() == studentName.lowercase() }?.todayMinutes ?: 0
        ProfileDialog(
            studentName = studentName,
            currentAvatar = avatarPic,
            todayMinutes = totalStudied,
            completionPercent = completionPercent,
            onDismiss = { showProfileDialog = false },
            onSaveAvatar = { newAvatar ->
                avatarPic = newAvatar
                sharedPref.edit().putString("student_avatar", newAvatar).apply()
                saveStudyProgress(db, activeDocId, studentName, newAvatar, 0, completionPercent, isStudying = activeSubject != null, subject = activeSubject ?: "")
            }
        )
    }

    // Interactive Time Adjuster Dialog
    editingRoutine?.let { routine ->
        InteractiveAlarmAdjusterDialog(
            routine = routine,
            onDismiss = { editingRoutine = null },
            onSave = { updated ->
                routines = routines.map { if (it.id == updated.id) updated else it }
                if (updated.isEnabled) {
                    AlarmScheduler.scheduleAlarm(context, updated.id, updated.subject, updated.hour, updated.minute)
                }
                editingRoutine = null
            }
        )
    }
}

// SECTION 1: SCROLLABLE ROUTINE & ALARM CREATION WITH 24-HOUR FORMAT SUPPORT
@Composable
fun RoutineAndAlarmsSection(
    subjectInput: String,
    onSubjectChange: (String) -> Unit,
    minutesInput: String,
    onMinutesChange: (String) -> Unit,
    hourInput: String,
    onHourChange: (String) -> Unit,
    minuteInput: String,
    onMinuteChange: (String) -> Unit,
    routines: List<RoutineItem>,
    completionPercent: Int,
    is24HourFormat: Boolean,
    onToggle24HourFormat: (Boolean) -> Unit,
    activeSubject: String?,
    onAddRoutine: () -> Unit,
    onToggleAlarm: (RoutineItem, Boolean) -> Unit,
    onStartRoutine: (RoutineItem) -> Unit,
    onEditRoutineAlarm: (RoutineItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Work Completion Progress Bar
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("🎯 Today's Work Completion", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("$completionPercent%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { completionPercent / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                )
            }
        }

        // Header + 24-Hour Format Toggle Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Add Routine & Daily Alarm", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (is24HourFormat) "24-Hr" else "12-Hr", fontSize = 12.sp)
                Spacer(Modifier.width(4.dp))
                Switch(
                    checked = is24HourFormat,
                    onCheckedChange = onToggle24HourFormat
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = subjectInput,
                onValueChange = onSubjectChange,
                label = { Text("Subject") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.5f)
            )
            Spacer(Modifier.width(4.dp))
            OutlinedTextField(
                value = minutesInput,
                onValueChange = onMinutesChange,
                label = { Text("Mins") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(0.8f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = hourInput,
                onValueChange = onHourChange,
                label = { Text(if (is24HourFormat) "Hour (0-23)" else "Hour (1-12)") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = minuteInput,
                onValueChange = onMinuteChange,
                label = { Text("Min (0-59)") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onAddRoutine,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Text("+ Add")
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Your Routines & Ringing Alarms", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))

        if (routines.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No study routines added yet. Create one above!", color = Color.Gray)
            }
        } else {
            Column {
                routines.forEach { item ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.subject, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    if (item.isCompleted) {
                                        Spacer(Modifier.width(6.dp))
                                        Text("✅ Done", fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                    }
                                }

                                val timeFormatted = if (is24HourFormat) {
                                    String.format("%02d:%02d", item.hour, item.minute)
                                } else {
                                    val amPm = if (item.hour >= 12) "PM" else "AM"
                                    val displayHour = if (item.hour % 12 == 0) 12 else item.hour % 12
                                    String.format("%02d:%02d %s", displayHour, item.minute, amPm)
                                }

                                Text("⏰ $timeFormatted | ⏱️ ${item.minutes} mins")
                                Text(
                                    "🔔 Ringing Alarm: ${if (item.isEnabled) "ENABLED" else "DISABLED"}",
                                    fontSize = 12.sp,
                                    color = if (item.isEnabled) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = item.isEnabled,
                                        onCheckedChange = { enabled -> onToggleAlarm(item, enabled) }
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    TextButton(onClick = { onEditRoutineAlarm(item) }) {
                                        Text("⏰ Adjust")
                                    }
                                }
                                Button(
                                    enabled = activeSubject == null && !item.isCompleted,
                                    onClick = { onStartRoutine(item) }
                                ) {
                                    Text(if (item.isCompleted) "Completed" else "Start")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// SECTION 2: NEPALI BIKRAM SAMBAT (BS) CALENDAR
@Composable
fun NepaliCalendarSection() {
    val todayNepali = remember { NepaliCalendarUtils.getCurrentNepaliDate() }
    var selectedMonthIndex by remember { mutableStateOf(todayNepali.monthIndex) }

    val monthName = NepaliCalendarUtils.nepaliMonths[selectedMonthIndex]
    val totalDays = NepaliCalendarUtils.getDaysInMonth(selectedMonthIndex)

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🇳🇵 NEPALI BIKRAM SAMBAT (BS) CALENDAR", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text("Today: ${todayNepali.year} ${todayNepali.monthName}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("${todayNepali.dayOfWeekName}, Day ${todayNepali.day}", fontSize = 14.sp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                selectedMonthIndex = if (selectedMonthIndex == 0) 11 else selectedMonthIndex - 1
            }) {
                Text("◀", fontSize = 18.sp)
            }
            Text(monthName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            IconButton(onClick = {
                selectedMonthIndex = (selectedMonthIndex + 1) % 12
            }) {
                Text("▶", fontSize = 18.sp)
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            NepaliCalendarUtils.weekDays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            items(totalDays) { index ->
                val dayNum = index + 1
                val isToday = (selectedMonthIndex == todayNepali.monthIndex && dayNum == todayNepali.day)

                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$dayNum",
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// SECTION 3: DUAL LEADERBOARDS (HIGH HOURS VS PLAN COMPLETION %)
@Composable
fun LeaderboardSection(leaderboard: List<StudentRecord>) {
    var leaderboardMode by remember { mutableStateOf(0) }

    val sortedList = remember(leaderboard, leaderboardMode) {
        if (leaderboardMode == 0) {
            leaderboard.sortedByDescending { it.todayMinutes }
        } else {
            leaderboard.sortedByDescending { it.completionPercent }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("🏆 Friends Study Leaderboards", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Compete for high hours & plan completion!", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))

        PrimaryTabRow(selectedTabIndex = leaderboardMode) {
            Tab(
                selected = leaderboardMode == 0,
                onClick = { leaderboardMode = 0 },
                text = { Text("⏱️ High Hours") }
            )
            Tab(
                selected = leaderboardMode == 1,
                onClick = { leaderboardMode = 1 },
                text = { Text("🎯 Plan Done %") }
            )
        }

        Spacer(Modifier.height(8.dp))

        if (sortedList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No friends on leaderboard yet. Be the first!", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sortedList.size) { rankIndex ->
                    val student = sortedList[rankIndex]
                    val rank = rankIndex + 1

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text("#$rank ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(student.avatar, fontSize = 20.sp)
                                }
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(student.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        if (student.isStudying) {
                                            Spacer(Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF4CAF50))
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (rank == 1) BadgeChip("👑 #1 Champ", Color(0xFFFFD700))
                                        if (student.isStudying) BadgeChip("🟢 Studying", Color(0xFF81C784))
                                        when {
                                            student.todayMinutes >= 500 -> BadgeChip("🏆 Legend", Color(0xFFE040FB))
                                            student.todayMinutes >= 200 -> BadgeChip("🥇 Master", Color(0xFFFFB74D))
                                            student.todayMinutes >= 100 -> BadgeChip("🥈 Pro", Color(0xFF90CAF9))
                                            student.todayMinutes >= 30 -> BadgeChip("🥉 Scholar", Color(0xFFA1887F))
                                            else -> BadgeChip("⚡ Rookie", Color(0xFFB0BEC5))
                                        }
                                    }
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    if (leaderboardMode == 0) "${student.todayMinutes} mins" else "${student.completionPercent}%",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    if (leaderboardMode == 0) "${student.completionPercent}% done" else "${student.todayMinutes} mins studied",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BadgeChip(text: String, containerColor: Color) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(end = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// Interactive Time Adjuster Dialog for Alarms
@Composable
fun InteractiveAlarmAdjusterDialog(
    routine: RoutineItem,
    onDismiss: () -> Unit,
    onSave: (RoutineItem) -> Unit
) {
    var selectedHour by remember { mutableStateOf(routine.hour) }
    var selectedMinute by remember { mutableStateOf(routine.minute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⏰ Adjust Alarm for ${routine.subject}") },
        text = {
            Column {
                Text("Select Quick Time Preset:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(onClick = { selectedHour = 7; selectedMinute = 0 }) { Text("🌅 Morning") }
                    Button(onClick = { selectedHour = 13; selectedMinute = 0 }) { Text("☀️ Noon") }
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(onClick = { selectedHour = 18; selectedMinute = 0 }) { Text("🌆 Evening") }
                    Button(onClick = { selectedHour = 21; selectedMinute = 0 }) { Text("🌙 Night") }
                }

                Spacer(Modifier.height(16.dp))
                Text("Fine-tune Time:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Hour", fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { selectedHour = if (selectedHour == 0) 23 else selectedHour - 1 }) { Text("-") }
                            Text(String.format("%02d", selectedHour), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            TextButton(onClick = { selectedHour = (selectedHour + 1) % 24 }) { Text("+") }
                        }
                    }
                    Text(":", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Minute", fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { selectedMinute = if (selectedMinute < 5) 55 else selectedMinute - 5 }) { Text("-") }
                            Text(String.format("%02d", selectedMinute), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            TextButton(onClick = { selectedMinute = (selectedMinute + 5) % 60 }) { Text("+") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(routine.copy(hour = selectedHour, minute = selectedMinute, isEnabled = true))
            }) {
                Text("Save & Enable Alarm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun saveStudyProgress(
    db: FirebaseFirestore,
    docId: String,
    studentName: String,
    avatar: String,
    minutesToAdd: Int,
    completionPercent: Int,
    isStudying: Boolean,
    subject: String
) {
    val cleanId = docId.ifBlank {
        studentName.trim().lowercase().replace(Regex("[^a-z0-9_]"), "_").ifBlank { "student" }
    }
    val updates = hashMapOf<String, Any>(
        "name" to studentName.trim(),
        "avatar" to avatar,
        "isStudying" to isStudying,
        "currentSubject" to subject,
        "completionPercent" to completionPercent
    )
    if (minutesToAdd > 0) {
        updates["todayMinutes"] = FieldValue.increment(minutesToAdd.toLong())
    }
    db.collection("students").document(cleanId).set(updates, SetOptions.merge())
}

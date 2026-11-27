package com.ps.padhaiplus

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.delay

// Data structure for a single study routine with time of day and alarm status
data class RoutineItem(
    val id: Int = (0..100000).random(),
    val subject: String = "",
    val minutes: Int = 30,
    val hour: Int = 8,
    val minute: Int = 0,
    val isEnabled: Boolean = true
)

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PadhaiPlusApp()
                }
            }
        }
    }
}

@Composable
fun PadhaiPlusApp() {
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val sharedPref = remember { context.getSharedPreferences("padhai_prefs", Context.MODE_PRIVATE) }

    // Persistent login state
    var studentName by remember { mutableStateOf(sharedPref.getString("student_name", "") ?: "") }
    var isLoggedIn by remember { mutableStateOf(studentName.isNotBlank()) }

    // Navigation sections state: 0 = Routines & Alarms, 1 = Nepali Calendar, 2 = Leaderboard
    var selectedSection by remember { mutableStateOf(0) }

    // Routine inputs & storage
    var subjectInput by remember { mutableStateOf("") }
    var minutesInput by remember { mutableStateOf("30") }
    var hourInput by remember { mutableStateOf("8") }
    var minuteInput by remember { mutableStateOf("0") }
    var routines by remember { mutableStateOf(listOf<RoutineItem>()) }

    // Interactive Time Adjuster dialog state
    var editingRoutine by remember { mutableStateOf<RoutineItem?>(null) }

    // Active Timer state
    var activeSubject by remember { mutableStateOf<String?>(null) }
    var secondsLeft by remember { mutableStateOf(0) }
    var totalSecondsPlanned by remember { mutableStateOf(0) }
    var isTimerPaused by remember { mutableStateOf(false) }

    // Leaderboard state
    var leaderboard by remember { mutableStateOf(listOf<Triple<String, Int, Boolean>>()) }

    // Observe Lifecycle to PAUSE timer when app leaves foreground
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                if (activeSubject != null) {
                    isTimerPaused = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 1. Listen to ALL students in real-time for leaderboard
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            db.collection("students").addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.map { doc ->
                        val name = doc.getString("name") ?: "Student"
                        val mins = doc.getLong("todayMinutes")?.toInt() ?: 0
                        val live = doc.getBoolean("isStudying") ?: false
                        Triple(name, mins, live)
                    }.sortedByDescending { it.second }
                    leaderboard = list
                }
            }
        }
    }

    // 2. Countdown Timer Loop (Pauses when app goes background)
    LaunchedEffect(activeSubject, secondsLeft, isTimerPaused) {
        if (activeSubject != null && secondsLeft > 0 && !isTimerPaused) {
            delay(1000L)
            secondsLeft -= 1
        } else if (activeSubject != null && secondsLeft == 0) {
            val earnedMins = totalSecondsPlanned / 60
            saveStudyProgress(db, studentName, earnedMins, isStudying = false, subject = "")
            activeSubject = null
            isTimerPaused = false
        }
    }

    // SCREEN 1: Persistent Name Login
    if (!isLoggedIn) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("PadhaiPLUS+", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text("Plan your routine. Track alarms. Compete with friends.")
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = studentName,
                onValueChange = { studentName = it },
                label = { Text("Enter Your Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (studentName.isNotBlank()) {
                        val cleanName = studentName.trim()
                        sharedPref.edit().putString("student_name", cleanName).apply()
                        val cleanId = cleanName.lowercase()
                        db.collection("students").document(cleanId).set(
                            mapOf("name" to cleanName, "isStudying" to false),
                            SetOptions.merge()
                        )
                        isLoggedIn = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Studying (Keep Me Logged In)")
            }
        }
        return
    }

    // MAIN APP LAYOUT WITH 3 SECTIONS & BOTTOM NAVIGATION
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Welcome, $studentName!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Logged in session saved", fontSize = 12.sp, color = Color.Gray)
                }
                TextButton(onClick = {
                    sharedPref.edit().remove("student_name").apply()
                    isLoggedIn = false
                }) {
                    Text("Switch User")
                }
            }

            Spacer(Modifier.height(8.dp))

            // ACTIVE TIMER CARD (Visible across sections if timer is active)
            if (activeSubject != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTimerPaused) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            if (isTimerPaused) "⏸️ TIMER PAUSED (App Background)" else "🟢 LIVE STUDYING: $activeSubject",
                            fontWeight = FontWeight.Bold
                        )
                        val mins = secondsLeft / 60
                        val secs = secondsLeft % 60
                        Text(String.format("%02d:%02d", mins, secs), fontSize = 36.sp, fontWeight = FontWeight.Bold)
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
                                    saveStudyProgress(db, studentName, earnedMins, isStudying = false, subject = "")
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
                        saveStudyProgress(db, studentName, 0, isStudying = true, subject = item.subject)
                    },
                    onEditRoutineAlarm = { item -> editingRoutine = item }
                )
                1 -> NepaliCalendarSection()
                2 -> LeaderboardSection(leaderboard = leaderboard)
            }
        }
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

// SECTION 1: Routine & Alarm System
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
    activeSubject: String?,
    onAddRoutine: () -> Unit,
    onToggleAlarm: (RoutineItem, Boolean) -> Unit,
    onStartRoutine: (RoutineItem) -> Unit,
    onEditRoutineAlarm: (RoutineItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Add Routine & Daily Alarm", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = subjectInput,
                onValueChange = onSubjectChange,
                label = { Text("Subject") },
                modifier = Modifier.weight(1.5f)
            )
            Spacer(Modifier.width(4.dp))
            OutlinedTextField(
                value = minutesInput,
                onValueChange = onMinutesChange,
                label = { Text("Mins") },
                modifier = Modifier.weight(0.8f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = hourInput,
                onValueChange = onHourChange,
                label = { Text("Hour (0-23)") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = minuteInput,
                onValueChange = onMinuteChange,
                label = { Text("Min (0-59)") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onAddRoutine,
                modifier = Modifier.height(56.dp)
            ) {
                Text("+ Add")
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Your Routines & Vibration Alarms", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))

        if (routines.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No study routines added yet. Create one above!", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(routines) { item ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.subject, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                val amPm = if (item.hour >= 12) "PM" else "AM"
                                val displayHour = if (item.hour % 12 == 0) 12 else item.hour % 12
                                val timeFormatted = String.format("%02d:%02d %s", displayHour, item.minute, amPm)
                                Text("🕒 $timeFormatted | ⏱️ ${item.minutes} mins")
                                Text(
                                    "📳 Alarm: ${if (item.isEnabled) "ENABLED" else "DISABLED"}",
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
                                    enabled = activeSubject == null,
                                    onClick = { onStartRoutine(item) }
                                ) {
                                    Text("Start")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// SECTION 2: Nepali Bikram Sambat (BS) Calendar
@Composable
fun NepaliCalendarSection() {
    val todayNepali = remember { NepaliCalendarUtils.getCurrentNepaliDate() }
    var selectedMonthIndex by remember { mutableStateOf(todayNepali.monthIndex) }

    val monthName = NepaliCalendarUtils.nepaliMonths[selectedMonthIndex]
    val totalDays = NepaliCalendarUtils.getDaysInMonth(selectedMonthIndex)

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
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

        // Month Navigation
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

        // Weekday Headers
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

        // Days Grid
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

// SECTION 3: Leaderboard with Dynamic Achievement Badges
@Composable
fun LeaderboardSection(leaderboard: List<Triple<String, Int, Boolean>>) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("🏆 Live Friends Study Leaderboard", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Earn badges as you log more study minutes!", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(12.dp))

        if (leaderboard.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No friends on leaderboard yet. Be the first!", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(leaderboard.size) { rankIndex ->
                    val (name, mins, isLive) = leaderboard[rankIndex]
                    val rank = rankIndex + 1

                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("#$rank ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    if (isLive) {
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
                                    // Dynamic Badges
                                    if (rank == 1) BadgeChip("👑 #1 Champ", Color(0xFFFFD700))
                                    if (isLive) BadgeChip("🟢 Studying Now", Color(0xFF81C784))
                                    when {
                                        mins >= 500 -> BadgeChip("🏆 Legend", Color(0xFFE040FB))
                                        mins >= 200 -> BadgeChip("🥇 Master", Color(0xFFFFB74D))
                                        mins >= 100 -> BadgeChip("🥈 Pro", Color(0xFF90CAF9))
                                        mins >= 30 -> BadgeChip("🥉 Scholar", Color(0xFFA1887F))
                                        else -> BadgeChip("⚡ Rookie", Color(0xFFB0BEC5))
                                    }
                                }
                            }
                            Text("$mins mins", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
    studentName: String,
    minutesToAdd: Int,
    isStudying: Boolean,
    subject: String
) {
    val cleanId = studentName.trim().lowercase()
    val updates = hashMapOf<String, Any>(
        "name" to studentName.trim(),
        "isStudying" to isStudying,
        "currentSubject" to subject
    )
    if (minutesToAdd > 0) {
        updates["todayMinutes"] = FieldValue.increment(minutesToAdd.toLong())
    }
    db.collection("students").document(cleanId).set(updates, SetOptions.merge())
}

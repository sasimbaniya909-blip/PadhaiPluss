package com.ps.padhaiplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay

// Data structure for a single study routine
data class RoutineItem(
    val id: String = "",
    val subject: String = "",
    val minutes: Int = 30
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    val db = remember { FirebaseFirestore.getInstance() }

    // Student info & screen state
    var studentName by remember { mutableStateOf("") }
    var isLoggedIn by remember { mutableStateOf(false) }

    // Routine inputs
    var subjectInput by remember { mutableStateOf("") }
    var minutesInput by remember { mutableStateOf("30") }
    var routines by remember { mutableStateOf(listOf<RoutineItem>()) }

    // Active Timer state
    var activeSubject by remember { mutableStateOf<String?>(null) }
    var secondsLeft by remember { mutableStateOf(0) }
    var totalSecondsPlanned by remember { mutableStateOf(0) }

    // Leaderboard state (List of Triple<Name, Minutes, IsLive>)
    var leaderboard by remember { mutableStateOf(listOf<Triple<String, Int, Boolean>>()) }

    // 1. Listen to ALL students in real-time for the comparison leaderboard
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

    // 2. Countdown Timer Loop
    LaunchedEffect(activeSubject, secondsLeft) {
        if (activeSubject != null && secondsLeft > 0) {
            delay(1000L)
            secondsLeft -= 1
        } else if (activeSubject != null && secondsLeft == 0) {
            // Finished full session! Save minutes to Firebase
            val earnedMins = totalSecondsPlanned / 60
            saveStudyProgress(db, studentName, earnedMins, isStudying = false, subject = "")
            activeSubject = null
        }
    }

    // SCREEN 1: Simple Name Login (Fast & reliable for Hackathon demo!)
    if (!isLoggedIn) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("PadhaiPLUS+", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Plan your routine. Compete with friends.")
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
                        val cleanId = studentName.trim().lowercase()
                        db.collection("students").document(cleanId).set(
                            mapOf("name" to studentName.trim(), "isStudying" to false),
                            SetOptions.merge()
                        )
                        isLoggedIn = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Studying")
            }
        }
        return
    }

    // SCREEN 2: Main Routine + Timer + Friends Comparison
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Welcome, $studentName!", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        // ACTIVE TIMER CARD (Shows only when studying)
        if (activeSubject != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("LIVE STUDYING: $activeSubject", fontWeight = FontWeight.Bold)
                    val mins = secondsLeft / 60
                    val secs = secondsLeft % 60
                    Text(String.format("%02d:%02d", mins, secs), fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        // Save partial minutes studied before stopping
                        val studiedSeconds = totalSecondsPlanned - secondsLeft
                        val earnedMins = studiedSeconds / 60
                        saveStudyProgress(db, studentName, earnedMins, isStudying = false, subject = "")
                        activeSubject = null
                    }) {
                        Text("Finish & Log Minutes")
                    }
                }
            }
        }

        // ADD ROUTINE SECTION
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = subjectInput,
                onValueChange = { subjectInput = it },
                label = { Text("Subject (e.g. Physics)") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = minutesInput,
                onValueChange = { minutesInput = it },
                label = { Text("Mins") },
                modifier = Modifier.width(80.dp)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                val mins = minutesInput.toIntOrNull() ?: 30
                if (subjectInput.isNotBlank()) {
                    routines = routines + RoutineItem(subject = subjectInput.trim(), minutes = mins)
                    subjectInput = ""
                }
            }) {
                Text("+")
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Today's Routine (Tap Start to Study)", fontWeight = FontWeight.Bold)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(routines) { item ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.subject, fontWeight = FontWeight.Bold)
                            Text("${item.minutes} mins")
                        }
                        Button(
                            enabled = activeSubject == null,
                            onClick = {
                                activeSubject = item.subject
                                totalSecondsPlanned = item.minutes * 60
                                secondsLeft = item.minutes * 60
                                saveStudyProgress(db, studentName, 0, isStudying = true, subject = item.subject)
                            }
                        ) {
                            Text("Start")
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // FRIENDS STUDY TIME COMPARISON LEADERBOARD
        Text("Live Friends Study Leaderboard", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(leaderboard) { (name, mins, isLive) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(if (isLive) "🟢 $name (Studying now)" else "⚪ $name")
                    Text("$mins mins", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Helper function to update Firebase
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
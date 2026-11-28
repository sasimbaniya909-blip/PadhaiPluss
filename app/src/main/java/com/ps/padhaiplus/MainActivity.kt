package com.ps.padhaiplus

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.ps.padhaiplus.ui.theme.AppThemeStyle
import com.ps.padhaiplus.ui.theme.LocalAppTheme
import com.ps.padhaiplus.ui.theme.PadhaiPlusTheme
import com.ps.padhaiplus.ui.theme.PastelBlobBackground
import com.ps.padhaiplus.ui.theme.SubjectSwatchColors
import com.ps.padhaiplus.ui.theme.SubjectSwatchIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

data class RoutineItem(
    val id: Int = (0..100000).random(),
    val subject: String = "",
    val minutes: Int = 60,
    val hour: Int = 8,
    val minute: Int = 0,
    val isEnabled: Boolean = true,
    val isCompleted: Boolean = false,
    val colorIndex: Int = 0,
)

data class StudentRecord(
    val name: String = "",
    val uid: String = "",
    val avatar: String = "🎓",
    val todayMinutes: Int = 0,
    val weeklyMinutes: Int = 0,
    val monthlyMinutes: Int = 0,
    val completionPercent: Int = 0,
    val isStudying: Boolean = false,
)

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            val context = LocalContext.current
            val sharedPref = remember {
                context.getSharedPreferences("padhaiplus_prefs", MODE_PRIVATE)
            }
            var currentTheme by remember {
                mutableStateOf(
                    AppThemeStyle.fromId(sharedPref.getString("app_theme", null))
                )
            }

            PadhaiPlusTheme(selectedTheme = currentTheme) {
                PadhaiPlusApp(
                    activity = this@MainActivity,
                    sharedPref = sharedPref,
                    currentTheme = currentTheme,
                    onSelectTheme = { newTheme ->
                        currentTheme = newTheme
                        sharedPref.edit { putString("app_theme", newTheme.id) }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PadhaiPlusApp(
    activity: ComponentActivity,
    sharedPref: SharedPreferences,
    currentTheme: AppThemeStyle,
    onSelectTheme: (AppThemeStyle) -> Unit,
) {
    val context = LocalContext.current
    val theme = LocalAppTheme.current
    val scope = rememberCoroutineScope()
    val db = remember {
        FirebaseFirestore.getInstance().apply {
            enableNetwork()
        }
    }
    val auth = remember { FirebaseAuth.getInstance() }
    val credentialManager = remember { CredentialManager.create(context) }

    // Login & Profile states
    var isLoggedIn by remember {
        mutableStateOf(sharedPref.getBoolean("is_logged_in", false) || auth.currentUser != null)
    }
    var studentName by remember {
        mutableStateOf(sharedPref.getString("student_name", "") ?: "")
    }
    var studentEmail by remember {
        mutableStateOf(sharedPref.getString("student_email", "") ?: "")
    }
    var studentNameInput by remember { mutableStateOf("") }
    var activeDocId by remember {
        mutableStateOf(sharedPref.getString("active_doc_id", "") ?: "")
    }
    var isLoggingIn by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var avatarPic by remember {
        mutableStateOf(sharedPref.getString("student_avatar", "🎓") ?: "🎓")
    }

    // Restore Google Auth session if user is already signed in
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null && !isLoggedIn) {
            val uid = currentUser.uid
            val fallbackName = currentUser.displayName?.takeIf { it.isNotBlank() } ?: "Student"
            val email = currentUser.email.orEmpty()
            val docId = uid.ifBlank { email.replace("@", "_").replace(".", "_") }
            db.collection("students").document(docId).get()
                .addOnSuccessListener { doc ->
                    studentName = if (doc.exists()) doc.getString("name") ?: fallbackName else fallbackName
                    avatarPic = if (doc.exists()) doc.getString("avatar") ?: avatarPic else avatarPic
                    studentEmail = email
                    activeDocId = docId
                    isLoggedIn = true
                }
                .addOnFailureListener {
                    studentName = fallbackName
                    studentEmail = email
                    activeDocId = docId
                    isLoggedIn = true
                }
        }
    }

    // Format & Navigation options
    var selectedSection by remember { mutableIntStateOf(0) }
    var is24HourFormat by remember { mutableStateOf(true) }

    // Routine inputs & storage (Hour & Minute only)
    var subjectInput by remember { mutableStateOf("") }
    var hourInput by remember { mutableStateOf("8") }
    var minuteInput by remember { mutableStateOf("0") }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    var routines by remember { mutableStateOf(loadRoutines(sharedPref)) }

    LaunchedEffect(routines) {
        saveRoutines(sharedPref, routines)
    }

    // Dialog & BottomSheet states
    var showAddSubjectSheet by remember { mutableStateOf(false) }
    var editingRoutine by remember { mutableStateOf<RoutineItem?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showThemeSelectionScreen by remember { mutableStateOf(false) }

    LaunchedEffect(selectedSection) {
        if (selectedSection != 3) {
            showThemeSelectionScreen = false
        }
    }

    // Active Timer state, ML Kit Face Detection, & 60s Switch Grace Period
    var activeSubject by remember { mutableStateOf<String?>(null) }
    var secondsLeft by remember { mutableIntStateOf(0) }
    var totalSecondsPlanned by remember { mutableIntStateOf(0) }
    var isTimerPaused by remember { mutableStateOf(false) }
    val isFaceDetectionEnabled = true
    var isFaceDetected by remember { mutableStateOf(true) }
    var backgroundGraceSeconds by remember { mutableIntStateOf(60) }
    var isAppInBackground by remember { mutableStateOf(false) }

    // Leaderboard state
    var leaderboard by remember { mutableStateOf(listOf<StudentRecord>()) }

    val myTodayStudiedMinutes =
        leaderboard.find {
            (activeDocId.isNotBlank() && it.uid == activeDocId) ||
                    it.name.equals(studentName, ignoreCase = true)
        }?.todayMinutes ?: 0

    // Work Completion Percentage (%) based on planned study time vs actual studied time
    val totalPlannedMins = routines.sumOf { it.minutes }.let { if (it == 0) 1 else it }
    val completionPercent =
        ((myTodayStudiedMinutes.toFloat() / totalPlannedMins.toFloat()) * 100).toInt().coerceIn(0, 100)

    // 1-MIN SWITCH WINDOW (Background lifecycle timer)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> isAppInBackground = true
                Lifecycle.Event.ON_RESUME -> {
                    isAppInBackground = false
                    backgroundGraceSeconds = 60
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Single Active Session Enforcement Check
    LaunchedEffect(isLoggedIn, activeDocId) {
        if (isLoggedIn && activeDocId.isNotBlank()) {
            db.collection("students").document(activeDocId).get().addOnSuccessListener { document ->
                val activeSession = document.getString("active_session")
                val localSession = sharedPref.getString("session_id", "")
                if (activeSession != null && localSession != null && activeSession != localSession) {
                    isLoggedIn = false
                    Toast.makeText(
                        context,
                        "Logged out: Account in use on another device",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // Background tracking without auto-pausing
    LaunchedEffect(isAppInBackground, activeSubject, backgroundGraceSeconds) {
        if (isAppInBackground && activeSubject != null && backgroundGraceSeconds > 0) {
            delay(1000L)
            backgroundGraceSeconds -= 1
        }
    }

    // Real-time Leaderboard Listener (Daily, Weekly, Monthly)
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            val todayKey = currentDayKey()
            val weekKey = currentWeekKey()
            val monthKey = currentMonthKey()
            db.collection("students").addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    leaderboard = snapshot.documents.map { doc ->
                        val docDayKey = doc.getString("dayKey")
                        val docWeekKey = doc.getString("weekKey")
                        val docMonthKey = doc.getString("monthKey")
                        val dailyMins = if (docDayKey == null || docDayKey == todayKey) {
                            doc.getLong("todayMinutes")?.toInt() ?: 0
                        } else {
                            0
                        }
                        val weeklyMins = if (docWeekKey == null || docWeekKey == weekKey) {
                            doc.getLong("weeklyMinutes")?.toInt() ?: dailyMins
                        } else {
                            0
                        }
                        val monthlyMins = if (docMonthKey == null || docMonthKey == monthKey) {
                            doc.getLong("monthlyMinutes")?.toInt() ?: weeklyMins
                        } else {
                            0
                        }
                        StudentRecord(
                            name = doc.getString("name") ?: "Student",
                            uid = doc.getString("uid") ?: doc.id,
                            avatar = doc.getString("avatar") ?: "🎓",
                            todayMinutes = dailyMins,
                            weeklyMinutes = weeklyMins,
                            monthlyMinutes = monthlyMins,
                            completionPercent = doc.getLong("completionPercent")?.toInt() ?: 0,
                            isStudying = doc.getBoolean("isStudying") ?: false
                        )
                    }
                }
            }
        }
    }

    // Countdown Timer Loop (does not auto-pause)
    LaunchedEffect(
        activeSubject,
        secondsLeft,
        isTimerPaused
    ) {
        if (activeSubject != null && secondsLeft > 0 && !isTimerPaused) {
            delay(1000L)
            secondsLeft -= 1
        } else if (activeSubject != null && secondsLeft == 0) {
            val earnedMins = totalSecondsPlanned / 60
            routines = routines.map {
                if (it.subject == activeSubject) it.copy(isCompleted = true) else it
            }
            val newCompletion = ((routines.filter { it.isCompleted }.sumOf { it.minutes }
                .toFloat() / routines.sumOf { it.minutes }.let { if (it == 0) 1 else it }) * 100)
                .toInt()

            saveStudyProgress(
                db = db,
                docId = activeDocId,
                studentName = studentName,
                avatar = avatarPic,
                minutesToAdd = earnedMins,
                completionPercent = newCompletion,
                isStudying = false,
                subject = ""
            )
            activeSubject = null
            isTimerPaused = false
        }
    }

    PastelBlobBackground {
        if (!isLoggedIn) {
            LoginScreen(
                studentNameInput = studentNameInput,
                onStudentNameChange = { studentNameInput = it },
                isLoggingIn = isLoggingIn,
                loginError = loginError,
                onGoogleSignIn = {
                    val webClientId = activity.getString(R.string.default_web_client_id)
                    if (webClientId.contains("000000000000")) {
                        loginError =
                            "Configure your Google Web Client ID in strings.xml (from Firebase Console > Authentication > Google)."
                        return@LoginScreen
                    }
                    scope.launch {
                        isLoggingIn = true
                        loginError = null
                        try {
                            val googleIdOption = GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId(webClientId)
                                .setAutoSelectEnabled(false)
                                .build()
                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .build()
                            val result = credentialManager.getCredential(
                                context = activity,
                                request = request
                            )
                            val credential = result.credential
                            if (credential is CustomCredential &&
                                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                            ) {
                                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                                val firebaseCred =
                                    GoogleAuthProvider.getCredential(googleCred.idToken, null)
                                auth.signInWithCredential(firebaseCred)
                                    .addOnSuccessListener { authResult ->
                                        val user = authResult.user
                                        val uid = user?.uid.orEmpty()
                                        val displayName =
                                            user?.displayName?.takeIf { it.isNotBlank() }
                                                ?: "Student"
                                        val email = user?.email.orEmpty()
                                        val docId = uid.ifBlank {
                                            email.replace("@", "_").replace(".", "_")
                                        }
                                        val docRef = db.collection("students").document(docId)
                                        docRef.get().addOnSuccessListener { doc ->
                                            val name = if (doc.exists()) {
                                                doc.getString("name") ?: displayName
                                            } else {
                                                displayName
                                            }
                                            val avatar = if (doc.exists()) {
                                                doc.getString("avatar") ?: "🎓"
                                            } else {
                                                "🎓"
                                            }
                                            val updates = hashMapOf<String, Any>(
                                                "name" to name,
                                                "uid" to uid,
                                                "email" to email,
                                                "avatar" to avatar,
                                                "isStudying" to false
                                            )
                                            if (!doc.exists()) {
                                                updates["todayMinutes"] = 0
                                                updates["weeklyMinutes"] = 0
                                                updates["monthlyMinutes"] = 0
                                                updates["dayKey"] = currentDayKey()
                                                updates["weekKey"] = currentWeekKey()
                                                updates["monthKey"] = currentMonthKey()
                                                updates["completionPercent"] = 0
                                            }
                                            docRef.set(updates, SetOptions.merge())
                                                .addOnSuccessListener {
                                                    isLoggingIn = false
                                                    studentName = name
                                                    studentEmail = email
                                                    avatarPic = avatar
                                                    activeDocId = docId
                                                    isLoggedIn = true
                                                    sharedPref.edit {
                                                        putBoolean("is_logged_in", true)
                                                        putString("student_name", name)
                                                        putString("student_email", email)
                                                        putString("active_doc_id", docId)
                                                        putString("student_avatar", avatar)
                                                    }
                                                }
                                                .addOnFailureListener { e ->
                                                    isLoggingIn = false
                                                    loginError =
                                                        "Firestore error: ${e.localizedMessage}"
                                                }
                                        }.addOnFailureListener { e ->
                                            isLoggingIn = false
                                            loginError = "Error: ${e.localizedMessage}"
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        isLoggingIn = false
                                        loginError = "Sign-in failed: ${e.localizedMessage}"
                                    }
                            } else {
                                isLoggingIn = false
                                loginError = "Unexpected credential type."
                            }
                        } catch (_: GetCredentialException) {
                            isLoggingIn = false
                            loginError = "Google Sign-In cancelled or unavailable."
                        } catch (e: Exception) {
                            isLoggingIn = false
                            loginError = "Error: ${e.localizedMessage}"
                        }
                    }
                },
                onStudentIdSignIn = {
                    val cleanName = studentNameInput.trim()
                    if (cleanName.isNotBlank()) {
                        isLoggingIn = true
                        loginError = null
                        val baseId = cleanName.lowercase()
                            .replace(Regex("[^a-z0-9_]"), "_")
                            .ifBlank { "student" }
                        val docRef = db.collection("students").document(baseId)
                        docRef.get()
                            .addOnSuccessListener { doc ->
                                if (doc.exists()) {
                                    isLoggingIn = false
                                    studentName = doc.getString("name") ?: cleanName
                                    studentEmail = doc.getString("email").orEmpty()
                                    avatarPic = doc.getString("avatar") ?: "🎓"
                                    activeDocId = baseId
                                    isLoggedIn = true
                                    sharedPref.edit {
                                        putBoolean("is_logged_in", true)
                                        putString("student_name", studentName)
                                        putString("student_email", studentEmail)
                                        putString("active_doc_id", activeDocId)
                                        putString("student_avatar", avatarPic)
                                    }
                                } else {
                                    val updates = hashMapOf<String, Any>(
                                        "name" to cleanName,
                                        "uid" to baseId,
                                        "avatar" to avatarPic,
                                        "isStudying" to false,
                                        "todayMinutes" to 0,
                                        "weeklyMinutes" to 0,
                                        "monthlyMinutes" to 0,
                                        "dayKey" to currentDayKey(),
                                        "weekKey" to currentWeekKey(),
                                        "monthKey" to currentMonthKey(),
                                        "completionPercent" to 0
                                    )
                                    docRef.set(updates, SetOptions.merge())
                                        .addOnSuccessListener {
                                            isLoggingIn = false
                                            studentName = cleanName
                                            studentEmail = ""
                                            activeDocId = baseId
                                            isLoggedIn = true
                                            sharedPref.edit {
                                                putBoolean("is_logged_in", true)
                                                putString("student_name", cleanName)
                                                putString("student_email", "")
                                                putString("active_doc_id", baseId)
                                                putString("student_avatar", avatarPic)
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            isLoggingIn = false
                                            loginError = "Error: ${e.localizedMessage}"
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                val isOffline = e is FirebaseFirestoreException &&
                                        e.code == FirebaseFirestoreException.Code.UNAVAILABLE
                                if (isOffline) {
                                    docRef.get(Source.CACHE)
                                        .addOnSuccessListener { cachedDoc ->
                                            isLoggingIn = false
                                            if (cachedDoc.exists()) {
                                                studentName =
                                                    cachedDoc.getString("name") ?: cleanName
                                                studentEmail =
                                                    cachedDoc.getString("email").orEmpty()
                                                avatarPic = cachedDoc.getString("avatar") ?: "🎓"
                                                activeDocId = baseId
                                                isLoggedIn = true
                                            } else {
                                                loginError =
                                                    "Offline: cannot create new ID without internet."
                                            }
                                        }.addOnFailureListener {
                                            isLoggingIn = false
                                            loginError = "Offline. Check internet connection."
                                        }
                                } else {
                                    isLoggingIn = false
                                    loginError = "Connection error: ${e.localizedMessage}"
                                }
                            }
                    } else {
                        loginError = "Please enter your Student Name or ID"
                    }
                }
            )
            return@PastelBlobBackground
        }

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                FloatingPillBottomBar(
                    selectedSection = selectedSection,
                    onSelectSection = { selectedSection = it }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(
                        start = 18.dp,
                        end = 18.dp,
                        top = 12.dp,
                        bottom = innerPadding.calculateBottomPadding()
                    )
            ) {
                when (selectedSection) {
                    0 -> RoutineDashboardSection(
                        studentName = studentName,
                        avatarPic = avatarPic,
                        currentTheme = currentTheme,
                        routines = routines,
                        totalPlannedMins = totalPlannedMins,
                        todayStudiedMinutes = myTodayStudiedMinutes,
                        completionPercent = completionPercent,
                        is24HourFormat = is24HourFormat,
                        onToggle24HourFormat = { is24HourFormat = it },
                        activeSubject = activeSubject,
                        secondsLeft = secondsLeft,
                        isTimerPaused = isTimerPaused,
                        isFaceDetected = isFaceDetected,
                        isFaceDetectionEnabled = isFaceDetectionEnabled,
                        isAppInBackground = isAppInBackground,
                        backgroundGraceSeconds = backgroundGraceSeconds,
                        onFacePresenceChanged = { isFaceDetected = it },
                        onTogglePause = { isTimerPaused = !isTimerPaused },
                        onFinishTimer = {
                            val studiedSeconds = totalSecondsPlanned - secondsLeft
                            val earnedMins = studiedSeconds / 60
                            routines = routines.map {
                                if (it.subject == activeSubject) it.copy(isCompleted = true) else it
                            }
                            val newCompletion =
                                ((routines.filter { it.isCompleted }.sumOf { it.minutes }
                                    .toFloat() / routines.sumOf { it.minutes }
                                    .let { if (it == 0) 1 else it }) * 100).toInt()

                            saveStudyProgress(
                                db = db,
                                docId = activeDocId,
                                studentName = studentName,
                                avatar = avatarPic,
                                minutesToAdd = earnedMins,
                                completionPercent = newCompletion,
                                isStudying = false,
                                subject = ""
                            )
                            activeSubject = null
                            isTimerPaused = false
                        },
                        onOpenProfile = { showProfileDialog = true },
                        onOpenAddSubject = { showAddSubjectSheet = true },
                        onToggleAlarm = { item, enabled ->
                            routines = routines.map {
                                if (it.id == item.id) it.copy(isEnabled = enabled) else it
                            }
                            if (enabled) {
                                AlarmScheduler.scheduleAlarm(
                                    context,
                                    item.id,
                                    item.subject,
                                    item.hour,
                                    item.minute
                                )
                            } else {
                                AlarmScheduler.cancelAlarm(context, item.id)
                            }
                        },
                        onStartRoutine = { item ->
                            activeSubject = item.subject
                            totalSecondsPlanned = item.minutes * 60
                            secondsLeft = item.minutes * 60
                            isTimerPaused = false
                            saveStudyProgress(
                                db = db,
                                docId = activeDocId,
                                studentName = studentName,
                                avatar = avatarPic,
                                minutesToAdd = 0,
                                completionPercent = completionPercent,
                                isStudying = true,
                                subject = item.subject
                            )
                        },
                        onEditRoutineAlarm = { item -> editingRoutine = item }
                    )

                    1 -> NepaliCalendarSection(
                        routines = routines,
                        todayStudiedMinutes = myTodayStudiedMinutes,
                        completionPercent = completionPercent
                    )

                    2 -> LeaderboardSection(leaderboard = leaderboard)

                    3 -> {
                        if (showThemeSelectionScreen) {
                            ThemeSelectionScreen(
                                currentTheme = currentTheme,
                                onSelectTheme = onSelectTheme,
                                onBack = { showThemeSelectionScreen = false }
                            )
                        } else {
                            SettingsSection(
                                studentName = studentName,
                                studentEmail = studentEmail,
                                avatarPic = avatarPic,
                                currentTheme = currentTheme,
                                onOpenProfile = { showProfileDialog = true },
                                onOpenThemeSelection = { showThemeSelectionScreen = true },
                                onLogOut = {
                                    auth.signOut()
                                    scope.launch {
                                        runCatching {
                                            credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                        }
                                    }
                                    activeSubject = null
                                    isTimerPaused = false
                                    studentName = ""
                                    studentEmail = ""
                                    activeDocId = ""
                                    isLoggedIn = false
                                    sharedPref.edit {
                                        putBoolean("is_logged_in", false)
                                        putString("student_name", "")
                                        putString("student_email", "")
                                        putString("active_doc_id", "")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Add Subject Modal Bottom Sheet (Hour & Minute only)
        if (showAddSubjectSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showAddSubjectSheet = false },
                sheetState = sheetState,
                containerColor = theme.cardSurface,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                AddSubjectSheetContent(
                    subjectInput = subjectInput,
                    onSubjectChange = { subjectInput = it },
                    hourInput = hourInput,
                    onHourChange = { hourInput = it },
                    minuteInput = minuteInput,
                    onMinuteChange = { minuteInput = it },
                    selectedColorIndex = selectedColorIndex,
                    onSelectColor = { selectedColorIndex = it },
                    onSave = {
                        val hr = hourInput.toIntOrNull().let {
                            if (it != null && it in 0..23) it else 8
                        }
                        val min = minuteInput.toIntOrNull().let {
                            if (it != null && it in 0..59) it else 0
                        }
                        if (subjectInput.isNotBlank()) {
                            val newRoutine = RoutineItem(
                                subject = subjectInput.trim(),
                                minutes = 60,
                                hour = hr,
                                minute = min,
                                isEnabled = true,
                                colorIndex = selectedColorIndex
                            )
                            routines = routines + newRoutine
                            AlarmScheduler.scheduleAlarm(
                                context,
                                newRoutine.id,
                                newRoutine.subject,
                                newRoutine.hour,
                                newRoutine.minute
                            )
                            subjectInput = ""
                            showAddSubjectSheet = false
                        } else {
                            Toast.makeText(
                                context,
                                "Please enter a subject name",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
        }

        // Profile & Theme Personalization Dialog
        if (showProfileDialog) {
            ProfileDialog(
                studentName = studentName,
                studentEmail = studentEmail,
                currentAvatar = avatarPic,
                todayMinutes = myTodayStudiedMinutes,
                completionPercent = completionPercent,
                currentTheme = currentTheme,
                onSelectTheme = onSelectTheme,
                onSignOut = {
                    showProfileDialog = false
                    auth.signOut()
                    scope.launch {
                        runCatching {
                            credentialManager.clearCredentialState(ClearCredentialStateRequest())
                        }
                    }
                    activeSubject = null
                    isTimerPaused = false
                    studentName = ""
                    studentEmail = ""
                    activeDocId = ""
                    isLoggedIn = false
                    sharedPref.edit {
                        putBoolean("is_logged_in", false)
                        putString("student_name", "")
                        putString("student_email", "")
                        putString("active_doc_id", "")
                    }
                },
                onDismiss = { showProfileDialog = false },
                onSaveAvatar = { newAvatar ->
                    avatarPic = newAvatar
                    sharedPref.edit { putString("student_avatar", newAvatar) }
                    saveStudyProgress(
                        db = db,
                        docId = activeDocId,
                        studentName = studentName,
                        avatar = newAvatar,
                        minutesToAdd = 0,
                        completionPercent = completionPercent,
                        isStudying = activeSubject != null,
                        subject = activeSubject ?: ""
                    )
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
                        AlarmScheduler.scheduleAlarm(
                            context,
                            updated.id,
                            updated.subject,
                            updated.hour,
                            updated.minute
                        )
                    }
                    editingRoutine = null
                }
            )
        }
    }
}

@Composable
fun LoginScreen(
    studentNameInput: String,
    onStudentNameChange: (String) -> Unit,
    isLoggingIn: Boolean,
    loginError: String?,
    onGoogleSignIn: () -> Unit,
    onStudentIdSignIn: () -> Unit,
) {
    val theme = LocalAppTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .shadow(10.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF19223B)),
            contentAlignment = Alignment.Center
        ) {
            Text("🎓", fontSize = 34.sp)
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "PadhaiPLUS+",
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            color = theme.primary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Plan routines, track presence and earn badges",
            fontSize = 14.sp,
            color = theme.textSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = theme.cardSurface,
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Welcome back",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Sign in with your Google account to sync your progress",
                    fontSize = 13.sp,
                    color = theme.textSecondary
                )

                Spacer(Modifier.height(20.dp))

                // Primary Google Sign-In Button
                Button(
                    enabled = !isLoggingIn,
                    onClick = onGoogleSignIn,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(8.dp, RoundedCornerShape(50))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "G",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = theme.primary
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (isLoggingIn) "Signing in..." else "Continue with Google",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = theme.borderAccent.copy(alpha = 0.25f)
                    )
                    Text(
                        "  or continue with Student ID  ",
                        fontSize = 12.sp,
                        color = theme.textSecondary
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = theme.borderAccent.copy(alpha = 0.25f)
                    )
                }
                Spacer(Modifier.height(16.dp))

                Text(
                    "Student name or ID",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.textPrimary
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = studentNameInput,
                    onValueChange = onStudentNameChange,
                    placeholder = {
                        Text(
                            "Enter student name or ID",
                            color = theme.textSecondary.copy(alpha = 0.6f)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.primary,
                        unfocusedBorderColor = theme.borderAccent.copy(alpha = 0.4f),
                        focusedContainerColor = theme.cardSurface,
                        unfocusedContainerColor = theme.cardSurface,
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    enabled = !isLoggingIn,
                    onClick = onStudentIdSignIn,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        "Sign in with ID",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.primary
                    )
                }

                if (!loginError.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = loginError,
                        fontSize = 12.sp,
                        color = Color(0xFFD32F2F),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Your progress is saved securely",
            fontSize = 12.sp,
            color = theme.textSecondary.copy(alpha = 0.75f)
        )
    }
}

@Composable
fun FloatingPillBottomBar(
    selectedSection: Int,
    onSelectSection: (Int) -> Unit,
) {
    val theme = LocalAppTheme.current
    val items = listOf("Routines", "Calendar", "Leaderboard", "Settings")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = theme.cardSurface,
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, label ->
                    val selected = selectedSection == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) theme.navSelectedBg else Color.Transparent)
                            .clickable { onSelectSection(index) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) theme.primary else theme.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RoutineDashboardSection(
    studentName: String,
    avatarPic: String,
    currentTheme: AppThemeStyle,
    routines: List<RoutineItem>,
    totalPlannedMins: Int,
    todayStudiedMinutes: Int,
    completionPercent: Int,
    is24HourFormat: Boolean,
    onToggle24HourFormat: (Boolean) -> Unit,
    activeSubject: String?,
    secondsLeft: Int,
    isTimerPaused: Boolean,
    isFaceDetected: Boolean,
    isFaceDetectionEnabled: Boolean,
    isAppInBackground: Boolean,
    backgroundGraceSeconds: Int,
    onFacePresenceChanged: (Boolean) -> Unit,
    onTogglePause: () -> Unit,
    onFinishTimer: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAddSubject: () -> Unit,
    onToggleAlarm: (RoutineItem, Boolean) -> Unit,
    onStartRoutine: (RoutineItem) -> Unit,
    onEditRoutineAlarm: (RoutineItem) -> Unit,
) {
    val theme = LocalAppTheme.current
    val todayNepali = remember { NepaliCalendarUtils.getCurrentNepaliDate() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Top Profile & Theme Pill Card
        Surface(
            shape = RoundedCornerShape(50),
            color = theme.cardSurface,
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, theme.borderAccent, RoundedCornerShape(50))
                .clip(RoundedCornerShape(50))
                .clickable { onOpenProfile() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(theme.softContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(avatarPic, fontSize = 22.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            studentName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Tap for profile & themes ⚙️",
                            fontSize = 12.sp,
                            color = theme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(theme.softContainer)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        "${currentTheme.emoji} ${currentTheme.displayName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("›", fontSize = 14.sp, color = theme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 2. Add Subject Pill Card
        Surface(
            shape = RoundedCornerShape(50),
            color = theme.cardSurface,
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, theme.borderAccent, RoundedCornerShape(50))
                .clip(RoundedCornerShape(50))
                .clickable { onOpenAddSubject() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(theme.softContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📖", fontSize = 22.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Add subject",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textPrimary
                        )
                        Text(
                            "Tap here to add subject",
                            fontSize = 12.sp,
                            color = theme.primary
                        )
                    }
                }
                Text(
                    "›",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.primary,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Planned vs Actual Study Time Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = theme.cardSurface,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "⏱️ Study Time: Planned vs Actual",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textPrimary
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Wanted to study", fontSize = 12.sp, color = theme.textSecondary)
                        Text("$totalPlannedMins mins", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = theme.primary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Actually studied", fontSize = 12.sp, color = theme.textSecondary)
                        Text("$todayStudiedMinutes mins", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // 3. Greeting & Today's Progress Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = theme.softContainer,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${NepaliCalendarUtils.getGreetingTitle(studentName)} ☀️",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${todayNepali.dayOfWeekName}, ${todayNepali.monthName} ${todayNepali.day} · ${routines.size} classes today",
                            fontSize = 13.sp,
                            color = theme.textSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(theme.cardSurface.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📚", fontSize = 28.sp)
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    "Today's progress",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.primary
                )
                Spacer(Modifier.height(6.dp))

                RoundedPillProgressBar(
                    progress = completionPercent / 100f,
                    fillColor = theme.primary,
                    trackColor = theme.cardSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                )

                Spacer(Modifier.height(6.dp))
                Text(
                    "$completionPercent% done",
                    fontSize = 12.sp,
                    color = theme.textSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 4. Active Timer Card (if studying)
        if (activeSubject != null) {
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = theme.cardSurface,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        color = if (isTimerPaused || !isFaceDetected) Color(0xFFE57373) else theme.primary,
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        when {
                            !isFaceDetected && isFaceDetectionEnabled ->
                                "⚠️ NO FACE DETECTED (Auto-Resumes)"
                            isAppInBackground ->
                                "⏱️ Switch Grace Window: ${backgroundGraceSeconds}s"
                            isTimerPaused -> "⏸️ TIMER PAUSED"
                            else -> "🟢 LIVE STUDYING: $activeSubject"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = theme.textPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    val mins = secondsLeft / 60
                    val secs = secondsLeft % 60
                    Text(
                        String.format(Locale.US, "%02d:%02d", mins, secs),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = theme.primary
                    )

                    if (isFaceDetectionEnabled) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                            ) {
                                FaceDetectionCameraPreview(
                                    onFacePresenceChanged = onFacePresenceChanged,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (isFaceDetected) "👤 Face Detected (Active)"
                                else "❌ Face Away! Auto-resumes on return",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFaceDetected) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onTogglePause,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
                        ) {
                            Text(if (isTimerPaused) "Resume" else "Pause")
                        }
                        Button(
                            onClick = onFinishTimer,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                        ) {
                            Text("Finish & Save")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // 5. Today's Schedule Header + 12/24 Hr Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Today's Schedule:",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = theme.textPrimary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(theme.cardSurface)
                    .clickable { onToggle24HourFormat(!is24HourFormat) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    if (is24HourFormat) "24-Hr" else "12-Hr",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.primary
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        if (routines.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = theme.cardSurface,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📅", fontSize = 32.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No classes added yet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = theme.textPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tap \"Add subject\" above to schedule your first class.",
                        fontSize = 13.sp,
                        color = theme.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            routines.forEach { item ->
                val swatchColor =
                    SubjectSwatchColors[item.colorIndex.coerceIn(0, SubjectSwatchColors.lastIndex)]
                val swatchIcon =
                    SubjectSwatchIcons[item.colorIndex.coerceIn(0, SubjectSwatchIcons.lastIndex)]

                val timeFormatted = if (is24HourFormat) {
                    String.format(Locale.US, "%02d:%02d", item.hour, item.minute)
                } else {
                    formatTime12Hr(item.hour, item.minute)
                }

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = theme.cardSurface,
                    shadowElevation = 5.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(swatchColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(swatchIcon, fontSize = 24.sp)
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                timeFormatted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = theme.textSecondary
                            )
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    item.subject,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.textPrimary
                                )
                                if (item.isCompleted) {
                                    Spacer(Modifier.width(6.dp))
                                    Text("✅", fontSize = 14.sp)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(theme.trackBg)
                                    .clickable { onEditRoutineAlarm(item) }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "⏰ Adjust time",
                                    fontSize = 11.sp,
                                    color = theme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Switch(
                                checked = item.isEnabled,
                                onCheckedChange = { enabled -> onToggleAlarm(item, enabled) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = theme.primary
                                )
                            )
                            Spacer(Modifier.height(4.dp))
                            Button(
                                enabled = activeSubject == null && !item.isCompleted,
                                onClick = { onStartRoutine(item) },
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = theme.primary,
                                    disabledContainerColor = theme.softContainer
                                ),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    if (item.isCompleted) "Done" else "Start",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.isCompleted) theme.primary else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun AddSubjectSheetContent(
    subjectInput: String,
    onSubjectChange: (String) -> Unit,
    hourInput: String,
    onHourChange: (String) -> Unit,
    minuteInput: String,
    onMinuteChange: (String) -> Unit,
    selectedColorIndex: Int,
    onSelectColor: (Int) -> Unit,
    onSave: () -> Unit,
) {
    val theme = LocalAppTheme.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 28.dp)
    ) {
        Text(
            "Add Subject:",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textPrimary
        )
        Spacer(Modifier.height(14.dp))

        Text(
            "Subject name",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = theme.textPrimary
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = subjectInput,
            onValueChange = onSubjectChange,
            placeholder = {
                Text("Enter subject name", color = theme.textSecondary.copy(alpha = 0.6f))
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = theme.primary,
                unfocusedBorderColor = theme.borderAccent.copy(alpha = 0.3f),
                focusedContainerColor = theme.trackBg,
                unfocusedContainerColor = theme.trackBg,
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(14.dp))
        Text(
            "Hour (O' Clock - 0 to 23):",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = theme.textPrimary
        )
        Spacer(Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(24) { h ->
                val isSelected = hourInput.toIntOrNull() == h
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) theme.primary else theme.trackBg,
                    modifier = Modifier
                        .clickable { onHourChange(h.toString()) }
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            String.format(Locale.US, "%02d", h),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else theme.textPrimary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "Minute (Min - 0 to 59):",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = theme.textPrimary
        )
        Spacer(Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(12) { mIndex ->
                val m = mIndex * 5
                val isSelected = minuteInput.toIntOrNull() == m
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) theme.primary else theme.trackBg,
                    modifier = Modifier
                        .clickable { onMinuteChange(m.toString()) }
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            String.format(Locale.US, "%02d", m),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else theme.textPrimary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Color",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = theme.textPrimary
        )
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SubjectSwatchColors.forEachIndexed { idx, color ->
                val isSelected = idx == selectedColorIndex
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(if (isSelected) 6.dp else 2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = if (isSelected) theme.textPrimary else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onSelectColor(idx) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(SubjectSwatchIcons[idx], fontSize = 18.sp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onSave,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(6.dp, RoundedCornerShape(50))
        ) {
            Text(
                "Save class",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// SECTION 2: VISUALLY UPGRADED NEPALI BIKRAM SAMBAT (BS) CALENDAR
@Composable
fun NepaliCalendarSection(
    routines: List<RoutineItem>,
    todayStudiedMinutes: Int,
    completionPercent: Int,
) {
    val theme = LocalAppTheme.current
    val todayNepali = remember { NepaliCalendarUtils.getCurrentNepaliDate() }
    var selectedYear by remember { mutableIntStateOf(todayNepali.year) }
    var selectedMonthIndex by remember { mutableIntStateOf(todayNepali.monthIndex) }
    var selectedDay by remember { mutableIntStateOf(todayNepali.day) }

    val monthName = NepaliCalendarUtils.nepaliMonths[selectedMonthIndex]
    val totalDays = NepaliCalendarUtils.getDaysInMonth(selectedMonthIndex, selectedYear)
    val startOffset =
        NepaliCalendarUtils.getFirstDayOfWeekOffset(selectedYear, selectedMonthIndex)

    val completedCount = routines.count { it.isCompleted }
    val latestRoutine = routines.lastOrNull { it.isCompleted } ?: routines.firstOrNull()
    val dailyGoalMinutes = routines.sumOf { it.minutes }.coerceAtLeast(60)
    val goalProgress = (todayStudiedMinutes.toFloat() / dailyGoalMinutes).coerceIn(0f, 1f)
    val goalPercent = (goalProgress * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "$monthName $selectedYear BS",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = theme.textPrimary
                )
                Text(
                    "🇳🇵 ${todayNepali.dayOfWeekName}, ${todayNepali.monthName} ${todayNepali.day}, ${todayNepali.year}",
                    fontSize = 12.sp,
                    color = theme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedYear != todayNepali.year || selectedMonthIndex != todayNepali.monthIndex) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(theme.softContainer)
                            .clickable {
                                selectedYear = todayNepali.year
                                selectedMonthIndex = todayNepali.monthIndex
                                selectedDay = todayNepali.day
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "Today",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.primary
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = theme.cardSurface,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (selectedMonthIndex == 0) {
                                selectedMonthIndex = 11
                                selectedYear -= 1
                            } else {
                                selectedMonthIndex -= 1
                            }
                            selectedDay = 1
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("‹", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = theme.primary)
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = theme.cardSurface,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (selectedMonthIndex == 11) {
                                selectedMonthIndex = 0
                                selectedYear += 1
                            } else {
                                selectedMonthIndex += 1
                            }
                            selectedDay = 1
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("›", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = theme.primary)
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            NepaliCalendarUtils.weekDays.forEachIndexed { idx, day ->
                Text(
                    text = day.take(1),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = if (idx == 6) Color(0xFFD14B5A) else theme.textSecondary
                )
            }
        }

        val totalCells = startOffset + totalDays
        val rows = (totalCells + 6) / 7

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNum = cellIndex - startOffset + 1
                        if (dayNum in 1..totalDays) {
                            val isToday =
                                selectedYear == todayNepali.year &&
                                        selectedMonthIndex == todayNepali.monthIndex &&
                                        dayNum == todayNepali.day
                            val isSelected = dayNum == selectedDay && !isToday
                            val isSaturday = col == 6
                            val hasRoutineDot = routines.isNotEmpty() && isToday

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isToday) theme.primary else theme.cardSurface,
                                shadowElevation = if (isToday) 8.dp else 3.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.05f)
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(
                                                1.5.dp,
                                                theme.borderAccent,
                                                RoundedCornerShape(14.dp)
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { selectedDay = dayNum }
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$dayNum",
                                        fontSize = 14.sp,
                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = when {
                                            isToday -> Color.White
                                            isSaturday -> Color(0xFFD14B5A)
                                            else -> theme.textPrimary
                                        }
                                    )
                                    if (hasRoutineDot) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 5.dp)
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = theme.cardSurface,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Daily goal",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.textPrimary
                    )
                    Text(
                        "$todayStudiedMinutes of $dailyGoalMinutes mins",
                        fontSize = 13.sp,
                        color = theme.textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(12.dp))
                RoundedPillProgressBar(
                    progress = goalProgress,
                    fillColor = theme.primary,
                    trackColor = theme.softContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "$goalPercent% complete · keep going! 💪",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.primary
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Today's Achievement:",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textPrimary
        )
        Spacer(Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = theme.cardSurface,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val badgeColor = latestRoutine?.let {
                    SubjectSwatchColors[it.colorIndex.coerceIn(0, SubjectSwatchColors.lastIndex)]
                } ?: theme.softContainer
                val badgeIcon = latestRoutine?.let {
                    SubjectSwatchIcons[it.colorIndex.coerceIn(0, SubjectSwatchIcons.lastIndex)]
                } ?: "🎯"

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(badgeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(badgeIcon, fontSize = 24.sp)
                }

                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (routines.isEmpty()) "$monthName $selectedDay, $selectedYear BS"
                        else "$completionPercent% of plan completed ($completedCount/${routines.size})",
                        fontSize = 12.sp,
                        color = theme.textSecondary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (completionPercent >= 100) "Plan Completed! 🎉" else (latestRoutine?.subject ?: "Ready to start studying"),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.textPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (completionPercent >= 100) theme.primary else theme.softContainer)
                        .border(3.dp, theme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (completionPercent >= 100) "✓" else "$completionPercent%",
                        fontSize = if (completionPercent >= 100) 20.sp else 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (completionPercent >= 100) Color.White else theme.primary
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// SECTION 3: DAILY / WEEKLY / MONTHLY LEADERBOARDS
@Composable
fun LeaderboardSection(leaderboard: List<StudentRecord>) {
    val theme = LocalAppTheme.current
    var timePeriod by remember { mutableIntStateOf(0) } // 0 = Daily, 1 = Weekly, 2 = Monthly
    var leaderboardMode by remember { mutableIntStateOf(0) } // 0 = High Hours, 1 = Plan Done %

    val periodMinutesFor: (StudentRecord) -> Int = { student ->
        when (timePeriod) {
            1 -> student.weeklyMinutes
            2 -> student.monthlyMinutes
            else -> student.todayMinutes
        }
    }

    val sortedList = remember(leaderboard, timePeriod, leaderboardMode) {
        if (leaderboardMode == 0) {
            leaderboard.sortedByDescending {
                when (timePeriod) {
                    1 -> it.weeklyMinutes
                    2 -> it.monthlyMinutes
                    else -> it.todayMinutes
                }
            }
        } else {
            leaderboard.sortedByDescending { it.completionPercent }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "🏆 Friends Study Leaderboards",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = theme.textPrimary
        )
        Text(
            "Compete across daily, weekly & monthly rankings!",
            fontSize = 13.sp,
            color = theme.textSecondary
        )
        Spacer(Modifier.height(12.dp))

        // Daily / Weekly / Monthly Period Switcher
        Surface(
            shape = RoundedCornerShape(50),
            color = theme.cardSurface,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Daily", "Weekly", "Monthly").forEachIndexed { idx, label ->
                    val selected = timePeriod == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) theme.primary else Color.Transparent)
                            .clickable { timePeriod = idx }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Color.White else theme.textSecondary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Metric Tab Switcher (High Hours vs Plan Done %)
        Surface(
            shape = RoundedCornerShape(50),
            color = theme.cardSurface,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                listOf("⏱️ High Hours", "🎯 Plan Done %").forEachIndexed { idx, title ->
                    val selected = leaderboardMode == idx
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .clickable { leaderboardMode = idx }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            title,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) theme.primary else theme.textSecondary
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) theme.primary else Color.Transparent)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        if (sortedList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No friends on leaderboard yet. Be the first!",
                    color = theme.textSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sortedList.size) { rankIndex ->
                    val student = sortedList[rankIndex]
                    val rank = rankIndex + 1
                    val totalMins = periodMinutesFor(student)
                    val hrs = totalMins / 60
                    val mins = totalMins % 60

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = theme.cardSurface,
                        shadowElevation = 5.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    "#$rank",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = theme.primary
                                )
                                Spacer(Modifier.width(12.dp))
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(theme.softContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(student.avatar, fontSize = 22.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            student.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = theme.textPrimary
                                        )
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
                                    Spacer(Modifier.height(6.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (rank == 1) {
                                            BadgeChip("👑 #1 Champ", Color(0xFFFFE47A), Color(0xFF3E2D00))
                                        }
                                        if (student.isStudying) {
                                            BadgeChip("🟢 Studying", Color(0xFFC8F0D2), Color(0xFF144D23))
                                        }
                                        when {
                                            totalMins >= 500 ->
                                                BadgeChip("🏆 Legend", Color(0xFFF3C6FF), Color(0xFF4A0E5C))
                                            totalMins >= 200 ->
                                                BadgeChip("🥇 Master", Color(0xFFFFDFB3), Color(0xFF5A3600))
                                            totalMins >= 100 ->
                                                BadgeChip("🥈 Pro", Color(0xFFCDE6FF), Color(0xFF0E3C66))
                                            totalMins >= 30 ->
                                                BadgeChip("🥉 Scholar", Color(0xFFE7DCD8), Color(0xFF452E28))
                                            else ->
                                                BadgeChip("🌱 Rookie", theme.trackBg, theme.textPrimary)
                                        }
                                    }
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    if (leaderboardMode == 0) "${hrs}hr ${mins}m"
                                    else "${student.completionPercent}%",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = theme.primary
                                )
                                Text(
                                    if (leaderboardMode == 0) "${student.completionPercent}% done"
                                    else "${hrs}hr ${mins}m",
                                    fontSize = 12.sp,
                                    color = theme.textSecondary
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
fun RoundedPillProgressBar(
    progress: Float,
    fillColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(trackColor)
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .height(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(fillColor)
            )
        }
    }
}

@Composable
fun BadgeChip(
    text: String,
    containerColor: Color,
    textColor: Color = Color.Black,
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// Interactive Time Adjuster Dialog for Alarms (matching sample #9)
@Composable
fun InteractiveAlarmAdjusterDialog(
    routine: RoutineItem,
    onDismiss: () -> Unit,
    onSave: (RoutineItem) -> Unit,
) {
    val theme = LocalAppTheme.current
    
    val initialHour24 = routine.hour
    val initialMinute = routine.minute
    
    val initialIsAm = initialHour24 < 12
    val initialHour12 = when {
        initialHour24 == 0 -> 12
        initialHour24 > 12 -> initialHour24 - 12
        else -> initialHour24
    }

    var selectedHour12 by remember { mutableIntStateOf(initialHour12) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }
    var isAm by remember { mutableStateOf(initialIsAm) }

    val hours = (1..12).toList()
    val minutes = (0..59).toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121212),
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                "Adjust Alarm for ${routine.subject}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WheelPicker(
                        items = hours.map { String.format(Locale.US, "%02d", it) },
                        selectedIndex = hours.indexOf(selectedHour12).coerceAtLeast(0),
                        onItemSelected = { index -> selectedHour12 = hours[index] },
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        ":",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE2E8F0),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    WheelPicker(
                        items = minutes.map { String.format(Locale.US, "%02d", it) },
                        selectedIndex = minutes.indexOf(selectedMinute).coerceAtLeast(0),
                        onItemSelected = { index -> selectedMinute = minutes[index] },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(
                        modifier = Modifier
                            .width(60.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "am",
                            fontSize = if (isAm) 24.sp else 18.sp,
                            fontWeight = if (isAm) FontWeight.Bold else FontWeight.Normal,
                            color = if (isAm) Color(0xFFFEE2E2) else Color(0xFF64748B),
                            modifier = Modifier
                                .clickable { isAm = true }
                                .padding(vertical = 8.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "pm",
                            fontSize = if (!isAm) 24.sp else 18.sp,
                            fontWeight = if (!isAm) FontWeight.Bold else FontWeight.Normal,
                            color = if (!isAm) Color(0xFFFEE2E2) else Color(0xFF64748B),
                            modifier = Modifier
                                .clickable { isAm = false }
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                onClick = {
                    val finalHour24 = when {
                        isAm && selectedHour12 == 12 -> 0
                        isAm -> selectedHour12
                        !isAm && selectedHour12 == 12 -> 12
                        else -> selectedHour12 + 12
                    }
                    onSave(routine.copy(hour = finalHour24, minute = selectedMinute, isEnabled = true))
                }
            ) {
                Text("Save & Enable Alarm", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (selectedIndex - 1).coerceAtLeast(0))

    LaunchedEffect(selectedIndex) {
        val target = (selectedIndex - 1).coerceIn(0, (items.size - 3).coerceAtLeast(0))
        listState.animateScrollToItem(target)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        items(items.size) { index ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clickable { onItemSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = items[index],
                    fontSize = if (isSelected) 28.sp else 18.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color(0xFFFEE2E2) else Color(0xFF64748B)
                )
            }
        }
    }
}

private fun formatTime12Hr(hour24: Int, minute: Int): String {
    val amPm = if (hour24 >= 12) "PM" else "AM"
    val displayHour = if (hour24 % 12 == 0) 12 else hour24 % 12
    return String.format(Locale.US, "%02d:%02d %s", displayHour, minute, amPm)
}

private fun currentDayKey(): String {
    val cal = Calendar.getInstance()
    return String.format(
        Locale.US,
        "%04d-%02d-%02d",
        cal[Calendar.YEAR],
        cal[Calendar.MONTH] + 1,
        cal[Calendar.DAY_OF_MONTH]
    )
}

private fun currentWeekKey(): String {
    val cal = Calendar.getInstance()
    return String.format(
        Locale.US,
        "%04d-W%02d",
        cal[Calendar.YEAR],
        cal[Calendar.WEEK_OF_YEAR]
    )
}

private fun currentMonthKey(): String {
    val cal = Calendar.getInstance()
    return String.format(
        Locale.US,
        "%04d-%02d",
        cal[Calendar.YEAR],
        cal[Calendar.MONTH] + 1
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
    subject: String,
) {
    val cleanId = docId.ifBlank {
        studentName.trim().lowercase().replace(Regex("[^a-z0-9_]"), "_").ifBlank { "student" }
    }
    val todayKey = currentDayKey()
    val weekKey = currentWeekKey()
    val monthKey = currentMonthKey()
    val docRef = db.collection("students").document(cleanId)

    val baseUpdates = hashMapOf<String, Any>(
        "name" to studentName.trim(),
        "uid" to cleanId,
        "avatar" to avatar,
        "isStudying" to isStudying,
        "currentSubject" to subject,
        "completionPercent" to completionPercent
    )

    if (minutesToAdd <= 0) {
        docRef.set(baseUpdates, SetOptions.merge())
        return
    }

    docRef.get().addOnSuccessListener { doc ->
        val storedDay = doc.getString("dayKey")
        val storedWeek = doc.getString("weekKey")
        val storedMonth = doc.getString("monthKey")

        baseUpdates["dayKey"] = todayKey
        baseUpdates["weekKey"] = weekKey
        baseUpdates["monthKey"] = monthKey

        baseUpdates["todayMinutes"] = if (storedDay == null || storedDay == todayKey) {
            FieldValue.increment(minutesToAdd.toLong())
        } else {
            minutesToAdd.toLong()
        }
        baseUpdates["weeklyMinutes"] = if (storedWeek == null || storedWeek == weekKey) {
            FieldValue.increment(minutesToAdd.toLong())
        } else {
            minutesToAdd.toLong()
        }
        baseUpdates["monthlyMinutes"] = if (storedMonth == null || storedMonth == monthKey) {
            FieldValue.increment(minutesToAdd.toLong())
        } else {
            minutesToAdd.toLong()
        }
        docRef.set(baseUpdates, SetOptions.merge())
    }.addOnFailureListener {
        baseUpdates["dayKey"] = todayKey
        baseUpdates["weekKey"] = weekKey
        baseUpdates["monthKey"] = monthKey
        baseUpdates["todayMinutes"] = FieldValue.increment(minutesToAdd.toLong())
        baseUpdates["weeklyMinutes"] = FieldValue.increment(minutesToAdd.toLong())
        baseUpdates["monthlyMinutes"] = FieldValue.increment(minutesToAdd.toLong())
        docRef.set(baseUpdates, SetOptions.merge())
    }
}

fun saveRoutines(sharedPref: SharedPreferences, routines: List<RoutineItem>) {
    val serialized = routines.joinToString(";;;") {
        "${it.id}:::${it.subject}:::${it.minutes}:::${it.hour}:::${it.minute}:::${it.isEnabled}:::${it.isCompleted}:::${it.colorIndex}"
    }
    sharedPref.edit { putString("saved_routines", serialized) }
}

fun loadRoutines(sharedPref: SharedPreferences): List<RoutineItem> {
    val serialized = sharedPref.getString("saved_routines", null) ?: return emptyList()
    if (serialized.isBlank()) return emptyList()
    return serialized.split(";;;").mapNotNull { entry ->
        val parts = entry.split(":::")
        if (parts.size >= 8) {
            RoutineItem(
                id = parts[0].toIntOrNull() ?: (0..100000).random(),
                subject = parts[1],
                minutes = parts[2].toIntOrNull() ?: 60,
                hour = parts[3].toIntOrNull() ?: 8,
                minute = parts[4].toIntOrNull() ?: 0,
                isEnabled = parts[5].toBoolean(),
                isCompleted = parts[6].toBoolean(),
                colorIndex = parts[7].toIntOrNull() ?: 0
            )
        } else null
    }
}

@Composable
fun SettingsSection(
    studentName: String,
    studentEmail: String,
    avatarPic: String,
    currentTheme: AppThemeStyle,
    onOpenProfile: () -> Unit,
    onOpenThemeSelection: () -> Unit,
    onLogOut: () -> Unit,
) {
    val theme = LocalAppTheme.current
    var classReminders by remember { mutableStateOf(true) }
    var dailySummary by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Text(
            "Settings",
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = theme.textPrimary
        )
        Text(
            "Make PadhaiPLUS+ work for you",
            fontSize = 14.sp,
            color = theme.textSecondary
        )
        Spacer(Modifier.height(20.dp))

        // Profile Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = theme.cardSurface,
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenProfile() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(theme.softContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(avatarPic, fontSize = 28.sp)
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        studentName.ifBlank { "Sasim" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = theme.textPrimary
                    )
                    Text(
                        "Manage profile & avatar",
                        fontSize = 13.sp,
                        color = theme.textSecondary
                    )
                }
                Text("›", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = theme.primary)
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Study preferences",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = theme.textPrimary
        )
        Spacer(Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = theme.cardSurface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Class reminders", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = theme.textPrimary)
                        Text("Get notified before each class", fontSize = 12.sp, color = theme.textSecondary)
                    }
                    Switch(
                        checked = classReminders,
                        onCheckedChange = { classReminders = it }
                    )
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = theme.borderAccent.copy(alpha = 0.2f))
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reminder time", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = theme.textPrimary)
                        Text("Get Reminder before time ends", fontSize = 12.sp, color = theme.textSecondary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("10 min before", fontSize = 13.sp, color = theme.primary, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(4.dp))
                        Text("›", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = theme.primary)
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = theme.borderAccent.copy(alpha = 0.2f))
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Daily study summary", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = theme.textPrimary)
                        Text("A little reflection on your progress", fontSize = 12.sp, color = theme.textSecondary)
                    }
                    Switch(
                        checked = dailySummary,
                        onCheckedChange = { dailySummary = it }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Theme selection",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = theme.textPrimary
        )
        Spacer(Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = theme.cardSurface,
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenThemeSelection() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Theme", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = theme.textPrimary)
                    Text("Choose your study space's look", fontSize = 12.sp, color = theme.textSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(currentTheme.displayName, fontSize = 13.sp, color = theme.primary, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(4.dp))
                    Text("›", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = theme.primary)
                }
            }
        }

        Spacer(Modifier.height(30.dp))

        OutlinedButton(
            onClick = onLogOut,
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Log out", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
        }

        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("PadhaiPLUS+", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = theme.textSecondary)
            Text("Your space to learn, one day at a time.", fontSize = 12.sp, color = theme.textSecondary.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun ThemeSelectionScreen(
    currentTheme: AppThemeStyle,
    onSelectTheme: (AppThemeStyle) -> Unit,
    onBack: () -> Unit,
) {
    val theme = LocalAppTheme.current
    var selectedAppTheme by remember { mutableStateOf(currentTheme) }
    var selectedAccent by remember { mutableStateOf("Lavender") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onBack() }.padding(vertical = 4.dp)
        ) {
            Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Theme", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = theme.textPrimary)
                Text("A little more you. A little more focus.", fontSize = 13.sp, color = theme.textSecondary)
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Appearance", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = theme.textPrimary)
        Text("Pick the light that feels right.", fontSize = 12.sp, color = theme.textSecondary)
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(
                Triple("Light", "☀️", true),
                Triple("Dark", "🌙", false),
                Triple("System", "⚡", false)
            ).forEach { (label, emoji, isSelected) ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = theme.cardSurface,
                    border = if (isSelected) BorderStroke(2.dp, theme.primary) else null,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(emoji, fontSize = 24.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = theme.textPrimary)
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .border(2.dp, if (isSelected) theme.primary else theme.borderAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(theme.primary)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Accent color", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = theme.textPrimary)
                Text("Choose your vibe", fontSize = 12.sp, color = theme.textSecondary)
            }
            Text(selectedAccent, fontSize = 13.sp, color = theme.textSecondary, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = theme.cardSurface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    Pair("Lavender", Color(0xFF6366F1)),
                    Pair("Ocean", Color(0xFF3B82F6)),
                    Pair("Sage", Color(0xFF10B981)),
                    Pair("Rose", Color(0xFFEC4899)),
                    Pair("Amber", Color(0xFFF59E0B))
                ).forEach { (name, color) ->
                    val isSelected = selectedAccent == name
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { selectedAccent = name },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Preview", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = theme.textPrimary)
        Spacer(Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = theme.cardSurface,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Your study space", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = theme.textPrimary)
                Text("A little progress, every day.", fontSize = 12.sp, color = theme.textSecondary)
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = theme.softContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("UP NEXT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = theme.primary)
                        Text("Mathematics", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = theme.textPrimary)
                        Text("2:00 PM – 3:00 PM", fontSize = 12.sp, color = theme.textSecondary)
                    }
                }
            }
        }

        Spacer(Modifier.height(30.dp))

        Button(
            onClick = {
                onSelectTheme(selectedAppTheme)
                onBack()
            },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Done", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Make your study space feel like home.",
            fontSize = 12.sp,
            color = theme.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
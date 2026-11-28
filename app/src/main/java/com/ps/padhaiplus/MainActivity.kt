package com.ps.padhaiplus

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ps.padhaiplus.ui.theme.AppThemeStyle
import com.ps.padhaiplus.ui.theme.LocalAppTheme
import com.ps.padhaiplus.ui.theme.PadhaiPlusTheme
import com.ps.padhaiplus.ui.theme.PastelBlobBackground
import com.ps.padhaiplus.ui.theme.SubjectSwatchIcons
import com.ps.padhaiplus.ui.theme.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class RoutineItem(
    val id: Int = (0..100000).random(),
    val subject: String = "",
    val startTime: String = "08:00",
    val endTime: String = "09:30",
    val minutes: Int = 90,
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
            var themeMode by remember {
                mutableStateOf(ThemeMode.fromId(sharedPref.getString("theme_mode", null)))
            }
            var currentTheme by remember {
                mutableStateOf(AppThemeStyle.fromId(sharedPref.getString("app_theme", null)))
            }

            PadhaiPlusTheme(themeMode = themeMode, selectedTheme = currentTheme) {
                PadhaiPlusApp(
                    activity = this@MainActivity,
                    sharedPref = sharedPref,
                    currentTheme = currentTheme,
                    themeMode = themeMode,
                    onSelectTheme = { newTheme ->
                        currentTheme = newTheme
                        sharedPref.edit { putString("app_theme", newTheme.id) }
                    },
                    onSelectThemeMode = { newMode ->
                        themeMode = newMode
                        sharedPref.edit { putString("theme_mode", newMode.id) }
                    }
                )
            }
        }
    }
}

// Custom App Logo Icon matching the stopwatch on open book artwork
@Composable
fun PadhaiPlusAppLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Open Book at bottom
            val leftBookPath = Path().apply {
                moveTo(w * 0.12f, h * 0.58f)
                lineTo(w * 0.5f, h * 0.65f)
                lineTo(w * 0.5f, h * 0.83f)
                lineTo(w * 0.12f, h * 0.78f)
                close()
            }
            drawPath(leftBookPath, color = Color(0xFFF1ECE1))

            val rightBookPath = Path().apply {
                moveTo(w * 0.5f, h * 0.65f)
                lineTo(w * 0.88f, h * 0.58f)
                lineTo(w * 0.88f, h * 0.78f)
                lineTo(w * 0.5f, h * 0.83f)
                close()
            }
            drawPath(rightBookPath, color = Color(0xFFE2D9C8))

            // 2. Stopwatch Gold Outer Frame & Ring
            val clockCenter = Offset(w * 0.5f, h * 0.41f)
            val clockRadius = w * 0.22f

            // Top Crown Button
            drawRoundRect(
                color = Color(0xFFF59E0B),
                topLeft = Offset(w * 0.43f, h * 0.13f),
                size = Size(w * 0.14f, h * 0.05f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRect(
                color = Color(0xFFF59E0B),
                topLeft = Offset(w * 0.47f, h * 0.17f),
                size = Size(w * 0.06f, h * 0.035f)
            )

            // Top-right Angled Pusher Button
            val pusherPath = Path().apply {
                moveTo(w * 0.65f, h * 0.17f)
                lineTo(w * 0.73f, h * 0.23f)
                lineTo(w * 0.69f, h * 0.27f)
                lineTo(w * 0.61f, h * 0.21f)
                close()
            }
            drawPath(pusherPath, color = Color(0xFFF59E0B))

            // Outer Gold Circle Ring
            drawCircle(
                color = Color(0xFFF59E0B),
                radius = clockRadius,
                center = clockCenter,
                style = Stroke(width = w * 0.045f)
            )

            // Inner Dark Clock Face
            drawCircle(
                color = Color(0xFF0F172A),
                radius = clockRadius - (w * 0.022f),
                center = clockCenter
            )

            // 3. Tick marks at 12, 3, 6, 9 o'clock
            val tickLength = w * 0.035f
            drawLine(
                color = Color(0xFF94A3B8),
                start = Offset(clockCenter.x, clockCenter.y - clockRadius + w * 0.04f),
                end = Offset(clockCenter.x, clockCenter.y - clockRadius + w * 0.04f + tickLength),
                strokeWidth = 3f
            )
            drawLine(
                color = Color(0xFF94A3B8),
                start = Offset(clockCenter.x, clockCenter.y + clockRadius - w * 0.04f - tickLength),
                end = Offset(clockCenter.x, clockCenter.y + clockRadius - w * 0.04f),
                strokeWidth = 3f
            )
            drawLine(
                color = Color(0xFF94A3B8),
                start = Offset(clockCenter.x - clockRadius + w * 0.04f, clockCenter.y),
                end = Offset(clockCenter.x - clockRadius + w * 0.04f + tickLength, clockCenter.y),
                strokeWidth = 3f
            )
            drawLine(
                color = Color(0xFF94A3B8),
                start = Offset(clockCenter.x + clockRadius - w * 0.04f - tickLength, clockCenter.y),
                end = Offset(clockCenter.x + clockRadius - w * 0.04f, clockCenter.y),
                strokeWidth = 3f
            )

            // 4. Clock Hands (Hour hand at 4 o'clock, Minute hand at 12 o'clock)
            drawLine(
                color = Color(0xFFFEF08A),
                start = clockCenter,
                end = Offset(clockCenter.x, clockCenter.y - clockRadius * 0.65f),
                strokeWidth = 5f
            )
            drawLine(
                color = Color(0xFFFEF08A),
                start = clockCenter,
                end = Offset(clockCenter.x + clockRadius * 0.45f, clockCenter.y + clockRadius * 0.28f),
                strokeWidth = 5f
            )

            // Pivot Center Dot
            drawCircle(
                color = Color(0xFFF59E0B),
                radius = w * 0.025f,
                center = clockCenter
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PadhaiPlusApp(
    activity: ComponentActivity,
    sharedPref: SharedPreferences,
    currentTheme: AppThemeStyle,
    themeMode: ThemeMode,
    onSelectTheme: (AppThemeStyle) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
) {
    val context = LocalContext.current
    val theme = LocalAppTheme.current
    val scope = rememberCoroutineScope()
    val db = remember { FirebaseFirestore.getInstance().apply { enableNetwork() } }
    val auth = remember { FirebaseAuth.getInstance() }
    val credentialManager = remember { CredentialManager.create(context) }

    // Login & Profile states
    var isLoggedIn by remember {
        mutableStateOf(sharedPref.getBoolean("is_logged_in", false) || auth.currentUser != null)
    }
    var studentName by remember {
        mutableStateOf(sharedPref.getString("student_name", "Sasim") ?: "Sasim")
    }
    var studentEmail by remember {
        mutableStateOf(sharedPref.getString("student_email", "") ?: "")
    }
    var studentNameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
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
            val fallbackName = currentUser.displayName?.takeIf { it.isNotBlank() } ?: "Sasim"
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

    // Navigation state
    var selectedSection by remember { mutableIntStateOf(0) }

    // Routine inputs & storage - STARTS EMPTY
    var subjectInput by remember { mutableStateOf("") }
    var startTimeInput by remember { mutableStateOf("08:00") }
    var endTimeInput by remember { mutableStateOf("09:30") }
    var editingRoutine by remember { mutableStateOf<RoutineItem?>(null) }

    var routines by remember {
        mutableStateOf(loadRoutines(sharedPref))
    }

    LaunchedEffect(routines) {
        saveRoutines(sharedPref, routines)
    }

    // Dialog & BottomSheet states
    var showAddSubjectSheet by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showThemeSelectionScreen by remember { mutableStateOf(false) }

    LaunchedEffect(selectedSection) {
        if (selectedSection != 3) {
            showThemeSelectionScreen = false
        }
    }

    // Active Study Session Timer state & Face Detection
    var activeRoutine by remember { mutableStateOf<RoutineItem?>(null) }
    var secondsLeft by remember { mutableIntStateOf(2700) }
    var isTimerPaused by remember { mutableStateOf(false) }
    var isFaceDetected by remember { mutableStateOf(true) }
    var isBannerDismissed by remember { mutableStateOf(false) }

    // Leaderboard state
    var leaderboard by remember { mutableStateOf(listOf<StudentRecord>()) }

    val myTodayStudiedMinutes = leaderboard.find {
        (activeDocId.isNotBlank() && it.uid == activeDocId) ||
                it.name.equals(studentName, ignoreCase = true)
    }?.todayMinutes ?: 0

    val totalPlannedMins = routines.sumOf { it.minutes }.let { if (it == 0) 1 else it }
    val completionPercent =
        ((routines.filter { it.isCompleted }.sumOf { it.minutes }.toFloat() / totalPlannedMins.toFloat()) * 100)
            .toInt().coerceIn(0, 100)

    // Real-time Leaderboard Listener
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            db.collection("students").addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val fetched = snapshot.documents.map { doc ->
                        StudentRecord(
                            name = doc.getString("name") ?: "Sasim",
                            uid = doc.getString("uid") ?: doc.id,
                            avatar = doc.getString("avatar") ?: "🎓",
                            todayMinutes = doc.getLong("todayMinutes")?.toInt() ?: 0,
                            weeklyMinutes = doc.getLong("weeklyMinutes")?.toInt() ?: 0,
                            monthlyMinutes = doc.getLong("monthlyMinutes")?.toInt() ?: 0,
                            completionPercent = doc.getLong("completionPercent")?.toInt() ?: 0,
                            isStudying = doc.getBoolean("isStudying") ?: false
                        )
                    }
                    leaderboard = if (fetched.isNotEmpty()) fetched else listOf(
                        StudentRecord(studentName.ifBlank { "Sasim" }, activeDocId, avatarPic, 0, 0, 0, 0, activeRoutine != null)
                    )
                }
            }
        }
    }

    // Countdown Timer Loop
    LaunchedEffect(activeRoutine, secondsLeft, isTimerPaused) {
        if (activeRoutine != null && secondsLeft > 0 && !isTimerPaused) {
            delay(1000)
            secondsLeft -= 1
        } else if (activeRoutine != null && secondsLeft == 0) {
            val finishedRoutine = activeRoutine
            if (finishedRoutine != null) {
                routines = routines.map {
                    if (it.id == finishedRoutine.id) it.copy(isCompleted = true) else it
                }
                val newCompletion = ((routines.filter { it.isCompleted }.sumOf { it.minutes }
                    .toFloat() / routines.sumOf { it.minutes }.let { if (it == 0) 1 else it }) * 100).toInt()

                saveStudyProgress(
                    db = db,
                    docId = activeDocId,
                    studentName = studentName,
                    avatar = avatarPic,
                    minutesToAdd = finishedRoutine.minutes,
                    completionPercent = newCompletion,
                    isStudying = false,
                    subject = ""
                )
            }
            activeRoutine = null
            isTimerPaused = false
        }
    }

    PastelBlobBackground {
        if (!isLoggedIn) {
            LoginScreen(
                studentNameInput = studentNameInput,
                onStudentNameChange = { studentNameInput = it },
                passwordInput = passwordInput,
                onPasswordChange = { passwordInput = it },
                isLoggingIn = isLoggingIn,
                loginError = loginError,
                onGoogleSignIn = {
                    val webClientId = activity.getString(R.string.default_web_client_id)
                    if (webClientId.contains("000000000000")) {
                        loginError = "Configure Google Web Client ID in strings.xml"
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
                            val result = credentialManager.getCredential(context = activity, request = request)
                            val credential = result.credential
                            if (credential is CustomCredential &&
                                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                            ) {
                                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                                val firebaseCred = GoogleAuthProvider.getCredential(googleCred.idToken, null)
                                auth.signInWithCredential(firebaseCred)
                                    .addOnSuccessListener { authResult ->
                                        val user = authResult.user
                                        val uid = user?.uid.orEmpty()
                                        val displayName = user?.displayName?.takeIf { it.isNotBlank() } ?: "Sasim"
                                        val email = user?.email.orEmpty()
                                        val docId = uid.ifBlank { email.replace("@", "_").replace(".", "_") }
                                        studentName = displayName
                                        studentEmail = email
                                        activeDocId = docId
                                        isLoggedIn = true
                                        isLoggingIn = false
                                        sharedPref.edit {
                                            putBoolean("is_logged_in", true)
                                            putString("student_name", displayName)
                                            putString("student_email", email)
                                            putString("active_doc_id", docId)
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        isLoggingIn = false
                                        loginError = "Sign-in failed: ${e.localizedMessage}"
                                    }
                            }
                        } catch (_: Exception) {
                            isLoggingIn = false
                            loginError = "Sign-in cancelled or unavailable."
                        }
                    }
                },
                onStudentIdSignIn = {
                    val cleanName = studentNameInput.trim().ifBlank { "Sasim" }
                    isLoggingIn = true
                    studentName = cleanName
                    activeDocId = cleanName.lowercase().replace(Regex("[^a-z0-9_]"), "_")
                    isLoggedIn = true
                    isLoggingIn = false
                    sharedPref.edit {
                        putBoolean("is_logged_in", true)
                        putString("student_name", cleanName)
                        putString("active_doc_id", activeDocId)
                    }
                }
            )
            return@PastelBlobBackground
        }

        // Full Screen "Study Time!!" when a class session is active
        if (activeRoutine != null) {
            StudyTimeScreen(
                secondsLeft = secondsLeft,
                isFaceDetected = isFaceDetected,
                isBannerDismissed = isBannerDismissed,
                onFacePresenceChanged = { isFaceDetected = it },
                onDismissBanner = { isBannerDismissed = true },
                onBack = {
                    activeRoutine = null
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
                        routines = routines,
                        completionPercent = completionPercent,
                        onOpenProfile = { showProfileDialog = true },
                        onOpenAddSubject = {
                            editingRoutine = null
                            subjectInput = ""
                            startTimeInput = "08:00"
                            endTimeInput = "09:30"
                            showAddSubjectSheet = true
                        },
                        onEditRoutine = { item ->
                            editingRoutine = item
                            subjectInput = item.subject
                            startTimeInput = item.startTime
                            endTimeInput = item.endTime
                            showAddSubjectSheet = true
                        },
                        onDeleteRoutine = { item ->
                            routines = routines.filter { it.id != item.id }
                        },
                        onStartRoutine = { item ->
                            activeRoutine = item
                            secondsLeft = 2700
                            isTimerPaused = false
                            isBannerDismissed = false
                        }
                    )

                    1 -> CalendarSection(
                        routines = routines,
                        onStartRoutine = { item ->
                            activeRoutine = item
                            secondsLeft = 2700
                            isTimerPaused = false
                            isBannerDismissed = false
                        }
                    )

                    2 -> LeaderboardSection(
                        studentName = studentName,
                    )

                    3 -> {
                        if (showThemeSelectionScreen) {
                            ThemeSelectionScreen(
                                currentTheme = currentTheme,
                                themeMode = themeMode,
                                onSelectTheme = onSelectTheme,
                                onSelectThemeMode = onSelectThemeMode,
                                onBack = { showThemeSelectionScreen = false }
                            )
                        } else {
                            SettingsSection(
                                studentName = studentName,
                                themeMode = themeMode,
                                onOpenProfile = { showProfileDialog = true },
                                onOpenThemeSelection = { showThemeSelectionScreen = true },
                                onLogOut = {
                                    auth.signOut()
                                    isLoggedIn = false
                                    studentName = "Sasim"
                                    activeDocId = ""
                                    sharedPref.edit { putBoolean("is_logged_in", false) }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Add / Edit Subject Modal Bottom Sheet
        if (showAddSubjectSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showAddSubjectSheet = false },
                sheetState = sheetState,
                containerColor = theme.cardSurface,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                AddSubjectSheetContent(
                    isEditing = editingRoutine != null,
                    subjectInput = subjectInput,
                    onSubjectChange = { subjectInput = it },
                    startTimeInput = startTimeInput,
                    onStartTimeChange = { startTimeInput = it },
                    endTimeInput = endTimeInput,
                    onEndTimeChange = { endTimeInput = it },
                    onSave = {
                        if (subjectInput.isNotBlank()) {
                            val currentEditing = editingRoutine
                            if (currentEditing != null) {
                                // Edit existing
                                routines = routines.map {
                                    if (it.id == currentEditing.id) {
                                        it.copy(
                                            subject = subjectInput.trim(),
                                            startTime = startTimeInput.ifBlank { "08:00" },
                                            endTime = endTimeInput.ifBlank { "09:30" }
                                        )
                                    } else it
                                }
                            } else {
                                // Add new
                                val newRoutine = RoutineItem(
                                    subject = subjectInput.trim(),
                                    startTime = startTimeInput.ifBlank { "08:00" },
                                    endTime = endTimeInput.ifBlank { "09:30" },
                                    minutes = 90,
                                    hour = 8,
                                    minute = 0,
                                    isEnabled = true,
                                    colorIndex = (0..5).random()
                                )
                                routines = routines + newRoutine
                            }
                            subjectInput = ""
                            editingRoutine = null
                            showAddSubjectSheet = false
                        } else {
                            Toast.makeText(context, "Please enter a subject name", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDelete = {
                        val currentEditing = editingRoutine
                        if (currentEditing != null) {
                            routines = routines.filter { it.id != currentEditing.id }
                        }
                        editingRoutine = null
                        showAddSubjectSheet = false
                    }
                )
            }
        }

        // Profile Dialog
        if (showProfileDialog) {
            ProfileDialog(
                studentName = studentName,
                studentEmail = studentEmail,
                currentAvatar = avatarPic,
                todayMinutes = myTodayStudiedMinutes,
                completionPercent = completionPercent,
                onSignOut = {
                    showProfileDialog = false
                    auth.signOut()
                    isLoggedIn = false
                    sharedPref.edit { putBoolean("is_logged_in", false) }
                },
                onDismiss = { showProfileDialog = false },
                onSaveAvatar = { newAvatar ->
                    avatarPic = newAvatar
                    sharedPref.edit { putString("student_avatar", newAvatar) }
                }
            )
        }
    }
}

// ------------------- SCREEN 1: LOGIN SCREEN -------------------
@Composable
fun LoginScreen(
    studentNameInput: String,
    onStudentNameChange: (String) -> Unit,
    passwordInput: String,
    onPasswordChange: (String) -> Unit,
    isLoggingIn: Boolean,
    loginError: String?,
    onGoogleSignIn: () -> Unit,
    onStudentIdSignIn: () -> Unit,
) {
    val theme = LocalAppTheme.current
    var isPasswordVisible by remember { mutableStateOf(false) }

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
        // App Icon Logo (Stopwatch on open book)
        PadhaiPlusAppLogo(modifier = Modifier.size(76.dp))

        Spacer(Modifier.height(16.dp))
        Text(
            "PadhaiPLUS+",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = theme.textPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Plan routines, track presence and earn badges",
            fontSize = 14.sp,
            color = theme.textSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        // White Sign-in Card
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = theme.cardSurface,
            shadowElevation = 8.dp,
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
                    "Sign in to continue to your dashboard",
                    fontSize = 13.sp,
                    color = theme.textSecondary
                )

                Spacer(Modifier.height(20.dp))

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
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.primary,
                        unfocusedBorderColor = theme.borderAccent,
                        focusedContainerColor = theme.cardSurface,
                        unfocusedContainerColor = theme.cardSurface,
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))
                Text(
                    "Password",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.textPrimary
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = onPasswordChange,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Text(
                            text = if (isPasswordVisible) "👁️" else "👁️‍🗨️",
                            fontSize = 16.sp,
                            modifier = Modifier
                                .clickable { isPasswordVisible = !isPasswordVisible }
                                .padding(8.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.primary,
                        unfocusedBorderColor = theme.borderAccent,
                        focusedContainerColor = theme.cardSurface,
                        unfocusedContainerColor = theme.cardSurface,
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    "Forgot password?",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.primary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onStudentIdSignIn,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .shadow(4.dp, RoundedCornerShape(50))
                ) {
                    Text(
                        if (isLoggingIn) "Signing in..." else "Sign in",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onGoogleSignIn,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🌐 ", fontSize = 14.sp)
                        Text(
                            "Continue with Google",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = theme.primary
                        )
                    }
                }

                if (!loginError.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = loginError,
                        fontSize = 12.sp,
                        color = Color(0xFFEF4444),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Row {
            Text("New student? ", fontSize = 13.sp, color = theme.textSecondary)
            Text(
                "Create account",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = theme.primary,
                modifier = Modifier.clickable { onStudentIdSignIn() }
            )
        }

        Spacer(Modifier.height(28.dp))
        Text(
            "Your progress is saved securely",
            fontSize = 12.sp,
            color = theme.textSecondary.copy(alpha = 0.75f)
        )
    }
}

// ------------------- SCREEN 2: ROUTINES / DASHBOARD -------------------
@Composable
fun RoutineDashboardSection(
    studentName: String,
    routines: List<RoutineItem>,
    completionPercent: Int,
    onOpenProfile: () -> Unit,
    onOpenAddSubject: () -> Unit,
    onEditRoutine: (RoutineItem) -> Unit,
    onDeleteRoutine: (RoutineItem) -> Unit,
    onStartRoutine: (RoutineItem) -> Unit,
) {
    val theme = LocalAppTheme.current
    val todayDateFormatted = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.US).format(Date())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onOpenProfile() }
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE2F1E8)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎓", fontSize = 24.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        studentName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.textPrimary
                    )
                    Text(
                        "Tap for profile & stats ⚙️",
                        fontSize = 12.sp,
                        color = theme.textSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onOpenProfile() }
                    .padding(8.dp)
            ) {
                Text(
                    "More ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textSecondary
                )
                Text(
                    "›",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = theme.textSecondary
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Greeting Card
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFFD7F0E5),
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Good morning,\n$studentName ☀️",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF194236),
                            lineHeight = 32.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "$todayDateFormatted · ${routines.size} classes today",
                            fontSize = 13.sp,
                            color = Color(0xFF2C5E4F),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier.size(90.dp, 70.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color(0xFFFBBF24),
                                radius = 12f,
                                center = Offset(size.width * 0.8f, size.height * 0.25f)
                            )
                            for (i in 0 until 8) {
                                val angle = i * 45 * Math.PI / 180
                                val startX = (size.width * 0.8f + 16 * Math.cos(angle)).toFloat()
                                val startY = (size.height * 0.25f + 16 * Math.sin(angle)).toFloat()
                                val endX = (size.width * 0.8f + 22 * Math.cos(angle)).toFloat()
                                val endY = (size.height * 0.25f + 22 * Math.sin(angle)).toFloat()
                                drawLine(
                                    color = Color(0xFFFBBF24),
                                    start = Offset(startX, startY),
                                    end = Offset(endX, endY),
                                    strokeWidth = 3f
                                )
                            }
                            val leftPage = Path().apply {
                                moveTo(size.width * 0.48f, size.height * 0.45f)
                                cubicTo(
                                    size.width * 0.35f, size.height * 0.35f,
                                    size.width * 0.15f, size.height * 0.4f,
                                    size.width * 0.05f, size.height * 0.45f
                                )
                                lineTo(size.width * 0.05f, size.height * 0.85f)
                                cubicTo(
                                    size.width * 0.15f, size.height * 0.8f,
                                    size.width * 0.35f, size.height * 0.75f,
                                    size.width * 0.48f, size.height * 0.88f
                                )
                                close()
                            }
                            drawPath(leftPage, color = Color.White)
                            drawPath(leftPage, color = Color(0xFFF97316), style = Stroke(width = 3f))

                            val rightPage = Path().apply {
                                moveTo(size.width * 0.52f, size.height * 0.45f)
                                cubicTo(
                                    size.width * 0.65f, size.height * 0.35f,
                                    size.width * 0.85f, size.height * 0.4f,
                                    size.width * 0.95f, size.height * 0.45f
                                )
                                lineTo(size.width * 0.95f, size.height * 0.85f)
                                cubicTo(
                                    size.width * 0.85f, size.height * 0.8f,
                                    size.width * 0.65f, size.height * 0.75f,
                                    size.width * 0.52f, size.height * 0.88f
                                )
                                close()
                            }
                            drawPath(rightPage, color = Color.White)
                            drawPath(rightPage, color = Color(0xFFF97316), style = Stroke(width = 3f))
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Today's progress", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF194236))
                    Text("$completionPercent% done", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF194236))
                }

                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                ) {
                    val progressFraction = (completionPercent / 100f).coerceIn(0f, 1f)
                    if (progressFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .height(10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF2D6A4F))
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // "Add subject" Pill Card
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xFFFFF2E6),
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFFDE8D3), RoundedCornerShape(50))
                .clip(RoundedCornerShape(50))
                .clickable { onOpenAddSubject() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8DFF5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎓", fontSize = 22.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Add subject", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D253B))
                        Text("Tap Here to add subject", fontSize = 12.sp, color = Color(0xFF7C6E8F))
                    }
                }

                Text("›", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A3E59), modifier = Modifier.padding(end = 8.dp))
            }
        }

        Spacer(Modifier.height(20.dp))

        // "Today's Schedule:" Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Today's Schedule:", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = theme.textPrimary)
            Text("${routines.size} classes", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = theme.textSecondary)
        }

        Spacer(Modifier.height(12.dp))

        // Empty state if no classes added yet
        if (routines.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = theme.cardSurface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📅", fontSize = 36.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No classes added yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.textPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tap \"Add subject\" above to create your schedule.",
                        fontSize = 13.sp,
                        color = theme.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Routine Items List with Edit & Delete Actions
            routines.forEachIndexed { index, item ->
                val iconBg = when (index % 3) {
                    0 -> Color(0xFFDFF2EC)
                    1 -> Color(0xFFE5F5D9)
                    else -> Color(0xFFFBE4D8)
                }
                val itemIcon = when (item.subject.lowercase()) {
                    "mathematics", "math" -> "📐"
                    "physics" -> "🧪"
                    "english literature", "english" -> "📖"
                    else -> SubjectSwatchIcons[item.colorIndex.coerceIn(0, SubjectSwatchIcons.lastIndex)]
                }

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = theme.cardSurface,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Click icon box or text to start routine
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(iconBg)
                                .clickable { onStartRoutine(item) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(itemIcon, fontSize = 26.sp)
                        }

                        Spacer(Modifier.width(16.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onStartRoutine(item) }
                        ) {
                            Text(
                                "${item.startTime} - ${item.endTime}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = theme.textSecondary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                item.subject,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.textPrimary
                            )
                        }

                        // Edit Button
                        Text(
                            "✏️",
                            fontSize = 16.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onEditRoutine(item) }
                                .padding(8.dp)
                        )

                        // Delete Button
                        Text(
                            "🗑️",
                            fontSize = 16.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onDeleteRoutine(item) }
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

// ------------------- SCREEN 3: CALENDAR SCREEN -------------------
@Composable
fun CalendarSection(
    routines: List<RoutineItem>,
    onStartRoutine: (RoutineItem) -> Unit,
) {
    val theme = LocalAppTheme.current
    val calendar = remember { Calendar.getInstance() }
    val currentMonthName = remember { SimpleDateFormat("MMMM yyyy", Locale.US).format(calendar.time) }
    var selectedDay by remember { mutableIntStateOf(27) }

    val daysInMonth = 30
    val startOffset = 6
    val achievementRoutine = routines.find { it.isCompleted } ?: routines.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp)
    ) {
        Text(currentMonthName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)

        Spacer(Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = theme.textSecondary
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        val totalCells = startOffset + daysInMonth
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
                        if (dayNum in 1..daysInMonth) {
                            val isSelected = dayNum == selectedDay
                            val hasDot = dayNum in listOf(3, 10, 18, 25)

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) theme.primary else theme.cardSurface,
                                shadowElevation = if (isSelected) 6.dp else 1.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { selectedDay = dayNum }
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$dayNum",
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else theme.textPrimary
                                    )
                                    if (hasDot && !isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 4.dp)
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFEC4899))
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

        Spacer(Modifier.height(20.dp))

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = theme.cardSurface,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Daily goal", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                    Text("3 of 5 study hours", fontSize = 13.sp, color = theme.textSecondary, fontWeight = FontWeight.Medium)
                }

                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFE2E8F0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(theme.primary)
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text("60% complete · keep going! 💪", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
            }
        }

        Spacer(Modifier.height(20.dp))

        Text("Today's Achievement:", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)

        Spacer(Modifier.height(12.dp))

        if (achievementRoutine != null) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = theme.cardSurface,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onStartRoutine(achievementRoutine) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFFE5F5D9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🧪", fontSize = 26.sp)
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${achievementRoutine.startTime} - ${achievementRoutine.endTime}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = theme.textSecondary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(achievementRoutine.subject, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFC7B8EA))
                            .border(3.dp, Color(0xFF9E86DC), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✔", fontSize = 18.sp, color = Color(0xFF6C5CE7), fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

// ------------------- SCREEN 4: LEADERBOARD SCREEN -------------------
@Composable
fun LeaderboardSection(
    studentName: String,
) {
    val theme = LocalAppTheme.current
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("🏆 Friends Study Leaderboards", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = theme.textPrimary)
        Text("Compete for high hours & plan completion!", fontSize = 13.sp, color = theme.textSecondary)

        Spacer(Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(50),
            color = theme.cardSurface,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                listOf("⏱️ High Hours", "🎯 Plan Done %").forEachIndexed { idx, title ->
                    val selected = selectedTab == idx
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .clickable { selectedTab = idx }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            title,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) theme.textPrimary else theme.textSecondary
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) theme.primary else Color.Transparent)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = theme.cardSurface,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("#1", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = theme.textPrimary)
                    Spacer(Modifier.width(14.dp))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8DFF5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎓", fontSize = 24.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(studentName.ifBlank { "Sasim" }, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = theme.textPrimary)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            BadgeChip("🔥 #1 Champ", Color(0xFFFDE68A), Color(0xFF78350F))
                            BadgeChip("🌱 Rookie", Color(0xFFE2E8F0), Color(0xFF334155))
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("0hr 0m", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = theme.textPrimary)
                    Text("0% done", fontSize = 12.sp, color = theme.textSecondary)
                }
            }
        }
    }
}

// ------------------- SCREEN 5: STUDY TIME!! -------------------
@Composable
fun StudyTimeScreen(
    secondsLeft: Int,
    isFaceDetected: Boolean,
    isBannerDismissed: Boolean,
    onFacePresenceChanged: (Boolean) -> Unit,
    onDismissBanner: () -> Unit,
    onBack: () -> Unit,
) {
    val theme = LocalAppTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Text("←", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Study Time!!", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                Text("Just you and your goals.", fontSize = 14.sp, color = theme.textSecondary)
            }
        }

        Spacer(Modifier.height(24.dp))

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFFC4C4CD),
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .clip(RoundedCornerShape(28.dp))
        ) {
            FaceDetectionCameraPreview(
                onFacePresenceChanged = onFacePresenceChanged,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.height(20.dp))

        if (!isBannerDismissed) {
            val bannerBg = if (isFaceDetected) Color(0xFFD7F0E5) else Color(0xFFFEE2E2)
            val bannerBorder = if (isFaceDetected) Color(0xFFA8E6CF) else Color(0xFFFCA5A5)
            val bannerTextColor = if (isFaceDetected) Color(0xFF194236) else Color(0xFF991B1B)

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = bannerBg,
                border = BorderStroke(1.dp, bannerBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isFaceDetected)
                            "Your Face is being detected. Please dont leave the cammera, Stay right There and Focus on your study"
                        else
                            "Could not detect your face. Please show your face on camera so we know you are focusing on your studies.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = bannerTextColor,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "✕",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = bannerTextColor,
                        modifier = Modifier
                            .clickable { onDismissBanner() }
                            .padding(4.dp)
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            "TIME LEFT:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textSecondary,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(4.dp))

        val mins = secondsLeft / 60
        val secs = secondsLeft % 60
        Text(
            String.format(Locale.US, "%02d:%02d", mins, secs),
            fontSize = 64.sp,
            fontWeight = FontWeight.Normal,
            color = theme.textPrimary
        )

        Spacer(Modifier.weight(1f))
    }
}

// ------------------- SCREEN 6: SETTINGS SCREEN -------------------
@Composable
fun SettingsSection(
    studentName: String,
    themeMode: ThemeMode,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onOpenProfile) {
                Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Settings", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = theme.textPrimary)
                Text("Make PadhaiPLUS+ work for you", fontSize = 13.sp, color = theme.textSecondary)
            }
        }

        Spacer(Modifier.height(20.dp))

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = theme.cardSurface,
            shadowElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
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
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8DFF5)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎓", fontSize = 26.sp)
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(studentName.ifBlank { "Sasim" }, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = theme.textPrimary)
                    Text("Manage profile & avatar", fontSize = 13.sp, color = theme.textSecondary)
                }
                Text("›", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = theme.textSecondary)
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Study preferences", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = theme.textPrimary)
        Spacer(Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = theme.cardSurface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Class reminders", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = theme.textPrimary)
                        Text("Get notified before each class", fontSize = 12.sp, color = theme.textSecondary)
                    }
                    Switch(
                        checked = classReminders,
                        onCheckedChange = { classReminders = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF6C5CE7))
                    )
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = theme.borderAccent.copy(alpha = 0.3f))
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reminder time", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = theme.textPrimary)
                        Text("Get Reminder before time ends", fontSize = 12.sp, color = theme.textSecondary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("10 min before ", fontSize = 13.sp, color = theme.textSecondary)
                        Text("›", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = theme.textSecondary)
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = theme.borderAccent.copy(alpha = 0.3f))
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Daily study summary", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = theme.textPrimary)
                        Text("A little reflection on your progress", fontSize = 12.sp, color = theme.textSecondary)
                    }
                    Switch(
                        checked = dailySummary,
                        onCheckedChange = { dailySummary = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF6C5CE7))
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Theme selection", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = theme.textPrimary)
        Spacer(Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = theme.cardSurface,
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .clickable { onOpenThemeSelection() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Theme", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = theme.textPrimary)
                    Text("Choose your study space's look", fontSize = 12.sp, color = theme.textSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(themeMode.displayName + " ", fontSize = 13.sp, color = theme.textSecondary)
                    Text("›", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = theme.textSecondary)
                }
            }
        }

        Spacer(Modifier.height(30.dp))

        OutlinedButton(
            onClick = onLogOut,
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Log out", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = theme.primary)
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

// ------------------- SCREEN 7: THEME SELECTION SCREEN -------------------
@Composable
fun ThemeSelectionScreen(
    currentTheme: AppThemeStyle,
    themeMode: ThemeMode,
    onSelectTheme: (AppThemeStyle) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onBack: () -> Unit,
) {
    val theme = LocalAppTheme.current
    var selectedAppTheme by remember { mutableStateOf(currentTheme) }
    var selectedThemeMode by remember { mutableStateOf(themeMode) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Theme", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = theme.textPrimary)
                Text("A little more you. A little more focus.", fontSize = 13.sp, color = theme.textSecondary)
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Appearance", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = theme.textPrimary)
        Text("Pick the light that feels right.", fontSize = 13.sp, color = theme.textSecondary)
        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(
                Triple(ThemeMode.LIGHT, "Light", Color(0xFFF1F5F9)),
                Triple(ThemeMode.DARK, "Dark", Color(0xFF1E293B)),
                Triple(ThemeMode.SYSTEM, "System", Color(0xFF475569))
            ).forEach { (mode, label, mockBg) ->
                val isSelected = (selectedThemeMode == mode)
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = theme.cardSurface,
                    border = if (isSelected) BorderStroke(2.dp, theme.primary) else null,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { selectedThemeMode = mode }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(mockBg)
                                .padding(8.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.width(36.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(theme.primary))
                                Box(modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(4.dp)).background(theme.cardSurface))
                                Box(modifier = Modifier.width(24.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(theme.textSecondary))
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = theme.textPrimary)
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
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
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "System follows your device's light or dark setting.",
            fontSize = 12.sp,
            color = theme.textSecondary
        )

        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Accent color", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = theme.textPrimary)
            }
            Text(selectedAppTheme.displayName, fontSize = 13.sp, color = theme.textSecondary, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = theme.cardSurface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppThemeStyle.entries.forEach { style ->
                    val isSelected = selectedAppTheme == style
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { selectedAppTheme = style }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(style.primaryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(style.displayName, fontSize = 11.sp, color = theme.textSecondary)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Preview", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = theme.textPrimary)
        Spacer(Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = theme.cardSurface,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Your study space", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = theme.textPrimary)
                Text("A little progress, every day.", fontSize = 12.sp, color = theme.textSecondary)
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = theme.trackBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("UP NEXT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = selectedAppTheme.primaryColor)
                        Spacer(Modifier.height(2.dp))
                        Text("Mathematics", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = theme.textPrimary)
                        Text("2:00 PM – 3:00 PM", fontSize = 12.sp, color = theme.textSecondary)
                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(theme.cardSurface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.55f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(selectedAppTheme.primaryColor)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = {
                onSelectTheme(selectedAppTheme)
                onSelectThemeMode(selectedThemeMode)
                onBack()
            },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = selectedAppTheme.primaryColor),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Done", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Make your study space feel like home.",
            fontSize = 12.sp,
            color = theme.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ------------------- SCREEN 8: ADD / EDIT SUBJECT SHEET -------------------
@Composable
fun AddSubjectSheetContent(
    isEditing: Boolean = false,
    subjectInput: String,
    onSubjectChange: (String) -> Unit,
    startTimeInput: String,
    onStartTimeChange: (String) -> Unit,
    endTimeInput: String,
    onEndTimeChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit = {},
) {
    val theme = LocalAppTheme.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 28.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFCBD5E1))
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            if (isEditing) "Edit Subject:" else "Add Subject:",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textPrimary
        )

        Spacer(Modifier.height(16.dp))

        Text("Subject name", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = subjectInput,
            onValueChange = onSubjectChange,
            placeholder = { Text("e.g. Mathematics", color = theme.textSecondary.copy(alpha = 0.5f)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = theme.primary,
                unfocusedBorderColor = theme.borderAccent,
                focusedContainerColor = theme.cardSurface,
                unfocusedContainerColor = theme.cardSurface,
                focusedTextColor = theme.textPrimary,
                unfocusedTextColor = theme.textPrimary,
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Start time", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = startTimeInput,
                    onValueChange = onStartTimeChange,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.primary,
                        unfocusedBorderColor = theme.borderAccent,
                        focusedContainerColor = theme.cardSurface,
                        unfocusedContainerColor = theme.cardSurface,
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("End time", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = endTimeInput,
                    onValueChange = onEndTimeChange,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.primary,
                        unfocusedBorderColor = theme.borderAccent,
                        focusedContainerColor = theme.cardSurface,
                        unfocusedContainerColor = theme.cardSurface,
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
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
                if (isEditing) "Update class" else "Save class",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        if (isEditing) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onDelete,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Delete class", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ------------------- FLOATING PILL BOTTOM NAVIGATION BAR -------------------
@Composable
fun FloatingPillBottomBar(
    selectedSection: Int,
    onSelectSection: (Int) -> Unit,
) {
    val theme = LocalAppTheme.current
    val items = listOf(
        Pair("Routines", "ic_routines"),
        Pair("Calendar", "ic_calendar"),
        Pair("Leaderboard", "ic_leaderboard"),
        Pair("Settings", "ic_settings")
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = theme.cardSurface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, (label, iconType) ->
                    val selected = selectedSection == index

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) theme.navSelectedBg else Color.Transparent)
                            .clickable { onSelectSection(index) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Canvas(modifier = Modifier.size(22.dp)) {
                                val iconColor = if (selected) theme.navSelectedText else theme.textSecondary
                                val strokeWidth = 3f

                                when (iconType) {
                                    "ic_routines" -> {
                                        val path = Path().apply {
                                            moveTo(size.width * 0.15f, size.height * 0.5f)
                                            lineTo(size.width * 0.5f, size.height * 0.15f)
                                            lineTo(size.width * 0.85f, size.height * 0.5f)
                                            lineTo(size.width * 0.85f, size.height * 0.85f)
                                            lineTo(size.width * 0.15f, size.height * 0.85f)
                                            close()
                                        }
                                        drawPath(path, color = iconColor, style = Stroke(width = strokeWidth))
                                        val door = Path().apply {
                                            moveTo(size.width * 0.4f, size.height * 0.85f)
                                            lineTo(size.width * 0.4f, size.height * 0.6f)
                                            lineTo(size.width * 0.6f, size.height * 0.6f)
                                            lineTo(size.width * 0.6f, size.height * 0.85f)
                                        }
                                        drawPath(door, color = iconColor, style = Stroke(width = strokeWidth))
                                    }

                                    "ic_calendar" -> {
                                        val rectPath = Path().apply {
                                            moveTo(size.width * 0.15f, size.height * 0.25f)
                                            lineTo(size.width * 0.85f, size.height * 0.25f)
                                            lineTo(size.width * 0.85f, size.height * 0.85f)
                                            lineTo(size.width * 0.15f, size.height * 0.85f)
                                            close()
                                        }
                                        drawPath(rectPath, color = iconColor, style = Stroke(width = strokeWidth))
                                        drawLine(iconColor, Offset(size.width * 0.15f, size.height * 0.42f), Offset(size.width * 0.85f, size.height * 0.42f), strokeWidth = strokeWidth)
                                        drawLine(iconColor, Offset(size.width * 0.35f, size.height * 0.1f), Offset(size.width * 0.35f, size.height * 0.25f), strokeWidth = strokeWidth)
                                        drawLine(iconColor, Offset(size.width * 0.65f, size.height * 0.1f), Offset(size.width * 0.65f, size.height * 0.25f), strokeWidth = strokeWidth)
                                    }

                                    "ic_leaderboard" -> {
                                        val trophyPath = Path().apply {
                                            moveTo(size.width * 0.25f, size.height * 0.2f)
                                            lineTo(size.width * 0.75f, size.height * 0.2f)
                                            lineTo(size.width * 0.65f, size.height * 0.6f)
                                            lineTo(size.width * 0.35f, size.height * 0.6f)
                                            close()
                                        }
                                        drawPath(trophyPath, color = iconColor, style = Stroke(width = strokeWidth))
                                        drawLine(iconColor, Offset(size.width * 0.5f, size.height * 0.6f), Offset(size.width * 0.5f, size.height * 0.8f), strokeWidth = strokeWidth)
                                        drawLine(iconColor, Offset(size.width * 0.3f, size.height * 0.8f), Offset(size.width * 0.7f, size.height * 0.8f), strokeWidth = strokeWidth)
                                    }

                                    else -> {
                                        drawCircle(iconColor, radius = size.width * 0.22f, center = Offset(size.width * 0.5f, size.height * 0.5f), style = Stroke(width = strokeWidth))
                                        for (i in 0 until 6) {
                                            val angle = i * 60 * Math.PI / 180
                                            val startX = (size.width * 0.5f + size.width * 0.22f * Math.cos(angle)).toFloat()
                                            val startY = (size.height * 0.5f + size.width * 0.22f * Math.sin(angle)).toFloat()
                                            val endX = (size.width * 0.5f + size.width * 0.38f * Math.cos(angle)).toFloat()
                                            val endY = (size.height * 0.5f + size.width * 0.38f * Math.sin(angle)).toFloat()
                                            drawLine(iconColor, Offset(startX, startY), Offset(endX, endY), strokeWidth = strokeWidth)
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) theme.navSelectedText else theme.textSecondary
                            )
                        }
                    }
                }
            }
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
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
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
    val cal = Calendar.getInstance()
    val todayKey = String.format(Locale.US, "%04d-%02d-%02d", cal[Calendar.YEAR], cal[Calendar.MONTH] + 1, cal[Calendar.DAY_OF_MONTH])
    val weekKey = String.format(Locale.US, "%04d-W%02d", cal[Calendar.YEAR], cal[Calendar.WEEK_OF_YEAR])
    val monthKey = String.format(Locale.US, "%04d-%02d", cal[Calendar.YEAR], cal[Calendar.MONTH] + 1)

    val docRef = db.collection("students").document(cleanId)

    val baseUpdates = hashMapOf<String, Any>(
        "name" to studentName.trim(),
        "uid" to cleanId,
        "avatar" to avatar,
        "isStudying" to isStudying,
        "currentSubject" to subject,
        "completionPercent" to completionPercent,
        "todayMinutes" to FieldValue.increment(minutesToAdd.toLong()),
        "weeklyMinutes" to FieldValue.increment(minutesToAdd.toLong()),
        "monthlyMinutes" to FieldValue.increment(minutesToAdd.toLong()),
        "dayKey" to todayKey,
        "weekKey" to weekKey,
        "monthKey" to monthKey
    )

    docRef.set(baseUpdates, SetOptions.merge())
}

fun saveRoutines(sharedPref: SharedPreferences, routines: List<RoutineItem>) {
    val serialized = routines.joinToString(";;;") {
        "${it.id}:::${it.subject}:::${it.startTime}:::${it.endTime}:::${it.minutes}:::${it.hour}:::${it.minute}:::${it.isEnabled}:::${it.isCompleted}:::${it.colorIndex}"
    }
    sharedPref.edit { putString("saved_routines", serialized) }
}

fun loadRoutines(sharedPref: SharedPreferences): List<RoutineItem> {
    val serialized = sharedPref.getString("saved_routines", null) ?: return emptyList()
    if (serialized.isBlank()) return emptyList()
    return serialized.split(";;;").mapNotNull { entry ->
        val parts = entry.split(":::")
        if (parts.size >= 10) {
            RoutineItem(
                id = parts[0].toIntOrNull() ?: (0..100000).random(),
                subject = parts[1],
                startTime = parts[2],
                endTime = parts[3],
                minutes = parts[4].toIntOrNull() ?: 90,
                hour = parts[5].toIntOrNull() ?: 8,
                minute = parts[6].toIntOrNull() ?: 0,
                isEnabled = parts[7].toBoolean(),
                isCompleted = parts[8].toBoolean(),
                colorIndex = parts[9].toIntOrNull() ?: 0
            )
        } else null
    }
}

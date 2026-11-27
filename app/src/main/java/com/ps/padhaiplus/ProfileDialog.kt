package com.ps.padhaiplus

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val AVATAR_OPTIONS = listOf("🎓", "🧙‍♂️", "🚀", "🦉", "⚡", "🌟", "💻", "🏆", "🦁", "🎨")

@Composable
fun ProfileDialog(
    studentName: String,
    currentAvatar: String,
    todayMinutes: Int,
    completionPercent: Int,
    onDismiss: () -> Unit,
    onSaveAvatar: (String) -> Unit
) {
    var selectedAvatar by remember { mutableStateOf(currentAvatar.ifBlank { "🎓" }) }

    // Gallery Photo Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedAvatar = "🖼️ Gallery Pic"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("👤 Student Profile") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Avatar Preview
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(selectedAvatar, fontSize = 36.sp)
                }

                Spacer(Modifier.height(8.dp))
                Text(studentName, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("PadhaiPlus Account", fontSize = 12.sp, color = Color.Gray)

                Spacer(Modifier.height(12.dp))

                // Stats Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Card(modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⏱️ Total Studied", fontSize = 11.sp, color = Color.Gray)
                            Text("$todayMinutes mins", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    Card(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎯 Plan Done", fontSize = 11.sp, color = Color.Gray)
                            Text("$completionPercent%", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Gallery Image Picker Button
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🖼️ Pick Profile Pic from Gallery")
                }

                Spacer(Modifier.height(12.dp))
                Text("Or Select Preset Avatar:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.height(110.dp)
                ) {
                    items(AVATAR_OPTIONS) { avatar ->
                        val isSelected = (avatar == selectedAvatar)
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedAvatar = avatar },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(avatar, fontSize = 22.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSaveAvatar(selectedAvatar)
                onDismiss()
            }) {
                Text("Save Profile Picture")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

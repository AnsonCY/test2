package hkcc.timetable

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ProfileScreen(
    viewModel: TimetableViewModel,
    groupmateViewModel: GroupmateViewModel = viewModel()
) {
    val credits by viewModel.userCredits.collectAsState()
    val context = LocalContext.current
    var importText by remember { mutableStateOf("") }

    // Profile info
    var userName by remember { mutableStateOf(groupmateViewModel.getUserName()) }
    var studentEmail by remember { mutableStateOf(groupmateViewModel.getStudentEmail()) }
    var phoneNumber by remember { mutableStateOf(groupmateViewModel.getPhoneNumber()) }
    var isEditing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Personal Information",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { isEditing = !isEditing }) {
                        Icon(
                            if (isEditing) Icons.Default.Close else Icons.Default.Edit,
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isEditing) "Cancel" else "Edit")
                    }
                }

                if (isEditing) {
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = { Text("Your Name") },
                        placeholder = { Text("Enter your full name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = studentEmail,
                        onValueChange = { studentEmail = it },
                        label = { Text("Student Email") },
                        placeholder = { Text("e.g., 24036212A@common.cpce-polyu.edu.hk") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("e.g., 9876 5432") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (userName.isNotBlank()) {
                                groupmateViewModel.setUserName(userName)
                                groupmateViewModel.setStudentEmail(studentEmail)
                                groupmateViewModel.setPhoneNumber(phoneNumber)
                                isEditing = false
                                Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Name is required", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Changes")
                    }
                } else {
                    // Display mode
                    InfoRow(Icons.Default.Person, "Name", if (userName.isNotBlank()) userName else "Not set")
                    InfoRow(Icons.Default.Email, "Student Email", if (studentEmail.isNotBlank()) studentEmail else "Not set")
                    InfoRow(Icons.Default.Phone, "Phone Number", if (phoneNumber.isNotBlank()) phoneNumber else "Not set")
                }
            }
        }

        HorizontalDivider()

        // Target Credits Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Academic Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = credits,
                    onValueChange = { viewModel.updateUserCredits(it) },
                    label = { Text("Target Credits") },
                    leadingIcon = { Icon(Icons.Filled.Star, null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        HorizontalDivider()

        // Backup & Share Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Backup & Share",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val config = viewModel.getConfigString()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Timetable Config", config)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Config copied!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Share, "")
                    Spacer(Modifier.width(8.dp))
                    Text("Copy Config Code")
                }

                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    label = { Text("Paste Config Code") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { viewModel.loadConfigString(importText) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = importText.isNotBlank()
                ) {
                    Text("Load Configuration")
                }
            }
        }
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = if (value != "Not set") FontWeight.Medium else FontWeight.Normal)
        }
    }
}
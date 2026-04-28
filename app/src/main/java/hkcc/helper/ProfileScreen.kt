package hkcc.helper

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
    var studentId by remember { 
        mutableStateOf(groupmateViewModel.getStudentEmail().substringBefore("@")) 
    }
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
                        value = studentId,
                        onValueChange = { studentId = it.uppercase() },
                        label = { Text("Student ID") },
                        placeholder = { Text("e.g., 2XXXXXXXA") },
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

                    // Show preview of generated email
                    if (studentId.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Your email will be:",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = "${studentId.uppercase()}@common.cpce-polyu.edu.hk",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val generatedEmail = if (studentId.isNotBlank()) "${studentId.uppercase()}@common.cpce-polyu.edu.hk" else ""
                            val hasContactInfo = studentId.isNotBlank() || phoneNumber.isNotBlank()

                            if (hasContactInfo) {
                                // Name can be blank
                                val finalName = userName.ifBlank {
                                    if (studentId.isNotBlank()) "Student $studentId" else "HKCC Student"
                                }
                                groupmateViewModel.setUserName(finalName)
                                groupmateViewModel.setStudentEmail(generatedEmail)
                                groupmateViewModel.setPhoneNumber(phoneNumber)
                                isEditing = false
                                Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Please enter either Student ID or Phone Number", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Changes")
                    }
                } else {
                    // Display mode
                    InfoRow(Icons.Default.Person, "Name",
                        groupmateViewModel.getUserName().ifBlank { "Not set" })
                    val savedEmail = groupmateViewModel.getStudentEmail()
                    InfoRow(Icons.Default.Email, "Student Email", savedEmail.ifBlank { "Not set" })
                    InfoRow(Icons.Default.Phone, "Phone Number",
                        groupmateViewModel.getPhoneNumber().ifBlank { "Not set" })
                }
            }
        }

        HorizontalDivider()

        // Target Credits Section
        val isLocked by viewModel.isCreditsLocked.collectAsState()
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Academic Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (isLocked) {
                        IconButton(onClick = { viewModel.unlockCredits() }) {
                            Icon(Icons.Default.Lock, "Unlock", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = credits,
                    onValueChange = { if (!isLocked) viewModel.updateUserCredits(it) },
                    label = { Text("Target Graduation Credits") },
                    leadingIcon = { Icon(Icons.Filled.Star, null) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLocked,
                    supportingText = if (isLocked) { { Text("Locked by Study Pattern") } } else null
                )
            }
        }

        HorizontalDivider()

// Study Pattern Section
        val studyPatterns by viewModel.studyPatterns.collectAsState()
        val savedPattern by viewModel.selectedStudyPattern.collectAsState()

        // 1. Add a state variable to control expand/collapse (default is false / collapsed)
        var isStudyPatternExpanded by remember { mutableStateOf(false) }

        if (studyPatterns.isNotEmpty()) {
            var selectedProgram by remember { mutableStateOf(savedPattern["program"] ?: "") }
            var selectedPattern by remember { mutableStateOf(savedPattern["pattern"] ?: "") }
            var selectedSemester by remember { mutableStateOf(savedPattern["semester"] ?: "") }
            var selectedCantonese by remember { mutableStateOf(savedPattern["cantonese"] ?: "") }
            var selectedEngLevel by remember { mutableStateOf(savedPattern["eng"] ?: "") }

            LaunchedEffect(savedPattern) {
                if (savedPattern.isNotEmpty()) {
                    selectedProgram = savedPattern["program"] ?: ""
                    selectedPattern = savedPattern["pattern"] ?: ""
                    selectedSemester = savedPattern["semester"] ?: ""
                    selectedCantonese = savedPattern["cantonese"] ?: ""
                    selectedEngLevel = savedPattern["eng"] ?: ""
                }
            }

            val programs = remember(studyPatterns) { studyPatterns.map { it.programCode }.distinct() }
            val patterns = remember(selectedProgram, studyPatterns) {
                studyPatterns.filter { it.programCode == selectedProgram }.map { it.studyPattern }.distinct()
            }
            val semesters = remember(selectedProgram, selectedPattern, studyPatterns) {
                studyPatterns.filter { it.programCode == selectedProgram && it.studyPattern == selectedPattern }
                    .map { it.semester }.distinct()
            }
            val cantoneseOptions = remember(selectedProgram, selectedPattern, selectedSemester, studyPatterns) {
                studyPatterns.filter {
                    it.programCode == selectedProgram &&
                            it.studyPattern == selectedPattern &&
                            it.semester == selectedSemester
                }.map { it.cantonesePutonghua }.distinct()
            }
            val engLevels = remember(selectedProgram, selectedPattern, selectedSemester, selectedCantonese, studyPatterns) {
                studyPatterns.filter {
                    it.programCode == selectedProgram &&
                            it.studyPattern == selectedPattern &&
                            it.semester == selectedSemester &&
                            it.cantonesePutonghua == selectedCantonese
                }.map { it.engLevel }.distinct()
            }

            var selectedBio by remember { mutableStateOf(savedPattern["bio"] ?: "") }
            var selectedChem by remember { mutableStateOf(savedPattern["chem"] ?: "") }
            var selectedPhy by remember { mutableStateOf(savedPattern["phy"] ?: "") }

            LaunchedEffect(savedPattern) {
                if (savedPattern.isNotEmpty()) {
                    selectedBio = savedPattern["bio"] ?: ""
                    selectedChem = savedPattern["chem"] ?: ""
                    selectedPhy = savedPattern["phy"] ?: ""
                }
            }

            val bioOptions = remember(selectedProgram, studyPatterns) {
                if (selectedProgram != "8C112-AS") emptyList() else
                    studyPatterns.filter { it.programCode == selectedProgram }.map { it.bioLevel }.distinct().filter { it.isNotBlank() }
            }
            val chemOptions = remember(selectedProgram, studyPatterns) {
                if (selectedProgram != "8C112-AS") emptyList() else
                    studyPatterns.filter { it.programCode == selectedProgram }.map { it.chemLevel }.distinct().filter { it.isNotBlank() }
            }
            val phyOptions = remember(selectedProgram, studyPatterns) {
                if (selectedProgram != "8C112-AS") emptyList() else
                    studyPatterns.filter { it.programCode == selectedProgram }.map { it.phyLevel }.distinct().filter { it.isNotBlank() }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    // 2. Add a Header Row with an icon button to toggle the state
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Study Pattern",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { isStudyPatternExpanded = !isStudyPatternExpanded }) {
                            Icon(
                                imageVector = if (isStudyPatternExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isStudyPatternExpanded) "Shrink" else "Expand"
                            )
                        }
                    }

                    // 3. Wrap the content inside an if-statement so it only renders when expanded
                    if (isStudyPatternExpanded) {
                        Text("Auto-select subjects and set graduation credits based on your program.", style = MaterialTheme.typography.bodySmall)

                        DropdownSelector("Program Code", selectedProgram, programs) { selectedProgram = it }
                        DropdownSelector("Study Pattern", selectedPattern, patterns) { selectedPattern = it }
                        DropdownSelector("Semester", selectedSemester, semesters) { selectedSemester = it }
                        DropdownSelector("Cantonese/Putonghua", selectedCantonese, cantoneseOptions) { selectedCantonese = it }

                        // --- DSE Level Section ---
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        Text("DSE Level", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)

                        // Eng Level applies to all programs
                        DropdownSelector("Eng Level", selectedEngLevel, engLevels) { selectedEngLevel = it }

                        // Science levels only apply to 8C112-AS
                        if (selectedProgram == "8C112-AS") {
                            DropdownSelector("Bio Level", selectedBio, bioOptions) { selectedBio = it }
                            DropdownSelector("Chem Level", selectedChem, chemOptions) { selectedChem = it }
                            DropdownSelector("Phy Level", selectedPhy, phyOptions) { selectedPhy = it }
                        }

                        Button(
                            onClick = {
                                val title = studyPatterns.find { it.programCode == selectedProgram }?.programTitle ?: ""
                                viewModel.applyStudyPattern(
                                    selectedProgram, title, selectedPattern, selectedSemester,
                                    selectedCantonese, selectedEngLevel, selectedBio, selectedChem, selectedPhy
                                )
                                Toast.makeText(context, "Study pattern applied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedProgram.isNotBlank() && selectedPattern.isNotBlank() &&
                                    selectedSemester.isNotBlank() && selectedCantonese.isNotBlank() &&
                                    selectedEngLevel.isNotBlank() &&
                                    (selectedProgram != "8C112-AS" || (selectedBio.isNotBlank() && selectedChem.isNotBlank() && selectedPhy.isNotBlank()))
                        ) {
                            Text("Apply Pattern")
                        }
                    }
                }
            }
            HorizontalDivider()
        }
        // Notification Settings Section
        val reminderMinutes by viewModel.reminderMinutes.collectAsState()
        val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notification Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { viewModel.toggleNotifications(it) }
                    )
                }
                
                if (notificationsEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Pre-class reminder (minutes): $reminderMinutes", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = reminderMinutes.toFloat(),
                        onValueChange = { viewModel.updateReminderMinutes(it.toInt()) },
                        valueRange = 0f..120f,
                        steps = 23
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("0mins", style = MaterialTheme.typography.labelSmall)
                        Text("30mins", style = MaterialTheme.typography.labelSmall)
                        Text("60mins", style = MaterialTheme.typography.labelSmall)
                        Text("90mins", style = MaterialTheme.typography.labelSmall)
                        Text("120mins", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    Text("Notifications are currently disabled.", style = MaterialTheme.typography.bodySmall)
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true, // Not editable
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                // MenuAnchorType.PrimaryNotEditable
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption) },
                    onClick = {
                        onSelect(selectionOption)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
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
package hkcc.timetable

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import hkcc.timetable.data.Subject
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupmateFinderScreen(
    viewModel: GroupmateViewModel = viewModel(),
    timetableViewModel: TimetableViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val myInvitations by viewModel.myInvitations.collectAsState()
    val courseInvitations by viewModel.courseInvitations.collectAsState()
    val selectedIds by timetableViewModel.selectedIds.collectAsState()
    val allSubjects by timetableViewModel.subjects.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedSubject by remember { mutableStateOf<Subject?>(null) }
    var invitationMessage by remember { mutableStateOf("") }
    var showInvitationsDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Get actual selected subjects
    val selectedSubjects = allSubjects.filter { selectedIds.contains(it.id) }
    val enrolledCourses = selectedSubjects
        .distinctBy { "${it.code}-${it.classNo}-${it.type}" }
        .sortedBy { it.code }

    // Show found invitations dialog
    if (showInvitationsDialog && courseInvitations.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {
                showInvitationsDialog = false
                viewModel.clearCourseInvitations()
            },
            title = {
                Text("Found Groupmates (${courseInvitations.size})")
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(courseInvitations) { invitation ->
                        FoundGroupmateCard(
                            invitation = invitation,
                            isOwn = invitation.userId == viewModel.getUserId(),
                            context = context
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showInvitationsDialog = false
                        viewModel.clearCourseInvitations()
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Create invitation dialog
    if (showCreateDialog && selectedSubject != null) {
        val subject = selectedSubject!!
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Post Invitation") },
            text = {
                Column {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("${subject.code} ${subject.name}", fontWeight = FontWeight.Bold)
                            Text("Class ${subject.classNo} - ${subject.type}${if (subject.subGroup.isNotEmpty()) " (Group ${subject.subGroup})" else ""}")
                            Text("${subject.dayOfWeek} ${subject.startTime}-${subject.endTime}")
                            Text(subject.venue)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = invitationMessage,
                        onValueChange = { invitationMessage = it },
                        label = { Text("Message (optional)") },
                        placeholder = { Text("Looking for group members...") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your contact info will be shared with others who find this invitation.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createInvitation(
                            courseCode = subject.code,
                            courseName = subject.name,
                            classNo = subject.classNo,
                            subGroup = subject.subGroup,
                            dayOfWeek = subject.dayOfWeek,
                            startTime = subject.startTime,
                            endTime = subject.endTime,
                            venue = subject.venue,
                            message = invitationMessage.ifEmpty { "Looking for groupmates for ${subject.code} ${subject.type}!" }
                        )
                        showCreateDialog = false
                        invitationMessage = ""
                    }
                ) {
                    Text("Post")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Show success/error snackbar
    LaunchedEffect(Unit) {
        scope.launch {
            viewModel.successMessage.collect { message ->
                snackbarHostState.showSnackbar(message)
            }
        }
        scope.launch {
            viewModel.errorMessage.collect { message ->
                snackbarHostState.showSnackbar(message)
            }
        }
        viewModel.loadMyInvitations()
    }

    // Show dialog when course invitations are found
    LaunchedEffect(courseInvitations) {
        if (courseInvitations.isNotEmpty()) {
            showInvitationsDialog = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Groupmate Finder",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Text(
                    text = "Find classmates to work with",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Profile info card - Display only, no edit
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Name: ${viewModel.getUserName()}", fontWeight = FontWeight.Bold)
                        }
                        if (viewModel.getStudentEmail().isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Icon(Icons.Default.Email, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(viewModel.getStudentEmail(), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (viewModel.getPhoneNumber().isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Icon(Icons.Default.Phone, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(viewModel.getPhoneNumber(), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (!viewModel.hasCompleteProfile()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Please update your contact info in Profile page",
                                    modifier = Modifier.padding(8.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Your invitations section
            item {
                Text(
                    text = "Your Invitations",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (myInvitations.isNotEmpty()) {
                items(myInvitations) { invitation ->
                    MyInvitationCard(
                        invitation = invitation,
                        onDelete = { viewModel.deleteInvitation(invitation.id) }
                    )
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = "No invitations posted yet",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Your courses section
            item {
                Text(
                    text = "Your Courses",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (enrolledCourses.isNotEmpty()) {
                items(enrolledCourses) { subject ->
                    CourseInvitationCard(
                        subject = subject,
                        onCreateInvitation = {
                            selectedSubject = subject
                            showCreateDialog = true
                        },
                        onFindGroupmates = {
                            viewModel.loadInvitationsForCourse(subject.code, subject.classNo)
                            scope.launch {
                                snackbarHostState.showSnackbar("Searching for groupmates in ${subject.code}...")
                            }
                        }
                    )
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Courses Selected!",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Go to 'Select Subjects' and select specific class sections to enable groupmate features.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MyInvitationCard(
    invitation: GroupInvitation,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${invitation.courseCode} - ${invitation.courseName}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Class ${invitation.classNo}${if (invitation.subGroup.isNotEmpty()) " Group ${invitation.subGroup}" else ""}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${invitation.dayOfWeek} ${invitation.startTime}-${invitation.endTime} @ ${invitation.venue}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }

            if (invitation.message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\"${invitation.message}\"",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dateFormat.format(Date(invitation.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Expires: ${dateFormat.format(Date(invitation.expiresAt))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FoundGroupmateCard(
    invitation: GroupInvitation,
    isOwn: Boolean,
    context: android.content.Context
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isOwn)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = invitation.userName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (isOwn) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "You",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${invitation.courseCode} - ${invitation.courseName}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Class ${invitation.classNo}${if (invitation.subGroup.isNotEmpty()) " Group ${invitation.subGroup}" else ""}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "${invitation.dayOfWeek} ${invitation.startTime}-${invitation.endTime} @ ${invitation.venue}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (invitation.message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\"${invitation.message}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Contact Information Section
            if (invitation.studentEmail.isNotBlank() || invitation.phoneNumber.isNotBlank()) {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Contact Information:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Email - Display Only (No Button)
                    if (invitation.studentEmail.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Email,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = invitation.studentEmail,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "HKCC Student Email",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Phone - With Call Button
                    if (invitation.phoneNumber.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Phone,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = invitation.phoneNumber,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = "tel:${invitation.phoneNumber}".toUri()
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Phone, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Call", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Posted: ${dateFormat.format(Date(invitation.createdAt))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CourseInvitationCard(
    subject: Subject,
    onCreateInvitation: () -> Unit,
    onFindGroupmates: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${subject.code} ${subject.name}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Class ${subject.classNo} - ${subject.type}${if (subject.subGroup.isNotEmpty()) " (Group ${subject.subGroup})" else ""}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${subject.dayOfWeek} ${subject.startTime}-${subject.endTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = subject.venue,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCreateInvitation,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Post")
                }

                Button(
                    onClick = onFindGroupmates,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Find")
                }
            }
        }
    }
}
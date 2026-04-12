package hkcc.timetable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun HomeScreen(navController: NavController, viewModel: TimetableViewModel) {
    val allSubjects by viewModel.subjects.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val credits by viewModel.userCredits.collectAsState()
    val uniqueSubjectsCount = remember(allSubjects) { allSubjects.distinctBy { it.code }.size }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (allSubjects.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp).clickable { navController.navigate("upload_csv") }
            ) {
                Column(Modifier.padding(32.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.AddCircle, null, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No Data Loaded", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Tap here to upload CSV manually\nor use assets/subjects.csv", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DashboardCard(Modifier.weight(1f), "Uploaded", "$uniqueSubjectsCount Subjects", Icons.Filled.Info, MaterialTheme.colorScheme.primaryContainer)
                DashboardCard(Modifier.weight(1f), "Selected", "${selectedIds.size} Sections", Icons.Filled.CheckCircle, MaterialTheme.colorScheme.secondaryContainer)
            }
            DashboardCard(Modifier.fillMaxWidth(), "Target Graduation Credits", credits, Icons.Filled.Star, MaterialTheme.colorScheme.tertiaryContainer)

            Text("Quick Actions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top=16.dp))
            QuickActionCard("Auto Plan", "Generate schedules", Icons.Filled.Build) { navController.navigate("auto_plan") }
            QuickActionCard("Select Subjects", "Manual selection", Icons.Filled.Edit) { navController.navigate("select_subjects") }
            QuickActionCard("Preview", "View Grid/List", Icons.Filled.DateRange) { navController.navigate("preview_timetable") }
            QuickActionCard("Academic & GPA", "GPA & Graduation", Icons.Filled.Star) { navController.navigate("academic") }
            QuickActionCard("Find Groupmates", "Find classmates", Icons.Filled.Person) { navController.navigate("groupmate_finder") }
        }
    }
}

@Composable
fun DashboardCard(modifier: Modifier, title: String, value: String, icon: ImageVector, color: Color) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = color)) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun QuickActionCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, "", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(subtitle, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

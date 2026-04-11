package hkcc.timetable

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import hkcc.timetable.data.Subject
import hkcc.timetable.ui.theme.MyApplicationTheme
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.time.LocalTime
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Log.d("MainActivity", "onCreate started")

        try {
            // Initialize Supabase
            Log.d("MainActivity", "Initializing Supabase...")
            SupabaseManager.init(this)
            Log.d("MainActivity", "Supabase initialized successfully")

            NotificationHelper.createNotificationChannel(this)

            Toast.makeText(this, "App started!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing Supabase", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }

        setContent { TimetableAppRoot() }
        Log.d("MainActivity", "onCreate completed")
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableAppRoot(viewModel: TimetableViewModel = viewModel()) {
    val themeMode by viewModel.themeMode.collectAsState()

    val useDarkTheme = when (themeMode) {
        TimetableViewModel.THEME_LIGHT -> false
        TimetableViewModel.THEME_DARK -> true
        else -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                context.assets.open("subjects.csv").use { inputStream ->
                    val subjects = parseCsvStream(inputStream)
                    if (subjects.isNotEmpty()) viewModel.addSubjects(subjects)
                }
            } catch (e: Exception) { }
        }
    }

    MyApplicationTheme(darkTheme = useDarkTheme) { TimetableApp(viewModel) }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableApp(viewModel: TimetableViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val groupmateViewModel: GroupmateViewModel = viewModel()
    val showGhosting by viewModel.showGhosting.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // SnackBar Host State
    val snackbarHostState = remember { SnackbarHostState() }

    val solidSubjects = remember(viewModel.subjects.collectAsState().value, viewModel.selectedIds.collectAsState().value) {
        val all = viewModel.subjects.value
        val ids = viewModel.selectedIds.value
        all.filter { ids.contains(it.id) }
    }
    val reminderMinutes by viewModel.reminderMinutes.collectAsState()

    LaunchedEffect(solidSubjects, reminderMinutes) {
        if (solidSubjects.isNotEmpty()) {
            NotificationHelper.scheduleClassReminders(context, solidSubjects, reminderMinutes)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            scope.launch { snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short) }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(24.dp))
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("HKCC Planner", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()

                NavigationItem("Home", Icons.Filled.Home, currentRoute == "home") { scope.launch { drawerState.close(); navController.navigate("home") { popUpTo("home") { inclusive = true }; launchSingleTop = true } } }
                NavigationItem("Upload CSV", Icons.Filled.AddCircle, currentRoute == "upload_csv") { scope.launch { drawerState.close(); navController.navigate("upload_csv") { popUpTo("home"); launchSingleTop = true } } }
                NavigationItem("Select Subjects", Icons.Filled.Edit, currentRoute == "select_subjects") { scope.launch { drawerState.close(); navController.navigate("select_subjects") { popUpTo("home"); launchSingleTop = true } } }
                NavigationItem("Auto Plan", Icons.Filled.Build, currentRoute == "auto_plan") { scope.launch { drawerState.close(); navController.navigate("auto_plan") { popUpTo("home"); launchSingleTop = true } } }
                NavigationItem("Preview", Icons.Filled.DateRange, currentRoute == "preview_timetable") { scope.launch { drawerState.close(); navController.navigate("preview_timetable") { popUpTo("home"); launchSingleTop = true } } }
                NavigationItem("Academic & GPA", Icons.Filled.Star, currentRoute == "academic") { scope.launch { drawerState.close(); navController.navigate("academic") { popUpTo("home"); launchSingleTop = true } } }
                NavigationItem("Find Groupmates", Icons.Filled.Person, currentRoute == "groupmate_finder") {
                    scope.launch { drawerState.close(); navController.navigate("groupmate_finder") { popUpTo("home"); launchSingleTop = true } }
                }
                NavigationItem("Profile", Icons.Filled.Person, currentRoute == "profile") { scope.launch { drawerState.close(); navController.navigate("profile") { popUpTo("home"); launchSingleTop = true } } }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text("Preview Hints", modifier = Modifier.weight(1f))
                    Switch(checked = showGhosting, onCheckedChange = { viewModel.toggleGhosting(it) })
                }

                Spacer(Modifier.height(16.dp))
                Text("Theme", modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ThemeOption("System", Icons.Default.Settings, themeMode == TimetableViewModel.THEME_SYSTEM) { viewModel.setThemeMode(TimetableViewModel.THEME_SYSTEM) }
                    ThemeOption("Light", rememberSunIcon(), themeMode == TimetableViewModel.THEME_LIGHT) { viewModel.setThemeMode(TimetableViewModel.THEME_LIGHT) }
                    ThemeOption("Dark", rememberMoonIcon(), themeMode == TimetableViewModel.THEME_DARK) { viewModel.setThemeMode(TimetableViewModel.THEME_DARK) }
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Timetable Helper") },
                    navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Filled.Menu, "") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding),
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
            ) {
                composable("home") { HomeScreen(navController, viewModel) }
                composable("select_subjects") { SubjectSelectionScreen(viewModel) }
                composable("preview_timetable") { TimetablePreviewScreen(viewModel) }
                composable("upload_csv") { CsvUploadScreen(viewModel) }
                composable("auto_plan") { AutoPlanScreen(navController, viewModel) }
                composable("academic") { AcademicScreen() }
                composable("groupmate_finder") {
                    GroupmateFinderScreen(groupmateViewModel, viewModel)
                }
                composable("profile") {
                    ProfileScreen(
                        viewModel = viewModel,
                        groupmateViewModel = groupmateViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeOption(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected, onClick = onClick, label = { Text(label) },
        leadingIcon = { if (selected) Icon(Icons.Filled.Check, null) else Icon(icon, null) }
    )
}

@Composable
fun NavigationItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = { Icon(icon, null) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

//  Screen: Home
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
            DashboardCard(Modifier.fillMaxWidth(), "Target Credits", credits, Icons.Filled.Star, MaterialTheme.colorScheme.tertiaryContainer)

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

//  Screen: Auto Plan
@Composable
fun AutoPlanScreen(navController: NavController, viewModel: TimetableViewModel) {
    val results by viewModel.generatedSchedules.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT")
    val selectedDays = remember { mutableStateListOf<String>() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Auto Planner", style = MaterialTheme.typography.headlineMedium)
        Text("Select days to AVOID campus:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEach { day ->
                FilterChip(selected = selectedDays.contains(day), onClick = { if(selectedDays.contains(day)) selectedDays.remove(day) else selectedDays.add(day) }, label = { Text(day.take(3)) })
            }
        }
        Button(onClick = { viewModel.generateAutoPlan(selectedDays.toSet()) }, modifier = Modifier.fillMaxWidth(), enabled = !isGenerating) {
            if (isGenerating) { CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary); Spacer(Modifier.width(8.dp)); Text("Calculating...") }
            else { Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Generate Schedules") }
        }
        Spacer(Modifier.height(16.dp))

        // Results List
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            itemsIndexed(results) { index, schedule ->
                val scheduleByDay = schedule.sortedBy { it.getDayIndex() }.groupBy { it.dayOfWeek }

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Schedule Plan ${index + 1}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("${schedule.size} Classes", style = MaterialTheme.typography.labelSmall)
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))

                        scheduleByDay.forEach { (day, subjects) ->
                            Row(Modifier.padding(vertical = 2.dp)) {
                                Text(day.take(3), fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp), style = MaterialTheme.typography.bodySmall)
                                Column {
                                    subjects.sortedBy { it.getStartMinutes() }.forEach { sub ->
                                        val typeGroup = if(sub.isLecture()) "Lect ${sub.classNo}" else "Tut ${sub.classNo}${sub.subGroup}"
                                        Text("${sub.startTime} - ${sub.code} ($typeGroup)", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.applySchedule(schedule); navController.navigate("preview_timetable") },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Apply This Plan")
                        }
                    }
                }
            }
        }
    }
}

// ProfileScreen is defined in ProfileScreen.kt

@Composable
fun AcademicScreen(academicViewModel: AcademicViewModel = viewModel(), timetableViewModel: TimetableViewModel = viewModel()) {
    val context = LocalContext.current
    val grades by academicViewModel.grades.collectAsState()
    val targetGpa by academicViewModel.targetGpa.collectAsState()
    val remainingCredits by academicViewModel.remainingCredits.collectAsState()
    val allSubjects by timetableViewModel.subjects.collectAsState()
    val selectedIds by timetableViewModel.selectedIds.collectAsState()
    val totalCreditsGoalStr by timetableViewModel.userCredits.collectAsState()
    val totalCreditsGoal = totalCreditsGoalStr.toIntOrNull() ?: 60
    
    val mySubjects = remember(allSubjects, selectedIds) {
        allSubjects.filter { selectedIds.contains(it.id) }.distinctBy { it.code }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingGrade by remember { mutableStateOf<CourseGrade?>(null) }

    // Initialize dialog state based on editingGrade
    var code by remember(showAddDialog) { mutableStateOf(editingGrade?.code ?: "") }
    var creds by remember(showAddDialog) { mutableStateOf(editingGrade?.credits?.toString() ?: "3") }
    var gradeValue by remember(showAddDialog) { mutableStateOf(editingGrade?.grade ?: "A") }
    var cluster by remember(showAddDialog) { mutableStateOf(editingGrade?.cluster ?: "") }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Academic & GPA", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        
        Card(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Overall GPA", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                Text(
                    String.format(Locale.US, "%.2f", academicViewModel.calculateGpa()),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Surface(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "${grades.sumOf { it.credits }} / $totalCreditsGoal Credits",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text("Graduation Progress", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val requirements = academicViewModel.getGraduationRequirements(totalCreditsGoal, mapOf("Cluster" to 15, "DS" to 18, "GS" to 9))
                requirements.forEach { req ->
                    val progress = if (req.required > 0) req.earned.toFloat() / req.required else 1f
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Text(req.category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("${req.earned}/${req.required}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress.coerceAtMost(1f) },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = if (progress >= 1f) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("GPA Planner", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = targetGpa, onValueChange = { academicViewModel.updateTargetGpa(it) }, label = { Text("Target GPA") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = remainingCredits, onValueChange = { academicViewModel.updateRemainingCredits(it) }, label = { Text("Rem. Credits") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                }
                val targetResult = academicViewModel.calculateRequiredGrades(targetGpa.toDoubleOrNull() ?: 0.0, remainingCredits.toIntOrNull() ?: 0)
                Text(targetResult, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Course Grades", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            FilledTonalButton(onClick = { 
                editingGrade = null
                showAddDialog = true 
            }) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("Add Grade")
            }
        }

        if (grades.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                Text("No grades added yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        grades.forEach { grade ->
            Card(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(grade.code, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        val desc = academicViewModel.getGradeDescription(grade.grade)
                        Text(
                            "${grade.credits} Credits • ${grade.cluster.ifBlank { "No Cluster" }}${if(desc.isNotEmpty()) " • $desc" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        grade.grade,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(onClick = { 
                        editingGrade = grade
                        showAddDialog = true
                    }) { Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.outline) }
                    IconButton(onClick = { academicViewModel.removeGrade(grade.id) }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Deadlines", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            var showDeadlineDialog by remember { mutableStateOf(false) }
            IconButton(onClick = { showDeadlineDialog = true }) { Icon(Icons.Default.Add, null) }
            
            if (showDeadlineDialog) {
                var title by remember { mutableStateOf("") }
                var dateStr by remember { mutableStateOf("2024-12-31") }
                AlertDialog(
                    onDismissRequest = { showDeadlineDialog = false },
                    title = { Text("Add Deadline") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task Title") })
                            OutlinedTextField(value = dateStr, onValueChange = { dateStr = it }, label = { Text("Due Date (YYYY-MM-DD)") })
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            try {
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                val date = sdf.parse(dateStr)
                                if (date != null) {
                                    val deadline = Deadline(title = title, dueDate = date.time, courseCode = "")
                                    academicViewModel.addDeadline(deadline)
                                    NotificationHelper.scheduleDeadlineReminder(context, deadline)
                                }
                            } catch (e: Exception) {}
                            showDeadlineDialog = false
                        }) { Text("Add") }
                    }
                )
            }
        }

        val deadlines by academicViewModel.deadlines.collectAsState()
        deadlines.forEach { deadline ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(deadline.title, fontWeight = FontWeight.Bold)
                        Text("Due: ${java.text.SimpleDateFormat("MMM dd, yyyy", Locale.US).format(java.util.Date(deadline.dueDate))}", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { academicViewModel.removeDeadline(deadline.id) }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false
                editingGrade = null
            },
            title = { Text(if (editingGrade == null) "Add Course Grade" else "Edit Course Grade", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (mySubjects.isNotEmpty() && editingGrade == null) {
                        Text("Enrolled Courses:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(mySubjects) { sub ->
                                FilterChip(
                                    selected = code == sub.code,
                                    onClick = {
                                        code = sub.code
                                        cluster = sub.cluster
                                    },
                                    label = { Text(sub.code) }
                                )
                            }
                        }
                    }
                    OutlinedTextField(value = code, onValueChange = { code = it.uppercase() }, label = { Text("Course Code") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = creds, onValueChange = { creds = it }, label = { Text("Credits") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                    
                    Text("Select Grade:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    val gradesList = listOf("A+", "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D+", "D", "F")
                    LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(gradesList) { g ->
                            FilterChip(
                                selected = gradeValue == g,
                                onClick = { gradeValue = g },
                                label = { Text(g) }
                            )
                        }
                    }

                    OutlinedTextField(value = cluster, onValueChange = { cluster = it }, label = { Text("Cluster Area") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (code.isNotBlank()) {
                            val newGrade = CourseGrade(
                                id = editingGrade?.id ?: java.util.UUID.randomUUID().toString(),
                                code = code,
                                name = "",
                                credits = creds.toIntOrNull() ?: 3,
                                grade = gradeValue,
                                semester = editingGrade?.semester ?: "Current",
                                cluster = cluster
                            )
                            if (editingGrade == null) {
                                academicViewModel.addGrade(newGrade)
                            } else {
                                academicViewModel.updateGrade(newGrade)
                            }
                            showAddDialog = false
                            editingGrade = null
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if (editingGrade == null) "Add" else "Save Changes") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddDialog = false
                    editingGrade = null
                }) { Text("Cancel") }
            }
        )
    }
}
@Composable
fun SubjectSelectionScreen(viewModel: TimetableViewModel) {
    val allSubjects by viewModel.subjects.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val selectedCodes by viewModel.selectedSubjectCodes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val clusterFilter by viewModel.clusterFilter.collectAsState()

    val filtered = remember(allSubjects, searchQuery, clusterFilter) {
        allSubjects.filter {
            (it.code.contains(searchQuery, true) || it.name.contains(searchQuery, true) || it.lecturer.contains(searchQuery, true)) &&
            (clusterFilter == "All" || it.cluster == clusterFilter)
        }
    }

    val clusters = remember(allSubjects) { listOf("All") + allSubjects.map { it.cluster }.filter { it.isNotEmpty() }.distinct().sorted() }

    val grouped = remember(filtered) { filtered.groupBy { it.code } }
    val sortedGroupKeys = remember(grouped, selectedCodes) { grouped.keys.sortedWith(compareByDescending<String> { selectedCodes.contains(it) }.thenBy { it }) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(value = searchQuery, onValueChange = { viewModel.updateSearch(it) }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), placeholder = { Text("Search Code, Lecturer...") }, leadingIcon = { Icon(Icons.Filled.Search, "") }, singleLine = true)

        LazyRow(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(clusters) { cluster ->
                FilterChip(
                    selected = clusterFilter == cluster,
                    onClick = { viewModel.updateClusterFilter(cluster) },
                    label = { Text(cluster) }
                )
            }
        }

        LazyColumn {
            items(sortedGroupKeys) { code ->
                SubjectGroupCard(code, grouped[code]!!, selectedIds, selectedCodes.contains(code), onToggleInterest = { viewModel.toggleSubjectInterest(code) }, onSelectClass = { viewModel.selectSubject(it) })
            }
        }
    }
}

@Composable
fun SubjectGroupCard(code: String, subjects: List<Subject>, selectedIds: Set<String>, isInterested: Boolean, onToggleInterest: () -> Unit, onSelectClass: (Subject) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val name = subjects.firstOrNull()?.name ?: ""
    val containerColor = if (isInterested) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (isInterested) MaterialTheme.colorScheme.primary else Color.Transparent

    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = containerColor), border = BorderStroke(1.dp, borderColor)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isInterested, onCheckedChange = { onToggleInterest() })
                Column(Modifier.weight(1f).clickable { expanded = !expanded }) { Text(code, fontWeight = FontWeight.Bold); Text(name, style = MaterialTheme.typography.bodySmall) }
                IconButton(onClick = { expanded = !expanded }) { Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, "") }
            }
            AnimatedVisibility(expanded) {
                Column(Modifier.padding(top = 8.dp)) {
                    val classes = subjects.groupBy { it.classNo }.toSortedMap()
                    classes.forEach { (cls, list) ->
                        val lecture = list.find { it.isLecture() }
                        val tuts = list.filter { !it.isLecture() }.sortedBy { it.subGroup }
                        if (lecture != null) {
                            val isSel = selectedIds.contains(lecture.id)
                            Row(Modifier.clickable { onSelectClass(lecture) }, verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(isSel, { onSelectClass(lecture) })
                                Column {
                                    Text("Class $cls (Lect) @ ${lecture.venue}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    if(lecture.lecturer.isNotEmpty()) Text("Lecturer: ${lecture.lecturer}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            if (isSel && tuts.isNotEmpty()) {
                                Column(Modifier.padding(start = 32.dp)) {
                                    tuts.forEach { tut ->
                                        Row(Modifier.clickable { onSelectClass(tut) }, verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(selectedIds.contains(tut.id), { onSelectClass(tut) })
                                            Column {
                                                Text("Group ${tut.subGroup} @ ${tut.venue}", style = MaterialTheme.typography.bodySmall)
                                                if(tut.lecturer.isNotEmpty()) Text("Tutor: ${tut.lecturer}", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

//  Screen: Preview
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TimetablePreviewScreen(viewModel: TimetableViewModel) {
    val allSubjects by viewModel.subjects.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val ghostSubjects by viewModel.ghostSubjects.collectAsState()
    val showGhosting by viewModel.showGhosting.collectAsState()
    val solidSubjects = remember(allSubjects, selectedIds) { allSubjects.filter { selectedIds.contains(it.id) } }
    var isGridView by remember { mutableStateOf(true) }

    // Export Logic State
    val context = LocalContext.current
    var showShareOptions by remember { mutableStateOf(false) }

    if (showShareOptions) {
        AlertDialog(
            onDismissRequest = { showShareOptions = false },
            title = { Text("Export & Share") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { generateTimetablePdf(context, solidSubjects); showShareOptions = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Build, null) // Changed icon to a valid one
                        Spacer(Modifier.width(8.dp))
                        Text("Export as PDF")
                    }
                    Button(
                        onClick = {
                            val config = viewModel.getConfigString()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Timetable Config", config)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Config code copied to clipboard!", Toast.LENGTH_SHORT).show()
                            showShareOptions = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Share via Config Code")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showShareOptions = false }) { Text("Close") } }
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = { showShareOptions = true }) { Icon(Icons.Default.Share, "Share", tint = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { isGridView = false }) { Icon(Icons.AutoMirrored.Filled.List, "List", tint = if(!isGridView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton(onClick = { isGridView = true }) { Icon(Icons.Filled.DateRange, "Grid", tint = if(isGridView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        // Wrap content in a Box that fills max size but allows scrolling
        Box(Modifier.fillMaxSize()) {
            if (isGridView) TimetableGrid(solidSubjects, ghostSubjects, viewModel, showGhosting) else TimetableListView(solidSubjects)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun generateTimetablePdf(context: Context, subjects: List<Subject>) {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()
        val textPaint = TextPaint().apply {
            color = android.graphics.Color.BLACK
            textSize = 8f
            typeface = Typeface.DEFAULT_BOLD
        }

        // 1. Draw Background
        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(0f, 0f, 595f, 842f, paint)

        // 2. Draw Grid Logic
        val startHour = 8
        val endHour = 22
        val startY = 40f
        val hourHeight = 50f
        val startX = 50f
        val colWidth = (595f - startX - 20f) / 6

        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT_BOLD

        // Draw Time Column
        paint.textAlign = Paint.Align.RIGHT
        paint.color = android.graphics.Color.BLACK
        for (h in startHour..endHour) {
            val y = startY + (h - startHour) * hourHeight
            canvas.drawText(String.format(Locale.US, "%02d:00", h), startX - 5f, y + 5f, paint)

            paint.color = android.graphics.Color.LTGRAY
            canvas.drawLine(startX, y, 595f - 20f, y, paint)
            paint.color = android.graphics.Color.BLACK
        }

        // Draw Days Header
        val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT")
        paint.color = android.graphics.Color.BLACK
        paint.textAlign = Paint.Align.CENTER
        days.forEachIndexed { index, day ->
            val x = startX + index * colWidth + colWidth / 2
            canvas.drawText(day, x, startY - 10f, paint)
        }

        // Vertical Lines
        paint.color = android.graphics.Color.LTGRAY
        for (i in 0..6) {
            val x = startX + i * colWidth
            canvas.drawLine(x, startY, x, startY + (endHour - startHour) * hourHeight, paint)
        }

        // Draw Subjects with Details
        subjects.forEach { sub ->
            val dayIdx = sub.getDayIndex()
            if (dayIdx in 0..5) {
                val startMin = sub.getStartMinutes()
                val durationMin = sub.getEndMinutes() - startMin

                val x = startX + dayIdx * colWidth
                val y = startY + (startMin - (startHour * 60)) * (hourHeight / 60f)
                val height = durationMin * (hourHeight / 60f)

                paint.color = sub.color.toArgb()
                canvas.drawRect(x + 1f, y + 1f, x + colWidth - 1f, y + height - 1f, paint)

                val content = "${sub.code}\n${sub.getShortTypeAndGroup()}\n${sub.venue}\n${sub.lecturer}"
                val staticLayout = StaticLayout.Builder.obtain(content, 0, content.length, textPaint, (colWidth - 4f).toInt())
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .build()

                canvas.save()
                canvas.translate(x + 2f, y + 2f)
                staticLayout.draw(canvas)
                canvas.restore()
            }
        }

        pdfDocument.finishPage(page)

        val filename = "Timetable_${System.currentTimeMillis()}.pdf"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/HKCC_Timetable")
        }

        val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), values)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { out -> pdfDocument.writeTo(out) }
            Toast.makeText(context, "PDF Saved!", Toast.LENGTH_LONG).show()
        }
        pdfDocument.close()

    } catch (e: Exception) {
        Toast.makeText(context, "PDF Error: ${e.message}", Toast.LENGTH_LONG).show()
        e.printStackTrace()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TimetableGrid(solidSubjects: List<Subject>, ghostSubjects: List<Subject>, viewModel: TimetableViewModel, showGhosting: Boolean) {
    val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT")
    val startHour = 8; val endHour = 22; val hourHeight = 60.dp; val headerHeight = 40.dp; val timeColWidth = 50.dp
    val scrollState = rememberScrollState()

    Row(Modifier.fillMaxSize()) {
        Column(Modifier.width(timeColWidth).padding(top = headerHeight).verticalScroll(scrollState)) {
            for (h in startHour..endHour) Text(String.format(Locale.US, "%02d:00", h), style = MaterialTheme.typography.labelSmall, modifier = Modifier.height(hourHeight).fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
        }
        Column(Modifier.weight(1f)) {
            Row(Modifier.height(headerHeight).fillMaxWidth()) {
                days.forEach { day -> Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) { Text(day, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) } }
            }
            Box(Modifier.fillMaxWidth().verticalScroll(scrollState)) {
                Column { for (i in startHour..endHour) Box(Modifier.height(hourHeight).fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) }
                ghostSubjects.forEach { sub -> TimetableBlock(sub, startHour, hourHeight, isGhost = true, isClickable = showGhosting, onClick = { viewModel.selectSubject(sub) }) }
                solidSubjects.forEach { sub -> TimetableBlock(sub, startHour, hourHeight, isGhost = false, isClickable = showGhosting, onClick = { viewModel.selectSubject(sub) }) }
                CurrentTimeIndicator(startHour, hourHeight)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CurrentTimeIndicator(startHour: Int, hourHeight: Dp) {
    val now = LocalTime.now()
    val currentMinutes = now.hour * 60 + now.minute
    val startMinutes = startHour * 60
    if (currentMinutes in startMinutes..(22 * 60)) {
        val minutesSinceStart = currentMinutes - startMinutes
        val topOffset = hourHeight * minutesSinceStart / 60
        Canvas(Modifier.fillMaxWidth().height(2.dp).offset(y = topOffset)) { drawLine(color = Color.Red, start = Offset(0f, 0f), end = Offset(size.width, 0f), strokeWidth = 4f) }
    }
}

@Composable
fun TimetableBlock(subject: Subject, startHour: Int, hourHeight: Dp, isGhost: Boolean, isClickable: Boolean = true, onClick: () -> Unit) {
    val dayIdx = subject.getDayIndex()
    if (dayIdx in 0..5) {
        val startMin = subject.getStartMinutes()
        val durationMin = subject.getEndMinutes() - startMin
        val topOffset = hourHeight * (startMin - startHour * 60) / 60
        val height = hourHeight * durationMin / 60

        val textColor = if (isGhost) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else Color.Black
        val modifier = if (isGhost) Modifier.dashedBorder(1.dp, MaterialTheme.colorScheme.primary, 4.dp).background(MaterialTheme.colorScheme.surface.copy(alpha=0.1f)) else Modifier.background(subject.color, RoundedCornerShape(4.dp))

        Row(Modifier.fillMaxWidth().height(height).offset(y = topOffset)) {
            if(dayIdx > 0) Spacer(Modifier.weight(dayIdx.toFloat()))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(1.dp).then(modifier).clickable(enabled = isClickable) { onClick() }.padding(2.dp)) {
                Column {
                    Text(subject.code, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = textColor, maxLines = 1)
                    Text(subject.getShortTypeAndGroup(), style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = textColor, maxLines = 1)
                    Text(subject.venue, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = textColor, maxLines = 1)
                    if(subject.lecturer.isNotEmpty()) Text(subject.lecturer, style = MaterialTheme.typography.labelSmall, fontSize = 7.sp, color = textColor, lineHeight = 8.sp, maxLines = 1)
                }
            }
            if(dayIdx < 5) Spacer(Modifier.weight((5 - dayIdx).toFloat()))
        }
    }
}

fun Modifier.dashedBorder(width: Dp, color: Color, radius: Dp) = drawBehind {
    val stroke = Stroke(width = width.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
    drawRoundRect(color = color, style = stroke, cornerRadius = CornerRadius(radius.toPx()))
}

@Composable
fun TimetableListView(subjects: List<Subject>) {
    // 1. Sort primarily by Day
    // 2. Sort secondarily by Start Time
    val sorted = subjects.sortedWith(compareBy({ it.getDayIndex() }, { it.getStartMinutes() }))

    // 3. Group by Day String to insert headers
    val groupedByDay = sorted.groupBy { it.dayOfWeek }

    LazyColumn(Modifier.fillMaxSize()) {
        groupedByDay.forEach { (day, dailySubjects) ->
            //  Day Header
            item {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            //  Subject for that day
            items(dailySubjects) { subject ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                ) {
                    Box(
                        Modifier
                            .width(6.dp)
                            .height(100.dp)
                            .background(subject.color, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                    )
                    Column(Modifier.padding(12.dp).weight(1f)) {
                        Text(subject.code, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(subject.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        Text(
                            "${subject.startTime} - ${subject.endTime} @ ${subject.venue}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (subject.lecturer.isNotEmpty()) {
                            Text("Lecturer: ${subject.lecturer}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "Group: ${subject.classNo} ${subject.subGroup} (${subject.type})",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CsvUploadScreen(viewModel: TimetableViewModel) {
    val context = LocalContext.current
    var showConfirm by remember { mutableStateOf(false) }
    var showAddManual by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val subjects = parseCsvStream(stream)
                if (subjects.isNotEmpty()) {
                    viewModel.addSubjects(subjects)
                    Toast.makeText(context, "Added ${subjects.size} entries", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false }, title = { Text("Reset All Data?") }, text = { Text("This will delete all subjects and selections.") },
            confirmButton = { TextButton(onClick = { viewModel.clearAll(); showConfirm = false }) { Text("Confirm", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Cancel") } },
            icon = { Icon(Icons.Filled.Warning, null) }
        )
    }

    if (showAddManual) {
        var code by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var day by remember { mutableStateOf("MON") }
        var start by remember { mutableStateOf("09:00") }
        var end by remember { mutableStateOf("11:00") }
        var venue by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddManual = false },
            title = { Text("Add Custom Course") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Course Code") })
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Course Name") })
                    OutlinedTextField(value = day, onValueChange = { day = it }, label = { Text("Day (MON, TUE...)") })
                    OutlinedTextField(value = start, onValueChange = { start = it }, label = { Text("Start Time (HH:mm)") })
                    OutlinedTextField(value = end, onValueChange = { end = it }, label = { Text("End Time (HH:mm)") })
                    OutlinedTextField(value = venue, onValueChange = { venue = it }, label = { Text("Venue") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (code.isNotBlank() && name.isNotBlank()) {
                        viewModel.addSubjects(listOf(Subject(
                            code = code, name = name, classNo = "CUSTOM", subGroup = "", type = "Lec",
                            dayOfWeek = day, startTime = start, endTime = end, campus = "Other", venue = venue
                        )))
                        showAddManual = false
                    }
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddManual = false }) { Text("Cancel") } }
        )
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.AddCircle, "", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Button(onClick = { launcher.launch("*/*") }, modifier = Modifier.fillMaxWidth(0.7f)) {
            Icon(Icons.Filled.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Import CSV File")
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { showAddManual = true }, modifier = Modifier.fillMaxWidth(0.7f)) {
            Icon(Icons.Filled.Edit, null)
            Spacer(Modifier.width(8.dp))
            Text("Add Manually")
        }
        Spacer(Modifier.height(32.dp))
        TextButton(onClick = { showConfirm = true }) {
            Icon(Icons.Filled.Delete, "")
            Spacer(Modifier.width(4.dp))
            Text("Clear All Data", color = MaterialTheme.colorScheme.error)
        }
    }
}

fun parseCsvStream(inputStream: InputStream): List<Subject> {
    val subjects = mutableListOf<Subject>()
    try {
        val reader = BufferedReader(InputStreamReader(inputStream))
        var curCode = ""; var curName = ""; var curClass = ""
        reader.useLines { lines ->
            lines.forEach { line ->
                var t = line.split("\t").map { it.trim() }
                if (t.size < 5) t = line.split(",").map { it.trim() }
                if (t.size >= 7) {
                    if (t[0].isNotEmpty()) curCode = t[0]; if (t[1].isNotEmpty()) curName = t[1]; if (t[2].isNotEmpty()) curClass = t[2]
                    if (curCode.contains("Subject Code", true)) return@forEach
                    val timeRange = t.getOrElse(6) { "" }
                    if (curCode.isNotEmpty() && timeRange.contains("-")) {
                        val times = timeRange.split("-")
                        subjects.add(Subject(
                            code = curCode, name = curName, classNo = curClass,
                            subGroup = t.getOrElse(3){""}, type = t.getOrElse(4){""}, dayOfWeek = t.getOrElse(5){""},
                            startTime = times[0].trim(), endTime = times[1].trim(), campus = t.getOrElse(7){""},
                            venue = t.getOrElse(8){""}, lecturer = t.getOrElse(9){""},
                            cluster = t.getOrElse(10){""}
                        ))
                    }
                }
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
    return subjects
}

@Composable
fun rememberMoonIcon(): ImageVector {
    return remember {
        ImageVector.Builder(
            name = "Moon",
            defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 3f)
                curveToRelative(-4.97f, 0f, -9f, 4.03f, -9f, 9f)
                reflectiveCurveToRelative(4.03f, 9f, 9f, 9f)
                reflectiveCurveToRelative(9f, -4.03f, 9f, -9f)
                curveToRelative(0f, -0.46f, -0.04f, -0.92f, -0.1f, -1.36f)
                curveToRelative(-0.98f, 1.37f, -2.58f, 2.26f, -4.4f, 2.26f)
                curveToRelative(-2.98f, 0f, -5.4f, -2.42f, -5.4f, -5.4f)
                curveToRelative(0f, -1.81f, 0.89f, -3.42f, 2.26f, -4.4f)
                curveToRelative(-0.44f, -0.06f, -0.9f, -0.1f, -1.36f, -0.1f)
                close()
            }
        }.build()
    }
}

@Composable
fun rememberSunIcon(): ImageVector {
    return remember {
        ImageVector.Builder(
            name = "Sun",
            defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 960f, viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(440f, 160f)
                lineToRelative(0f, -120f)
                lineToRelative(80f, 0f)
                lineToRelative(0f, 120f)
                lineToRelative(-80f, 0f)
                close()
                moveTo(440f, 920f)
                lineToRelative(0f, -120f)
                lineToRelative(80f, 0f)
                lineToRelative(0f, 120f)
                lineToRelative(-80f, 0f)
                close()
                moveTo(800f, 520f)
                lineToRelative(0f, -80f)
                lineToRelative(120f, 0f)
                lineToRelative(0f, 80f)
                lineToRelative(-120f, 0f)
                close()
                moveTo(40f, 520f)
                lineToRelative(0f, -80f)
                lineToRelative(120f, 0f)
                lineToRelative(0f, 80f)
                lineToRelative(-120f, 0f)
                close()
                moveTo(748f, 268f)
                lineToRelative(-56f, -56f)
                lineToRelative(70f, -72f)
                lineToRelative(58f, 58f)
                lineToRelative(-72f, 70f)
                close()
                moveTo(198f, 820f)
                lineToRelative(-58f, -58f)
                lineToRelative(72f, -70f)
                lineToRelative(56f, 56f)
                lineToRelative(-70f, 72f)
                close()
                moveTo(762f, 820f)
                lineToRelative(-70f, -72f)
                lineToRelative(56f, -56f)
                lineToRelative(72f, 70f)
                lineToRelative(-58f, 58f)
                close()
                moveTo(212f, 268f)
                lineToRelative(-72f, -70f)
                lineToRelative(58f, -58f)
                lineToRelative(70f, 72f)
                lineToRelative(-56f, 56f)
                close()
                moveTo(480f, 760f)
                quadToRelative(-117f, 0f, -198.5f, -81.5f)
                reflectiveQuadTo(200f, 480f)
                reflectiveQuadToRelative(81.5f, -198.5f)
                reflectiveQuadTo(480f, 200f)
                reflectiveQuadToRelative(198.5f, 81.5f)
                reflectiveQuadTo(760f, 480f)
                reflectiveQuadToRelative(-81.5f, 198.5f)
                reflectiveQuadTo(480f, 760f)
                close()
                moveTo(480f, 680f)
                quadToRelative(83f, 0f, 141.5f, -58.5f)
                reflectiveQuadTo(680f, 480f)
                reflectiveQuadToRelative(-58.5f, -141.5f)
                reflectiveQuadTo(480f, 280f)
                reflectiveQuadToRelative(-141.5f, 58.5f)
                reflectiveQuadTo(280f, 480f)
                reflectiveQuadToRelative(58.5f, 141.5f)
                reflectiveQuadTo(480f, 680f)
                close()
            }
        }.build()
    }
}
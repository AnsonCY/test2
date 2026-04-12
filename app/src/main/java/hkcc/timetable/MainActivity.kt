package hkcc.timetable

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import hkcc.timetable.ui.theme.MyApplicationTheme
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

    val appBarTitle = when (currentRoute) {
        "home" -> "Home"
        "upload_csv" -> "Upload CSV"
        "select_subjects" -> "Select Subjects"
        "auto_plan" -> "Auto Planner"
        "preview_timetable" -> "Preview Timetable"
        "academic" -> "Academic & GPA"
        "groupmate_finder" -> "Find Groupmates"
        "profile" -> "Profile"
        else -> "HKCC Planner"
    }

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
                    title = { Text(appBarTitle) },
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

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.auth.attemptAutoSignIn
import com.example.data.auth.authStateFlow
import com.example.data.auth.signOut
import com.example.data.firebase.configureAppCheck
import com.example.data.model.CanvasFormat
import com.example.data.model.StudioCategory
import com.example.ui.screens.AiLabScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CanvasEditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.TemplatesScreen
import com.example.ui.theme.CoralSpark
import com.example.ui.theme.PosterStudioTheme
import com.example.ui.viewmodel.StudioViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

const val ROUTE_HOME = "home"
const val ROUTE_AI_LAB = "ai_lab"
const val ROUTE_TEMPLATES = "templates"
const val ROUTE_PROJECTS = "projects"
const val ROUTE_EDITOR = "editor"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before Compose UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            PosterStudioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { },
                onUnauthenticated = { },
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { })
    } else {
        key(user.uid) {
            AuthenticatedStudioShell(
                user = user,
                onSignOut = {
                    signOut(credentialManager = credentialManager, scope = scope)
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedStudioShell(
    user: FirebaseUser,
    onSignOut: () -> Unit
) {
    val viewModel: StudioViewModel = viewModel(
        key = user.uid,
        factory = StudioViewModel.provideFactory(user.uid)
    )

    LaunchedEffect(user.uid) {
        viewModel.syncUserProfile(
            displayName = user.displayName ?: "Creator",
            email = user.email,
            photoUrl = user.photoUrl?.toString()
        )
    }

    val projectsState by viewModel.projectsState.collectAsStateWithLifecycle()
    val aiHistoryState by viewModel.aiHistoryState.collectAsStateWithLifecycle()
    val aiImageState by viewModel.aiImageState.collectAsStateWithLifecycle()
    val aiPhotoEditState by viewModel.aiPhotoEditState.collectAsStateWithLifecycle()
    val aiCopyState by viewModel.aiCopyState.collectAsStateWithLifecycle()
    val pickedPhoto by viewModel.pickedPhoto.collectAsStateWithLifecycle()
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_HOME
    var aiInitialMode by rememberSaveable { mutableIntStateOf(0) }

    val showBottomBar = currentRoute != ROUTE_EDITOR

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_HOME,
                        onClick = {
                            navController.navigate(ROUTE_HOME) {
                                popUpTo(ROUTE_HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == ROUTE_HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = stringResource(id = R.string.nav_home)
                            )
                        },
                        label = { Text(stringResource(id = R.string.nav_home)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CoralSpark,
                            selectedTextColor = CoralSpark,
                            indicatorColor = CoralSpark.copy(alpha = 0.16f)
                        ),
                        modifier = Modifier.testTag("nav_home_tab")
                    )
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_AI_LAB,
                        onClick = {
                            navController.navigate(ROUTE_AI_LAB) {
                                popUpTo(ROUTE_HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == ROUTE_AI_LAB) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                                contentDescription = stringResource(id = R.string.nav_ai_lab)
                            )
                        },
                        label = { Text(stringResource(id = R.string.nav_ai_lab)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CoralSpark,
                            selectedTextColor = CoralSpark,
                            indicatorColor = CoralSpark.copy(alpha = 0.16f)
                        ),
                        modifier = Modifier.testTag("nav_ai_lab_tab")
                    )
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_TEMPLATES,
                        onClick = {
                            navController.navigate(ROUTE_TEMPLATES) {
                                popUpTo(ROUTE_HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == ROUTE_TEMPLATES) Icons.Filled.GridView else Icons.Outlined.GridView,
                                contentDescription = stringResource(id = R.string.nav_templates)
                            )
                        },
                        label = { Text(stringResource(id = R.string.nav_templates)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CoralSpark,
                            selectedTextColor = CoralSpark,
                            indicatorColor = CoralSpark.copy(alpha = 0.16f)
                        ),
                        modifier = Modifier.testTag("nav_templates_tab")
                    )
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_PROJECTS,
                        onClick = {
                            navController.navigate(ROUTE_PROJECTS) {
                                popUpTo(ROUTE_HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == ROUTE_PROJECTS) Icons.Filled.Folder else Icons.Outlined.Folder,
                                contentDescription = stringResource(id = R.string.nav_projects)
                            )
                        },
                        label = { Text(stringResource(id = R.string.nav_projects)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CoralSpark,
                            selectedTextColor = CoralSpark,
                            indicatorColor = CoralSpark.copy(alpha = 0.16f)
                        ),
                        modifier = Modifier.testTag("nav_projects_tab")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = ROUTE_HOME
            ) {
                composable(ROUTE_HOME) {
                    HomeScreen(
                        userName = user.displayName ?: "Creator",
                        userPhotoUrl = user.photoUrl?.toString(),
                        projectsState = projectsState,
                        onSignOut = onSignOut,
                        onOpenAiPosterTab = {
                            aiInitialMode = 0
                            navController.navigate(ROUTE_AI_LAB)
                        },
                        onOpenAiPhotoTab = {
                            aiInitialMode = 1
                            navController.navigate(ROUTE_AI_LAB)
                        },
                        onCreateBlankFormat = { fmt ->
                            viewModel.openBlankFormat(fmt)
                            navController.navigate(ROUTE_EDITOR)
                        },
                        onOpenTemplate = { tpl ->
                            viewModel.openFromTemplate(tpl)
                            navController.navigate(ROUTE_EDITOR)
                        },
                        onOpenProject = { proj ->
                            viewModel.openExistingProject(proj)
                            navController.navigate(ROUTE_EDITOR)
                        },
                        onOpenPickedPhotoInEditor = { bmp ->
                            viewModel.openFromBitmap(
                                bitmap = bmp,
                                title = "Photo Edit",
                                format = CanvasFormat.POSTER_3_4,
                                category = StudioCategory.PHOTO_EDIT
                            )
                            navController.navigate(ROUTE_EDITOR)
                        },
                        onViewAllTemplates = { navController.navigate(ROUTE_TEMPLATES) },
                        onViewAllProjects = { navController.navigate(ROUTE_PROJECTS) }
                    )
                }

                composable(ROUTE_AI_LAB) {
                    AiLabScreen(
                        initialMode = aiInitialMode,
                        aiImageState = aiImageState,
                        aiPhotoEditState = aiPhotoEditState,
                        aiCopyState = aiCopyState,
                        pickedPhoto = pickedPhoto,
                        onSetPickedPhoto = viewModel::setPickedPhoto,
                        onGeneratePoster = viewModel::generatePosterOrBanner,
                        onApplyAiPhotoEdit = viewModel::applyAiPhotoEdit,
                        onGenerateCopy = viewModel::generateCopySuggestion,
                        onOpenGeneratedInEditor = { success ->
                            viewModel.openFromBitmap(
                                bitmap = success.bitmap,
                                title = success.prompt.take(36),
                                format = success.format,
                                category = if (success.isPhotoEdit) StudioCategory.PHOTO_EDIT else success.format.defaultCategory,
                                localPath = success.localPath
                            )
                            navController.navigate(ROUTE_EDITOR)
                        },
                        onSaveGeneratedToProjects = viewModel::saveAiResultAsProject,
                        onApplyCopyToNewPoster = { suggestion ->
                            viewModel.openBlankFormat(CanvasFormat.POSTER_3_4)
                            viewModel.applyCopySuggestionToEditor(suggestion)
                            navController.navigate(ROUTE_EDITOR)
                        },
                        onNotify = viewModel::notifyMessage
                    )
                }

                composable(ROUTE_TEMPLATES) {
                    TemplatesScreen(
                        onSelectTemplate = { tpl ->
                            viewModel.openFromTemplate(tpl)
                            navController.navigate(ROUTE_EDITOR)
                        }
                    )
                }

                composable(ROUTE_PROJECTS) {
                    ProjectsScreen(
                        projectsState = projectsState,
                        aiHistoryState = aiHistoryState,
                        onOpenProject = { proj ->
                            viewModel.openExistingProject(proj)
                            navController.navigate(ROUTE_EDITOR)
                        },
                        onToggleFavorite = viewModel::toggleFavorite,
                        onRenameProject = viewModel::renameProject,
                        onDeleteProject = viewModel::deleteProject,
                        onDeleteAiRecord = viewModel::deleteAiHistoryRecord,
                        onCreateNewPoster = {
                            viewModel.openBlankFormat(CanvasFormat.POSTER_3_4)
                            navController.navigate(ROUTE_EDITOR)
                        },
                        onOpenAiBitmapInEditor = { bmp, title, fmt ->
                            viewModel.openFromBitmap(
                                bitmap = bmp,
                                title = title,
                                format = fmt,
                                category = fmt.defaultCategory
                            )
                            navController.navigate(ROUTE_EDITOR)
                        }
                    )
                }

                composable(ROUTE_EDITOR) {
                    CanvasEditorScreen(
                        editorState = editorState,
                        aiCopyState = aiCopyState,
                        onBack = { navController.popBackStack() },
                        onUndo = viewModel::undo,
                        onRedo = viewModel::redo,
                        onToggleGrid = viewModel::toggleGrid,
                        onUpdateTitle = viewModel::updateProjectTitle,
                        onSelectLayer = viewModel::selectLayer,
                        onMoveLayer = viewModel::moveLayerPosition,
                        onSetFormat = viewModel::setCanvasFormat,
                        onSetGradient = viewModel::setGradientBackground,
                        onSetPhotoBitmap = viewModel::setEditorPhotoBitmap,
                        onSetFilterPreset = viewModel::setFilterPreset,
                        onUpdatePhotoAdjustments = viewModel::updatePhotoAdjustments,
                        onAddTextLayer = viewModel::addTextLayer,
                        onAddShapeLayer = viewModel::addShapeLayer,
                        onUpdateSelectedLayer = viewModel::updateSelectedLayer,
                        onDeleteSelectedLayer = viewModel::deleteSelectedLayer,
                        onGenerateInEditorCopy = viewModel::generateCopySuggestion,
                        onApplyCopySuggestion = viewModel::applyCopySuggestionToEditor,
                        onSaveToCloud = viewModel::saveActiveProject,
                        onNotify = viewModel::notifyMessage
                    )
                }
            }
        }
    }
}

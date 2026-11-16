package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.TeacherModule
import com.example.ui.components.AdminAllocationsSheet
import com.example.ui.components.BottomNavTab
import com.example.ui.components.ClassPickerSheet
import com.example.ui.components.ElimuBottomBar
import com.example.ui.components.ElimuTopChrome
import com.example.ui.components.PdfExportReadyDialog
import com.example.ui.components.ruledNotebookBackground
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.BroadsheetScreen
import com.example.ui.screens.FeePaymentsScreen
import com.example.ui.screens.FirstSignInLockScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.KicdDesignsScreen
import com.example.ui.screens.LearnerProgressScreen
import com.example.ui.screens.LearnersScreen
import com.example.ui.screens.LessonPlansScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MarksbookScreen
import com.example.ui.screens.MeScreen
import com.example.ui.screens.ObservationsScreen
import com.example.ui.screens.PathwaysScreen
import com.example.ui.screens.RecordsOfWorkScreen
import com.example.ui.screens.ReportCardsScreen
import com.example.ui.screens.SchemesScreen
import com.example.ui.screens.ServicesHubScreen
import com.example.ui.screens.TimetableScreen
import com.example.ui.screens.WhatsAppScreen
import com.example.ui.theme.ElimuTrackTheme
import com.example.ui.theme.LocalElimuColors
import com.example.ui.viewmodel.AppDestination
import com.example.ui.viewmodel.TeacherUiState
import com.example.ui.viewmodel.TeacherViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
    )
    val app = application as ElimuTrackApplication
    setContent {
      val vm: TeacherViewModel = viewModel(
        factory = TeacherViewModel.Factory(app.repository, app.pdfExportManager)
      )
      val state by vm.uiState.collectAsStateWithLifecycle()

      LaunchedEffect(state.darkMode) {
        val barStyle = if (state.darkMode) {
          SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        } else {
          SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
      }

      ElimuTrackTheme(darkTheme = state.darkMode) {
        ElimuTrackRoot(state = state, viewModel = vm)
      }
    }
  }
}

@Composable
fun ElimuTrackRoot(
  state: TeacherUiState,
  viewModel: TeacherViewModel,
) {
  val extra = LocalElimuColors.current
  val snackbarHostState = remember { SnackbarHostState() }
  var showClassPicker by remember { mutableStateOf(false) }
  var showAdminPreview by remember { mutableStateOf(false) }
  var openedFromServices by remember { mutableStateOf(false) }

  val savePdfLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/pdf"),
  ) { uri ->
    val pdf = state.exportedPdf
    if (uri != null && pdf != null) {
      viewModel.savePdfToUri(pdf, uri)
    }
  }

  LaunchedEffect(state.snackbarMessage) {
    val msg = state.snackbarMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearSnackbar()
    }
  }

  if (!state.isSessionLoaded) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
      Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = extra.navy)
      }
    }
    return
  }

  val session = state.session
  if (session == null) {
    LoginScreen(
      isLoggingIn = state.isLoggingIn,
      loginError = state.loginError,
      onBackendLogin = viewModel::loginWithBackend,
      onOfflineWorkspace = { teacherName, tscNumber, schoolName, backendUrl, grades, username ->
        viewModel.enterOfflineWorkspace(teacherName, tscNumber, schoolName, backendUrl, grades, username)
      },
    )
    return
  }

  val allowed = session.allowedModules
  val activeClass = session.allocatedClasses.firstOrNull { it.grade == state.selectedGrade }
  val selectedClassDisplay = activeClass?.displayName ?: state.selectedGrade

  if (state.destination == AppDestination.MODULE) {
    BackHandler {
      if (openedFromServices) {
        viewModel.openServices()
      } else {
        viewModel.openHome()
      }
    }
  } else if (state.destination == AppDestination.SERVICES || state.destination == AppDestination.ME) {
    BackHandler {
      viewModel.openHome()
    }
  }

  val activeBottomTab = when (state.destination) {
    AppDestination.HOME -> BottomNavTab.HOME
    AppDestination.SERVICES -> BottomNavTab.SERVICES
    AppDestination.ME -> BottomNavTab.ME
    AppDestination.MODULE -> when {
      state.activeModule == TeacherModule.ATTENDANCE && !openedFromServices -> BottomNavTab.REGISTER
      state.activeModule == TeacherModule.MARKSBOOK && !openedFromServices -> BottomNavTab.MARKS
      else -> BottomNavTab.SERVICES
    }
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      ElimuTopChrome(
        session = session,
        selectedClassDisplay = selectedClassDisplay,
        pendingSyncCount = state.syncStatus.pendingUploads,
        isSyncing = state.syncStatus.isSyncing,
        onSyncClick = viewModel::syncNow,
        onOpenClassPicker = { showClassPicker = true },
      )
    },
    bottomBar = {
      if (!session.mustChangePassword) {
        ElimuBottomBar(
          activeTab = activeBottomTab,
          showRegister = TeacherModule.ATTENDANCE in allowed,
          showMarks = TeacherModule.MARKSBOOK in allowed,
          onSelectTab = { tab ->
            when (tab) {
              BottomNavTab.HOME -> {
                openedFromServices = false
                viewModel.openHome()
              }
              BottomNavTab.REGISTER -> {
                openedFromServices = false
                viewModel.navigateToModule(TeacherModule.ATTENDANCE)
              }
              BottomNavTab.MARKS -> {
                openedFromServices = false
                viewModel.navigateToModule(TeacherModule.MARKSBOOK)
              }
              BottomNavTab.SERVICES -> {
                openedFromServices = false
                viewModel.openServices()
              }
              BottomNavTab.ME -> {
                openedFromServices = false
                viewModel.openMe()
              }
            }
          },
        )
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .ruledNotebookBackground(extra.ruleLine),
      contentAlignment = Alignment.TopCenter,
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 760.dp),
      ) {
        if (session.mustChangePassword) {
          FirstSignInLockScreen(
            defaultUsername = session.username,
            onSaveAndContinue = viewModel::updateLoginCredentials,
          )
        } else {
          when (state.destination) {
            AppDestination.HOME -> HomeScreen(
              state = state,
              onNavigateModule = { mod ->
                openedFromServices = mod != TeacherModule.ATTENDANCE && mod != TeacherModule.MARKSBOOK
                viewModel.navigateToModule(mod)
              },
              onSelectClassAndRegister = { grade ->
                viewModel.selectGrade(grade)
                openedFromServices = false
                viewModel.navigateToModule(TeacherModule.ATTENDANCE)
              },
              onOpenAdminPreview = { showAdminPreview = true },
            )

            AppDestination.SERVICES -> ServicesHubScreen(
              state = state,
              onNavigateModule = { mod ->
                openedFromServices = true
                viewModel.navigateToModule(mod)
              },
            )

            AppDestination.ME -> MeScreen(
              state = state,
              onToggleDarkMode = viewModel::toggleDarkMode,
              onUpdateLogin = viewModel::updateLoginCredentials,
              onOpenAdminPreview = { showAdminPreview = true },
              onSyncNow = viewModel::syncNow,
              onLogout = viewModel::logout,
            )

            AppDestination.MODULE -> ModuleRouter(
              state = state,
              viewModel = viewModel,
              showBackToServices = openedFromServices || (state.activeModule != TeacherModule.ATTENDANCE && state.activeModule != TeacherModule.MARKSBOOK),
              onBackToServices = { viewModel.openServices() },
              onNavigateFromModule = { mod ->
                openedFromServices = true
                viewModel.navigateToModule(mod)
              },
            )
          }
        }
      }
    }
  }

  if (showClassPicker) {
    val countByGrade = state.learners.groupBy { it.grade }.mapValues { it.value.size }
    ClassPickerSheet(
      allocatedClasses = session.allocatedClasses,
      currentGrade = state.selectedGrade,
      learnerCountByGrade = countByGrade,
      onSelectClass = { cls -> viewModel.selectGrade(cls.grade) },
      onDismiss = { showClassPicker = false },
    )
  }

  if (showAdminPreview) {
    AdminAllocationsSheet(
      session = session,
      onToggleGrade = viewModel::toggleAllocatedGrade,
      onUpdateModules = viewModel::updateModulePermissions,
      onToggleFirstSignIn = viewModel::setFirstSignInRequired,
      onDismiss = { showAdminPreview = false },
    )
  }

  state.exportedPdf?.let { pdf ->
    PdfExportReadyDialog(
      exported = pdf,
      onDismiss = viewModel::dismissPdfDialog,
      onOpenPdf = { viewModel.openPdf(pdf, shareOnly = false) },
      onSharePdf = { viewModel.openPdf(pdf, shareOnly = true) },
      onSaveCopy = { savePdfLauncher.launch(pdf.file.name) },
    )
  }
}

@Composable
private fun ModuleRouter(
  state: TeacherUiState,
  viewModel: TeacherViewModel,
  showBackToServices: Boolean,
  onBackToServices: () -> Unit,
  onNavigateFromModule: (TeacherModule) -> Unit,
) {
  val backCallback: (() -> Unit)? = if (showBackToServices) onBackToServices else null
  when (state.activeModule) {
    TeacherModule.DASHBOARD -> HomeScreen(
      state = state,
      onNavigateModule = onNavigateFromModule,
      onSelectClassAndRegister = { grade ->
        viewModel.selectGrade(grade)
        viewModel.navigateToModule(TeacherModule.ATTENDANCE)
      },
      onOpenAdminPreview = {},
    )
    TeacherModule.TIMETABLE -> TimetableScreen(
      state = state,
      onSelectGrade = viewModel::selectGrade,
      onUpdateSlot = viewModel::updateTimetableSlot,
      onResetGradeTimetable = viewModel::resetTimetableForGrade,
      onExportTimetablePdf = viewModel::exportTimetablePdf,
      onBackToServices = onBackToServices,
    )
    TeacherModule.SCHEMES -> SchemesScreen(
      state = state,
      onSelectGrade = viewModel::selectGrade,
      onSaveScheme = viewModel::saveScheme,
      onDeleteScheme = viewModel::deleteScheme,
      onExportPdf = viewModel::exportSchemePdf,
      onPlanLessonRow = viewModel::planFromSchemeRow,
      onClearKicdPrefill = viewModel::clearPendingKicdPrefill,
      onBackToServices = onBackToServices,
    )
    TeacherModule.LESSON_PLANS -> LessonPlansScreen(
      state = state,
      onSavePlan = viewModel::saveLessonPlan,
      onDeletePlan = viewModel::deleteLessonPlan,
      onExportPdf = viewModel::exportLessonPlanPdf,
      onClearKicdPrefill = viewModel::clearPendingKicdPrefill,
      onClearPlanPrefill = viewModel::clearPendingPlanPrefill,
      onBackToServices = onBackToServices,
    )
    TeacherModule.RECORD_OF_WORK -> RecordsOfWorkScreen(
      state = state,
      onSaveRecord = viewModel::saveRecordOfWork,
      onDeleteRecord = viewModel::deleteRecordOfWork,
      onExportPdf = viewModel::exportRecordsPdf,
      onBackToServices = onBackToServices,
    )
    TeacherModule.ATTENDANCE -> AttendanceScreen(
      state = state,
      onSaveAttendance = viewModel::saveAttendance,
      onPopulateStarterRoster = viewModel::populateStarterRoster,
      onBackToServices = backCallback,
    )
    TeacherModule.MARKSBOOK -> MarksbookScreen(
      state = state,
      onSaveMarksBatch = viewModel::saveMarksBatch,
      onPopulateStarterRoster = viewModel::populateStarterRoster,
      onBackToServices = backCallback,
    )
    TeacherModule.PROGRESS -> LearnerProgressScreen(
      state = state,
      onSaveProgress = viewModel::saveProgressEntry,
      onExportLearnerPdf = viewModel::exportStudentProgressPdf,
      onPopulateStarterRoster = viewModel::populateStarterRoster,
      onBackToServices = onBackToServices,
    )
    TeacherModule.REPORT_CARDS -> ReportCardsScreen(
      state = state,
      onExportLearnerPdf = viewModel::exportStudentProgressPdf,
      onShowMessage = viewModel::showMessage,
      onPopulateStarterRoster = viewModel::populateStarterRoster,
      onBackToServices = onBackToServices,
    )
    TeacherModule.BROADSHEET -> BroadsheetScreen(
      state = state,
      onPopulateStarterRoster = viewModel::populateStarterRoster,
      onShowMessage = viewModel::showMessage,
      onBackToServices = onBackToServices,
    )
    TeacherModule.KICD_DESIGNS -> KicdDesignsScreen(
      state = state,
      onSelectGrade = viewModel::selectGrade,
      onUseInScheme = viewModel::useKicdInScheme,
      onUseInLessonPlan = viewModel::useKicdInLessonPlan,
      onBackToServices = onBackToServices,
    )
    TeacherModule.LEARNERS -> LearnersScreen(
      state = state,
      onAddLearner = viewModel::addLearner,
      onPopulateStarterRoster = viewModel::populateStarterRoster,
      onNavigateModule = onNavigateFromModule,
      onExportLearnerPdf = viewModel::exportStudentProgressPdf,
      onBackToServices = onBackToServices,
    )
    TeacherModule.OBSERVATIONS -> ObservationsScreen(
      state = state,
      onSaveObservation = viewModel::saveObservation,
      onDeleteObservation = viewModel::deleteObservation,
      onPopulateStarterRoster = viewModel::populateStarterRoster,
      onBackToServices = onBackToServices,
    )
    TeacherModule.PATHWAYS -> PathwaysScreen(
      state = state,
      onBackToServices = onBackToServices,
    )
    TeacherModule.FEE_PAYMENTS -> FeePaymentsScreen(
      state = state,
      onPostFeePayment = viewModel::postFeePayment,
      onPopulateStarterRoster = viewModel::populateStarterRoster,
      onBackToServices = onBackToServices,
    )
    TeacherModule.WHATSAPP -> WhatsAppScreen(
      state = state,
      onSaveGuardianMessage = viewModel::saveGuardianMessage,
      onPopulateStarterRoster = viewModel::populateStarterRoster,
      onBackToServices = onBackToServices,
    )
  }
}

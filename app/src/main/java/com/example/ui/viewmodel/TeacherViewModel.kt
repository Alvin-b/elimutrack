package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.curriculum.KicdCurriculumRepository
import com.example.data.curriculum.BundledCurriculum
import com.example.data.remote.ApiException
import com.example.data.repository.TeacherRepository
import com.example.domain.model.AllocatedClass
import com.example.domain.model.ApprovedResource
import com.example.domain.model.AttendanceRecord
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.CbcPerformanceLevel
import com.example.domain.model.DashboardSummary
import com.example.domain.model.FeePaymentRecord
import com.example.domain.model.GuardianMessage
import com.example.domain.model.KicdDesignItem
import com.example.domain.model.Learner
import com.example.domain.model.LearnerProgressEntry
import com.example.domain.model.LessonPlan
import com.example.domain.model.LessonPlanPrefill
import com.example.domain.model.LessonPlanTemplateSection
import com.example.domain.model.MarkEntry
import com.example.domain.model.ObservationNote
import com.example.domain.model.RecordOfWork
import com.example.domain.model.RecordOfWorkPrefill
import com.example.domain.model.SchemeOfWork
import com.example.domain.model.SchemeRow
import com.example.domain.model.SyncStatus
import com.example.domain.model.TeacherModule
import com.example.domain.model.TeacherSession
import com.example.domain.model.TimetableSlot
import com.example.pdf.ExportedPdf
import com.example.pdf.PdfExportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class AppDestination {
  HOME,
  SERVICES,
  ME,
  MODULE,
}

data class TeacherUiState(
  val isSessionLoaded: Boolean = false,
  val session: TeacherSession? = null,
  val destination: AppDestination = AppDestination.HOME,
  val activeModule: TeacherModule = TeacherModule.DASHBOARD,
  val selectedStream: String = "",
  val selectedGrade: String = "Grade 7",
  val darkMode: Boolean = true,
  val dashboardSummary: DashboardSummary? = null,
  val lessonPlanTemplate: List<LessonPlanTemplateSection> = emptyList(),
  val remoteKicdDesigns: List<KicdDesignItem> = emptyList(),
  val remoteResources: List<ApprovedResource> = emptyList(),
  val feePayments: List<FeePaymentRecord> = emptyList(),
  val schemes: List<SchemeOfWork> = emptyList(),
  val bundledKicdDesigns: List<KicdDesignItem> = emptyList(),
  val lessonPlans: List<LessonPlan> = emptyList(),
  val recordsOfWork: List<RecordOfWork> = emptyList(),
  val learners: List<Learner> = emptyList(),
  val attendance: List<AttendanceRecord> = emptyList(),
  val marks: List<MarkEntry> = emptyList(),
  val observations: List<ObservationNote> = emptyList(),
  val progressEntries: List<LearnerProgressEntry> = emptyList(),
  val guardianMessages: List<GuardianMessage> = emptyList(),
  val timetable: List<TimetableSlot> = emptyList(),
  val syncStatus: SyncStatus = SyncStatus(),
  val isLoggingIn: Boolean = false,
  val loginError: String? = null,
  val snackbarMessage: String? = null,
  val exportedPdf: ExportedPdf? = null,
  val exportedCsv: Pair<String, String>? = null,
  val pendingKicdPrefill: KicdDesignItem? = null,
  val pendingPlanPrefill: LessonPlanPrefill? = null,
  val pendingRecordPrefill: RecordOfWorkPrefill? = null,
) {
  val activeClass: AllocatedClass?
    get() = session?.allocatedClasses?.firstOrNull {
      it.streamKey.equals(selectedStream, ignoreCase = true) ||
        it.displayName.equals(selectedStream, ignoreCase = true)
    } ?: session?.allocatedClasses?.firstOrNull()

  fun matchesActiveClass(streamValue: String, gradeValue: String): Boolean {
    val active = activeClass
    val itemNormStream = normClass(streamValue)
    val itemNormGrade = (KicdCurriculumRepository.normalizeGrade(gradeValue)
      ?: KicdCurriculumRepository.extractGradeOrNull(streamValue)
      ?: gradeValue).trim()

    if (active != null) {
      val activeNormGrade = (KicdCurriculumRepository.normalizeGrade(active.grade) ?: active.grade).trim()
      if (itemNormStream.isNotBlank()) {
        if (itemNormStream == normClass(active.streamKey) || itemNormStream == normClass(active.displayName)) {
          return true
        }
        if (active.stream.isNotBlank()) {
          val activeSuffix = normClass(active.stream)
          if (itemNormStream == activeSuffix &&
            (itemNormGrade.isBlank() || itemNormGrade.equals(activeNormGrade, ignoreCase = true))
          ) {
            return true
          }
          // Handle cases where streamValue is "East" or "Grade 4 - East" vs active "Grade 4 East"
          if (itemNormStream.endsWith(" $activeSuffix") &&
            (itemNormGrade.isBlank() || itemNormGrade.equals(activeNormGrade, ignoreCase = true))
          ) {
            return true
          }
        } else {
          // Assigned class has no stream suffix (e.g. "Grade 7"): match any learner/record in that grade
          if (itemNormGrade.equals(activeNormGrade, ignoreCase = true) ||
            itemNormStream.startsWith(normClass(activeNormGrade))
          ) {
            return true
          }
        }
        return false
      }
      return itemNormGrade.equals(activeNormGrade, ignoreCase = true)
    }

    if (selectedStream.isBlank() && session?.allocatedClasses.isNullOrEmpty()) {
      return true
    }
    if (selectedStream.isNotBlank() && itemNormStream.isNotBlank()) {
      if (itemNormStream == normClass(selectedStream)) return true
    }
    val selectedNormGrade = (KicdCurriculumRepository.normalizeGrade(selectedGrade) ?: selectedGrade).trim()
    return itemNormGrade.equals(selectedNormGrade, ignoreCase = true)
  }

  private fun normClass(raw: String): String =
    raw.trim().lowercase().replace(Regex("[\\s\\-_]+"), " ")
}

class TeacherViewModel(
  private val repository: TeacherRepository,
  private val pdfExportManager: PdfExportManager,
) : ViewModel() {

  private val destinationFlow = MutableStateFlow(AppDestination.HOME)
  private val activeModuleFlow = MutableStateFlow(TeacherModule.DASHBOARD)
  private val selectedStreamFlow = MutableStateFlow("")
  private val syncStatusFlow = MutableStateFlow(SyncStatus())
  private val isLoggingInFlow = MutableStateFlow(false)
  private val loginErrorFlow = MutableStateFlow<String?>(null)
  private val snackbarFlow = MutableStateFlow<String?>(null)
  private val exportedPdfFlow = MutableStateFlow<ExportedPdf?>(null)
  private val exportedCsvFlow = MutableStateFlow<Pair<String, String>?>(null)
  private val pendingKicdFlow = MutableStateFlow<KicdDesignItem?>(null)
  private val pendingPlanFlow = MutableStateFlow<LessonPlanPrefill?>(null)
  private val pendingRecordFlow = MutableStateFlow<RecordOfWorkPrefill?>(null)
  private val sessionLoadedFlow = MutableStateFlow(false)

  private val dashboardSummaryFlow = MutableStateFlow<DashboardSummary?>(null)
  private val lessonPlanTemplateFlow = MutableStateFlow<List<LessonPlanTemplateSection>>(emptyList())
  private val remoteKicdDesignsFlow = MutableStateFlow<List<KicdDesignItem>>(emptyList())
  private val remoteResourcesFlow = MutableStateFlow<List<ApprovedResource>>(emptyList())
  private val feePaymentsFlow = MutableStateFlow<List<FeePaymentRecord>>(emptyList())

  private var initialRefreshDone = false

  init {
    viewModelScope.launch {
      val verifyResult = repository.verifySessionOnLaunch()
      sessionLoadedFlow.value = true
      verifyResult.onSuccess { session ->
        if (session != null) {
          ensureValidSelectedStream(session)
          if (!session.mustChangePassword && !initialRefreshDone) {
            initialRefreshDone = true
            refreshActiveStream()
          }
        }
      }.onFailure { err ->
        if (err is ApiException && (err.httpCode == 401 || err.code == "UNAUTHENTICATED")) {
          loginErrorFlow.value = err.message
        }
      }
    }

    viewModelScope.launch {
      repository.sessionFlow.collect { session ->
        sessionLoadedFlow.value = true
        if (session != null) {
          ensureValidSelectedStream(session)
          if (destinationFlow.value == AppDestination.MODULE &&
            activeModuleFlow.value != TeacherModule.DASHBOARD &&
            activeModuleFlow.value !in session.allowedModules
          ) {
            destinationFlow.value = AppDestination.HOME
            activeModuleFlow.value = TeacherModule.DASHBOARD
          }
        } else {
          dashboardSummaryFlow.value = null
          lessonPlanTemplateFlow.value = emptyList()
          feePaymentsFlow.value = emptyList()
          selectedStreamFlow.value = ""
          initialRefreshDone = false
        }
      }
    }

    viewModelScope.launch {
      repository.lessonPlanTemplateFlow.collect { template ->
        if (template.isNotEmpty()) lessonPlanTemplateFlow.value = template
      }
    }
  }

  private fun ensureValidSelectedStream(session: TeacherSession) {
    val classes = session.allocatedClasses
    if (classes.isEmpty()) return
    val current = selectedStreamFlow.value
    val match = classes.firstOrNull {
      it.streamKey.equals(current, ignoreCase = true) ||
        it.displayName.equals(current, ignoreCase = true)
    }
    if (match == null) {
      selectedStreamFlow.value = classes.first().streamKey
    }
  }

  private val coreDataFlow = combine(
    repository.sessionFlow,
    repository.darkModeFlow,
    repository.schemesFlow,
    repository.lessonPlansFlow,
    repository.recordsOfWorkFlow,
  ) { session, dark, schemes, plans, records ->
    CoreDataBundle(session, dark, schemes, plans, records)
  }

  private val classDataFlow = combine(
    repository.learnersFlow,
    repository.attendanceFlow,
    repository.marksFlow,
    repository.observationsFlow,
    repository.timetableFlow,
  ) { learners, attendance, marks, observations, timetable ->
    ClassDataBundle(learners, attendance, marks, observations, timetable)
  }

  private val commDataFlow = combine(
    repository.progressEntriesFlow,
    repository.guardianMessagesFlow,
  ) { prog, msgs ->
    prog to msgs
  }

  private val uiControlFlow = combine(
    destinationFlow,
    activeModuleFlow,
    selectedStreamFlow,
    syncStatusFlow,
    isLoggingInFlow,
  ) { dest, module, stream, sync, loggingIn ->
    UiControlBundle(dest, module, stream, sync, loggingIn)
  }

  private val extrasFlow = combine(
    sessionLoadedFlow,
    loginErrorFlow,
    snackbarFlow,
    exportedPdfFlow,
    repository.lastSyncedAtFlow,
  ) { loaded, loginErr, snack, pdf, lastSync ->
    ExtrasBundle(loaded, loginErr, snack, pdf, lastSync)
  }

  private val remoteExtrasFlow = combine(
    combine(dashboardSummaryFlow, lessonPlanTemplateFlow) { summary, template -> summary to template },
    combine(remoteKicdDesignsFlow, remoteResourcesFlow, feePaymentsFlow) { kicd, resources, payments ->
      Triple(kicd, resources, payments)
    },
    BundledCurriculum.designsFlow,
  ) { (summary, template), (kicd, resources, payments), bundled ->
    RemoteExtrasBundle(summary, template, kicd, resources, payments, bundled)
  }

  private val prefillsFlow = combine(pendingKicdFlow, pendingPlanFlow, pendingRecordFlow, exportedCsvFlow) { k, p, r, csv ->
    PrefillsBundle(k, p, r, csv)
  }

  val uiState: StateFlow<TeacherUiState> = combine(
    combine(coreDataFlow, classDataFlow, commDataFlow) { core, cls, comm -> Triple(core, cls, comm) },
    combine(uiControlFlow, extrasFlow, remoteExtrasFlow, prefillsFlow) { ctrl, ext, rem, pre ->
      UiTuple(ctrl, ext, rem, pre)
    },
  ) { dataTriple, uiTuple ->
    val (core, cls, comm) = dataTriple
    val ctrl = uiTuple.ctrl
    val ext = uiTuple.ext
    val rem = uiTuple.rem
    val pre = uiTuple.pre

    val session = core.session
    val resolvedStream = session?.allocatedClasses?.firstOrNull {
      it.streamKey.equals(ctrl.stream, ignoreCase = true) ||
        it.displayName.equals(ctrl.stream, ignoreCase = true)
    }?.streamKey
      ?: session?.allocatedClasses?.firstOrNull()?.streamKey
      ?: ctrl.stream.ifBlank { cls.learners.firstOrNull()?.stream.orEmpty() }
    val resolvedGrade = session?.allocatedClasses?.firstOrNull {
      it.streamKey.equals(resolvedStream, ignoreCase = true)
    }?.grade
      ?: cls.learners.firstOrNull {
        resolvedStream.isNotBlank() && it.stream.equals(resolvedStream, ignoreCase = true)
      }?.grade?.let { KicdCurriculumRepository.normalizeGrade(it) ?: it }
      ?: KicdCurriculumRepository.extractGradeOrNull(resolvedStream)
      ?: cls.learners.firstOrNull()?.grade?.let { KicdCurriculumRepository.normalizeGrade(it) ?: it }
      ?: "Grade 7"

    val pendingUploads = core.schemes.count { !it.synced } +
      core.plans.count { !it.synced } +
      core.records.count { !it.synced } +
      cls.attendance.count { !it.synced } +
      cls.marks.count { !it.synced } +
      cls.observations.count { !it.synced }

    TeacherUiState(
      isSessionLoaded = ext.loaded,
      session = session,
      destination = ctrl.dest,
      activeModule = ctrl.module,
      selectedStream = resolvedStream,
      selectedGrade = resolvedGrade,
      darkMode = core.dark,
      dashboardSummary = rem.summary,
      lessonPlanTemplate = rem.template,
      remoteKicdDesigns = rem.kicd,
      remoteResources = rem.resources,
      feePayments = rem.payments,
      schemes = core.schemes,
      bundledKicdDesigns = rem.bundledKicd,
      lessonPlans = core.plans,
      recordsOfWork = core.records,
      learners = cls.learners,
      attendance = cls.attendance,
      marks = cls.marks,
      observations = cls.observations,
      progressEntries = comm.first,
      guardianMessages = comm.second,
      timetable = cls.timetable,
      syncStatus = ctrl.sync.copy(
        lastSyncedAt = ext.lastSync ?: ctrl.sync.lastSyncedAt,
        pendingUploads = pendingUploads,
      ),
      isLoggingIn = ctrl.loggingIn,
      loginError = ext.loginErr,
      snackbarMessage = ext.snack,
      exportedPdf = ext.pdf,
      exportedCsv = pre.csv,
      pendingKicdPrefill = pre.kicd,
      pendingPlanPrefill = pre.plan,
      pendingRecordPrefill = pre.record,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = TeacherUiState(),
  )

  fun openHome() {
    destinationFlow.value = AppDestination.HOME
    activeModuleFlow.value = TeacherModule.DASHBOARD
  }

  fun openServices() {
    destinationFlow.value = AppDestination.SERVICES
  }

  fun openMe() {
    destinationFlow.value = AppDestination.ME
  }

  fun navigateToModule(module: TeacherModule) {
    if (module == TeacherModule.DASHBOARD) {
      openHome()
      return
    }
    val allowed = uiState.value.session?.allowedModules ?: emptySet()
    if (module in allowed) {
      activeModuleFlow.value = module
      destinationFlow.value = AppDestination.MODULE
      refreshModuleExtrasIfNeeded(module)
    } else {
      snackbarFlow.value = "${module.label} is not enabled for your account."
    }
  }

  private fun refreshModuleExtrasIfNeeded(module: TeacherModule) {
    val stream = uiState.value.selectedStream
    viewModelScope.launch {
      if (module in TeacherModule.rosterDependentModules && uiState.value.learners.isEmpty()) {
        repository.fetchLearners(stream)
      }
      when (module) {
        TeacherModule.LEARNERS -> {
          repository.fetchLearners(stream).onFailure { handleApiFailure(it, silent = true, deniedModule = module) }
        }
        TeacherModule.ATTENDANCE -> {
          repository.fetchLearners(stream)
          val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
          repository.fetchAttendance(stream, today).onFailure { handleApiFailure(it, silent = true, deniedModule = module) }
        }
        TeacherModule.MARKSBOOK, TeacherModule.REPORT_CARDS, TeacherModule.BROADSHEET, TeacherModule.PATHWAYS -> {
          repository.fetchMarks(stream).onFailure { handleApiFailure(it, silent = true, deniedModule = TeacherModule.MARKSBOOK) }
        }
        TeacherModule.LESSON_PLANS -> {
          repository.fetchLessonPlans(stream).onSuccess { (tpl, _) ->
            if (tpl.isNotEmpty()) lessonPlanTemplateFlow.value = tpl
          }.onFailure { handleApiFailure(it, silent = true, deniedModule = module) }
        }
        TeacherModule.FEE_PAYMENTS -> {
          repository.fetchPayments(stream).onSuccess {
            feePaymentsFlow.value = it
          }.onFailure { handleApiFailure(it, silent = true, deniedModule = module) }
        }
        TeacherModule.WHATSAPP -> {
          repository.fetchWhatsApp().onFailure { handleApiFailure(it, silent = true, deniedModule = module) }
        }
        TeacherModule.TIMETABLE -> {
          repository.fetchTimetable().onFailure { handleApiFailure(it, silent = true, deniedModule = module) }
        }
        TeacherModule.SCHEMES -> {
          repository.fetchSchemes(stream).onFailure { handleApiFailure(it, silent = true, deniedModule = module) }
        }
        TeacherModule.RECORD_OF_WORK -> {
          repository.fetchRecordWork(stream).onFailure { handleApiFailure(it, silent = true, deniedModule = module) }
        }
        TeacherModule.PROGRESS -> {
          repository.fetchProgress(stream).onFailure { handleApiFailure(it, silent = true, deniedModule = module) }
        }
        TeacherModule.OBSERVATIONS -> {
          repository.fetchObservations(stream).onFailure { handleApiFailure(it, silent = true, deniedModule = module) }
        }
        else -> Unit
      }
    }
  }

  fun selectStream(streamKey: String) {
    val session = uiState.value.session ?: return
    val matched = session.allocatedClasses.firstOrNull {
      it.streamKey.equals(streamKey, ignoreCase = true) ||
        it.displayName.equals(streamKey, ignoreCase = true)
    } ?: return
    selectedStreamFlow.value = matched.streamKey
    if (!session.mustChangePassword) {
      refreshActiveStream()
    }
  }

  fun selectGrade(grade: String) {
    val session = uiState.value.session ?: return
    val matched = session.allocatedClasses.firstOrNull { it.grade.equals(grade, ignoreCase = true) }
    if (matched != null) {
      selectStream(matched.streamKey)
    }
  }

  fun clearSnackbar() {
    snackbarFlow.value = null
  }

  fun clearLoginError() {
    loginErrorFlow.value = null
  }

  fun showMessage(message: String) {
    snackbarFlow.value = message
  }

  fun dismissPdfDialog() {
    exportedPdfFlow.value = null
  }

  fun clearExportedCsv() {
    exportedCsvFlow.value = null
  }

  fun clearPendingKicdPrefill() {
    pendingKicdFlow.value = null
  }

  fun clearPendingPlanPrefill() {
    pendingPlanFlow.value = null
  }

  fun clearPendingRecordPrefill() {
    pendingRecordFlow.value = null
  }

  fun useKicdInScheme(design: KicdDesignItem) {
    val allowed = uiState.value.session?.allowedModules ?: emptySet()
    if (TeacherModule.SCHEMES !in allowed) {
      snackbarFlow.value = "Schemes of work is not enabled for your account."
      return
    }
    pendingKicdFlow.value = design
    navigateToModule(TeacherModule.SCHEMES)
  }

  fun useKicdInLessonPlan(design: KicdDesignItem) {
    val allowed = uiState.value.session?.allowedModules ?: emptySet()
    if (TeacherModule.LESSON_PLANS !in allowed) {
      snackbarFlow.value = "Lesson plans is not enabled for your account."
      return
    }
    pendingKicdFlow.value = design
    navigateToModule(TeacherModule.LESSON_PLANS)
  }

  fun planFromSchemeRow(topic: String, outcomes: String) {
    val allowed = uiState.value.session?.allowedModules ?: emptySet()
    if (TeacherModule.LESSON_PLANS !in allowed) {
      snackbarFlow.value = "Lesson plans is not enabled for your account."
      return
    }
    pendingPlanFlow.value = LessonPlanPrefill(subStrand = topic, outcomes = outcomes)
    navigateToModule(TeacherModule.LESSON_PLANS)
  }

  /** Carries the exact scheme lesson into a new lesson plan, offline included. */
  fun planFromSavedScheme(scheme: SchemeOfWork, row: SchemeRow) {
    val allowed = uiState.value.session?.allowedModules ?: emptySet()
    if (TeacherModule.LESSON_PLANS !in allowed) {
      snackbarFlow.value = "Lesson plans is not enabled for your account."
      return
    }
    pendingPlanFlow.value = LessonPlanPrefill(
      schemeId = row.remoteId.orEmpty(),
      schemeLocalId = scheme.id,
      learningArea = scheme.learningArea,
      term = scheme.term,
      week = row.week,
      lesson = row.lesson,
      strand = row.strand,
      subStrand = row.subStrand,
      outcomes = row.specificOutcomes,
      keyInquiryQuestion = row.keyInquiryQuestion,
      learningExperiences = row.learningExperiences,
      learningResources = row.learningResources,
    )
    navigateToModule(TeacherModule.LESSON_PLANS)
  }

  /** Turns a saved lesson plan into a record of work with the same links. */
  fun recordFromLessonPlan(plan: LessonPlan) {
    val allowed = uiState.value.session?.allowedModules ?: emptySet()
    if (TeacherModule.RECORD_OF_WORK !in allowed) {
      snackbarFlow.value = "Record of work is not enabled for your account."
      return
    }
    val topic = plan.topic.ifBlank { plan.subStrand }
    pendingRecordFlow.value = RecordOfWorkPrefill(
      schemeId = plan.schemeId,
      schemeLocalId = plan.schemeLocalId,
      lessonPlanId = plan.remoteId,
      lessonPlanLocalId = plan.id,
      learningArea = plan.learningArea,
      term = plan.term,
      week = plan.week,
      lesson = plan.lessonNumber,
      strand = plan.strand,
      subStrand = plan.subStrand,
      topic = topic,
      workCovered = topic,
      classSize = plan.rollCount,
    )
    navigateToModule(TeacherModule.RECORD_OF_WORK)
  }

  // --- Authentication & Session Management ---

  fun login(username: String, password: String) {
    loginWithBackend(TeacherSession.DEFAULT_BACKEND_URL, username, password)
  }

  fun loginWithBackend(backendUrl: String, username: String, password: String) {
    if (username.isBlank() || password.isBlank()) {
      loginErrorFlow.value = "Enter your username and password."
      return
    }
    viewModelScope.launch {
      isLoggingInFlow.value = true
      loginErrorFlow.value = null
      val result = repository.loginToBackend(backendUrl, username, password)
      isLoggingInFlow.value = false
      result.onFailure { err ->
        loginErrorFlow.value = err.message ?: "Unable to sign in. Check your connection and try again."
      }.onSuccess { session ->
        ensureValidSelectedStream(session)
        snackbarFlow.value = "Signed in as ${session.fullName}"
        if (!session.mustChangePassword) {
          openHome()
          initialRefreshDone = true
          refreshActiveStream()
        }
      }
    }
  }

  fun changeLogin(username: String, currentPassword: String, newPassword: String) {
    if (currentPassword.isBlank()) {
      snackbarFlow.value = "Enter your current password."
      return
    }
    if (newPassword.length < 6) {
      snackbarFlow.value = "Use a new password of at least 6 characters."
      return
    }
    viewModelScope.launch {
      isLoggingInFlow.value = true
      val res = repository.changeLogin(username, currentPassword, newPassword)
      isLoggingInFlow.value = false
      res.onSuccess { updated ->
        ensureValidSelectedStream(updated)
        openHome()
        snackbarFlow.value = "Login updated"
        initialRefreshDone = true
        refreshActiveStream()
      }.onFailure { err ->
        handleApiFailure(err)
      }
    }
  }

  fun saveServerUrl(rawUrl: String) {
    if (rawUrl.isBlank()) {
      snackbarFlow.value = "Enter a valid server URL."
      return
    }
    viewModelScope.launch {
      repository.saveBackendUrl(rawUrl)
      snackbarFlow.value = "Server URL updated"
      refreshActiveStream()
    }
  }

  fun syncNow() {
    refreshActiveStream(showToast = true)
  }

  fun refreshActiveStream(showToast: Boolean = false) {
    val initialSession = uiState.value.session ?: return
    if (initialSession.mustChangePassword) return
    viewModelScope.launch {
      syncStatusFlow.value = syncStatusFlow.value.copy(isSyncing = true, isError = false)
      val activeSession = if (showToast) {
        repository.verifySessionOnLaunch().getOrElse { err ->
          syncStatusFlow.value = syncStatusFlow.value.copy(isSyncing = false, lastMessage = err.message, isError = true)
          handleApiFailure(err)
          return@launch
        } ?: initialSession
      } else {
        initialSession
      }
      if (activeSession.mustChangePassword) {
        syncStatusFlow.value = syncStatusFlow.value.copy(isSyncing = false)
        return@launch
      }
      ensureValidSelectedStream(activeSession)
      val stream = selectedStreamFlow.value.ifBlank {
        activeSession.allocatedClasses.firstOrNull()?.streamKey
          ?: uiState.value.selectedStream
      }
      val res = repository.refreshAllForStream(stream)
      res.onSuccess { summary ->
        if (summary != null) {
          dashboardSummaryFlow.value = summary
        }
        val allowed = activeSession.allowedModules
        if (TeacherModule.LESSON_PLANS in allowed) {
          repository.fetchLessonPlans(stream).onSuccess { (tpl, _) ->
            if (tpl.isNotEmpty()) lessonPlanTemplateFlow.value = tpl
          }
        }
        if (TeacherModule.FEE_PAYMENTS in allowed) {
          repository.fetchPayments(stream).onSuccess {
            feePaymentsFlow.value = it
          }
        }
        if (TeacherModule.WHATSAPP in allowed) {
          repository.fetchWhatsApp()
        }
        val label = stream.ifBlank { "account" }
        syncStatusFlow.value = syncStatusFlow.value.copy(
          isSyncing = false,
          lastMessage = "Synced $label",
          isError = false,
        )
        if (showToast) {
          snackbarFlow.value = "Synced $label"
        }
      }.onFailure { err ->
        syncStatusFlow.value = syncStatusFlow.value.copy(
          isSyncing = false,
          lastMessage = err.message,
          isError = true,
        )
        handleApiFailure(err)
      }
    }
  }

  fun fetchAttendanceForDay(day: String) {
    val stream = uiState.value.selectedStream
    viewModelScope.launch {
      repository.fetchAttendance(stream, day).onFailure {
        handleApiFailure(it, silent = true, deniedModule = TeacherModule.ATTENDANCE)
      }
    }
  }

  fun loadKicdAndResources(grade: String, subjectId: String) {
    viewModelScope.launch {
      repository.fetchKicdAndResources(grade, subjectId).onSuccess { (designs, resources) ->
        remoteKicdDesignsFlow.value = designs
        remoteResourcesFlow.value = resources
      }.onFailure {
        handleApiFailure(it, silent = true, deniedModule = TeacherModule.KICD_DESIGNS)
      }
    }
  }

  fun toggleDarkMode(enabled: Boolean) {
    viewModelScope.launch { repository.setDarkMode(enabled) }
  }

  fun logout() {
    viewModelScope.launch {
      repository.logout()
      openHome()
    }
  }

  // --- Remote CRUD Operations ---

  fun saveScheme(
    existingId: String?,
    title: String,
    grade: String,
    learningArea: String,
    term: String,
    year: Int,
    rows: List<SchemeRow>,
  ) {
    val stream = uiState.value.selectedStream.ifBlank {
      uiState.value.learners.firstOrNull()?.stream.orEmpty()
    }
    if (stream.isBlank()) {
      snackbarFlow.value = "No class stream assigned to your account yet."
      return
    }
    val subjectId = KicdCurriculumRepository.subjectIdForArea(grade, learningArea)
    val schemeId = existingId ?: "sch_${stream}_${learningArea}_${term}".lowercase().replace(" ", "_")
    val session = uiState.value.session
    val localScheme = SchemeOfWork(
      id = schemeId,
      title = title.ifBlank { "$stream · $learningArea ($term)" },
      schoolName = session?.schoolName.orEmpty(),
      teacherName = session?.fullName.orEmpty(),
      grade = grade,
      stream = stream,
      learningArea = learningArea,
      term = term,
      year = year,
      rows = rows,
      updatedAt = System.currentTimeMillis(),
      synced = false,
    )
    viewModelScope.launch {
      repository.saveScheme(localScheme)
      repository.saveSchemesBulkRemote(stream, grade, subjectId, term, rows)
        .onSuccess {
          repository.markSchemeSynced(schemeId)
          snackbarFlow.value = "Scheme saved and sent to the school server"
        }.onFailure { err ->
          if (err is ApiException) {
            handleApiFailure(err, deniedModule = TeacherModule.SCHEMES)
          } else {
            snackbarFlow.value = "Scheme saved offline. It will sync when you are back online."
          }
        }
    }
  }

  fun deleteScheme(scheme: SchemeOfWork) {
    viewModelScope.launch {
      repository.deleteSchemeRemote(scheme)
        .onSuccess {
          snackbarFlow.value = "Deleted scheme"
        }.onFailure { err ->
          handleApiFailure(err, deniedModule = TeacherModule.SCHEMES)
        }
    }
  }

  fun saveLessonPlan(plan: LessonPlan, isUpdate: Boolean, contentMap: Map<String, String>) {
    val stream = uiState.value.selectedStream.ifBlank { plan.stream }
    val existing = uiState.value.lessonPlans.firstOrNull { it.id == plan.id }
    val scopedPlan = plan.copy(
      stream = stream,
      remoteId = plan.remoteId.ifBlank { existing?.remoteId.orEmpty() },
      updatedAt = System.currentTimeMillis(),
      synced = false,
    )
    viewModelScope.launch {
      repository.saveLessonPlan(scopedPlan)
      repository.saveLessonPlanRemote(
        scopedPlan,
        isUpdate = isUpdate || scopedPlan.remoteId.isNotBlank(),
        contentMap = contentMap,
      ).onSuccess {
        snackbarFlow.value = "Lesson plan saved and linked to its scheme"
      }.onFailure { err ->
        if (err is ApiException) {
          handleApiFailure(err, deniedModule = TeacherModule.LESSON_PLANS)
        } else {
          snackbarFlow.value = "Lesson plan saved offline. It will sync when you are back online."
        }
      }
    }
  }

  fun deleteLessonPlan(id: String) {
    val plan = uiState.value.lessonPlans.firstOrNull { it.id == id } ?: return
    viewModelScope.launch {
      repository.deleteLessonPlanRemote(plan)
        .onSuccess {
          snackbarFlow.value = "Deleted lesson plan"
        }.onFailure { err ->
          handleApiFailure(err, deniedModule = TeacherModule.LESSON_PLANS)
        }
    }
  }

  fun saveRecordOfWork(record: RecordOfWork, isUpdate: Boolean = false) {
    val stream = uiState.value.selectedStream.ifBlank { record.stream }
    val existing = uiState.value.recordsOfWork.firstOrNull { it.id == record.id }
    val scopedRecord = record.copy(
      stream = stream,
      remoteId = record.remoteId.ifBlank { existing?.remoteId.orEmpty() },
      updatedAt = System.currentTimeMillis(),
      synced = false,
    )
    viewModelScope.launch {
      repository.saveRecordOfWork(scopedRecord)
      repository.saveRecordWorkRemote(
        scopedRecord,
        isUpdate = isUpdate || scopedRecord.remoteId.isNotBlank(),
      ).onSuccess {
        snackbarFlow.value = "Record of work saved and linked"
      }.onFailure { err ->
        if (err is ApiException) {
          handleApiFailure(err, deniedModule = TeacherModule.RECORD_OF_WORK)
        } else {
          snackbarFlow.value = "Record saved offline. It will sync when you are back online."
        }
      }
    }
  }

  fun deleteRecordOfWork(id: String) {
    val record = uiState.value.recordsOfWork.firstOrNull { it.id == id } ?: return
    viewModelScope.launch {
      repository.deleteRecordWorkRemote(record)
        .onSuccess {
          snackbarFlow.value = "Deleted record of work"
        }.onFailure { err ->
          handleApiFailure(err, deniedModule = TeacherModule.RECORD_OF_WORK)
        }
    }
  }

  fun addLearner(
    admissionNo: String,
    nemisUpi: String,
    fullName: String,
    gender: String,
    guardianPhone: String,
  ) {
    if (fullName.isBlank() || admissionNo.isBlank()) {
      snackbarFlow.value = "Enter the learner's full name and admission number."
      return
    }
    val stream = uiState.value.selectedStream.ifBlank {
      uiState.value.learners.firstOrNull()?.stream.orEmpty()
    }
    if (stream.isBlank()) {
      snackbarFlow.value = "No class stream assigned to your account yet."
      return
    }
    val grade = uiState.value.selectedGrade
    viewModelScope.launch {
      repository.createLearnerRemote(
        admissionNo = admissionNo,
        nemisUpi = nemisUpi,
        fullName = fullName,
        gradeLevel = grade,
        stream = stream,
        gender = gender,
        guardianPhone = guardianPhone,
      ).onSuccess { created ->
        snackbarFlow.value = "Added ${created.fullName} to $stream"
      }.onFailure { err ->
        handleApiFailure(err, deniedModule = TeacherModule.LEARNERS)
      }
    }
  }

  fun saveAttendance(
    date: String,
    grade: String,
    learningArea: String,
    lessonSlot: String,
    statuses: Map<String, Pair<AttendanceStatus, String>>,
  ) {
    val stream = uiState.value.selectedStream.ifBlank {
      uiState.value.learners.firstOrNull()?.stream.orEmpty()
    }
    if (stream.isBlank()) {
      snackbarFlow.value = "No class stream assigned to your account yet."
      return
    }
    viewModelScope.launch {
      val classLearners = uiState.value.learners.filter {
        uiState.value.matchesActiveClass(it.stream, it.grade)
      }
      val records = classLearners.mapNotNull { learner ->
        val pair = statuses[learner.id] ?: return@mapNotNull null
        AttendanceRecord(
          id = "${date}_${stream}_${learner.id}".replace(" ", "_"),
          date = date,
          grade = grade,
          stream = stream,
          learningArea = learningArea,
          lessonSlot = lessonSlot,
          learnerId = learner.id,
          learnerName = learner.fullName,
          admissionNo = learner.admissionNo,
          status = pair.first,
          remarks = pair.second,
          synced = true,
        )
      }
      if (records.isEmpty()) return@launch
      repository.submitAttendanceRemote(stream, date, records)
        .onSuccess {
          snackbarFlow.value = "Register saved (${records.size} learners)"
        }.onFailure { err ->
          handleApiFailure(err, deniedModule = TeacherModule.ATTENDANCE)
        }
    }
  }

  fun saveMarksBatch(
    learningAreaOrId: String,
    term: String,
    assessmentType: String,
    scores: Map<String, Double>,
  ) {
    val stream = uiState.value.selectedStream.ifBlank {
      uiState.value.learners.firstOrNull()?.stream.orEmpty()
    }
    if (stream.isBlank()) {
      snackbarFlow.value = "No class stream assigned to your account yet."
      return
    }
    val grade = uiState.value.selectedGrade
    val subjectId = KicdCurriculumRepository.subjectIdForArea(grade, learningAreaOrId)
    val areaName = KicdCurriculumRepository.areaNameForSubjectId(subjectId)
    viewModelScope.launch {
      val classLearners = uiState.value.learners.filter {
        uiState.value.matchesActiveClass(it.stream, it.grade)
      }
      val existingByLearner = uiState.value.marks
        .filter { m ->
          uiState.value.matchesActiveClass(m.stream, m.grade) &&
            KicdCurriculumRepository.subjectIdForArea(grade, m.learningArea) == subjectId
        }
        .associateBy { it.learnerId }
      val allEntries = classLearners.mapNotNull { learner ->
        val s = scores[learner.id] ?: return@mapNotNull null
        val clamped = s.coerceIn(0.0, 100.0)
        MarkEntry(
          id = "${learner.id}_${subjectId}_${term}_${assessmentType}".replace(" ", "_"),
          learnerId = learner.id,
          learnerName = learner.fullName,
          admissionNo = learner.admissionNo,
          grade = grade,
          stream = stream,
          learningArea = areaName,
          term = term,
          assessmentType = assessmentType,
          score = clamped,
          maxScore = 100.0,
          level = CbcPerformanceLevel.fromScore(clamped),
          updatedAt = System.currentTimeMillis(),
          synced = true,
        )
      }
      val changedEntries = allEntries.filter { entry ->
        val existingScore = existingByLearner[entry.learnerId]?.score
        existingScore == null || existingScore != entry.score
      }
      val entriesToSend = changedEntries.ifEmpty { allEntries }
      if (entriesToSend.isEmpty()) return@launch
      repository.submitMarksRemote(stream, subjectId, entriesToSend)
        .onSuccess {
          snackbarFlow.value = "$areaName scores saved (${entriesToSend.size})"
        }.onFailure { err ->
          handleApiFailure(err, deniedModule = TeacherModule.MARKSBOOK)
        }
    }
  }

  fun saveObservation(
    learner: Learner,
    observationType: String,
    competency: String,
    rating: CbcPerformanceLevel,
    note: String,
  ) {
    if (note.isBlank()) {
      snackbarFlow.value = "Fill in observation note first"
      return
    }
    viewModelScope.launch {
      val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
      val fullNote = "[$observationType · $competency · ${rating.code}] ${note.trim()}"
      val local = ObservationNote(
        id = "obs_${UUID.randomUUID().toString().take(8)}",
        learnerId = learner.id,
        learnerName = learner.fullName,
        grade = learner.grade,
        observationType = observationType,
        competency = competency,
        rating = rating,
        note = note.trim(),
        date = today,
        synced = true,
      )
      repository.submitObservationRemote(learner, today, fullNote, local)
        .onSuccess {
          snackbarFlow.value = "Saved observation"
        }.onFailure { err ->
          handleApiFailure(err, deniedModule = TeacherModule.OBSERVATIONS)
        }
    }
  }

  fun saveProgressEntry(
    learner: Learner,
    learningArea: String,
    stage: String,
    note: String,
  ) {
    viewModelScope.launch {
      val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
      repository.saveProgressRemote(
        learner = learner,
        day = today,
        stage = stage,
        note = note,
        learningArea = learningArea,
      ).onSuccess {
        snackbarFlow.value = "Saved progress"
      }.onFailure { err ->
        handleApiFailure(err, deniedModule = TeacherModule.PROGRESS)
      }
    }
  }

  fun deleteProgressEntry(id: String) {
    viewModelScope.launch {
      repository.deleteProgressRemote(id)
        .onSuccess {
          snackbarFlow.value = "Deleted progress entry"
        }.onFailure { err ->
          handleApiFailure(err, deniedModule = TeacherModule.PROGRESS)
        }
    }
  }

  fun saveGuardianMessage(
    learner: Learner,
    message: String,
  ) {
    if (message.isBlank()) {
      snackbarFlow.value = "Fill in message first"
      return
    }
    viewModelScope.launch {
      repository.postWhatsAppRemote(learner, message.trim())
        .onSuccess {
          snackbarFlow.value = "Message sent to guardian"
        }.onFailure { err ->
          handleApiFailure(err, deniedModule = TeacherModule.WHATSAPP)
        }
    }
  }

  fun postFeePayment(
    learner: Learner,
    amountPaid: Double,
    method: String,
    reference: String,
    day: String,
    note: String,
  ) {
    if (amountPaid <= 0) {
      snackbarFlow.value = "Enter the amount received"
      return
    }
    val stream = uiState.value.selectedStream
    viewModelScope.launch {
      repository.postFeePaymentRemote(
        learner = learner,
        amount = amountPaid,
        method = method,
        reference = reference,
        day = day.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) },
        note = note,
      ).onSuccess {
        if (stream.isNotBlank()) {
          repository.fetchPayments(stream).onSuccess { feePaymentsFlow.value = it }
        }
        snackbarFlow.value = "Payment posted"
      }.onFailure { err ->
        handleApiFailure(err, deniedModule = TeacherModule.FEE_PAYMENTS)
      }
    }
  }

  fun exportBroadsheetCsv() {
    val stream = uiState.value.selectedStream.ifBlank {
      uiState.value.learners.firstOrNull()?.stream.orEmpty()
    }
    viewModelScope.launch {
      repository.fetchBroadsheetCsv(stream)
        .onSuccess { csv ->
          val fileName = "Broadsheet_${stream.ifBlank { "Class" }.replace(" ", "_")}.csv"
          exportedCsvFlow.value = fileName to csv
        }.onFailure { err ->
          handleApiFailure(err, deniedModule = TeacherModule.BROADSHEET)
        }
    }
  }

  fun saveCsvToUri(csvContent: String, targetUri: Uri) {
    viewModelScope.launch {
      withContext(Dispatchers.IO) { pdfExportManager.writeTextToUri(csvContent, targetUri) }
      exportedCsvFlow.value = null
      snackbarFlow.value = "Saved broadsheet CSV"
    }
  }

  private fun handleApiFailure(
    err: Throwable,
    silent: Boolean = false,
    deniedModule: TeacherModule? = null,
  ) {
    if (err is ApiException) {
      when {
        err.httpCode == 401 || err.code == "UNAUTHENTICATED" -> {
          viewModelScope.launch {
            repository.logout()
            openHome()
          }
          loginErrorFlow.value = err.message
          snackbarFlow.value = err.message
          return
        }
        err.code == "MUST_CHANGE_LOGIN" -> {
          viewModelScope.launch {
            repository.markMustChangeLogin()
          }
          snackbarFlow.value = err.message
          return
        }
        err.code == "OUT_OF_SCOPE" -> {
          snackbarFlow.value = err.message
          viewModelScope.launch {
            repository.verifySessionOnLaunch().onSuccess { s ->
              if (s != null) {
                ensureValidSelectedStream(s)
                refreshActiveStream()
              }
            }
          }
          return
        }
        err.code == "SERVICE_DENIED" -> {
          snackbarFlow.value = err.message
          viewModelScope.launch {
            if (deniedModule != null) {
              repository.disableServiceInSession(deniedModule)
              if (activeModuleFlow.value == deniedModule) {
                openHome()
              }
            } else {
              repository.verifySessionOnLaunch()
            }
          }
          return
        }
      }
    }
    if (!silent) {
      snackbarFlow.value = err.message ?: "Request failed"
    }
  }

  // --- PDF Exports ---

  fun exportSchemePdf(scheme: SchemeOfWork) {
    viewModelScope.launch {
      exportedPdfFlow.value = withContext(Dispatchers.IO) { pdfExportManager.exportSchemeOfWork(scheme) }
    }
  }

  fun exportLessonPlanPdf(plan: LessonPlan) {
    viewModelScope.launch {
      exportedPdfFlow.value = withContext(Dispatchers.IO) { pdfExportManager.exportLessonPlan(plan) }
    }
  }

  fun exportRecordsPdf(grade: String, learningArea: String) {
    val session = uiState.value.session ?: return
    val matching = uiState.value.recordsOfWork.filter {
      uiState.value.matchesActiveClass(it.stream, it.grade) &&
        (learningArea == "All" || it.learningArea == learningArea)
    }
    if (matching.isEmpty()) {
      snackbarFlow.value = "Add at least one Record of Work entry to export PDF."
      return
    }
    viewModelScope.launch {
      exportedPdfFlow.value = withContext(Dispatchers.IO) {
        pdfExportManager.exportRecordOfWork(
          records = matching,
          schoolName = session.schoolName,
          teacherName = session.fullName,
          grade = grade,
          learningArea = learningArea,
          term = session.currentTerm,
        )
      }
    }
  }

  fun exportStudentProgressPdf(learner: Learner, overallComment: String) {
    val session = uiState.value.session ?: return
    val marks = uiState.value.marks.filter { it.learnerId == learner.id }
    val att = uiState.value.attendance.filter { it.learnerId == learner.id }
    val obs = uiState.value.observations.filter { it.learnerId == learner.id }
    viewModelScope.launch {
      exportedPdfFlow.value = withContext(Dispatchers.IO) {
        pdfExportManager.exportStudentProgressReport(
          schoolName = session.schoolName,
          teacherName = session.fullName,
          term = session.currentTerm,
          year = session.currentYear,
          learner = learner,
          marks = marks,
          attendance = att,
          observations = obs,
          overallComment = overallComment,
        )
      }
    }
  }

  fun exportTimetablePdf(grade: String) {
    val session = uiState.value.session ?: return
    val targetGrade = KicdCurriculumRepository.normalizeGrade(grade) ?: grade
    val slots = uiState.value.timetable.filter {
      val g = KicdCurriculumRepository.normalizeGrade(it.grade) ?: it.grade
      uiState.value.matchesActiveClass(it.room, it.grade) || g.equals(targetGrade, ignoreCase = true)
    }
    viewModelScope.launch {
      exportedPdfFlow.value = withContext(Dispatchers.IO) {
        pdfExportManager.exportTimetable(session.schoolName, grade, session.currentTerm, session.currentYear, slots)
      }
    }
  }

  fun openPdf(exported: ExportedPdf, shareOnly: Boolean = false) {
    runCatching { pdfExportManager.openOrSharePdf(exported, shareOnly) }
      .onFailure { snackbarFlow.value = "No PDF viewer app installed; use Save Copy." }
  }

  fun savePdfToUri(exported: ExportedPdf, targetUri: Uri) {
    viewModelScope.launch {
      withContext(Dispatchers.IO) { pdfExportManager.copyPdfToUri(exported.file, targetUri) }
      snackbarFlow.value = "Saved ${exported.file.name}"
    }
  }

  class Factory(
    private val repository: TeacherRepository,
    private val pdfExportManager: PdfExportManager,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return TeacherViewModel(repository, pdfExportManager) as T
    }
  }
}

private data class CoreDataBundle(
  val session: TeacherSession?,
  val dark: Boolean,
  val schemes: List<SchemeOfWork>,
  val plans: List<LessonPlan>,
  val records: List<RecordOfWork>,
)

private data class ClassDataBundle(
  val learners: List<Learner>,
  val attendance: List<AttendanceRecord>,
  val marks: List<MarkEntry>,
  val observations: List<ObservationNote>,
  val timetable: List<TimetableSlot>,
)

private data class UiControlBundle(
  val dest: AppDestination,
  val module: TeacherModule,
  val stream: String,
  val sync: SyncStatus,
  val loggingIn: Boolean,
)

private data class ExtrasBundle(
  val loaded: Boolean,
  val loginErr: String?,
  val snack: String?,
  val pdf: ExportedPdf?,
  val lastSync: Long?,
)

private data class RemoteExtrasBundle(
  val summary: DashboardSummary?,
  val template: List<LessonPlanTemplateSection>,
  val kicd: List<KicdDesignItem>,
  val resources: List<ApprovedResource>,
  val payments: List<FeePaymentRecord>,
  val bundledKicd: List<KicdDesignItem>,
)

private data class UiTuple(
  val ctrl: UiControlBundle,
  val ext: ExtrasBundle,
  val rem: RemoteExtrasBundle,
  val pre: PrefillsBundle,
)

private data class PrefillsBundle(
  val kicd: KicdDesignItem?,
  val plan: LessonPlanPrefill?,
  val record: RecordOfWorkPrefill?,
  val csv: Pair<String, String>?,
)

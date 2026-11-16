package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.curriculum.KicdCurriculumRepository
import com.example.data.repository.TeacherRepository
import com.example.domain.model.AllocatedClass
import com.example.domain.model.AttendanceRecord
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.CbcPerformanceLevel
import com.example.domain.model.GuardianMessage
import com.example.domain.model.KicdDesignItem
import com.example.domain.model.Learner
import com.example.domain.model.LearnerProgressEntry
import com.example.domain.model.LessonPlan
import com.example.domain.model.MarkEntry
import com.example.domain.model.ObservationNote
import com.example.domain.model.RecordOfWork
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
  val selectedGrade: String = "Grade 4",
  val darkMode: Boolean = true,
  val schemes: List<SchemeOfWork> = emptyList(),
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
  val pendingKicdPrefill: KicdDesignItem? = null,
  val pendingPlanPrefill: Pair<String, String>? = null,
)

class TeacherViewModel(
  private val repository: TeacherRepository,
  private val pdfExportManager: PdfExportManager,
) : ViewModel() {

  private val destinationFlow = MutableStateFlow(AppDestination.HOME)
  private val activeModuleFlow = MutableStateFlow(TeacherModule.DASHBOARD)
  private val selectedGradeFlow = MutableStateFlow("Grade 4")
  private val syncStatusFlow = MutableStateFlow(SyncStatus())
  private val isLoggingInFlow = MutableStateFlow(false)
  private val loginErrorFlow = MutableStateFlow<String?>(null)
  private val snackbarFlow = MutableStateFlow<String?>(null)
  private val exportedPdfFlow = MutableStateFlow<ExportedPdf?>(null)
  private val pendingKicdFlow = MutableStateFlow<KicdDesignItem?>(null)
  private val pendingPlanFlow = MutableStateFlow<Pair<String, String>?>(null)
  private val sessionLoadedFlow = MutableStateFlow(false)

  init {
    viewModelScope.launch { repository.ensureTimetableSeeded() }
    viewModelScope.launch {
      repository.sessionFlow.collect { session ->
        sessionLoadedFlow.value = true
        if (session != null) {
          val firstGrade = session.allocatedClasses.firstOrNull()?.grade
          if (firstGrade != null && selectedGradeFlow.value !in session.allocatedClasses.map { it.grade }) {
            selectedGradeFlow.value = firstGrade
          }
          if (destinationFlow.value == AppDestination.MODULE &&
            activeModuleFlow.value != TeacherModule.DASHBOARD &&
            activeModuleFlow.value !in session.allowedModules
          ) {
            destinationFlow.value = AppDestination.HOME
            activeModuleFlow.value = TeacherModule.DASHBOARD
          }
        }
      }
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
    selectedGradeFlow,
    syncStatusFlow,
    isLoggingInFlow,
  ) { dest, module, grade, sync, loggingIn ->
    UiControlBundle(dest, module, grade, sync, loggingIn)
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

  private val prefillsFlow = combine(pendingKicdFlow, pendingPlanFlow) { k, p -> k to p }

  val uiState: StateFlow<TeacherUiState> = combine(
    combine(coreDataFlow, classDataFlow, commDataFlow) { core, cls, comm -> Triple(core, cls, comm) },
    combine(uiControlFlow, extrasFlow, prefillsFlow) { ctrl, ext, pre -> Triple(ctrl, ext, pre) },
  ) { dataTriple, uiTriple ->
    val (core, cls, comm) = dataTriple
    val (ctrl, ext, pre) = uiTriple
    val pendingUploads = core.schemes.count { !it.synced } +
      core.plans.count { !it.synced } +
      core.records.count { !it.synced } +
      cls.attendance.count { !it.synced } +
      cls.marks.count { !it.synced } +
      cls.observations.count { !it.synced }

    TeacherUiState(
      isSessionLoaded = ext.loaded,
      session = core.session,
      destination = ctrl.dest,
      activeModule = ctrl.module,
      selectedGrade = ctrl.grade,
      darkMode = core.dark,
      schemes = core.schemes,
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
      pendingKicdPrefill = pre.first,
      pendingPlanPrefill = pre.second,
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
    val allowed = uiState.value.session?.allowedModules ?: TeacherModule.defaultTeacherModules
    if (module in allowed) {
      activeModuleFlow.value = module
      destinationFlow.value = AppDestination.MODULE
    } else {
      snackbarFlow.value = "${module.label} has not been enabled by Admin."
    }
  }

  fun selectGrade(grade: String) {
    selectedGradeFlow.value = grade
  }

  fun clearSnackbar() {
    snackbarFlow.value = null
  }

  fun showMessage(message: String) {
    snackbarFlow.value = message
  }

  fun dismissPdfDialog() {
    exportedPdfFlow.value = null
  }

  fun clearPendingKicdPrefill() {
    pendingKicdFlow.value = null
  }

  fun clearPendingPlanPrefill() {
    pendingPlanFlow.value = null
  }

  fun useKicdInScheme(design: KicdDesignItem) {
    val allowed = uiState.value.session?.allowedModules ?: TeacherModule.defaultTeacherModules
    if (TeacherModule.SCHEMES !in allowed) {
      snackbarFlow.value = "Schemes of work is switched off by Admin."
      return
    }
    pendingKicdFlow.value = design
    selectedGradeFlow.value = design.grade
    navigateToModule(TeacherModule.SCHEMES)
  }

  fun useKicdInLessonPlan(design: KicdDesignItem) {
    val allowed = uiState.value.session?.allowedModules ?: TeacherModule.defaultTeacherModules
    if (TeacherModule.LESSON_PLANS !in allowed) {
      snackbarFlow.value = "Lesson plans is switched off by Admin."
      return
    }
    pendingKicdFlow.value = design
    selectedGradeFlow.value = design.grade
    navigateToModule(TeacherModule.LESSON_PLANS)
  }

  fun planFromSchemeRow(topic: String, outcomes: String) {
    val allowed = uiState.value.session?.allowedModules ?: TeacherModule.defaultTeacherModules
    if (TeacherModule.LESSON_PLANS !in allowed) {
      snackbarFlow.value = "Lesson plans is switched off by Admin."
      return
    }
    pendingPlanFlow.value = topic to outcomes
    navigateToModule(TeacherModule.LESSON_PLANS)
  }

  fun loginWithBackend(backendUrl: String, identifier: String, password: String) {
    if (identifier.isBlank() || password.isBlank()) {
      loginErrorFlow.value = "Enter your username and password."
      return
    }
    viewModelScope.launch {
      isLoggingInFlow.value = true
      loginErrorFlow.value = null
      val result = repository.loginToBackend(backendUrl, identifier, password)
      isLoggingInFlow.value = false
      result.onFailure { err ->
        loginErrorFlow.value = err.message ?: "Unable to connect to backend."
      }.onSuccess {
        snackbarFlow.value = "Signed in as ${it.fullName}"
      }
    }
  }

  fun enterOfflineWorkspace(
    teacherName: String,
    tscNumber: String,
    schoolName: String,
    backendUrl: String,
    allocatedGrades: List<String>,
    username: String = "",
  ) {
    viewModelScope.launch {
      isLoggingInFlow.value = true
      loginErrorFlow.value = null
      repository.enterOfflineWorkspace(teacherName, tscNumber, schoolName, backendUrl, allocatedGrades, username)
      isLoggingInFlow.value = false
      snackbarFlow.value = "Teacher workspace ready"
    }
  }

  fun syncNow() {
    viewModelScope.launch {
      syncStatusFlow.value = syncStatusFlow.value.copy(isSyncing = true, isError = false)
      val res = repository.syncWithBackend()
      res.onSuccess { msg ->
        syncStatusFlow.value = syncStatusFlow.value.copy(isSyncing = false, lastMessage = msg, isError = false)
        snackbarFlow.value = msg
      }.onFailure { err ->
        syncStatusFlow.value = syncStatusFlow.value.copy(isSyncing = false, lastMessage = err.message, isError = true)
        snackbarFlow.value = err.message ?: "Sync failed"
      }
    }
  }

  fun toggleDarkMode(enabled: Boolean) {
    viewModelScope.launch { repository.setDarkMode(enabled) }
  }

  fun updateModulePermissions(modules: Set<TeacherModule>) {
    viewModelScope.launch {
      val keys = modules.map { it.key }
      repository.updateModulePermissions(keys)
      snackbarFlow.value = "Updated services (${keys.size} active)"
    }
  }

  fun toggleAllocatedGrade(grade: String) {
    viewModelScope.launch {
      val session = uiState.value.session ?: return@launch
      val existing = session.allocatedClasses
      val hasGrade = existing.any { it.grade == grade }
      val updated = if (hasGrade) {
        if (existing.size <= 1) {
          snackbarFlow.value = "A teacher needs at least one class"
          return@launch
        }
        existing.filterNot { it.grade == grade }
      } else {
        val defaultStreams = listOf("Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta", "Theta", "Iota")
        val gradeNum = grade.filter { it.isDigit() }.toIntOrNull()?.minus(1) ?: 0
        val stream = defaultStreams.getOrElse(gradeNum) { "East" }
        val level = KicdCurriculumRepository.levelForGrade(grade)
        val areas = KicdCurriculumRepository.cbcLearningAreasByLevel[level].orEmpty()
        existing + AllocatedClass("cls_${grade.lowercase().replace(" ", "_")}", grade, stream, areas)
      }
      repository.updateAllocatedClasses(updated)
      if (selectedGradeFlow.value !in updated.map { it.grade }) {
        selectedGradeFlow.value = updated.first().grade
      }
    }
  }

  fun updateLoginCredentials(username: String, password: String) {
    if (password.length < 6) {
      snackbarFlow.value = "Use a password of at least 6 characters"
      return
    }
    viewModelScope.launch {
      repository.updateLoginCredentials(username.trim().ifBlank { "alvina" }, mustChangePassword = false)
      snackbarFlow.value = "Login updated"
    }
  }

  fun setFirstSignInRequired(required: Boolean) {
    viewModelScope.launch {
      val current = uiState.value.session ?: return@launch
      repository.updateLoginCredentials(current.username, mustChangePassword = required)
    }
  }

  fun logout() {
    viewModelScope.launch {
      repository.logout()
      openHome()
    }
  }

  // CRUD
  fun saveScheme(
    existingId: String?,
    title: String,
    grade: String,
    learningArea: String,
    term: String,
    year: Int,
    rows: List<SchemeRow>,
  ) {
    val session = uiState.value.session ?: return
    viewModelScope.launch {
      val scheme = SchemeOfWork(
        id = existingId ?: "sch_${UUID.randomUUID().toString().take(8)}",
        title = title.ifBlank { "$grade $learningArea $term Scheme" },
        schoolName = session.schoolName,
        teacherName = session.fullName,
        grade = grade,
        learningArea = learningArea,
        term = term,
        year = year,
        rows = rows,
        updatedAt = System.currentTimeMillis(),
        synced = false,
      )
      repository.saveScheme(scheme)
      snackbarFlow.value = "Scheme of work saved"
    }
  }

  fun deleteScheme(id: String) {
    viewModelScope.launch {
      repository.deleteScheme(id)
      snackbarFlow.value = "Deleted scheme"
    }
  }

  fun saveLessonPlan(plan: LessonPlan) {
    viewModelScope.launch {
      repository.saveLessonPlan(plan.copy(updatedAt = System.currentTimeMillis(), synced = false))
      snackbarFlow.value = "Lesson plan saved"
    }
  }

  fun deleteLessonPlan(id: String) {
    viewModelScope.launch {
      repository.deleteLessonPlan(id)
      snackbarFlow.value = "Deleted lesson plan"
    }
  }

  fun saveRecordOfWork(record: RecordOfWork) {
    viewModelScope.launch {
      repository.saveRecordOfWork(record.copy(updatedAt = System.currentTimeMillis(), synced = false))
      snackbarFlow.value = "Record of work saved"
    }
  }

  fun deleteRecordOfWork(id: String) {
    viewModelScope.launch {
      repository.deleteRecordOfWork(id)
      snackbarFlow.value = "Deleted record of work"
    }
  }

  fun updateTimetableSlot(slot: TimetableSlot) {
    viewModelScope.launch {
      repository.saveTimetableSlot(slot)
      snackbarFlow.value = "Updated ${slot.dayOfWeek} (${slot.timeRange})"
    }
  }

  fun resetTimetableForGrade(grade: String) {
    viewModelScope.launch {
      repository.resetTimetableForGrade(grade)
      snackbarFlow.value = "Restored KICD template for $grade"
    }
  }

  fun addLearner(
    admissionNo: String,
    nemisUpi: String,
    fullName: String,
    grade: String,
    stream: String,
    gender: String,
    guardianPhone: String,
    pathwayInterest: String,
  ) {
    if (fullName.isBlank()) return
    viewModelScope.launch {
      val learner = Learner(
        id = "lrn_${UUID.randomUUID().toString().take(8)}",
        admissionNo = admissionNo.ifBlank { "ADM-${(4100..4999).random()}" },
        nemisUpi = nemisUpi.ifBlank { "NEM-${(7000..7999).random()}" },
        fullName = fullName.trim(),
        grade = grade,
        stream = stream.trim(),
        gender = gender,
        guardianPhone = guardianPhone.trim(),
        pathwayInterest = pathwayInterest,
      )
      repository.saveLearner(learner)
      snackbarFlow.value = "Added ${learner.fullName} to $grade"
    }
  }

  fun populateStarterRoster(grade: String, stream: String) {
    viewModelScope.launch {
      repository.populateStarterClassRoster(grade, stream)
      snackbarFlow.value = "Loaded $grade $stream roster"
    }
  }

  fun saveAttendance(
    date: String,
    grade: String,
    stream: String,
    learningArea: String,
    lessonSlot: String,
    statuses: Map<String, Pair<AttendanceStatus, String>>,
  ) {
    viewModelScope.launch {
      val learnersInGrade = uiState.value.learners.filter { it.grade == grade }
      val records = learnersInGrade.mapNotNull { learner ->
        val pair = statuses[learner.id] ?: return@mapNotNull null
        AttendanceRecord(
          id = "${date}_${grade}_${learningArea}_${lessonSlot}_${learner.id}".replace(" ", "_"),
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
          synced = false,
        )
      }
      repository.saveAttendanceBatch(records)
      snackbarFlow.value = "Register saved (${records.size} learners)"
    }
  }

  fun saveMarkEntry(
    learner: Learner,
    learningArea: String,
    term: String,
    assessmentType: String,
    score: Double,
    comment: String,
  ) {
    viewModelScope.launch {
      val clamped = score.coerceIn(0.0, 100.0)
      val entry = MarkEntry(
        id = "${learner.id}_${learningArea}_${term}_${assessmentType}".replace(" ", "_"),
        learnerId = learner.id,
        learnerName = learner.fullName,
        admissionNo = learner.admissionNo,
        grade = learner.grade,
        stream = learner.stream,
        learningArea = learningArea,
        term = term,
        assessmentType = assessmentType,
        score = clamped,
        maxScore = 100.0,
        level = CbcPerformanceLevel.fromScore(clamped),
        teacherComment = comment.trim(),
        updatedAt = System.currentTimeMillis(),
        synced = false,
      )
      repository.saveMarkEntry(entry)
    }
  }

  fun saveMarksBatch(
    grade: String,
    learningArea: String,
    term: String,
    assessmentType: String,
    scores: Map<String, Double>,
  ) {
    viewModelScope.launch {
      val learners = uiState.value.learners.filter { it.grade == grade }
      val entries = learners.mapNotNull { learner ->
        val s = scores[learner.id] ?: return@mapNotNull null
        val clamped = s.coerceIn(0.0, 100.0)
        MarkEntry(
          id = "${learner.id}_${learningArea}_${term}_${assessmentType}".replace(" ", "_"),
          learnerId = learner.id,
          learnerName = learner.fullName,
          admissionNo = learner.admissionNo,
          grade = learner.grade,
          stream = learner.stream,
          learningArea = learningArea,
          term = term,
          assessmentType = assessmentType,
          score = clamped,
          maxScore = 100.0,
          level = CbcPerformanceLevel.fromScore(clamped),
          updatedAt = System.currentTimeMillis(),
          synced = false,
        )
      }
      repository.saveMarkEntries(entries)
      snackbarFlow.value = "$learningArea scores saved"
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
      val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
      repository.saveObservation(
        ObservationNote(
          id = "obs_${UUID.randomUUID().toString().take(8)}",
          learnerId = learner.id,
          learnerName = learner.fullName,
          grade = learner.grade,
          observationType = observationType,
          competency = competency,
          rating = rating,
          note = note.trim(),
          date = today,
          synced = false,
        )
      )
      snackbarFlow.value = "Saved observation"
    }
  }

  fun deleteObservation(id: String) {
    viewModelScope.launch { repository.deleteObservation(id) }
  }

  fun saveProgressEntry(
    learner: Learner,
    learningArea: String,
    stage: String,
    note: String,
  ) {
    if (note.isBlank()) {
      snackbarFlow.value = "Fill in progress note first"
      return
    }
    viewModelScope.launch {
      val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
      repository.saveProgressEntry(
        LearnerProgressEntry(
          id = "prog_${UUID.randomUUID().toString().take(8)}",
          learnerId = learner.id,
          learnerName = learner.fullName,
          grade = learner.grade,
          learningArea = learningArea,
          stage = stage,
          note = note.trim(),
          date = today,
          synced = false,
        )
      )
      snackbarFlow.value = "Saved progress"
    }
  }

  fun saveGuardianMessage(
    learner: Learner,
    template: String,
    message: String,
  ) {
    if (message.isBlank()) {
      snackbarFlow.value = "Fill in message first"
      return
    }
    viewModelScope.launch {
      val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
      repository.saveGuardianMessage(
        GuardianMessage(
          id = "wa_${UUID.randomUUID().toString().take(8)}",
          learnerId = learner.id,
          learnerName = learner.fullName,
          guardianPhone = learner.guardianPhone,
          grade = learner.grade,
          template = template,
          message = message.trim(),
          date = today,
          synced = false,
        )
      )
      snackbarFlow.value = "Guardian message logged"
    }
  }

  fun postFeePayment(learner: Learner, amountPaid: Double) {
    if (amountPaid <= 0) {
      snackbarFlow.value = "Enter the amount received"
      return
    }
    viewModelScope.launch {
      repository.postFeePayment(learner.id, learner.feeBalanceKes, amountPaid)
      snackbarFlow.value = "Payment posted. Receipt ready."
    }
  }

  // PDF Exports
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
      it.grade == grade && (learningArea == "All" || it.learningArea == learningArea)
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
    val slots = uiState.value.timetable.filter { it.grade == grade }
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
  val grade: String,
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

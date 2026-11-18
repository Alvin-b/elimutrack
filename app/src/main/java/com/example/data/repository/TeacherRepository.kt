package com.example.data.repository

import com.example.data.curriculum.KicdCurriculumRepository
import com.example.data.curriculum.DefaultLessonPlanTemplate
import com.example.data.local.AttendanceEntity
import com.example.data.local.ElimuTrackDatabase
import com.example.data.local.GuardianMessageEntity
import com.example.data.local.LearnerEntity
import com.example.data.local.LearnerProgressEntity
import com.example.data.local.LessonPlanEntity
import com.example.data.local.LessonPlanTemplateEntity
import com.example.data.local.MarkEntryEntity
import com.example.data.local.ObservationEntity
import com.example.data.local.RecordOfWorkEntity
import com.example.data.local.SchemeEntity
import com.example.data.local.SessionPreferences
import com.example.data.local.TimetableSlotEntity
import com.example.data.remote.ApiException
import com.example.data.remote.AttendancePostItemDto
import com.example.data.remote.AttendancePostRequestDto
import com.example.data.remote.BackendApiParser
import com.example.data.remote.ChangeLoginRequestDto
import com.example.data.remote.DeleteIdRequestDto
import com.example.data.remote.ElimuTrackApiService
import com.example.data.remote.LearnerCreateRequestDto
import com.example.data.remote.LessonPlanApiRequestDto
import com.example.data.remote.LoginRequestDto
import com.example.data.remote.MarkPutItemDto
import com.example.data.remote.MarksPutRequestDto
import com.example.data.remote.NetworkModule
import com.example.data.remote.ObservationPostItemDto
import com.example.data.remote.ObservationPostRequestDto
import com.example.data.remote.PaymentPostRequestDto
import com.example.data.remote.ProgressPostRequestDto
import com.example.data.remote.RecordWorkApiDto
import com.example.data.remote.SchemeBulkRequestDto
import com.example.data.remote.SchemeBulkRowDto
import com.example.data.remote.SchemeRowApiDto
import com.example.data.remote.WhatsAppPostRequestDto
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
import com.example.domain.model.LessonPlanTemplateSection
import com.example.domain.model.MarkEntry
import com.example.domain.model.ObservationNote
import com.example.domain.model.RecordOfWork
import com.example.domain.model.SchemeOfWork
import com.example.domain.model.SchemeRow
import com.example.domain.model.TeacherModule
import com.example.domain.model.TeacherSession
import com.example.domain.model.TimetableSlot
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject

class TeacherRepository(
  private val db: ElimuTrackDatabase,
  private val prefs: SessionPreferences,
) {
  private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
  private val rowListType = Types.newParameterizedType(List::class.java, SchemeRow::class.java)
  private val rowsAdapter = moshi.adapter<List<SchemeRow>>(rowListType)
  private val templateSectionsType = Types.newParameterizedType(
    List::class.java,
    LessonPlanTemplateSection::class.java,
  )
  private val templateSectionsAdapter = moshi.adapter<List<LessonPlanTemplateSection>>(templateSectionsType)
  private val contentMapType = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
  private val contentMapAdapter = moshi.adapter<Map<String, String>>(contentMapType)

  val sessionFlow: Flow<TeacherSession?> = prefs.sessionFlow
  val darkModeFlow: Flow<Boolean> = prefs.darkModeFlow
  val backendUrlFlow: Flow<String> = prefs.backendUrlFlow
  val lastSyncedAtFlow: Flow<Long?> = prefs.lastSyncedAtFlow

  val schemesFlow: Flow<List<SchemeOfWork>> = db.schemeDao().observeAll().map { list -> list.map { it.toDomain() } }
  val lessonPlansFlow: Flow<List<LessonPlan>> = db.lessonPlanDao().observeAll().map { list -> list.map { it.toDomain() } }
  val lessonPlanTemplateFlow: Flow<List<LessonPlanTemplateSection>> =
    db.lessonPlanTemplateDao().observe().map { entity ->
      entity?.let { row ->
        runCatching { templateSectionsAdapter.fromJson(row.sectionsJson) }.getOrNull().orEmpty()
      }.orEmpty()
    }
  val recordsOfWorkFlow: Flow<List<RecordOfWork>> = db.recordOfWorkDao().observeAll().map { list -> list.map { it.toDomain() } }
  val learnersFlow: Flow<List<Learner>> = db.learnerDao().observeAll().map { list -> list.map { it.toDomain() } }
  val attendanceFlow: Flow<List<AttendanceRecord>> = db.attendanceDao().observeAll().map { list -> list.map { it.toDomain() } }
  val marksFlow: Flow<List<MarkEntry>> = db.markEntryDao().observeAll().map { list -> list.map { it.toDomain() } }
  val observationsFlow: Flow<List<ObservationNote>> = db.observationDao().observeAll().map { list -> list.map { it.toDomain() } }
  val progressEntriesFlow: Flow<List<LearnerProgressEntry>> = db.learnerProgressDao().observeAll().map { list -> list.map { it.toDomain() } }
  val guardianMessagesFlow: Flow<List<GuardianMessage>> = db.guardianMessageDao().observeAll().map { list -> list.map { it.toDomain() } }
  val timetableFlow: Flow<List<TimetableSlot>> = db.timetableDao().observeAll().map { list -> list.map { it.toDomain() } }

  private val kicdCache = mutableMapOf<Pair<String, String>, Pair<List<KicdDesignItem>, List<ApprovedResource>>>()

  private fun apiFor(session: TeacherSession): ElimuTrackApiService =
    NetworkModule.createService(session.backendUrl, session.authToken)

  private fun todayIso(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

  suspend fun ensureTimetableSeeded() {
    val existingTotal = db.timetableDao().count()
    if (existingTotal == 0) {
      val all = KicdCurriculumRepository.cbcGrades.flatMap { grade ->
        KicdCurriculumRepository.defaultTimetableForGrade(grade)
      }.map { it.toEntity() }
      db.timetableDao().upsertAll(all)
    } else {
      for (grade in KicdCurriculumRepository.cbcGrades) {
        val expectedPerDay = KicdCurriculumRepository.timeSlotsForGrade(grade).size
        if (db.timetableDao().countForGrade(grade) < expectedPerDay * 5) {
          db.timetableDao().deleteForGrade(grade)
          db.timetableDao().upsertAll(
            KicdCurriculumRepository.defaultTimetableForGrade(grade).map { it.toEntity() }
          )
        }
      }
    }
  }

  /** Keeps the school's lesson-plan template available on a cold offline start. */
  suspend fun ensureLessonPlanTemplateSeeded() {
    if (db.lessonPlanTemplateDao().get() == null) {
      db.lessonPlanTemplateDao().upsert(
        LessonPlanTemplateEntity(
          name = DefaultLessonPlanTemplate.NAME,
          sectionsJson = templateSectionsAdapter.toJson(DefaultLessonPlanTemplate.sections),
          updatedAt = System.currentTimeMillis(),
        )
      )
    }
  }

  suspend fun saveTimetableSlot(slot: TimetableSlot) {
    db.timetableDao().upsert(slot.toEntity())
  }

  suspend fun resetTimetableForGrade(grade: String) {
    db.timetableDao().deleteForGrade(grade)
    db.timetableDao().upsertAll(
      KicdCurriculumRepository.defaultTimetableForGrade(grade).map { it.toEntity() }
    )
  }

  // --- Authentication & Session Lifecycle ---

  suspend fun loginToBackend(
    backendUrl: String,
    username: String,
    password: String,
  ): Result<TeacherSession> = runCatching {
    val normalized = NetworkModule.normalizeBaseUrl(backendUrl)
    val api = NetworkModule.createService(normalized)
    val raw = BackendApiParser.unwrapOrThrow(api.login(LoginRequestDto(username.trim(), password)))
    val session = BackendApiParser.parseSession(raw, existingToken = "", backendUrl = normalized)
    if (session.authToken.isBlank()) {
      throw IllegalStateException("Server did not return an authentication token.")
    }
    clearCachedDomainTables()
    prefs.saveSession(session)
    prefs.saveBackendUrl(normalized)
    session
  }

  suspend fun verifySessionOnLaunch(): Result<TeacherSession?> = runCatching {
    val current = prefs.sessionFlow.first() ?: return@runCatching null
    if (current.authToken.isBlank()) return@runCatching null
    val api = apiFor(current)
    try {
      val raw = BackendApiParser.unwrapOrThrow(api.getSession())
      val refreshed = BackendApiParser.parseSession(raw, current.authToken, current.backendUrl)
      prefs.saveSession(refreshed)
      refreshed
    } catch (e: ApiException) {
      when {
        e.httpCode == 401 || e.code == "UNAUTHENTICATED" -> {
          clearSessionAndData()
          throw e
        }
        e.code == "MUST_CHANGE_LOGIN" -> {
          val locked = current.copy(mustChangePassword = true)
          prefs.saveSession(locked)
          locked
        }
        else -> current
      }
    }
  }

  suspend fun changeLogin(
    username: String,
    currentPassword: String,
    newPassword: String,
  ): Result<TeacherSession> = runCatching {
    val current = prefs.sessionFlow.first() ?: throw IllegalStateException("No active teacher session")
    val api = apiFor(current)
    val trimmedUser = username.trim().ifBlank { null }
    BackendApiParser.unwrapOrThrow(
      api.changeLogin(
        ChangeLoginRequestDto(
          username = trimmedUser,
          currentPassword = currentPassword,
          newPassword = newPassword,
        )
      )
    )
    val refreshedRaw = BackendApiParser.unwrapOrThrow(api.getSession())
    val updated = BackendApiParser.parseSession(refreshedRaw, current.authToken, current.backendUrl)
      .copy(mustChangePassword = false)
    prefs.saveSession(updated)
    updated
  }

  suspend fun logout() {
    val current = prefs.sessionFlow.first()
    if (current != null && current.authToken.isNotBlank()) {
      runCatching { apiFor(current).logout() }
    }
    clearSessionAndData()
  }

  private suspend fun clearSessionAndData() {
    prefs.clearSession()
    clearCachedDomainTables()
  }

  private suspend fun clearCachedDomainTables() {
    db.schemeDao().deleteAll()
    db.lessonPlanDao().deleteAll()
    db.lessonPlanTemplateDao().deleteAll()
    db.recordOfWorkDao().deleteAll()
    db.learnerDao().deleteAll()
    db.attendanceDao().deleteAll()
    db.markEntryDao().deleteAll()
    db.observationDao().deleteAll()
    db.learnerProgressDao().deleteAll()
    db.guardianMessageDao().deleteAll()
  }

  suspend fun saveBackendUrl(rawUrl: String) {
    val normalized = NetworkModule.normalizeBaseUrl(rawUrl)
    prefs.saveBackendUrl(normalized)
    val current = prefs.sessionFlow.first() ?: return
    prefs.saveSession(current.copy(backendUrl = normalized))
  }

  suspend fun disableServiceInSession(module: TeacherModule) {
    val current = prefs.sessionFlow.first() ?: return
    val filtered = current.allowedModuleKeys.filterNot {
      TeacherModule.fromKey(it) == module
    }
    prefs.saveSession(current.copy(allowedModuleKeys = filtered))
  }

  suspend fun markMustChangeLogin() {
    val current = prefs.sessionFlow.first() ?: return
    if (!current.mustChangePassword) {
      prefs.saveSession(current.copy(mustChangePassword = true))
    }
  }

  // --- Remote Fetching for Active Stream / Class ---

  suspend fun fetchDashboard(stream: String): Result<DashboardSummary> = runCatching {
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("No active session")
    val api = apiFor(session)
    val queryStream = stream.trim().ifBlank { null }
    val raw = BackendApiParser.unwrapOrThrow(api.getDashboard(queryStream))
    BackendApiParser.parseDashboard(raw, stream)
  }

  suspend fun fetchLearners(stream: String): Result<List<Learner>> = runCatching {
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("No active session")
    val api = apiFor(session)
    val queryStream = stream.trim().ifBlank { null }
    val raw = BackendApiParser.unwrapOrThrow(api.getLearners(queryStream))
    var learners = BackendApiParser.parseLearners(raw, stream)
    if (learners.isEmpty() && queryStream != null) {
      val fallbackRaw = runCatching { BackendApiParser.unwrapOrThrow(api.getLearners(null)) }.getOrNull()
      if (!fallbackRaw.isNullOrBlank()) {
        val allTeacherLearners = BackendApiParser.parseLearners(fallbackRaw, stream)
        if (allTeacherLearners.isNotEmpty()) {
          learners = allTeacherLearners
          db.learnerDao().deleteAll()
          db.learnerDao().upsertAll(learners.map { it.toEntity() })
          return@runCatching learners
        }
      }
    }
    if (queryStream != null) {
      db.learnerDao().deleteForStream(stream)
    } else {
      db.learnerDao().deleteAll()
    }
    if (learners.isNotEmpty()) {
      db.learnerDao().upsertAll(learners.map { it.toEntity() })
    }
    learners
  }

  suspend fun fetchAttendance(stream: String, day: String): Result<List<AttendanceRecord>> = runCatching {
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("No active session")
    val api = apiFor(session)
    val queryStream = stream.trim().ifBlank { null }
    val raw = BackendApiParser.unwrapOrThrow(api.getAttendance(queryStream, day))
    val parsed = BackendApiParser.parseAttendance(raw, stream, day)
    val learnersById = db.learnerDao().observeAll().first().associateBy { it.id }
    val enriched = parsed.map { rec ->
      val lrn = learnersById[rec.learnerId]
      if (lrn != null) rec.copy(learnerName = lrn.fullName, admissionNo = lrn.admissionNo) else rec
    }
    db.attendanceDao().deleteForStreamDay(stream, day)
    if (enriched.isNotEmpty()) {
      db.attendanceDao().upsertAll(enriched.map { it.toEntity() })
    }
    enriched
  }

  suspend fun fetchMarks(stream: String): Result<List<MarkEntry>> = runCatching {
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("No active session")
    val api = apiFor(session)
    val queryStream = stream.trim().ifBlank { null }
    val raw = BackendApiParser.unwrapOrThrow(api.getMarks(queryStream))
    val parsed = BackendApiParser.parseMarks(raw, stream, session.currentTerm)
    val learnersById = db.learnerDao().observeAll().first().associateBy { it.id }
    val enriched = parsed.map { m ->
      val lrn = learnersById[m.learnerId]
      if (lrn != null) m.copy(learnerName = lrn.fullName, admissionNo = lrn.admissionNo) else m
    }
    if (queryStream != null) {
      db.markEntryDao().deleteForStream(stream)
    } else {
      db.markEntryDao().deleteAll()
    }
    if (enriched.isNotEmpty()) {
      db.markEntryDao().upsertAll(enriched.map { it.toEntity() })
    }
    enriched
  }

  suspend fun fetchObservations(stream: String, day: String? = null): Result<List<ObservationNote>> = runCatching {
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("No active session")
    val api = apiFor(session)
    val queryStream = stream.trim().ifBlank { null }
    val raw = BackendApiParser.unwrapOrThrow(api.getObservations(queryStream, day))
    val parsed = BackendApiParser.parseObservations(raw, stream, day ?: todayIso())
    val learnersById = db.learnerDao().observeAll().first().associateBy { it.id }
    val enriched = parsed.map { obs ->
      val lrn = learnersById[obs.learnerId]
      if (lrn != null) obs.copy(learnerName = lrn.fullName) else obs
    }
    if (day == null) {
      db.observationDao().deleteAll()
    }
    if (enriched.isNotEmpty()) {
      db.observationDao().upsertAll(enriched.map { it.toEntity() })
    }
    enriched
  }

  suspend fun fetchSchemes(stream: String, subjectId: String? = null, term: String? = null): Result<List<SchemeOfWork>> = runCatching {
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("No active session")
    val api = apiFor(session)
    val queryStream = stream.trim().ifBlank { null }
    val raw = BackendApiParser.unwrapOrThrow(api.getSchemes(queryStream, subjectId, term))
    val parsed = BackendApiParser.parseSchemes(raw, stream, session.schoolName, session.fullName, term ?: session.currentTerm)
    // Keep edits that have not reached the server yet; they are re-uploaded on
    // the next refresh instead of being wiped by a successful fetch.
    val pending = db.schemeDao().getUnsynced()
    val remoteById = parsed.associateBy { it.id }
    val mergedPending = pending.map { local ->
      val remote = remoteById[local.id] ?: return@map local
      val remoteRows = remote.rows.associateBy { it.week to it.lesson }
      local.copy(
        rows = local.rows.map { row ->
          if (row.remoteId.isNullOrBlank()) {
            val match = remoteRows[row.week to row.lesson]
            if (match?.remoteId != null) row.copy(remoteId = match.remoteId) else row
          } else row
        }
      )
    }
    if (subjectId == null && term == null) {
      db.schemeDao().deleteAll()
    }
    if (parsed.isNotEmpty()) {
      db.schemeDao().upsertAll(parsed.map { it.toEntity() })
    }
    if (mergedPending.isNotEmpty()) {
      db.schemeDao().upsertAll(mergedPending)
    }
    parsed
  }

  suspend fun fetchLessonPlans(fallbackStream: String): Result<Pair<List<LessonPlanTemplateSection>, List<LessonPlan>>> = runCatching {
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("No active session")
    val api = apiFor(session)
    val raw = BackendApiParser.unwrapOrThrow(api.getLessonPlans())
    val (template, plans) = BackendApiParser.parseLessonPlans(raw, session.schoolName, session.fullName, fallbackStream)
    if (template.isNotEmpty()) {
      db.lessonPlanTemplateDao().upsert(
        LessonPlanTemplateEntity(
          name = DefaultLessonPlanTemplate.NAME,
          sectionsJson = templateSectionsAdapter.toJson(template),
          updatedAt = System.currentTimeMillis(),
        )
      )
    }
    val pending = db.lessonPlanDao().getUnsynced()
    db.lessonPlanDao().deleteAll()
    if (plans.isNotEmpty()) {
      db.lessonPlanDao().upsertAll(plans.map { it.toEntity() })
    }
    if (pending.isNotEmpty()) {
      db.lessonPlanDao().upsertAll(pending)
    }
    template to plans
  }

  suspend fun fetchRecordWork(stream: String): Result<List<RecordOfWork>> = runCatching {
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("No active session")
    val api = apiFor(session)
    val queryStream = stream.trim().ifBlank { null }
    val raw = BackendApiParser.unwrapOrThrow(api.getRecordWork(queryStream))
    val parsed = BackendApiParser.parseRecordWork(raw, stream, session.schoolName, session.fullName, session.currentTerm)
    val pending = db.recordOfWorkDao().getUnsynced()
    db.recordOfWorkDao().deleteAll()
    if (parsed.isNotEmpty()) {
      db.recordOfWorkDao().upsertAll(parsed.map { it.toEntity() })
    }
    if (pending.isNotEmpty()) {
      db.recordOfWorkDao().upsertAll(pending)
    }
    parsed
  }

  suspend fun fetchProgress(stream: String): Result<List<LearnerProgressEntry>> = runCatching {
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("No active session")
    val api = apiFor(session)
    val queryStream = stream.trim().ifBlank { null }
    val raw = BackendApiParser.unwrapOrThrow(api.getProgress(queryStream))
    val parsed = BackendApiParser.parseProgress(raw, stream)
    db.learnerProgressDao().deleteAll()
    if (parsed.isNotEmpty()) {
      db.learnerProgressDao().upsertAll(parsed.map { it.toEntity() })
    }
    parsed
  }

  suspend fun fetchTimetable(): Result<List<TimetableSlot>> = runCatching {
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("No active session")
    val api = apiFor(session)
    val raw = BackendApiParser.unwrapOrThrow(api.getTimetable())
    val slots = BackendApiParser.parseTimetable(raw)
    if (slots.isNotEmpty()) {
      ensureTimetableSeeded()
      // Canonical IDs let a server entry replace the bundled template slot
      // without deleting the rest of the week that the server did not return.
      db.timetableDao().upsertAll(
        slots.map { slot ->
          slot.copy(id = "${slot.grade}_${slot.dayOfWeek}_${slot.slotIndex}").toEntity()
        }
      )
    }
    slots
  }

  suspend fun fetchKicdAndResources(
    grade: String,
    subjectId: String,
  ): Result<Pair<List<KicdDesignItem>, List<ApprovedResource>>> = runCatching {
    val key = grade to subjectId
    kicdCache[key]?.let { return@runCatching it }
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("No active session")
    val api = apiFor(session)
    val subjectName = KicdCurriculumRepository.areaNameForSubjectId(subjectId)
    val kicdRaw = BackendApiParser.unwrapOrThrow(api.getKicd(grade, subjectId))
    val designs = BackendApiParser.parseKicdDesigns(kicdRaw, grade, subjectName)
    val resourcesRaw = runCatching { BackendApiParser.unwrapOrThrow(api.getResources(grade, subjectId)) }.getOrDefault("")
    val resources = if (resourcesRaw.isNotBlank()) BackendApiParser.parseResources(resourcesRaw, grade, subjectName) else emptyList()
    val pair = designs to resources
    kicdCache[key] = pair
    pair
  }

  suspend fun fetchPayments(stream: String): Result<List<FeePaymentRecord>> = runCatching {
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("No active session")
    val api = apiFor(session)
    val queryStream = stream.trim().ifBlank { null }
    val raw = BackendApiParser.unwrapOrThrow(api.getPayments(queryStream))
    BackendApiParser.parsePayments(raw)
  }

  suspend fun fetchWhatsApp(learnerId: String? = null): Result<List<GuardianMessage>> = runCatching {
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("No active session")
    val api = apiFor(session)
    val raw = BackendApiParser.unwrapOrThrow(api.getWhatsApp(learnerId))
    val msgs = BackendApiParser.parseWhatsAppMessages(raw)
    if (msgs.isNotEmpty()) {
      db.guardianMessageDao().upsertAll(msgs.map { it.toEntity() })
    }
    msgs
  }

  suspend fun fetchBroadsheetCsv(stream: String): Result<String> = runCatching {
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("No active session")
    val api = apiFor(session)
    BackendApiParser.unwrapOrThrow(api.getBroadsheetCsv(stream.trim().ifBlank { null }))
  }

  /**
   * Syncs all permitted modules for the active stream from the live backend API.
   */
  suspend fun refreshAllForStream(
    stream: String,
    day: String = todayIso(),
  ): Result<DashboardSummary?> = runCatching {
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("No active teacher session")
    // Bundled timetable templates are always written to Room first so the
    // timetable remains readable when the school server cannot be reached.
    ensureTimetableSeeded()
    if (session.authToken.isBlank()) {
      prefs.updateLastSyncedAt(System.currentTimeMillis())
      return@runCatching null
    }
    syncPendingChanges(stream)
    val allowed = session.allowedModules
    val needsRoster = allowed.any { it in TeacherModule.rosterDependentModules }

    if (needsRoster) {
      val rosterRes = fetchLearners(stream)
      if (TeacherModule.LEARNERS in allowed) {
        rosterRes.handleModuleError(TeacherModule.LEARNERS)
      }
    }

    val summary = if (TeacherModule.DASHBOARD in allowed) {
      fetchDashboard(stream).handleModuleError(TeacherModule.DASHBOARD)
    } else null

    if (TeacherModule.ATTENDANCE in allowed) {
      fetchAttendance(stream, day).handleModuleError(TeacherModule.ATTENDANCE)
    }
    if (TeacherModule.MARKSBOOK in allowed || TeacherModule.REPORT_CARDS in allowed || TeacherModule.BROADSHEET in allowed || TeacherModule.PATHWAYS in allowed) {
      fetchMarks(stream).handleModuleError(TeacherModule.MARKSBOOK)
    }
    if (TeacherModule.OBSERVATIONS in allowed) {
      fetchObservations(stream, day).handleModuleError(TeacherModule.OBSERVATIONS)
    }
    if (TeacherModule.SCHEMES in allowed) {
      fetchSchemes(stream).handleModuleError(TeacherModule.SCHEMES)
    }
    if (TeacherModule.RECORD_OF_WORK in allowed) {
      fetchRecordWork(stream).handleModuleError(TeacherModule.RECORD_OF_WORK)
    }
    if (TeacherModule.PROGRESS in allowed) {
      fetchProgress(stream).handleModuleError(TeacherModule.PROGRESS)
    }
    if (TeacherModule.TIMETABLE in allowed) {
      fetchTimetable().handleModuleError(TeacherModule.TIMETABLE)
    }
    prefs.updateLastSyncedAt(System.currentTimeMillis())
    summary
  }

  /**
   * Uploads work created while offline, oldest first. Failures stay marked
   * unsynced and are retried on the next refresh; they are never discarded.
   */
  suspend fun syncPendingChanges(stream: String) {
    val session = prefs.sessionFlow.first() ?: return
    if (session.authToken.isBlank()) return

    for (entity in db.schemeDao().getUnsynced()) {
      val scheme = entity.toDomain()
      val subjectId = KicdCurriculumRepository.subjectIdForArea(scheme.grade, scheme.learningArea)
      saveSchemesBulkRemote(
        scheme.stream.ifBlank { stream },
        scheme.grade,
        subjectId,
        scheme.term,
        scheme.rows,
      )
        .onSuccess { db.schemeDao().markSynced(scheme.id) }
    }

    val schemes = db.schemeDao().getAll().map { it.toDomain() }
    for (entity in db.lessonPlanDao().getUnsynced()) {
      var plan = entity.toDomain()
      if (plan.schemeId.isBlank() && plan.schemeLocalId.isNotBlank()) {
        val scheme = schemes.firstOrNull { it.id == plan.schemeLocalId }
        val row = scheme?.rows?.firstOrNull { it.week == plan.week && it.lesson == plan.lessonNumber }
          ?: scheme?.rows?.firstOrNull { it.week == plan.week }
        plan = plan.copy(schemeId = row?.remoteId.orEmpty())
      }
      saveLessonPlanRemote(plan, isUpdate = plan.remoteId.isNotBlank(), contentMap = plan.contentMapForSync())
        .onSuccess {
          if (plan.remoteId.isNotBlank()) db.lessonPlanDao().markSynced(plan.id, plan.remoteId)
        }
    }

    val plans = db.lessonPlanDao().getAll().map { it.toDomain() }
    for (entity in db.recordOfWorkDao().getUnsynced()) {
      var record = entity.toDomain()
      val plan = plans.firstOrNull { it.id == record.lessonPlanLocalId }
        ?: plans.firstOrNull {
          it.week == record.week && it.lessonNumber == record.lessonNumber &&
            it.stream.equals(record.stream, ignoreCase = true) &&
            (record.schemeId.isBlank() || it.schemeId == record.schemeId)
        }
      if (record.lessonPlanId.isBlank() && plan != null) {
        record = record.copy(lessonPlanId = plan.remoteId)
      }
      if (record.schemeId.isBlank()) {
        val schemeId = plan?.schemeId.orEmpty()
        val scheme = schemes.firstOrNull { it.id == record.schemeLocalId }
        val row = scheme?.rows?.firstOrNull { it.week == record.week && it.lesson == record.lessonNumber }
        record = record.copy(schemeId = schemeId.ifBlank { row?.remoteId.orEmpty() })
      }
      saveRecordWorkRemote(record, isUpdate = record.remoteId.isNotBlank())
        .onSuccess {
          if (record.remoteId.isNotBlank()) db.recordOfWorkDao().markSynced(record.id, record.remoteId)
        }
    }
  }

  private fun LessonPlan.contentMapForSync(): Map<String, String> = buildMap {
    if (extraContent.isNotEmpty()) putAll(extraContent)
    fun putIfMissing(key: String, value: String) {
      if (get(key).isNullOrBlank() && value.isNotBlank()) put(key, value)
    }
    putIfMissing("sec1", specificOutcomes)
    putIfMissing("sec3", coreCompetencies)
    putIfMissing("sec2", keyInquiryQuestions)
    putIfMissing("sec6", learningResources)
    putIfMissing("sec13", organizationOfLearning)
    putIfMissing("sec7", introduction)
    putIfMissing("sec8", lessonDevelopment)
    putIfMissing("sec9", conclusion)
    putIfMissing("sec10", extendedActivity)
    putIfMissing("sec12", reflection)
  }

  private suspend fun <T> Result<T>.handleModuleError(module: TeacherModule): T? {
    val err = exceptionOrNull()
    if (err is ApiException) {
      when {
        err.httpCode == 401 || err.code == "UNAUTHENTICATED" -> {
          clearSessionAndData()
          throw err
        }
        err.code == "MUST_CHANGE_LOGIN" -> {
          val cur = prefs.sessionFlow.first()
          if (cur != null) prefs.saveSession(cur.copy(mustChangePassword = true))
          throw err
        }
        err.code == "SERVICE_DENIED" -> {
          disableServiceInSession(module)
          return null
        }
      }
    }
    return getOrNull()
  }

  // --- Remote Write Operations Matching TEACHER_API ---

  suspend fun createLearnerRemote(
    admissionNo: String,
    nemisUpi: String,
    fullName: String,
    gradeLevel: String,
    stream: String,
    gender: String,
    guardianPhone: String,
  ): Result<Learner> = runCatching {
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("Not signed in")
    val api = apiFor(session)
    val raw = BackendApiParser.unwrapOrThrow(
      api.createLearner(
        LearnerCreateRequestDto(
          name = fullName.trim(),
          admissionNo = admissionNo.trim(),
          stream = stream,
          gradeLevel = gradeLevel,
          gender = gender,
          nemisUpi = nemisUpi.trim(),
          guardianPhone = guardianPhone.trim(),
        )
      )
    )
    val created = BackendApiParser.parseCreatedLearner(raw, stream)
      ?: Learner(
        id = "lrn_${System.currentTimeMillis()}",
        admissionNo = admissionNo.trim(),
        nemisUpi = nemisUpi.trim(),
        fullName = fullName.trim(),
        grade = gradeLevel,
        stream = stream,
        gender = gender,
        guardianPhone = guardianPhone.trim(),
      )
    db.learnerDao().upsert(created.toEntity())
    created
  }

  suspend fun submitAttendanceRemote(
    stream: String,
    day: String,
    items: List<AttendanceRecord>,
  ): Result<Unit> = runCatching {
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("Not signed in")
    val api = apiFor(session)
    val payload = AttendancePostRequestDto(
      items = items.map { rec ->
        val st = when (rec.status) {
          AttendanceStatus.PRESENT -> "present"
          AttendanceStatus.ABSENT -> "absent"
          AttendanceStatus.LATE -> "late"
          AttendanceStatus.EXCUSED -> "absent"
        }
        AttendancePostItemDto(learnerId = rec.learnerId, day = day, status = st)
      }
    )
    BackendApiParser.unwrapOrThrow(api.postAttendance(payload))
    db.attendanceDao().upsertAll(items.map { it.copy(synced = true).toEntity() })
  }

  suspend fun submitMarksRemote(
    stream: String,
    subjectId: String,
    entries: List<MarkEntry>,
  ): Result<Unit> = runCatching {
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("Not signed in")
    val api = apiFor(session)
    val payload = MarksPutRequestDto(
      items = entries.map { MarkPutItemDto(learnerId = it.learnerId, subjectId = subjectId, score = it.score) }
    )
    BackendApiParser.unwrapOrThrow(api.putMarks(payload))
    db.markEntryDao().upsertAll(entries.map { it.copy(synced = true).toEntity() })
  }

  suspend fun submitObservationRemote(
    learner: Learner,
    day: String,
    note: String,
    localRecord: ObservationNote,
  ): Result<Unit> = runCatching {
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("Not signed in")
    val api = apiFor(session)
    val payload = ObservationPostRequestDto(
      items = listOf(ObservationPostItemDto(learnerId = learner.id, day = day, note = note))
    )
    BackendApiParser.unwrapOrThrow(api.postObservations(payload))
    db.observationDao().upsert(localRecord.copy(synced = true).toEntity())
  }

  suspend fun saveSchemesBulkRemote(
    stream: String,
    grade: String,
    subjectId: String,
    term: String,
    rows: List<SchemeRow>,
  ): Result<Unit> = runCatching {
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("Not signed in")
    val api = apiFor(session)
    if (rows.isEmpty()) return@runCatching Unit
    val existingRows = rows.filter { !it.remoteId.isNullOrBlank() }
    val newRows = rows.filter { it.remoteId.isNullOrBlank() }

    for (r in existingRows) {
      BackendApiParser.unwrapOrThrow(
        api.updateSchemeRow(
          SchemeRowApiDto(
            id = r.remoteId,
            stream = stream,
            subjectId = subjectId,
            term = term,
            week = r.week,
            lesson = r.lesson,
            strand = r.strand,
            subStrand = r.subStrand,
            outcomes = r.specificOutcomes,
            experiences = r.learningExperiences,
            inquiry = r.keyInquiryQuestion,
            resources = r.learningResources,
            assessment = r.assessmentMethods,
            remarks = r.reflection,
          )
        )
      )
    }

    if (newRows.size == 1) {
      val r = newRows.first()
      BackendApiParser.unwrapOrThrow(
        api.createSchemeRow(
          SchemeRowApiDto(
            stream = stream,
            subjectId = subjectId,
            term = term,
            week = r.week,
            lesson = r.lesson,
            strand = r.strand,
            subStrand = r.subStrand,
            outcomes = r.specificOutcomes,
            experiences = r.learningExperiences,
            inquiry = r.keyInquiryQuestion,
            resources = r.learningResources,
            assessment = r.assessmentMethods,
            remarks = r.reflection,
          )
        )
      )
    } else if (newRows.size > 1) {
      BackendApiParser.unwrapOrThrow(
        api.createSchemesBulk(
          SchemeBulkRequestDto(
            stream = stream,
            subjectId = subjectId,
            term = term,
            rows = newRows.map { r ->
              SchemeBulkRowDto(
                week = r.week,
                lesson = r.lesson,
                strand = r.strand,
                subStrand = r.subStrand,
                outcomes = r.specificOutcomes,
                experiences = r.learningExperiences,
                inquiry = r.keyInquiryQuestion,
                resources = r.learningResources,
                assessment = r.assessmentMethods,
                remarks = r.reflection,
              )
            },
          )
        )
      )
    }
    runCatching { fetchSchemes(stream, subjectId, term) }
    Unit
  }

  suspend fun deleteSchemeRemote(scheme: SchemeOfWork): Result<Unit> = runCatching {
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("Not signed in")
    val api = apiFor(session)
    for (row in scheme.rows) {
      val rid = row.remoteId ?: continue
      BackendApiParser.unwrapOrThrow(api.deleteSchemeRow(DeleteIdRequestDto(rid)))
    }
    db.schemeDao().deleteById(scheme.id)
  }

  suspend fun saveLessonPlanRemote(
    plan: LessonPlan,
    isUpdate: Boolean,
    contentMap: Map<String, String>,
  ): Result<Unit> = runCatching {
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("Not signed in")
    val api = apiFor(session)
    val subjectId = KicdCurriculumRepository.subjectIdForArea(plan.grade, plan.learningArea)
    val updateRemote = isUpdate && plan.remoteId.isNotBlank()
    val req = LessonPlanApiRequestDto(
      id = if (updateRemote) plan.remoteId else null,
      stream = plan.stream,
      subjectId = subjectId,
      lessonDate = plan.date.ifBlank { todayIso() },
      topic = plan.topic.ifBlank { plan.subStrand },
      subStrand = plan.subStrand.ifBlank { plan.topic },
      schemeId = plan.schemeId.ifBlank { null },
      term = plan.term,
      week = plan.week,
      lesson = plan.lessonNumber,
      strand = plan.strand.ifBlank { plan.topic },
      content = contentMap,
    )
    val raw = if (updateRemote) {
      BackendApiParser.unwrapOrThrow(api.updateLessonPlan(req))
    } else {
      BackendApiParser.unwrapOrThrow(api.createLessonPlan(req))
    }
    val savedRemoteId = if (updateRemote) plan.remoteId else extractCreatedId(raw, "plan", "row")
    if (savedRemoteId.isNotBlank()) {
      db.lessonPlanDao().markSynced(plan.id, savedRemoteId)
    }
    runCatching { fetchLessonPlans(plan.stream) }
    Unit
  }

  suspend fun deleteLessonPlanRemote(plan: LessonPlan): Result<Unit> = runCatching {
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("Not signed in")
    val api = apiFor(session)
    if (plan.remoteId.isNotBlank()) {
      BackendApiParser.unwrapOrThrow(api.deleteLessonPlan(DeleteIdRequestDto(plan.remoteId)))
    }
    db.lessonPlanDao().deleteById(plan.id)
  }

  suspend fun saveRecordWorkRemote(
    record: RecordOfWork,
    isUpdate: Boolean,
  ): Result<Unit> = runCatching {
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("Not signed in")
    val api = apiFor(session)
    val subjectId = KicdCurriculumRepository.subjectIdForArea(record.grade, record.learningArea)
    val updateRemote = isUpdate && record.remoteId.isNotBlank()
    val req = RecordWorkApiDto(
      id = if (updateRemote) record.remoteId else null,
      stream = record.stream,
      subjectId = subjectId,
      day = record.date.ifBlank { todayIso() },
      topic = record.topic.ifBlank { record.subStrand.ifBlank { record.strand } },
      term = record.term,
      strand = record.strand,
      subStrand = record.subStrand,
      workCovered = record.workCovered,
      schemeId = record.schemeId.ifBlank { null },
      lessonPlanId = record.lessonPlanId.ifBlank { null },
      week = record.week,
      lesson = record.lessonNumber,
      classSize = record.classSize,
      absences = record.absencesToday,
      homework = record.homework,
      remarks = record.challengesRemarks,
    )
    val raw = if (updateRemote) {
      BackendApiParser.unwrapOrThrow(api.updateRecordWork(req))
    } else {
      BackendApiParser.unwrapOrThrow(api.createRecordWork(req))
    }
    val savedRemoteId = if (updateRemote) record.remoteId else extractCreatedId(raw, "row", "record")
    if (savedRemoteId.isNotBlank()) {
      db.recordOfWorkDao().markSynced(record.id, savedRemoteId)
    }
    runCatching { fetchRecordWork(record.stream) }
    Unit
  }

  suspend fun deleteRecordWorkRemote(record: RecordOfWork): Result<Unit> = runCatching {
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("Not signed in")
    val api = apiFor(session)
    if (record.remoteId.isNotBlank()) {
      BackendApiParser.unwrapOrThrow(api.deleteRecordWork(DeleteIdRequestDto(record.remoteId)))
    }
    db.recordOfWorkDao().deleteById(record.id)
  }

  private fun extractCreatedId(raw: String, vararg wrappers: String): String {
    val root = runCatching { JSONObject(raw) }.getOrNull() ?: return ""
    for (key in wrappers) {
      val nested = root.optJSONObject(key)
      val id = nested?.optString("id").orEmpty()
      if (id.isNotBlank()) return id
    }
    return root.optString("id").orEmpty()
  }

  suspend fun saveProgressRemote(
    learner: Learner,
    day: String,
    stage: String,
    note: String,
    learningArea: String,
  ): Result<Unit> = runCatching {
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("Not signed in")
    val api = apiFor(session)
    val subjectId = KicdCurriculumRepository.subjectIdForArea(learner.grade, learningArea)
    BackendApiParser.unwrapOrThrow(
      api.createProgress(
        ProgressPostRequestDto(
          learnerId = learner.id,
          day = day,
          stage = stage,
          note = note.trim().ifBlank { null },
          subjectId = subjectId,
        )
      )
    )
    fetchProgress(learner.stream)
  }

  suspend fun deleteProgressRemote(id: String): Result<Unit> = runCatching {
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("Not signed in")
    val api = apiFor(session)
    BackendApiParser.unwrapOrThrow(api.deleteProgress(DeleteIdRequestDto(id)))
    db.learnerProgressDao().deleteById(id)
  }

  suspend fun postFeePaymentRemote(
    learner: Learner,
    amount: Double,
    method: String,
    reference: String,
    day: String,
    note: String,
  ): Result<Unit> = runCatching {
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("Not signed in")
    val api = apiFor(session)
    BackendApiParser.unwrapOrThrow(
      api.createPayment(
        PaymentPostRequestDto(
          learnerId = learner.id,
          amount = amount,
          method = method,
          reference = reference,
          day = day,
          note = note.trim().ifBlank { null },
        )
      )
    )
    val updated = (learner.feeBalanceKes - amount).coerceAtLeast(0.0)
    db.learnerDao().updateFeeBalance(learner.id, updated)
  }

  suspend fun postWhatsAppRemote(
    learner: Learner,
    message: String,
  ): Result<Unit> = runCatching {
    val session = prefs.sessionFlow.first() ?: throw IllegalStateException("Not signed in")
    val api = apiFor(session)
    BackendApiParser.unwrapOrThrow(
      api.postWhatsApp(
        WhatsAppPostRequestDto(
          learnerId = learner.id,
          message = message,
        )
      )
    )
    fetchWhatsApp(learner.id)
  }

  // --- Local Room helpers (preserved for unit testing & local caching) ---

  suspend fun setDarkMode(enabled: Boolean) = prefs.setDarkMode(enabled)

  suspend fun saveScheme(scheme: SchemeOfWork) = db.schemeDao().upsert(scheme.toEntity())
  suspend fun markSchemeSynced(id: String) = db.schemeDao().markSynced(id)
  suspend fun deleteScheme(id: String) = db.schemeDao().deleteById(id)

  suspend fun saveLessonPlan(plan: LessonPlan) = db.lessonPlanDao().upsert(plan.toEntity())
  suspend fun deleteLessonPlan(id: String) = db.lessonPlanDao().deleteById(id)

  suspend fun saveRecordOfWork(record: RecordOfWork) = db.recordOfWorkDao().upsert(record.toEntity())
  suspend fun deleteRecordOfWork(id: String) = db.recordOfWorkDao().deleteById(id)

  suspend fun saveLearner(learner: Learner) = db.learnerDao().upsert(learner.toEntity())
  suspend fun deleteLearner(id: String) = db.learnerDao().deleteById(id)

  suspend fun postFeePayment(learnerId: String, currentBalance: Double, amountPaid: Double) {
    val updated = (currentBalance - amountPaid).coerceAtLeast(0.0)
    db.learnerDao().updateFeeBalance(learnerId, updated)
  }

  suspend fun saveAttendanceBatch(records: List<AttendanceRecord>) {
    db.attendanceDao().upsertAll(records.map { it.toEntity() })
  }

  suspend fun saveMarkEntry(entry: MarkEntry) = db.markEntryDao().upsert(entry.toEntity())
  suspend fun saveMarkEntries(entries: List<MarkEntry>) = db.markEntryDao().upsertAll(entries.map { it.toEntity() })

  suspend fun saveObservation(note: ObservationNote) = db.observationDao().upsert(note.toEntity())
  suspend fun deleteObservation(id: String) = db.observationDao().deleteById(id)

  suspend fun saveProgressEntry(entry: LearnerProgressEntry) = db.learnerProgressDao().upsert(entry.toEntity())
  suspend fun saveGuardianMessage(msg: GuardianMessage) = db.guardianMessageDao().upsert(msg.toEntity())

  // --- Mapping Helpers ---
  private fun SchemeEntity.toDomain(): SchemeOfWork {
    val parsedRows = runCatching { rowsAdapter.fromJson(rowsJson) }.getOrNull().orEmpty()
    return SchemeOfWork(id, title, schoolName, teacherName, grade, stream, learningArea, term, year, parsedRows, updatedAt, synced)
  }

  private fun SchemeOfWork.toEntity(): SchemeEntity =
    SchemeEntity(id, title, schoolName, teacherName, grade, stream, learningArea, term, year, rowsAdapter.toJson(rows), updatedAt, synced)

  private fun LessonPlanEntity.toDomain(): LessonPlan = LessonPlan(
    id = id, schoolName = schoolName, teacherName = teacherName, grade = grade, stream = stream,
    learningArea = learningArea, term = term, week = week, lessonNumber = lessonNumber, date = date,
    timeSlot = timeSlot, rollCount = rollCount, strand = strand, subStrand = subStrand, topic = topic,
    specificOutcomes = specificOutcomes, keyInquiryQuestions = keyInquiryQuestions,
    coreCompetencies = coreCompetencies, values = valuesText, pcis = pcis,
    learningResources = learningResources, organizationOfLearning = organizationOfLearning,
    introduction = introduction, lessonDevelopment = lessonDevelopment, conclusion = conclusion,
    extendedActivity = extendedActivity, reflection = reflection, updatedAt = updatedAt, synced = synced,
    remoteId = remoteId, schemeId = schemeId, schemeLocalId = schemeLocalId,
    extraContent = runCatching { contentMapAdapter.fromJson(extraContentJson) }.getOrNull().orEmpty(),
  )

  private fun LessonPlan.toEntity(): LessonPlanEntity = LessonPlanEntity(
    id = id, schoolName = schoolName, teacherName = teacherName, grade = grade, stream = stream,
    learningArea = learningArea, term = term, week = week, lessonNumber = lessonNumber, date = date,
    timeSlot = timeSlot, rollCount = rollCount, strand = strand, subStrand = subStrand, topic = topic,
    specificOutcomes = specificOutcomes, keyInquiryQuestions = keyInquiryQuestions,
    coreCompetencies = coreCompetencies, valuesText = values, pcis = pcis,
    learningResources = learningResources, organizationOfLearning = organizationOfLearning,
    introduction = introduction, lessonDevelopment = lessonDevelopment, conclusion = conclusion,
    extendedActivity = extendedActivity, reflection = reflection, updatedAt = updatedAt, synced = synced,
    remoteId = remoteId, schemeId = schemeId, schemeLocalId = schemeLocalId,
    extraContentJson = contentMapAdapter.toJson(extraContent),
  )

  private fun RecordOfWorkEntity.toDomain(): RecordOfWork = RecordOfWork(
    id = id, schoolName = schoolName, teacherName = teacherName, grade = grade, stream = stream,
    learningArea = learningArea, term = term, year = year, week = week, date = date, strand = strand,
    subStrand = subStrand, workCovered = workCovered, absencesToday = absencesToday, homework = homework,
    challengesRemarks = challengesRemarks, teacherInitials = teacherInitials, updatedAt = updatedAt,
    synced = synced, topic = topic, lessonNumber = lessonNumber, classSize = classSize,
    schemeId = schemeId, lessonPlanId = lessonPlanId, remoteId = remoteId, schemeLocalId = schemeLocalId,
    lessonPlanLocalId = lessonPlanLocalId,
  )

  private fun RecordOfWork.toEntity(): RecordOfWorkEntity = RecordOfWorkEntity(
    id = id, schoolName = schoolName, teacherName = teacherName, grade = grade, stream = stream,
    learningArea = learningArea, term = term, year = year, week = week, date = date, strand = strand,
    subStrand = subStrand, workCovered = workCovered, absencesToday = absencesToday, homework = homework,
    challengesRemarks = challengesRemarks, teacherInitials = teacherInitials, updatedAt = updatedAt,
    synced = synced, topic = topic, lessonNumber = lessonNumber, classSize = classSize,
    schemeId = schemeId, lessonPlanId = lessonPlanId, remoteId = remoteId, schemeLocalId = schemeLocalId,
    lessonPlanLocalId = lessonPlanLocalId,
  )

  private fun LearnerEntity.toDomain(): Learner = Learner(
    id, admissionNo, nemisUpi, fullName, grade, stream, gender, guardianPhone, pathwayInterest,
    totalFeeKes, feeBalanceKes, attendanceHistory5Days, termAttendancePct,
  )

  private fun Learner.toEntity(): LearnerEntity = LearnerEntity(
    id, admissionNo, nemisUpi, fullName, grade, stream, gender, guardianPhone, pathwayInterest,
    totalFeeKes, feeBalanceKes, attendanceHistory5Days, termAttendancePct,
  )

  private fun AttendanceEntity.toDomain(): AttendanceRecord = AttendanceRecord(
    id, date, grade, stream, learningArea, lessonSlot, learnerId, learnerName, admissionNo,
    runCatching { AttendanceStatus.valueOf(statusName) }.getOrDefault(AttendanceStatus.PRESENT), remarks, synced,
  )

  private fun AttendanceRecord.toEntity(): AttendanceEntity =
    AttendanceEntity(id, date, grade, stream, learningArea, lessonSlot, learnerId, learnerName, admissionNo, status.name, remarks, synced)

  private fun MarkEntryEntity.toDomain(): MarkEntry = MarkEntry(
    id, learnerId, learnerName, admissionNo, grade, stream, learningArea, term, assessmentType,
    score, maxScore, CbcPerformanceLevel.fromScore(score), teacherComment, updatedAt, synced,
  )

  private fun MarkEntry.toEntity(): MarkEntryEntity = MarkEntryEntity(
    id, learnerId, learnerName, admissionNo, grade, stream, learningArea, term, assessmentType,
    score, maxScore, CbcPerformanceLevel.fromScore(score).code, teacherComment, updatedAt, synced,
  )

  private fun ObservationEntity.toDomain(): ObservationNote = ObservationNote(
    id, learnerId, learnerName, grade, observationType, competency,
    CbcPerformanceLevel.entries.firstOrNull { it.code == ratingCode } ?: CbcPerformanceLevel.ME, note, date, synced,
  )

  private fun ObservationNote.toEntity(): ObservationEntity =
    ObservationEntity(id, learnerId, learnerName, grade, observationType, competency, rating.code, note, date, synced)

  private fun LearnerProgressEntity.toDomain(): LearnerProgressEntry =
    LearnerProgressEntry(id, learnerId, learnerName, grade, learningArea, stage, note, date, synced)

  private fun LearnerProgressEntry.toEntity(): LearnerProgressEntity =
    LearnerProgressEntity(id, learnerId, learnerName, grade, learningArea, stage, note, date, synced)

  private fun GuardianMessageEntity.toDomain(): GuardianMessage =
    GuardianMessage(id, learnerId, learnerName, guardianPhone, grade, template, message, date, synced)

  private fun GuardianMessage.toEntity(): GuardianMessageEntity =
    GuardianMessageEntity(id, learnerId, learnerName, guardianPhone, grade, template, message, date, synced)

  private fun TimetableSlotEntity.toDomain(): TimetableSlot =
    TimetableSlot(id, grade, dayOfWeek, slotIndex, timeRange, learningArea, isBreak, room, teacherId)

  private fun TimetableSlot.toEntity(): TimetableSlotEntity =
    TimetableSlotEntity(id, grade, dayOfWeek, slotIndex, timeRange, learningArea, isBreak, room, teacherId)
}

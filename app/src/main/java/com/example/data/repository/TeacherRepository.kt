package com.example.data.repository

import com.example.data.curriculum.KicdCurriculumRepository
import com.example.data.local.AttendanceEntity
import com.example.data.local.ElimuTrackDatabase
import com.example.data.local.GuardianMessageEntity
import com.example.data.local.LearnerEntity
import com.example.data.local.LearnerProgressEntity
import com.example.data.local.LessonPlanEntity
import com.example.data.local.MarkEntryEntity
import com.example.data.local.ObservationEntity
import com.example.data.local.RecordOfWorkEntity
import com.example.data.local.SchemeEntity
import com.example.data.local.SessionPreferences
import com.example.data.local.TimetableSlotEntity
import com.example.data.remote.AttendanceDto
import com.example.data.remote.LessonPlanDto
import com.example.data.remote.LoginRequestDto
import com.example.data.remote.MarkEntryDto
import com.example.data.remote.NetworkModule
import com.example.data.remote.ObservationDto
import com.example.data.remote.RecordOfWorkDto
import com.example.data.remote.SchemeDto
import com.example.data.remote.TeacherBootstrapDto
import com.example.domain.model.AllocatedClass
import com.example.domain.model.AttendanceRecord
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.CbcPerformanceLevel
import com.example.domain.model.GuardianMessage
import com.example.domain.model.Learner
import com.example.domain.model.LearnerProgressEntry
import com.example.domain.model.LessonPlan
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

class TeacherRepository(
  private val db: ElimuTrackDatabase,
  private val prefs: SessionPreferences,
) {
  private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
  private val rowListType = Types.newParameterizedType(List::class.java, SchemeRow::class.java)
  private val rowsAdapter = moshi.adapter<List<SchemeRow>>(rowListType)

  val sessionFlow: Flow<TeacherSession?> = prefs.sessionFlow
  val backendUrlFlow: Flow<String> = prefs.backendUrlFlow
  val darkModeFlow: Flow<Boolean> = prefs.darkModeFlow
  val lastSyncedAtFlow: Flow<Long?> = prefs.lastSyncedAtFlow

  val schemesFlow: Flow<List<SchemeOfWork>> = db.schemeDao().observeAll().map { list ->
    list.map { it.toDomain() }
  }

  val lessonPlansFlow: Flow<List<LessonPlan>> = db.lessonPlanDao().observeAll().map { list ->
    list.map { it.toDomain() }
  }

  val recordsOfWorkFlow: Flow<List<RecordOfWork>> = db.recordOfWorkDao().observeAll().map { list ->
    list.map { it.toDomain() }
  }

  val learnersFlow: Flow<List<Learner>> = db.learnerDao().observeAll().map { list ->
    list.map { it.toDomain() }
  }

  val attendanceFlow: Flow<List<AttendanceRecord>> = db.attendanceDao().observeAll().map { list ->
    list.map { it.toDomain() }
  }

  val marksFlow: Flow<List<MarkEntry>> = db.markEntryDao().observeAll().map { list ->
    list.map { it.toDomain() }
  }

  val observationsFlow: Flow<List<ObservationNote>> = db.observationDao().observeAll().map { list ->
    list.map { it.toDomain() }
  }

  val progressEntriesFlow: Flow<List<LearnerProgressEntry>> = db.learnerProgressDao().observeAll().map { list ->
    list.map { it.toDomain() }
  }

  val guardianMessagesFlow: Flow<List<GuardianMessage>> = db.guardianMessageDao().observeAll().map { list ->
    list.map { it.toDomain() }
  }

  val timetableFlow: Flow<List<TimetableSlot>> = db.timetableDao().observeAll().map { list ->
    list.map { it.toDomain() }
  }

  suspend fun ensureTimetableSeeded() {
    if (db.timetableDao().count() == 0) {
      val defaults = KicdCurriculumRepository.cbcGrades.flatMap { grade ->
        KicdCurriculumRepository.defaultTimetableForGrade(grade)
      }
      db.timetableDao().upsertAll(defaults.map { it.toEntity() })
    } else {
      KicdCurriculumRepository.cbcGrades.forEach { grade ->
        val expected = KicdCurriculumRepository.timeSlotsForGrade(grade).size * 5
        if (db.timetableDao().countForGrade(grade) < expected) {
          db.timetableDao().deleteForGrade(grade)
          db.timetableDao().upsertAll(
            KicdCurriculumRepository.defaultTimetableForGrade(grade).map { it.toEntity() }
          )
        }
      }
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

  suspend fun loginToBackend(
    backendUrl: String,
    identifier: String,
    password: String,
  ): Result<TeacherSession> = runCatching {
    val normalized = NetworkModule.normalizeBaseUrl(backendUrl)
    val api = NetworkModule.createService(normalized)
    val response = api.login(LoginRequestDto(identifier.trim(), password))
    if (!response.isSuccessful) {
      throw IllegalStateException("Backend returned HTTP ${response.code()}: ${response.message()}")
    }
    val body = response.body() ?: throw IllegalStateException("Empty login response from server")
    val teacher = body.teacher
    val session = TeacherSession(
      teacherId = teacher.teacherId,
      fullName = teacher.fullName,
      username = identifier.trim().substringBefore("@"),
      email = teacher.email,
      tscNumber = teacher.tscNumber,
      schoolName = teacher.schoolName,
      currentTerm = teacher.currentTerm,
      currentYear = teacher.currentYear,
      badgeLabel = teacher.badgeLabel,
      backendUrl = normalized,
      authToken = body.token,
      isOfflineWorkspace = false,
      allowedModuleKeys = teacher.allowedModules.ifEmpty {
        TeacherModule.defaultTeacherModules.map { it.key }
      },
      allocatedClasses = teacher.allocatedClasses.map {
        AllocatedClass(
          id = it.id,
          grade = it.grade,
          stream = it.stream,
          learningAreas = it.learningAreas,
          learnerCount = it.learnerCount,
        )
      },
    )
    prefs.saveSession(session)
    ensureTimetableSeeded()
    syncWithBackend(session)
    session
  }

  suspend fun enterOfflineWorkspace(
    teacherName: String,
    tscNumber: String,
    schoolName: String,
    backendUrl: String,
    allocatedGrades: List<String>,
    username: String = "",
  ): TeacherSession {
    val defaultStreams = listOf("Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta", "Theta", "Iota")
    val classes = allocatedGrades.ifEmpty { listOf("Grade 4", "Grade 7", "Grade 9") }.mapIndexed { idx, grade ->
      val gradeNum = grade.filter { it.isDigit() }.toIntOrNull()?.minus(1) ?: idx
      val stream = defaultStreams.getOrElse(gradeNum) { "East" }
      val level = KicdCurriculumRepository.levelForGrade(grade)
      val areas = KicdCurriculumRepository.cbcLearningAreasByLevel[level].orEmpty()
      AllocatedClass(
        id = "cls_${grade.lowercase().replace(" ", "_")}_$idx",
        grade = grade,
        stream = stream,
        learningAreas = areas,
        learnerCount = 0,
      )
    }
    val resolvedName = teacherName.ifBlank { "Alvin Ojiambo" }
    val resolvedUsername = username.trim().ifBlank {
      resolvedName.trim().lowercase().replace(" ", ".").ifBlank { "alvina" }
    }
    val session = TeacherSession(
      teacherId = "local_${tscNumber.ifBlank { "teacher" }}",
      fullName = resolvedName,
      username = resolvedUsername,
      email = "teacher@fretsschool.ac.ke",
      tscNumber = tscNumber.ifBlank { "TSC-482910" },
      schoolName = schoolName.ifBlank { "Fret's School" },
      currentTerm = "Term 3",
      currentYear = 2026,
      badgeLabel = "CAMPUS PREMIUM",
      backendUrl = NetworkModule.normalizeBaseUrl(backendUrl),
      authToken = "",
      isOfflineWorkspace = true,
      allowedModuleKeys = TeacherModule.defaultTeacherModules.map { it.key },
      allocatedClasses = classes,
    )
    prefs.saveSession(session)
    ensureTimetableSeeded()
    return session
  }

  suspend fun syncWithBackend(activeSession: TeacherSession? = null): Result<String> = runCatching {
    val session = activeSession ?: prefs.sessionFlow.first()
      ?: throw IllegalStateException("No active teacher session")
    if (session.isOfflineWorkspace || session.authToken.isBlank()) {
      ensureTimetableSeeded()
      prefs.updateLastSyncedAt(System.currentTimeMillis())
      return@runCatching "Everything is synced (Local workspace)"
    }
    val api = NetworkModule.createService(session.backendUrl, session.authToken)

    val unsyncedSchemes = db.schemeDao().getUnsynced()
    if (unsyncedSchemes.isNotEmpty() && api.pushSchemes(unsyncedSchemes.map { it.toDto() }).isSuccessful) {
      db.schemeDao().upsertAll(unsyncedSchemes.map { it.copy(synced = true) })
    }
    val unsyncedPlans = db.lessonPlanDao().getUnsynced()
    if (unsyncedPlans.isNotEmpty() && api.pushLessonPlans(unsyncedPlans.map { it.toDto() }).isSuccessful) {
      db.lessonPlanDao().upsertAll(unsyncedPlans.map { it.copy(synced = true) })
    }
    val unsyncedRecords = db.recordOfWorkDao().getUnsynced()
    if (unsyncedRecords.isNotEmpty() && api.pushRecordsOfWork(unsyncedRecords.map { it.toDto() }).isSuccessful) {
      db.recordOfWorkDao().upsertAll(unsyncedRecords.map { it.copy(synced = true) })
    }
    val unsyncedAttendance = db.attendanceDao().getUnsynced()
    if (unsyncedAttendance.isNotEmpty() && api.pushAttendance(unsyncedAttendance.map { it.toDto() }).isSuccessful) {
      db.attendanceDao().upsertAll(unsyncedAttendance.map { it.copy(synced = true) })
    }
    val unsyncedMarks = db.markEntryDao().getUnsynced()
    if (unsyncedMarks.isNotEmpty() && api.pushMarks(unsyncedMarks.map { it.toDto() }).isSuccessful) {
      db.markEntryDao().upsertAll(unsyncedMarks.map { it.copy(synced = true) })
    }
    val unsyncedObs = db.observationDao().getUnsynced()
    if (unsyncedObs.isNotEmpty() && api.pushObservations(unsyncedObs.map { it.toDto() }).isSuccessful) {
      db.observationDao().upsertAll(unsyncedObs.map { it.copy(synced = true) })
    }

    val bootstrapRes = api.fetchBootstrap()
    if (!bootstrapRes.isSuccessful) {
      throw IllegalStateException("Sync failed: HTTP ${bootstrapRes.code()}")
    }
    val bootstrap: TeacherBootstrapDto = bootstrapRes.body()
      ?: throw IllegalStateException("Empty bootstrap payload")

    val updatedSession = session.copy(
      fullName = bootstrap.teacher.fullName,
      schoolName = bootstrap.teacher.schoolName,
      currentTerm = bootstrap.teacher.currentTerm,
      currentYear = bootstrap.teacher.currentYear,
      badgeLabel = bootstrap.teacher.badgeLabel,
      allowedModuleKeys = bootstrap.teacher.allowedModules.ifEmpty { session.allowedModuleKeys },
      allocatedClasses = bootstrap.teacher.allocatedClasses.map {
        AllocatedClass(it.id, it.grade, it.stream, it.learningAreas, it.learnerCount)
      },
    )
    prefs.saveSession(updatedSession)

    if (bootstrap.learners.isNotEmpty()) {
      db.learnerDao().upsertAll(bootstrap.learners.map {
        LearnerEntity(
          id = it.id,
          admissionNo = it.admissionNo,
          nemisUpi = "NEM-${it.admissionNo.takeLast(4)}",
          fullName = it.fullName,
          grade = it.grade,
          stream = it.stream,
          gender = it.gender,
          guardianPhone = it.guardianPhone,
          pathwayInterest = it.pathwayInterest,
          totalFeeKes = 45000.0,
          feeBalanceKes = it.feeBalanceKes,
          attendanceHistory5Days = "PPPPP",
          termAttendancePct = 95,
        )
      })
    }
    if (bootstrap.timetable.isNotEmpty()) {
      db.timetableDao().upsertAll(bootstrap.timetable.map {
        TimetableSlotEntity(it.id, it.grade, it.dayOfWeek, it.slotIndex, it.timeRange, it.learningArea, it.isBreak, it.room, it.teacherId)
      })
    }
    if (bootstrap.marks.isNotEmpty()) {
      db.markEntryDao().upsertAll(bootstrap.marks.map {
        MarkEntryEntity(
          id = it.id,
          learnerId = it.learnerId,
          learnerName = it.learnerName,
          admissionNo = it.admissionNo,
          grade = it.grade,
          stream = it.stream,
          learningArea = it.learningArea,
          term = it.term,
          assessmentType = it.assessmentType,
          score = it.score,
          maxScore = it.maxScore,
          levelCode = CbcPerformanceLevel.fromScore(it.score).code,
          teacherComment = it.teacherComment,
          updatedAt = System.currentTimeMillis(),
          synced = true,
        )
      })
    }
    if (bootstrap.attendance.isNotEmpty()) {
      db.attendanceDao().upsertAll(bootstrap.attendance.map {
        AttendanceEntity(it.id, it.date, it.grade, it.stream, it.learningArea, it.lessonSlot, it.learnerId, it.learnerName, it.admissionNo, it.status, it.remarks, true)
      })
    }
    if (bootstrap.schemes.isNotEmpty()) {
      db.schemeDao().upsertAll(bootstrap.schemes.map {
        SchemeEntity(it.id, it.title, it.schoolName, it.teacherName, it.grade, it.learningArea, it.term, it.year, it.rowsJson, it.updatedAt, true)
      })
    }
    if (bootstrap.lessonPlans.isNotEmpty()) {
      db.lessonPlanDao().upsertAll(bootstrap.lessonPlans.map {
        LessonPlanEntity(
          it.id, it.schoolName, it.teacherName, it.grade, it.stream, it.learningArea, it.term, it.week, it.lessonNumber, it.date, it.timeSlot, it.rollCount,
          it.strand, it.subStrand, it.subStrand, it.specificOutcomes, it.keyInquiryQuestions, it.coreCompetencies, it.values, it.pcis, it.learningResources,
          "Pair and small group work", it.introduction, it.lessonDevelopment, it.conclusion, it.extendedActivity, it.reflection, it.updatedAt, true,
        )
      })
    }
    if (bootstrap.recordsOfWork.isNotEmpty()) {
      db.recordOfWorkDao().upsertAll(bootstrap.recordsOfWork.map {
        RecordOfWorkEntity(
          it.id, it.schoolName, it.teacherName, it.grade, it.stream, it.learningArea, it.term, it.year, it.week, it.date, it.strand, it.subStrand,
          it.workCovered, "", "", it.challengesRemarks, it.teacherInitials, it.updatedAt, true,
        )
      })
    }
    if (bootstrap.observations.isNotEmpty()) {
      db.observationDao().upsertAll(bootstrap.observations.map {
        ObservationEntity(it.id, it.learnerId, it.learnerName, it.grade, "Strength", it.competency, it.ratingCode, it.note, it.date, true)
      })
    }

    prefs.updateLastSyncedAt(System.currentTimeMillis())
    "Synced with ElimuTrack backend"
  }

  suspend fun updateModulePermissions(moduleKeys: List<String>) {
    val current = prefs.sessionFlow.first() ?: return
    prefs.saveSession(current.copy(allowedModuleKeys = moduleKeys))
  }

  suspend fun updateAllocatedClasses(classes: List<AllocatedClass>) {
    val current = prefs.sessionFlow.first() ?: return
    prefs.saveSession(current.copy(allocatedClasses = classes))
  }

  suspend fun updateLoginCredentials(username: String, mustChangePassword: Boolean) {
    val current = prefs.sessionFlow.first() ?: return
    prefs.saveSession(current.copy(username = username, mustChangePassword = mustChangePassword))
  }

  suspend fun setDarkMode(enabled: Boolean) = prefs.setDarkMode(enabled)

  suspend fun logout() = prefs.clearSession()

  suspend fun saveScheme(scheme: SchemeOfWork) = db.schemeDao().upsert(scheme.toEntity())
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

  suspend fun populateStarterClassRoster(grade: String, stream: String) {
    val prefix = grade.filter { it.isDigit() }.ifBlank { "1" }
    val starterLearners = listOf(
      Learner("lrn_${prefix}_1", "ADM-41${prefix}0", "NEM-70${prefix}1", "Amina Mohamed", grade, stream, "Female", "+254712000101", "STEM", 45000.0, 0.0, "PPPPP", 100),
      Learner("lrn_${prefix}_2", "ADM-41${prefix}1", "NEM-70${prefix}2", "Brian Otieno", grade, stream, "Male", "+254712097102", "STEM", 45000.0, 15000.0, "PPAPP", 92),
      Learner("lrn_${prefix}_3", "ADM-41${prefix}2", "NEM-70${prefix}3", "Cynthia Wanjiru", grade, stream, "Female", "+254712194103", "Social Sciences", 45000.0, 0.0, "PPPAP", 96),
      Learner("lrn_${prefix}_4", "ADM-41${prefix}3", "NEM-70${prefix}4", "Dennis Kiprop", grade, stream, "Male", "+254712291104", "Arts & Sports Science", 45000.0, 30000.0, "APAAP", 78),
      Learner("lrn_${prefix}_5", "ADM-41${prefix}4", "NEM-70${prefix}5", "Esther Achieng", grade, stream, "Female", "+254712388105", "STEM", 45000.0, 0.0, "PPPPP", 100),
      Learner("lrn_${prefix}_6", "ADM-41${prefix}5", "NEM-70${prefix}6", "Hassan Abdi", grade, stream, "Male", "+254712485106", "Social Sciences", 45000.0, 7000.0, "PAPPP", 88),
    )
    db.learnerDao().upsertAll(starterLearners.map { it.toEntity() })
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

  // Mapping Helpers
  private fun SchemeEntity.toDomain(): SchemeOfWork {
    val parsedRows = runCatching { rowsAdapter.fromJson(rowsJson) }.getOrNull().orEmpty()
    return SchemeOfWork(id, title, schoolName, teacherName, grade, learningArea, term, year, parsedRows, updatedAt, synced)
  }

  private fun SchemeOfWork.toEntity(): SchemeEntity =
    SchemeEntity(id, title, schoolName, teacherName, grade, learningArea, term, year, rowsAdapter.toJson(rows), updatedAt, synced)

  private fun SchemeEntity.toDto(): SchemeDto =
    SchemeDto(id, title, schoolName, teacherName, grade, learningArea, term, year, rowsJson, updatedAt)

  private fun LessonPlanEntity.toDomain(): LessonPlan = LessonPlan(
    id, schoolName, teacherName, grade, stream, learningArea, term, week, lessonNumber, date, timeSlot, rollCount,
    strand, subStrand, topic, specificOutcomes, keyInquiryQuestions, coreCompetencies, valuesText, pcis,
    learningResources, organizationOfLearning, introduction, lessonDevelopment, conclusion, extendedActivity, reflection, updatedAt, synced,
  )

  private fun LessonPlan.toEntity(): LessonPlanEntity = LessonPlanEntity(
    id, schoolName, teacherName, grade, stream, learningArea, term, week, lessonNumber, date, timeSlot, rollCount,
    strand, subStrand, topic, specificOutcomes, keyInquiryQuestions, coreCompetencies, values, pcis,
    learningResources, organizationOfLearning, introduction, lessonDevelopment, conclusion, extendedActivity, reflection, updatedAt, synced,
  )

  private fun LessonPlanEntity.toDto(): LessonPlanDto = LessonPlanDto(
    id, schoolName, teacherName, grade, stream, learningArea, term, week, lessonNumber, date, timeSlot, rollCount,
    strand, subStrand, specificOutcomes, keyInquiryQuestions, coreCompetencies, valuesText, pcis,
    learningResources, introduction, lessonDevelopment, conclusion, extendedActivity, reflection, updatedAt,
  )

  private fun RecordOfWorkEntity.toDomain(): RecordOfWork = RecordOfWork(
    id, schoolName, teacherName, grade, stream, learningArea, term, year, week, date, strand, subStrand,
    workCovered, absencesToday, homework, challengesRemarks, teacherInitials, updatedAt, synced,
  )

  private fun RecordOfWork.toEntity(): RecordOfWorkEntity = RecordOfWorkEntity(
    id, schoolName, teacherName, grade, stream, learningArea, term, year, week, date, strand, subStrand,
    workCovered, absencesToday, homework, challengesRemarks, teacherInitials, updatedAt, synced,
  )

  private fun RecordOfWorkEntity.toDto(): RecordOfWorkDto = RecordOfWorkDto(
    id, schoolName, teacherName, grade, stream, learningArea, term, year, week, date, strand, subStrand,
    workCovered, challengesRemarks, teacherInitials, updatedAt,
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

  private fun AttendanceEntity.toDto(): AttendanceDto =
    AttendanceDto(id, date, grade, stream, learningArea, lessonSlot, learnerId, learnerName, admissionNo, statusName, remarks)

  private fun MarkEntryEntity.toDomain(): MarkEntry = MarkEntry(
    id, learnerId, learnerName, admissionNo, grade, stream, learningArea, term, assessmentType,
    score, maxScore, CbcPerformanceLevel.fromScore(score), teacherComment, updatedAt, synced,
  )

  private fun MarkEntry.toEntity(): MarkEntryEntity = MarkEntryEntity(
    id, learnerId, learnerName, admissionNo, grade, stream, learningArea, term, assessmentType,
    score, maxScore, CbcPerformanceLevel.fromScore(score).code, teacherComment, updatedAt, synced,
  )

  private fun MarkEntryEntity.toDto(): MarkEntryDto =
    MarkEntryDto(id, learnerId, learnerName, admissionNo, grade, stream, learningArea, term, assessmentType, score, maxScore, teacherComment)

  private fun ObservationEntity.toDomain(): ObservationNote = ObservationNote(
    id, learnerId, learnerName, grade, observationType, competency,
    CbcPerformanceLevel.entries.firstOrNull { it.code == ratingCode } ?: CbcPerformanceLevel.ME, note, date, synced,
  )

  private fun ObservationNote.toEntity(): ObservationEntity =
    ObservationEntity(id, learnerId, learnerName, grade, observationType, competency, rating.code, note, date, synced)

  private fun ObservationEntity.toDto(): ObservationDto =
    ObservationDto(id, learnerId, learnerName, grade, competency, ratingCode, note, date)

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

package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.curriculum.KicdCurriculumRepository
import com.example.data.local.ElimuTrackDatabase
import com.example.data.local.SessionPreferences
import com.example.data.remote.BackendApiParser
import com.example.data.remote.NetworkModule
import com.example.data.repository.TeacherRepository
import com.example.domain.model.AllocatedClass
import com.example.domain.model.AttendanceRecord
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.CbcPerformanceLevel
import com.example.domain.model.Learner
import com.example.domain.model.LessonPlan
import com.example.domain.model.MarkEntry
import com.example.domain.model.RecordOfWork
import com.example.domain.model.SchemeOfWork
import com.example.domain.model.SchemeRow
import com.example.domain.model.TeacherModule
import com.example.domain.model.TeacherSession
import com.example.ui.viewmodel.TeacherUiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `app name matches ElimuTrack`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ElimuTrack", appName)
  }

  @Test
  fun `admin module permissions filter teacher modules accurately`() {
    val session = TeacherSession(
      teacherId = "t1",
      fullName = "Fret Mwaniki",
      email = "fret@school.ke",
      tscNumber = "TSC-100",
      schoolName = "Fret's School",
      allowedModuleKeys = listOf("dashboard", "schemes", "lessonplans", "recordwork", "attendance", "marks", "fees", "kicd"),
    )
    val allowed = session.allowedModules
    assertTrue(TeacherModule.DASHBOARD in allowed)
    assertTrue(TeacherModule.SCHEMES in allowed)
    assertTrue(TeacherModule.LESSON_PLANS in allowed)
    assertTrue(TeacherModule.RECORD_OF_WORK in allowed)
    assertTrue(TeacherModule.ATTENDANCE in allowed)
    assertTrue(TeacherModule.MARKSBOOK in allowed)
    assertTrue(TeacherModule.FEE_PAYMENTS in allowed)
    assertTrue(TeacherModule.KICD_DESIGNS in allowed)
    assertFalse(TeacherModule.WHATSAPP in allowed)
    assertEquals(TeacherModule.WHATSAPP, TeacherModule.fromKey("whatsapp"))
  }

  @Test
  fun `cbc rubric level maps scores accurately`() {
    assertEquals(CbcPerformanceLevel.EE, CbcPerformanceLevel.fromScore(85.0))
    assertEquals(CbcPerformanceLevel.ME, CbcPerformanceLevel.fromScore(65.0))
    assertEquals(CbcPerformanceLevel.AE, CbcPerformanceLevel.fromScore(40.0))
    assertEquals(CbcPerformanceLevel.BE, CbcPerformanceLevel.fromScore(35.0))
  }

  @Test
  fun `kicd designs and timetable cover grades 1 to 9 with lower upper and junior formats`() {
    assertTrue(KicdCurriculumRepository.kicdDesigns.isNotEmpty())
    val g1Slots = KicdCurriculumRepository.defaultTimetableForGrade("Grade 1")
    val g4Slots = KicdCurriculumRepository.defaultTimetableForGrade("Grade 4")
    val g7Slots = KicdCurriculumRepository.defaultTimetableForGrade("Grade 7")
    assertEquals(40, g1Slots.size) // Lower Primary: 5 days * 8 slots
    assertEquals(55, g4Slots.size) // Upper Primary: 5 days * 11 slots
    assertEquals(60, g7Slots.size) // Junior School: 5 days * 12 slots
    assertFalse(g1Slots.any { it.learningArea == "LUNCH BREAK" })
    assertTrue(g4Slots.any { it.learningArea == "LUNCH BREAK" && it.isBreak })
    assertTrue(g7Slots.any { it.learningArea == "LUNCH BREAK" && it.isBreak })
  }

  @Test
  fun `subject id mapping matches primary and junior secondary api ids`() {
    assertEquals("math_p", KicdCurriculumRepository.subjectIdForArea("Grade 4", "Mathematics"))
    assertEquals("env_p", KicdCurriculumRepository.subjectIdForArea("Grade 3", "Environmental Activities"))
    assertEquals("math", KicdCurriculumRepository.subjectIdForArea("Grade 7", "Mathematics"))
    assertEquals("isci", KicdCurriculumRepository.subjectIdForArea("Grade 8", "Integrated Science"))
  }

  @Test
  fun `network module normalizes backend urls`() {
    assertEquals(
      "https://school.example.com/api/",
      NetworkModule.normalizeBaseUrl("school.example.com/api"),
    )
    assertEquals(
      "http://10.0.2.2:8080/api/",
      NetworkModule.normalizeBaseUrl("http://10.0.2.2:8080/api/"),
    )
    assertEquals(
      "https://site-4457d1964dd64e19ba7fec208a2066ee.freebuff.page/api/",
      NetworkModule.normalizeBaseUrl("https://site-4457d1964dd64e19ba7fec208a2066ee.freebuff.page"),
    )
  }

  @Test
  fun `backend parser parses login session, streams, and mustChangeLogin`() {
    val json = """
      {
        "token": "tok_abc123",
        "role": "teacher",
        "tenant": { "name": "Fret Academy", "currentTerm": "Term 2", "currentYear": 2026 },
        "services": ["dashboard", "marks", "lessonplans", "attendance"],
        "streams": ["Grade 4 East", "Grade 7 North"],
        "staff": {
          "id": "stf_1",
          "name": "Alvina Njoroge",
          "username": "alvina",
          "tscNumber": "TSC-9981",
          "mustChangeLogin": true
        }
      }
    """.trimIndent()
    val session = BackendApiParser.parseSession(
      raw = json,
      existingToken = "",
      backendUrl = TeacherSession.DEFAULT_BACKEND_URL,
    )
    assertEquals("tok_abc123", session.authToken)
    assertEquals("Alvina Njoroge", session.fullName)
    assertEquals("alvina", session.username)
    assertEquals("Fret Academy", session.schoolName)
    assertEquals("Term 2", session.currentTerm)
    assertTrue(session.mustChangePassword)
    assertEquals(listOf("Grade 4 East", "Grade 7 North"), session.allocatedClasses.map { it.streamKey })
    assertEquals("Grade 4", session.allocatedClasses.first().grade)
    assertTrue(TeacherModule.LESSON_PLANS in session.allowedModules)
    assertFalse(TeacherModule.FEE_PAYMENTS in session.allowedModules)
  }

  @Test
  fun `backend parser parses dashboard summary, timetable periods+entries, and lesson plan template`() {
    val dashRaw = """
      {
        "learners": 34,
        "attendancePct": 94,
        "presentToday": 32,
        "absentToday": 2,
        "classMean": 71
      }
    """.trimIndent()
    val dash = BackendApiParser.parseDashboard(dashRaw, "Grade 7 North")
    assertEquals(34, dash.learnerCount)
    assertEquals(94, dash.attendancePct)
    assertEquals(71, dash.meanScore)
    assertNull(dash.schemesCount)

    val ttRaw = """
      {
        "periods": [
          { "id": "p1", "label": "Lesson 1", "start": "08:20", "end": "09:00" },
          { "id": "b1", "label": "BREAK", "start": "09:40", "end": "10:00" }
        ],
        "entries": [
          { "id": "e1", "stream": "Grade 7 North", "day": "Monday", "periodId": "p1", "subjectId": "math", "teacher": "Alvina" },
          { "id": "e2", "stream": "Grade 7 North", "day": "Monday", "periodId": "b1", "subjectId": "", "teacher": "" }
        ]
      }
    """.trimIndent()
    val slots = BackendApiParser.parseTimetable(ttRaw)
    assertEquals(2, slots.size)
    assertEquals("MON", slots[0].dayOfWeek)
    assertEquals("08:20 - 09:00", slots[0].timeRange)
    assertEquals("MATHEMATICS", slots[0].learningArea)
    assertTrue(slots[1].isBreak)

    val lpRaw = """
      {
        "template": {
          "sections": [
            { "key": "sec1", "label": "Specific Learning Outcomes" },
            { "key": "sec6", "label": "Introduction" }
          ]
        },
        "plans": [
          {
            "id": "lp_99",
            "stream": "Grade 7 North",
            "subjectId": "math",
            "term": "Term 1",
            "week": 3,
            "lesson": 2,
            "lessonDate": "2026-02-12",
            "topic": "Integers",
            "content": { "sec1": "Add integers", "sec6": "Number line warm-up" }
          }
        ]
      }
    """.trimIndent()
    val (sections, plans) = BackendApiParser.parseLessonPlans(lpRaw, "Fret Academy", "Alvina", "Grade 7 North")
    assertEquals(2, sections.size)
    assertEquals("sec1", sections[0].key)
    assertEquals(1, plans.size)
    assertEquals("2026-02-12", plans[0].date)
    assertEquals("Add integers", plans[0].specificOutcomes)
    assertEquals("Number line warm-up", plans[0].introduction)
  }

  @Test
  fun `repository persists schemes, lesson plans, learners, attendance and marks`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, ElimuTrackDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val prefs = SessionPreferences(context)
    val repo = TeacherRepository(db, prefs)

    try {
      repo.saveScheme(
        SchemeOfWork(
          id = "sch_1",
          title = "Grade 1 Math Term 1",
          schoolName = "Fret's School",
          teacherName = "Fret Mwaniki",
          grade = "Grade 1",
          learningArea = "Mathematical Activities",
          term = "Term 1",
          year = 2026,
          rows = listOf(
            SchemeRow(1, 1, "1.0 Numbers", "1.1 Counting", "Count 1-50", "Why count?", "Use bottle tops", "Counters", "Oral check")
          ),
          updatedAt = System.currentTimeMillis(),
        )
      )
      assertEquals(1, repo.schemesFlow.first().size)

      repo.saveLessonPlan(
        LessonPlan(
          id = "lp_1",
          schoolName = "Fret's School",
          teacherName = "Fret Mwaniki",
          grade = "Grade 1",
          stream = "East",
          learningArea = "Mathematical Activities",
          term = "Term 1",
          week = 1,
          lessonNumber = 1,
          date = "2026-02-02",
          timeSlot = "9.00 / 9.40",
          rollCount = 32,
          strand = "1.0 Numbers",
          subStrand = "1.1 Counting",
          specificOutcomes = "Count forward to 50",
          keyInquiryQuestions = "Why do we count?",
          coreCompetencies = "Critical Thinking",
          values = "Unity",
          pcis = "Life skills",
          learningResources = "Bottle tops",
          introduction = "Warm up song",
          lessonDevelopment = "Pair counting",
          conclusion = "Oral check",
          extendedActivity = "Count spoons at home",
          reflection = "Good progress",
          updatedAt = System.currentTimeMillis(),
        )
      )
      assertEquals(1, repo.lessonPlansFlow.first().size)

      val learner = Learner(
        id = "l1",
        admissionNo = "ADM-101",
        nemisUpi = "NEM-7001",
        fullName = "Amani Wanjiku",
        grade = "Grade 1",
        stream = "East",
        gender = "Female",
        feeBalanceKes = 5000.0,
      )
      repo.saveLearner(learner)
      repo.postFeePayment("l1", 5000.0, 2000.0)
      repo.saveAttendanceBatch(
        listOf(
          AttendanceRecord("a1", "2026-02-02", "Grade 1", "East", "Mathematical Activities", "9.00 / 9.40", "l1", "Amani Wanjiku", "ADM-101", AttendanceStatus.PRESENT)
        )
      )
      repo.saveMarkEntry(
        MarkEntry("m1", "l1", "Amani Wanjiku", "ADM-101", "Grade 1", "East", "Mathematical Activities", "Term 1", "CAT 1", 86.0)
      )

      assertEquals(3000.0, repo.learnersFlow.first().first().feeBalanceKes, 0.01)
      assertEquals(1, repo.attendanceFlow.first().size)
      assertEquals(CbcPerformanceLevel.EE, repo.marksFlow.first().first().level)

      repo.ensureTimetableSeeded()
      val originalG4 = repo.timetableFlow.first().first { it.grade == "Grade 4" && it.dayOfWeek == "MON" && it.slotIndex == 0 }
      repo.saveTimetableSlot(originalG4.copy(learningArea = "KISWAHILI REMEDIAL", room = "Lab 2"))
      val editedG4 = repo.timetableFlow.first().first { it.id == originalG4.id }
      assertEquals("KISWAHILI REMEDIAL", editedG4.learningArea)
      assertEquals("Lab 2", editedG4.room)

      repo.resetTimetableForGrade("Grade 4")
      val resetG4 = repo.timetableFlow.first().first { it.id == originalG4.id }
      assertEquals(originalG4.learningArea, resetG4.learningArea)

      repo.saveLessonPlan(
        LessonPlan(
          id = "lp_extra",
          schoolName = "Fret's School",
          teacherName = "Alvina",
          grade = "Grade 4",
          stream = "Grade 4 East",
          learningArea = "Mathematics",
          term = "Term 1",
          week = 2,
          lessonNumber = 3,
          date = "2026-03-01",
          timeSlot = "8:20 - 9:00",
          rollCount = 30,
          strand = "Numbers",
          subStrand = "Fractions",
          topic = "Fractions",
          specificOutcomes = "Identify halves",
          keyInquiryQuestions = "What is half?",
          coreCompetencies = "Critical Thinking",
          values = "Unity",
          pcis = "Life skills",
          learningResources = "Paper strips",
          introduction = "Fold paper",
          lessonDevelopment = "Compare halves",
          conclusion = "Recap",
          extendedActivity = "Home task",
          reflection = "Clear",
          updatedAt = System.currentTimeMillis(),
          extraContent = mapOf("sec1" to "Identify halves", "customSection" to "Extra template note"),
        )
      )
      val savedPlan = repo.lessonPlansFlow.first().first { it.id == "lp_extra" }
      assertEquals("Extra template note", savedPlan.extraContent["customSection"])

      repo.saveRecordOfWork(
        RecordOfWork(
          id = "row_roundtrip",
          schoolName = "Fret's School",
          teacherName = "Alvina",
          grade = "Grade 4",
          stream = "Grade 4 East",
          learningArea = "Mathematics",
          term = "Term 1",
          year = 2026,
          week = 2,
          date = "2026-03-02",
          strand = "Numbers",
          subStrand = "Fractions",
          workCovered = "Covered halves and quarters",
          absencesToday = "1",
          homework = "Page 14",
          challengesRemarks = "Good participation",
          teacherInitials = "AN",
          updatedAt = System.currentTimeMillis(),
          topic = "Equivalent Fractions",
          lessonNumber = 4,
          classSize = 31,
        )
      )
      val savedRow = repo.recordsOfWorkFlow.first().first { it.id == "row_roundtrip" }
      assertEquals("Equivalent Fractions", savedRow.topic)
      assertEquals(4, savedRow.lessonNumber)
      assertEquals(31, savedRow.classSize)
    } finally {
      db.close()
    }
  }

  @Test
  fun `module aliases, nested object permissions, numeric grades, and class matching handle server shapes`() {
    assertEquals(TeacherModule.SCHEMES, TeacherModule.fromKey("schemes_of_work"))
    assertEquals(TeacherModule.SCHEMES, TeacherModule.fromKey("schemesOfWork"))
    assertEquals(TeacherModule.RECORD_OF_WORK, TeacherModule.fromKey("record_of_work"))
    assertEquals(TeacherModule.RECORD_OF_WORK, TeacherModule.fromKey("recordOfWork"))
    assertEquals(TeacherModule.PROGRESS, TeacherModule.fromKey("learner_progress"))
    assertEquals(TeacherModule.LESSON_PLANS, TeacherModule.fromKey("lesson_plans"))

    val nestedSessionJson = """
      {
        "token": "tok_nested",
        "staff": {
          "id": "stf_2",
          "name": "David Otieno",
          "username": "david",
          "services": {
            "schemes": true,
            "lessonPlans": true,
            "recordOfWork": true,
            "timetable": true,
            "learners": true,
            "whatsapp": false
          },
          "streams": ["4 - East", "Grade 7"]
        }
      }
    """.trimIndent()
    val parsedSession = BackendApiParser.parseSession(nestedSessionJson, "", TeacherSession.DEFAULT_BACKEND_URL)
    assertTrue(TeacherModule.SCHEMES in parsedSession.allowedModules)
    assertTrue(TeacherModule.LESSON_PLANS in parsedSession.allowedModules)
    assertTrue(TeacherModule.RECORD_OF_WORK in parsedSession.allowedModules)
    assertTrue(TeacherModule.TIMETABLE in parsedSession.allowedModules)
    assertTrue(TeacherModule.LEARNERS in parsedSession.allowedModules)
    assertFalse(TeacherModule.WHATSAPP in parsedSession.allowedModules)
    assertEquals("Grade 4", parsedSession.allocatedClasses.first().grade)

    val learnersRaw = """
      {
        "learners": [
          { "id": "l_e", "name": "Faith Akinyi", "admissionNo": "A-10", "stream": "East", "gradeLevel": "4" }
        ]
      }
    """.trimIndent()
    val parsedLearners = BackendApiParser.parseLearners(learnersRaw, "")
    assertEquals(1, parsedLearners.size)
    assertEquals("Grade 4", parsedLearners.first().grade)
    assertEquals("East", parsedLearners.first().stream)

    val stateWithSuffix = TeacherUiState(
      session = TeacherSession(
        teacherId = "t2",
        fullName = "Alvina",
        email = "a@s.ke",
        tscNumber = "T-1",
        schoolName = "School",
        allocatedClasses = listOf(AllocatedClass("Grade 7 Eta", "Grade 7", "Eta", emptyList())),
      ),
      selectedStream = "Grade 7 Eta",
      selectedGrade = "Grade 7",
    )
    assertTrue(stateWithSuffix.matchesActiveClass("Grade 7 - Eta", "Grade 7"))
    assertTrue(stateWithSuffix.matchesActiveClass("Eta", "7"))
    assertFalse(stateWithSuffix.matchesActiveClass("Grade 7 North", "Grade 7"))

    val stateGradeOnly = stateWithSuffix.copy(
      session = stateWithSuffix.session!!.copy(
        allocatedClasses = listOf(AllocatedClass("Grade 7", "Grade 7", "", emptyList())),
      ),
      selectedStream = "Grade 7",
      selectedGrade = "Grade 7",
    )
    assertTrue(stateGradeOnly.matchesActiveClass("Eta", "Grade 7"))
    assertTrue(stateGradeOnly.matchesActiveClass("", "7"))
  }
}

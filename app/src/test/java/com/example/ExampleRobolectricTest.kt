package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.curriculum.KicdCurriculumRepository
import com.example.data.local.ElimuTrackDatabase
import com.example.data.local.SessionPreferences
import com.example.data.remote.NetworkModule
import com.example.data.repository.TeacherRepository
import com.example.domain.model.AttendanceRecord
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.CbcPerformanceLevel
import com.example.domain.model.Learner
import com.example.domain.model.LessonPlan
import com.example.domain.model.MarkEntry
import com.example.domain.model.SchemeOfWork
import com.example.domain.model.SchemeRow
import com.example.domain.model.TeacherModule
import com.example.domain.model.TeacherSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
      allowedModuleKeys = listOf("dashboard", "schemes", "lesson_plans", "attendance", "marks"),
    )
    val allowed = session.allowedModules
    assertTrue(TeacherModule.DASHBOARD in allowed)
    assertTrue(TeacherModule.SCHEMES in allowed)
    assertTrue(TeacherModule.LESSON_PLANS in allowed)
    assertTrue(TeacherModule.ATTENDANCE in allowed)
    assertTrue(TeacherModule.MARKSBOOK in allowed)
    assertFalse(TeacherModule.FEE_PAYMENTS in allowed)
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
  fun `network module normalizes backend urls`() {
    assertEquals(
      "https://school.example.com/api/",
      NetworkModule.normalizeBaseUrl("school.example.com/api"),
    )
    assertEquals(
      "http://10.0.2.2:8080/api/",
      NetworkModule.normalizeBaseUrl("http://10.0.2.2:8080/api/"),
    )
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
    } finally {
      db.close()
    }
  }
}

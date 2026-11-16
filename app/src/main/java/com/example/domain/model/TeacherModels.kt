package com.example.domain.model

import com.squareup.moshi.JsonClass

enum class ServiceGroup(val label: String) {
  OVERVIEW("Overview"),
  TEACHING("Teaching"),
  LEARNERS("Learners"),
  ASSESSMENT("Assessment"),
  SCHOOL("School"),
}

/**
 * The 16 services an admin can grant from Staff & Access (exact match to prototype SV list).
 */
enum class TeacherModule(
  val key: String,
  val label: String,
  val shortLabel: String,
  val description: String,
  val group: ServiceGroup,
) {
  DASHBOARD("dashboard", "Dashboard", "Today", "Overview tiles for your classes", ServiceGroup.OVERVIEW),
  LESSON_PLANS("lesson_plans", "Lesson plans", "Plans", "Write and store lesson plans", ServiceGroup.TEACHING),
  SCHEMES("schemes", "Schemes of work", "Schemes", "Termly scheme rows from KICD designs", ServiceGroup.TEACHING),
  TIMETABLE("timetable", "Timetable", "Timetable", "Your weekly lessons", ServiceGroup.TEACHING),
  RECORD_OF_WORK("record_of_work", "Record of work", "Records", "What was actually taught", ServiceGroup.TEACHING),
  KICD_DESIGNS("kicd_designs", "KICD designs", "KICD", "Curriculum design library", ServiceGroup.TEACHING),
  LEARNERS("learners", "Learners", "Learners", "Records and guardian contacts", ServiceGroup.LEARNERS),
  OBSERVATIONS("observations", "Observations", "Observe", "Notes on how learners are doing", ServiceGroup.LEARNERS),
  PROGRESS("progress", "Learner progress", "Progress", "Stage of achievement per learner", ServiceGroup.LEARNERS),
  REPORT_CARDS("report_cards", "Report cards", "Reports", "End-of-term summaries", ServiceGroup.LEARNERS),
  ATTENDANCE("attendance", "Attendance", "Register", "Daily class register", ServiceGroup.ASSESSMENT),
  MARKSBOOK("marksbook", "Marksbook", "Marks", "Enter and edit subject scores", ServiceGroup.ASSESSMENT),
  BROADSHEET("broadsheet", "Broadsheet", "Broadsheet", "Ranked class analysis", ServiceGroup.ASSESSMENT),
  PATHWAYS("pathways", "Pathways", "Pathways", "Grade 9 pathway recommendations", ServiceGroup.ASSESSMENT),
  FEE_PAYMENTS("fee_payments", "Fee payments", "Fees", "Balances and payment posting", ServiceGroup.SCHOOL),
  WHATSAPP("whatsapp", "WhatsApp", "WhatsApp", "Messages to guardians", ServiceGroup.SCHOOL);

  companion object {
    fun fromKey(key: String): TeacherModule? = when (key.trim().lowercase()) {
      "recordwork" -> RECORD_OF_WORK
      "kicd" -> KICD_DESIGNS
      "reportcards" -> REPORT_CARDS
      "marks" -> MARKSBOOK
      "fees" -> FEE_PAYMENTS
      "approved_resources" -> KICD_DESIGNS
      else -> entries.firstOrNull { it.key.equals(key.trim(), ignoreCase = true) }
    }

    val defaultTeacherModules: Set<TeacherModule> = entries.toSet()
  }
}

enum class CbcPerformanceLevel(
  val code: String,
  val title: String,
  val rangeLabel: String,
  val points: Int,
) {
  EE("EE", "Exceeding Expectation", "80 and above", 4),
  ME("ME", "Meeting Expectation", "60 to 79", 3),
  AE("AE", "Approaching Expectation", "40 to 59", 2),
  BE("BE", "Below Expectation", "Below 40", 1);

  companion object {
    fun fromScore(score: Double): CbcPerformanceLevel = when {
      score >= 80.0 -> EE
      score >= 60.0 -> ME
      score >= 40.0 -> AE
      else -> BE
    }
  }
}

enum class AttendanceStatus(val label: String, val shortCode: String) {
  PRESENT("Present", "P"),
  ABSENT("Absent", "A"),
  LATE("Late", "L"),
  EXCUSED("Excused", "E"),
}

@JsonClass(generateAdapter = true)
data class AllocatedClass(
  val id: String,
  val grade: String,
  val stream: String,
  val learningAreas: List<String>,
  val learnerCount: Int = 0,
) {
  val displayName: String get() = if (stream.isBlank()) grade else "$grade $stream"
}

@JsonClass(generateAdapter = true)
data class TeacherSession(
  val teacherId: String,
  val fullName: String,
  val username: String = "alvina",
  val email: String,
  val tscNumber: String,
  val schoolName: String,
  val currentTerm: String = "Term 3",
  val currentYear: Int = 2026,
  val badgeLabel: String = "CAMPUS PREMIUM",
  val backendUrl: String = DEFAULT_BACKEND_URL,
  val authToken: String = "",
  val isOfflineWorkspace: Boolean = false,
  val mustChangePassword: Boolean = false,
  val allowedModuleKeys: List<String> = TeacherModule.defaultTeacherModules.map { it.key },
  val allocatedClasses: List<AllocatedClass> = emptyList(),
) {
  val allowedModules: Set<TeacherModule>
    get() = allowedModuleKeys.mapNotNull { TeacherModule.fromKey(it) }.toSet()

  companion object {
    const val DEFAULT_BACKEND_URL = "https://site-4457d1964dd64e19ba7fec208a2066ee.freebuff.page/api/"
  }
}

data class SyncStatus(
  val isSyncing: Boolean = false,
  val lastSyncedAt: Long? = null,
  val pendingUploads: Int = 0,
  val lastMessage: String? = null,
  val isError: Boolean = false,
)

package com.example.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequestDto(
  val identifier: String,
  val password: String,
)

@JsonClass(generateAdapter = true)
data class AllocatedClassDto(
  val id: String,
  val grade: String,
  val stream: String = "",
  val learningAreas: List<String> = emptyList(),
  val learnerCount: Int = 0,
)

@JsonClass(generateAdapter = true)
data class TeacherProfileDto(
  val teacherId: String,
  val fullName: String,
  val email: String,
  val tscNumber: String = "",
  val schoolName: String = "Fret's School",
  val currentTerm: String = "Term 1",
  val currentYear: Int = 2026,
  val badgeLabel: String = "CAMPUS PREMIUM",
  val allowedModules: List<String> = emptyList(),
  val allocatedClasses: List<AllocatedClassDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class LoginResponseDto(
  val token: String,
  val teacher: TeacherProfileDto,
)

@JsonClass(generateAdapter = true)
data class LearnerDto(
  val id: String,
  val admissionNo: String,
  val fullName: String,
  val grade: String,
  val stream: String = "",
  val gender: String = "",
  val guardianPhone: String = "",
  val pathwayInterest: String = "STEM",
  val feeBalanceKes: Double = 0.0,
)

@JsonClass(generateAdapter = true)
data class TimetableSlotDto(
  val id: String,
  val grade: String,
  val dayOfWeek: String,
  val slotIndex: Int,
  val timeRange: String,
  val learningArea: String,
  val isBreak: Boolean = false,
  val room: String = "",
  val teacherId: String = "",
)

@JsonClass(generateAdapter = true)
data class MarkEntryDto(
  val id: String,
  val learnerId: String,
  val learnerName: String,
  val admissionNo: String,
  val grade: String,
  val stream: String = "",
  val learningArea: String,
  val term: String,
  val assessmentType: String,
  val score: Double,
  val maxScore: Double = 100.0,
  val teacherComment: String = "",
)

@JsonClass(generateAdapter = true)
data class AttendanceDto(
  val id: String,
  val date: String,
  val grade: String,
  val stream: String = "",
  val learningArea: String,
  val lessonSlot: String,
  val learnerId: String,
  val learnerName: String,
  val admissionNo: String,
  val status: String,
  val remarks: String = "",
)

@JsonClass(generateAdapter = true)
data class SchemeDto(
  val id: String,
  val title: String,
  val schoolName: String,
  val teacherName: String,
  val grade: String,
  val learningArea: String,
  val term: String,
  val year: Int,
  val rowsJson: String,
  val updatedAt: Long,
)

@JsonClass(generateAdapter = true)
data class LessonPlanDto(
  val id: String,
  val schoolName: String,
  val teacherName: String,
  val grade: String,
  val stream: String,
  val learningArea: String,
  val term: String,
  val week: Int,
  val lessonNumber: Int,
  val date: String,
  val timeSlot: String,
  val rollCount: Int,
  val strand: String,
  val subStrand: String,
  val specificOutcomes: String,
  val keyInquiryQuestions: String,
  val coreCompetencies: String,
  val values: String,
  val pcis: String,
  val learningResources: String,
  val introduction: String,
  val lessonDevelopment: String,
  val conclusion: String,
  val extendedActivity: String,
  val reflection: String,
  val updatedAt: Long,
)

@JsonClass(generateAdapter = true)
data class RecordOfWorkDto(
  val id: String,
  val schoolName: String,
  val teacherName: String,
  val grade: String,
  val stream: String,
  val learningArea: String,
  val term: String,
  val year: Int,
  val week: Int,
  val date: String,
  val strand: String,
  val subStrand: String,
  val workCovered: String,
  val challengesRemarks: String,
  val teacherInitials: String,
  val updatedAt: Long,
)

@JsonClass(generateAdapter = true)
data class ObservationDto(
  val id: String,
  val learnerId: String,
  val learnerName: String,
  val grade: String,
  val competency: String,
  val ratingCode: String,
  val note: String,
  val date: String,
)

@JsonClass(generateAdapter = true)
data class TeacherBootstrapDto(
  val teacher: TeacherProfileDto,
  val learners: List<LearnerDto> = emptyList(),
  val timetable: List<TimetableSlotDto> = emptyList(),
  val marks: List<MarkEntryDto> = emptyList(),
  val attendance: List<AttendanceDto> = emptyList(),
  val schemes: List<SchemeDto> = emptyList(),
  val lessonPlans: List<LessonPlanDto> = emptyList(),
  val recordsOfWork: List<RecordOfWorkDto> = emptyList(),
  val observations: List<ObservationDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class SyncAckDto(
  val ok: Boolean = true,
  val count: Int = 0,
  val message: String = "",
)

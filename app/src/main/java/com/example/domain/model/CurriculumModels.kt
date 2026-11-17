package com.example.domain.model

import com.squareup.moshi.JsonClass

data class KicdDesignItem(
  val id: String,
  val level: String,
  val grade: String,
  val learningArea: String,
  val strand: String,
  val subStrand: String,
  val suggestedLessons: Int,
  val specificLearningOutcomes: List<String>,
  val keyInquiryQuestions: List<String>,
  val learningExperiences: List<String>,
  val coreCompetencies: List<String>,
  val values: List<String>,
  val pcis: List<String>,
  val suggestedResources: List<String>,
  val assessmentRubric: Map<String, String>,
  val sourceType: String = "Official KICD",
)

@JsonClass(generateAdapter = true)
data class SchemeRow(
  val week: Int,
  val lesson: Int,
  val strand: String,
  val subStrand: String,
  val specificOutcomes: String,
  val keyInquiryQuestion: String,
  val learningExperiences: String,
  val learningResources: String,
  val assessmentMethods: String,
  val reflection: String = "",
  val remoteId: String? = null,
)

data class SchemeOfWork(
  val id: String,
  val title: String,
  val schoolName: String,
  val teacherName: String,
  val grade: String,
  val learningArea: String,
  val term: String,
  val year: Int,
  val rows: List<SchemeRow>,
  val updatedAt: Long,
  val synced: Boolean = false,
)

data class LessonPlanTemplateSection(
  val key: String,
  val label: String,
)

data class LessonPlan(
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
  val topic: String = subStrand,
  val specificOutcomes: String,
  val keyInquiryQuestions: String,
  val coreCompetencies: String,
  val values: String,
  val pcis: String,
  val learningResources: String,
  val organizationOfLearning: String = "Pair & small group work inside classroom",
  val introduction: String,
  val lessonDevelopment: String,
  val conclusion: String,
  val extendedActivity: String,
  val reflection: String,
  val updatedAt: Long,
  val synced: Boolean = false,
  val extraContent: Map<String, String> = emptyMap(),
)

data class RecordOfWork(
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
  val absencesToday: String = "",
  val homework: String = "",
  val challengesRemarks: String,
  val teacherInitials: String,
  val updatedAt: Long,
  val synced: Boolean = false,
  val topic: String = subStrand,
  val lessonNumber: Int = 1,
  val classSize: Int = 0,
  val schemeId: String = "",
  val lessonPlanId: String = "",
)

data class TimetableSlot(
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

data class Learner(
  val id: String,
  val admissionNo: String,
  val nemisUpi: String = "",
  val fullName: String,
  val grade: String,
  val stream: String,
  val gender: String,
  val guardianPhone: String = "",
  val pathwayInterest: String = "STEM",
  val totalFeeKes: Double = 45000.0,
  val feeBalanceKes: Double = 0.0,
  val attendanceHistory5Days: String = "PPPPP",
  val termAttendancePct: Int = 96,
)

data class AttendanceRecord(
  val id: String,
  val date: String,
  val grade: String,
  val stream: String,
  val learningArea: String,
  val lessonSlot: String,
  val learnerId: String,
  val learnerName: String,
  val admissionNo: String,
  val status: AttendanceStatus,
  val remarks: String = "",
  val synced: Boolean = false,
)

data class MarkEntry(
  val id: String,
  val learnerId: String,
  val learnerName: String,
  val admissionNo: String,
  val grade: String,
  val stream: String,
  val learningArea: String,
  val term: String,
  val assessmentType: String,
  val score: Double,
  val maxScore: Double = 100.0,
  val level: CbcPerformanceLevel = CbcPerformanceLevel.fromScore(score),
  val teacherComment: String = "",
  val updatedAt: Long = System.currentTimeMillis(),
  val synced: Boolean = false,
)

data class ObservationNote(
  val id: String,
  val learnerId: String,
  val learnerName: String,
  val grade: String,
  val observationType: String = "Strength", // Strength, Needs support, Participation, Wellbeing
  val competency: String,
  val rating: CbcPerformanceLevel,
  val note: String,
  val date: String,
  val synced: Boolean = false,
)

data class LearnerProgressEntry(
  val id: String,
  val learnerId: String,
  val learnerName: String,
  val grade: String,
  val learningArea: String,
  val stage: String, // Beginning, Developing, Secure, Exceeding
  val note: String,
  val date: String,
  val synced: Boolean = false,
)

data class GuardianMessage(
  val id: String,
  val learnerId: String,
  val learnerName: String,
  val guardianPhone: String,
  val grade: String,
  val template: String, // Fee reminder, Absence check, Progress update
  val message: String,
  val date: String,
  val synced: Boolean = false,
)

data class FeePaymentRecord(
  val id: String,
  val learnerId: String,
  val learnerName: String,
  val amount: Double,
  val method: String,
  val reference: String,
  val day: String,
  val note: String = "",
)

data class ApprovedResource(
  val id: String,
  val title: String,
  val publisher: String,
  val grade: String,
  val learningArea: String,
  val resourceType: String,
  val kicdApprovalCode: String,
)

data class SubjectInfo(
  val id: String,
  val name: String,
)

data class PathwayInfo(
  val id: String,
  val name: String,
  val tracks: List<String>,
  val coreLearningAreas: List<String>,
  val careerExamples: List<String>,
)

package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schemes_of_work")
data class SchemeEntity(
  @PrimaryKey val id: String,
  val title: String,
  val schoolName: String,
  val teacherName: String,
  val grade: String,
  val learningArea: String,
  val term: String,
  val year: Int,
  val rowsJson: String,
  val updatedAt: Long,
  val synced: Boolean,
)

@Entity(tableName = "lesson_plans")
data class LessonPlanEntity(
  @PrimaryKey val id: String,
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
  val topic: String,
  val specificOutcomes: String,
  val keyInquiryQuestions: String,
  val coreCompetencies: String,
  val valuesText: String,
  val pcis: String,
  val learningResources: String,
  val organizationOfLearning: String,
  val introduction: String,
  val lessonDevelopment: String,
  val conclusion: String,
  val extendedActivity: String,
  val reflection: String,
  val updatedAt: Long,
  val synced: Boolean,
  val extraContentJson: String = "{}",
)

@Entity(tableName = "records_of_work")
data class RecordOfWorkEntity(
  @PrimaryKey val id: String,
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
  val absencesToday: String,
  val homework: String,
  val challengesRemarks: String,
  val teacherInitials: String,
  val updatedAt: Long,
  val synced: Boolean,
  val topic: String = subStrand,
  val lessonNumber: Int = 1,
  val classSize: Int = 0,
  val schemeId: String = "",
  val lessonPlanId: String = "",
)

@Entity(tableName = "learners")
data class LearnerEntity(
  @PrimaryKey val id: String,
  val admissionNo: String,
  val nemisUpi: String,
  val fullName: String,
  val grade: String,
  val stream: String,
  val gender: String,
  val guardianPhone: String,
  val pathwayInterest: String,
  val totalFeeKes: Double,
  val feeBalanceKes: Double,
  val attendanceHistory5Days: String,
  val termAttendancePct: Int,
)

@Entity(tableName = "attendance_records")
data class AttendanceEntity(
  @PrimaryKey val id: String,
  val date: String,
  val grade: String,
  val stream: String,
  val learningArea: String,
  val lessonSlot: String,
  val learnerId: String,
  val learnerName: String,
  val admissionNo: String,
  val statusName: String,
  val remarks: String,
  val synced: Boolean,
)

@Entity(tableName = "mark_entries")
data class MarkEntryEntity(
  @PrimaryKey val id: String,
  val learnerId: String,
  val learnerName: String,
  val admissionNo: String,
  val grade: String,
  val stream: String,
  val learningArea: String,
  val term: String,
  val assessmentType: String,
  val score: Double,
  val maxScore: Double,
  val levelCode: String,
  val teacherComment: String,
  val updatedAt: Long,
  val synced: Boolean,
)

@Entity(tableName = "observation_notes")
data class ObservationEntity(
  @PrimaryKey val id: String,
  val learnerId: String,
  val learnerName: String,
  val grade: String,
  val observationType: String,
  val competency: String,
  val ratingCode: String,
  val note: String,
  val date: String,
  val synced: Boolean,
)

@Entity(tableName = "learner_progress")
data class LearnerProgressEntity(
  @PrimaryKey val id: String,
  val learnerId: String,
  val learnerName: String,
  val grade: String,
  val learningArea: String,
  val stage: String,
  val note: String,
  val date: String,
  val synced: Boolean,
)

@Entity(tableName = "guardian_messages")
data class GuardianMessageEntity(
  @PrimaryKey val id: String,
  val learnerId: String,
  val learnerName: String,
  val guardianPhone: String,
  val grade: String,
  val template: String,
  val message: String,
  val date: String,
  val synced: Boolean,
)

@Entity(tableName = "timetable_slots")
data class TimetableSlotEntity(
  @PrimaryKey val id: String,
  val grade: String,
  val dayOfWeek: String,
  val slotIndex: Int,
  val timeRange: String,
  val learningArea: String,
  val isBreak: Boolean,
  val room: String,
  val teacherId: String,
)

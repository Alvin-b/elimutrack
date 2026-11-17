package com.example.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequestDto(
  val username: String,
  val password: String,
)

@JsonClass(generateAdapter = true)
data class ChangeLoginRequestDto(
  val username: String? = null,
  val currentPassword: String,
  val newPassword: String,
)

@JsonClass(generateAdapter = true)
data class LearnerCreateRequestDto(
  val name: String,
  val admissionNo: String,
  val stream: String,
  val gradeLevel: String,
  val gender: String,
  val nemisUpi: String = "",
  val knecNo: String = "",
  val guardianPhone: String = "",
  val boarding: Boolean = false,
  val admissionFeePaid: Boolean = false,
  val additionalFees: Double = 0.0,
)

@JsonClass(generateAdapter = true)
data class DeleteIdRequestDto(
  val id: String,
)

@JsonClass(generateAdapter = true)
data class MarkPutItemDto(
  val learnerId: String,
  val subjectId: String,
  val score: Double,
)

@JsonClass(generateAdapter = true)
data class MarksPutRequestDto(
  val items: List<MarkPutItemDto>,
)

@JsonClass(generateAdapter = true)
data class AttendancePostItemDto(
  val learnerId: String,
  val day: String,
  val status: String, // present | absent | late
)

@JsonClass(generateAdapter = true)
data class AttendancePostRequestDto(
  val items: List<AttendancePostItemDto>,
)

@JsonClass(generateAdapter = true)
data class ObservationPostItemDto(
  val learnerId: String,
  val day: String,
  val note: String,
)

@JsonClass(generateAdapter = true)
data class ObservationPostRequestDto(
  val items: List<ObservationPostItemDto>,
)

@JsonClass(generateAdapter = true)
data class SchemeRowApiDto(
  val id: String? = null,
  val stream: String,
  val subjectId: String,
  val term: String,
  val week: Int,
  val lesson: Int,
  val strand: String,
  val subStrand: String,
  val outcomes: String,
  val experiences: String,
  val inquiry: String,
  val resources: String,
  val assessment: String,
  val remarks: String,
)

@JsonClass(generateAdapter = true)
data class SchemeBulkRowDto(
  val week: Int,
  val lesson: Int,
  val strand: String,
  val subStrand: String,
  val outcomes: String,
  val experiences: String,
  val inquiry: String,
  val resources: String,
  val assessment: String,
  val remarks: String,
)

@JsonClass(generateAdapter = true)
data class SchemeBulkRequestDto(
  val stream: String,
  val subjectId: String,
  val term: String,
  val rows: List<SchemeBulkRowDto>,
)

@JsonClass(generateAdapter = true)
data class LessonPlanApiRequestDto(
  val id: String? = null,
  val stream: String,
  val subjectId: String,
  val lessonDate: String,
  val topic: String,
  val subStrand: String,
  val schemeId: String? = null,
  val term: String,
  val week: Int,
  val lesson: Int,
  val strand: String,
  val content: Map<String, String>,
)

@JsonClass(generateAdapter = true)
data class RecordWorkApiDto(
  val id: String? = null,
  val stream: String,
  val subjectId: String,
  val day: String,
  val topic: String,
  val term: String,
  val strand: String,
  val subStrand: String,
  val workCovered: String,
  val schemeId: String? = null,
  val lessonPlanId: String? = null,
  val week: Int,
  val lesson: Int,
  val classSize: Int,
  val absences: String,
  val homework: String,
  val remarks: String,
)

@JsonClass(generateAdapter = true)
data class ProgressPostRequestDto(
  val learnerId: String,
  val day: String,
  val stage: String, // Beginning | Developing | Secure | Exceeding
  val note: String? = null,
  val subjectId: String? = null,
)

@JsonClass(generateAdapter = true)
data class PaymentPostRequestDto(
  val learnerId: String,
  val amount: Double,
  val method: String, // cash | mpesa | bank | cheque
  val reference: String,
  val day: String,
  val note: String? = null,
)

@JsonClass(generateAdapter = true)
data class WhatsAppPostRequestDto(
  val learnerId: String,
  val message: String,
)

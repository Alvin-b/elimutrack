package com.example.data.remote

import com.example.data.curriculum.KicdCurriculumRepository
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
import com.example.domain.model.TeacherSession
import com.example.domain.model.TimetableSlot
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response

class ApiException(
  val httpCode: Int,
  val code: String,
  override val message: String,
) : Exception(message)

object BackendApiParser {

  fun unwrapOrThrow(response: Response<ResponseBody>): String {
    if (response.isSuccessful) {
      return response.body()?.string().orEmpty()
    }
    val errRaw = response.errorBody()?.string().orEmpty()
    val parsed = runCatching { JSONObject(errRaw) }.getOrNull()
    val errCode = parsed?.optString("code").orEmpty().ifBlank {
      if (response.code() == 401) "UNAUTHENTICATED" else ""
    }
    val rawErrMsg = parsed?.optString("error")?.takeIf { it.isNotBlank() }
      ?: parsed?.optString("message")?.takeIf { it.isNotBlank() }
    val errMsg = when (errCode) {
      "BAD_CREDENTIALS" -> rawErrMsg ?: "Wrong username or password."
      "UNAUTHENTICATED" -> rawErrMsg ?: "Session expired. Sign in again."
      else -> rawErrMsg ?: "Request failed (${response.code()})"
    }
    throw ApiException(response.code(), errCode, errMsg)
  }

  private fun JSONObject.str(vararg keys: String): String {
    for (k in keys) {
      if (has(k) && !isNull(k)) {
        val v = opt(k)
        if (v != null && v.toString().isNotBlank() && v.toString() != "null") {
          return v.toString()
        }
      }
    }
    return ""
  }

  private fun JSONObject.intVal(vararg keys: String, default: Int = 0): Int {
    for (k in keys) {
      if (has(k) && !isNull(k)) {
        val v = opt(k)
        if (v is Number) return v.toInt()
        v?.toString()?.toIntOrNull()?.let { return it }
      }
    }
    return default
  }

  private fun JSONObject.doubleVal(vararg keys: String, default: Double = 0.0): Double {
    for (k in keys) {
      if (has(k) && !isNull(k)) {
        val v = opt(k)
        if (v is Number) return v.toDouble()
        v?.toString()?.toDoubleOrNull()?.let { return it }
      }
    }
    return default
  }

  private fun JSONObject.boolVal(vararg keys: String, default: Boolean = false): Boolean {
    for (k in keys) {
      if (has(k) && !isNull(k)) {
        val v = opt(k)
        if (v is Boolean) return v
        if (v is String) return v.equals("true", ignoreCase = true) || v == "1"
        if (v is Number) return v.toInt() != 0
      }
    }
    return default
  }

  private fun extractArray(raw: String, vararg wrapperKeys: String): JSONArray {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return JSONArray()
    if (trimmed.startsWith("[")) {
      return runCatching { JSONArray(trimmed) }.getOrElse { JSONArray() }
    }
    val obj = runCatching { JSONObject(trimmed) }.getOrNull() ?: return JSONArray()
    for (k in wrapperKeys.toList() + listOf("items", "data", "rows", "records", "list")) {
      val arr = obj.optJSONArray(k)
      if (arr != null) return arr
    }
    return JSONArray()
  }

  private fun extractObject(raw: String): JSONObject =
    runCatching { JSONObject(raw.trim()) }.getOrElse { JSONObject() }

  private fun JSONArray.toStringList(): List<String> = buildList {
    for (i in 0 until length()) {
      val item = opt(i)
      when (item) {
        is String -> if (item.isNotBlank()) add(item)
        is JSONObject -> {
          val s = item.str("key", "id", "name", "code", "service", "stream")
          if (s.isNotBlank()) add(s)
        }
        else -> item?.toString()?.takeIf { it.isNotBlank() && it != "null" }?.let(::add)
      }
    }
  }

  fun parseSession(
    raw: String,
    existingToken: String,
    backendUrl: String,
  ): TeacherSession {
    val root = extractObject(raw)
    val userObj = root.optJSONObject("user") ?: JSONObject()
    val staffObj = root.optJSONObject("staff") ?: userObj
    val tenantObj = root.optJSONObject("tenant")
    val tenantStr = if (tenantObj == null) root.str("tenant", "schoolName") else ""

    val token = root.str("token").ifBlank { existingToken }
    val role = root.str("role").ifBlank { staffObj.str("role").ifBlank { "teacher" } }
    val mustChangeLogin = staffObj.boolVal("mustChangeLogin", "mustChangePassword") ||
      root.boolVal("mustChangeLogin", "mustChangePassword")

    val fullName = staffObj.str("name", "fullName", "displayName").ifBlank {
      userObj.str("name", "fullName").ifBlank { "Teacher" }
    }
    val username = staffObj.str("username").ifBlank {
      userObj.str("email", "username").ifBlank { fullName.lowercase().replace(" ", "") }
    }
    val email = userObj.str("email").ifBlank { staffObj.str("email").ifBlank { username } }
    val tsc = staffObj.str("tscNumber", "tsc", "staffNo")
    val staffId = staffObj.str("id", "teacherId", "staffId").ifBlank { userObj.str("sub").ifBlank { username } }

    val schoolName = tenantObj?.str("name", "schoolName", "title")?.ifBlank { null }
      ?: tenantStr.ifBlank { "ElimuTrack School" }
    val currentTerm = tenantObj?.str("currentTerm", "term")?.ifBlank { null }
      ?: root.str("currentTerm", "term").ifBlank { "Term 1" }
    val currentYear = tenantObj?.intVal("currentYear", "year", default = 0)?.takeIf { it > 2000 }
      ?: root.intVal("currentYear", "year", default = 2026)
    val badgeLabel = tenantObj?.str("plan", "badge", "badgeLabel")?.uppercase()?.ifBlank { null }
      ?: "CAMPUS"

    val rawServices = (root.optJSONArray("services")
      ?: root.optJSONArray("allowedModules")
      ?: JSONArray()).toStringList()

    val streamsArr = root.optJSONArray("streams")
      ?: root.optJSONArray("classes")
      ?: JSONArray()
    val allocatedClasses = buildList {
      for (i in 0 until streamsArr.length()) {
        when (val el = streamsArr.opt(i)) {
          is String -> if (el.isNotBlank()) {
            val grade = KicdCurriculumRepository.extractGradeFromStream(el)
            val suffix = el.replace(grade, "", ignoreCase = true).trim()
            val areas = KicdCurriculumRepository.subjectsForGrade(grade).map { it.name }
            add(
              AllocatedClass(
                id = el,
                grade = grade,
                stream = suffix,
                learningAreas = areas,
                learnerCount = 0,
                streamKey = el,
              )
            )
          }
          is JSONObject -> {
            val streamKey = el.str("stream", "name", "id", "className", "label")
            val rawGrade = el.str("grade", "gradeLevel").ifBlank { KicdCurriculumRepository.extractGradeFromStream(streamKey) }
            val streamSuffix = el.str("streamName", "section").ifBlank {
              streamKey.replace(rawGrade, "", ignoreCase = true).trim()
            }
            val areas = el.optJSONArray("learningAreas")?.toStringList()?.ifEmpty { null }
              ?: KicdCurriculumRepository.subjectsForGrade(rawGrade).map { it.name }
            add(
              AllocatedClass(
                id = el.str("id").ifBlank { streamKey },
                grade = rawGrade,
                stream = streamSuffix,
                learningAreas = areas,
                learnerCount = el.intVal("learnerCount", "count", default = 0),
                streamKey = streamKey.ifBlank { rawGrade },
              )
            )
          }
        }
      }
    }

    return TeacherSession(
      teacherId = staffId,
      fullName = fullName,
      username = username,
      email = email,
      tscNumber = tsc,
      schoolName = schoolName,
      currentTerm = currentTerm,
      currentYear = currentYear,
      badgeLabel = badgeLabel,
      role = role,
      backendUrl = backendUrl,
      authToken = token,
      isOfflineWorkspace = false,
      mustChangePassword = mustChangeLogin,
      allowedModuleKeys = rawServices,
      allocatedClasses = allocatedClasses,
    )
  }

  fun parseDashboard(raw: String, stream: String): DashboardSummary {
    val d = extractObject(raw)
    return DashboardSummary(
      stream = stream,
      learnerCount = if (d.has("learners")) d.intVal("learners") else null,
      attendancePct = if (d.has("attendancePct")) d.intVal("attendancePct") else null,
      presentToday = if (d.has("presentToday")) d.intVal("presentToday") else null,
      absentToday = if (d.has("absentToday")) d.intVal("absentToday") else null,
      meanScore = if (d.has("classMean")) d.intVal("classMean") else null,
      marksEntered = if (d.has("marksEntered")) d.intVal("marksEntered") else null,
      feeCollectedKes = if (d.has("feesCollected")) d.doubleVal("feesCollected") else null,
      feeOutstandingKes = if (d.has("feesOutstanding")) d.doubleVal("feesOutstanding") else null,
      plansCount = if (d.has("lessonsPlanned")) d.intVal("lessonsPlanned") else null,
      schemesCount = if (d.has("schemes")) d.intVal("schemes") else null,
      observationsCount = if (d.has("observations")) d.intVal("observations") else null,
    )
  }

  fun parseSingleLearner(o: JSONObject, fallbackStream: String): Learner {
    val streamVal = o.str("stream").ifBlank { fallbackStream }
    val gradeVal = o.str("gradeLevel", "grade").ifBlank {
      KicdCurriculumRepository.extractGradeFromStream(streamVal)
    }
    return Learner(
      id = o.str("id", "learnerId"),
      admissionNo = o.str("admissionNo"),
      nemisUpi = o.str("nemisUpi"),
      fullName = o.str("name", "fullName"),
      grade = gradeVal,
      stream = streamVal,
      gender = o.str("gender"),
      guardianPhone = o.str("guardianPhone"),
      pathwayInterest = o.str("pathwayInterest").ifBlank { "STEM" },
      totalFeeKes = o.doubleVal("feeTotal", "totalFeeKes", default = 0.0),
      feeBalanceKes = o.doubleVal("balance", "feeBalanceKes", default = 0.0),
      attendanceHistory5Days = o.str("attendanceHistory5Days").ifBlank { "PPPPP" },
      termAttendancePct = o.intVal("attendancePct", "termAttendancePct", default = 100),
    )
  }

  fun parseLearners(raw: String, fallbackStream: String): List<Learner> {
    val arr = extractArray(raw, "learners")
    return buildList {
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val l = parseSingleLearner(o, fallbackStream)
        if (l.id.isNotBlank()) add(l)
      }
    }
  }

  fun parseCreatedLearner(raw: String, fallbackStream: String): Learner? {
    val root = extractObject(raw)
    val o = root.optJSONObject("learner") ?: root
    val l = parseSingleLearner(o, fallbackStream)
    return if (l.id.isNotBlank()) l else null
  }

  fun parseMarks(raw: String, fallbackStream: String, fallbackTerm: String): List<MarkEntry> {
    val arr = extractArray(raw, "marks")
    val fallbackGrade = KicdCurriculumRepository.extractGradeFromStream(fallbackStream)
    return buildList {
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val learnerId = o.str("learnerId")
        val subjectId = o.str("subjectId", "learningArea")
        if (learnerId.isBlank() || subjectId.isBlank()) continue
        val score = o.doubleVal("score", default = 0.0)
        val areaName = KicdCurriculumRepository.areaNameForSubjectId(subjectId)
        val assessmentType = o.str("assessmentType").ifBlank { "CAT 1" }
        val termVal = o.str("term").ifBlank { fallbackTerm }
        val id = "${learnerId}_${subjectId}_${termVal}_${assessmentType}".replace(" ", "_")
        add(
          MarkEntry(
            id = id,
            learnerId = learnerId,
            learnerName = o.str("learnerName", "name"),
            admissionNo = o.str("admissionNo"),
            grade = fallbackGrade,
            stream = fallbackStream,
            learningArea = areaName,
            term = termVal,
            assessmentType = assessmentType,
            score = score,
            maxScore = 100.0,
            level = CbcPerformanceLevel.fromScore(score),
            teacherComment = "",
            updatedAt = System.currentTimeMillis(),
            synced = true,
          )
        )
      }
    }
  }

  fun parseAttendance(raw: String, fallbackStream: String, fallbackDay: String): List<AttendanceRecord> {
    val arr = extractArray(raw, "records", "attendance")
    val fallbackGrade = KicdCurriculumRepository.extractGradeFromStream(fallbackStream)
    return buildList {
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val learnerId = o.str("learnerId")
        if (learnerId.isBlank()) continue
        val day = o.str("day", "date").ifBlank { fallbackDay }
        val rawStatus = o.str("status").lowercase()
        val status = when (rawStatus) {
          "present" -> AttendanceStatus.PRESENT
          "absent" -> AttendanceStatus.ABSENT
          "late" -> AttendanceStatus.LATE
          "excused" -> AttendanceStatus.EXCUSED
          else -> if (rawStatus.startsWith("a")) AttendanceStatus.ABSENT else AttendanceStatus.PRESENT
        }
        val id = "${day}_${fallbackStream}_${learnerId}".replace(" ", "_")
        add(
          AttendanceRecord(
            id = id,
            date = day,
            grade = fallbackGrade,
            stream = fallbackStream,
            learningArea = "Class Register",
            lessonSlot = "Daily",
            learnerId = learnerId,
            learnerName = o.str("learnerName", "name"),
            admissionNo = o.str("admissionNo"),
            status = status,
            remarks = "",
            synced = true,
          )
        )
      }
    }
  }

  fun parseObservations(raw: String, fallbackStream: String, fallbackDay: String): List<ObservationNote> {
    val arr = extractArray(raw, "observations")
    val fallbackGrade = KicdCurriculumRepository.extractGradeFromStream(fallbackStream)
    return buildList {
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val learnerId = o.str("learnerId")
        val note = o.str("note")
        if (learnerId.isBlank() && note.isBlank()) continue
        val day = o.str("day", "date").ifBlank { fallbackDay }
        add(
          ObservationNote(
            id = o.str("id").ifBlank { "obs_${learnerId}_${day}_${i}" },
            learnerId = learnerId,
            learnerName = o.str("learnerName", "name"),
            grade = fallbackGrade,
            observationType = "Observation",
            competency = "CBC Observation",
            rating = CbcPerformanceLevel.ME,
            note = note,
            date = day,
            synced = true,
          )
        )
      }
    }
  }

  fun parseSchemes(
    raw: String,
    fallbackStream: String,
    schoolName: String,
    teacherName: String,
    fallbackTerm: String,
  ): List<SchemeOfWork> {
    val arr = extractArray(raw, "rows", "schemes")
    val fallbackGrade = KicdCurriculumRepository.extractGradeFromStream(fallbackStream)
    val rowList = buildList {
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val streamVal = o.str("stream").ifBlank { fallbackStream }
        val gradeVal = KicdCurriculumRepository.extractGradeFromStream(streamVal).ifBlank { fallbackGrade }
        val subjectId = o.str("subjectId", "learningArea")
        val areaName = KicdCurriculumRepository.areaNameForSubjectId(subjectId)
        val termVal = o.str("term").ifBlank { fallbackTerm }
        val row = SchemeRow(
          week = o.intVal("week", default = 1),
          lesson = o.intVal("lesson", default = 1),
          strand = o.str("strand"),
          subStrand = o.str("subStrand"),
          specificOutcomes = o.str("outcomes"),
          keyInquiryQuestion = o.str("inquiry"),
          learningExperiences = o.str("experiences"),
          learningResources = o.str("resources"),
          assessmentMethods = o.str("assessment"),
          reflection = o.str("remarks"),
          remoteId = o.str("id").ifBlank { null },
        )
        add(Triple(Triple(gradeVal, areaName, termVal), streamVal, row))
      }
    }

    return rowList.groupBy { "${it.second}|${it.first.second}|${it.first.third}" }.map { (_, entries) ->
      val (gradeVal, areaName, termVal) = entries.first().first
      val streamVal = entries.first().second
      val keyId = "sch_${streamVal}_${areaName}_${termVal}".lowercase().replace(" ", "_")
      SchemeOfWork(
        id = keyId,
        title = "$streamVal · $areaName ($termVal)",
        schoolName = schoolName,
        teacherName = teacherName,
        grade = gradeVal,
        stream = streamVal,
        learningArea = areaName,
        term = termVal,
        year = 2026,
        rows = entries.map { it.third }.sortedWith(compareBy({ it.week }, { it.lesson })),
        updatedAt = System.currentTimeMillis(),
        synced = true,
      )
    }
  }

  fun parseLessonPlans(
    raw: String,
    schoolName: String,
    teacherName: String,
    fallbackStream: String,
  ): Pair<List<LessonPlanTemplateSection>, List<LessonPlan>> {
    val root = extractObject(raw)
    val templateObj = root.optJSONObject("template")
    val templateArr = templateObj?.optJSONArray("sections") ?: root.optJSONArray("template") ?: JSONArray()
    val template = buildList {
      for (i in 0 until templateArr.length()) {
        val el = templateArr.optJSONObject(i) ?: continue
        val k = el.str("key", "id")
        val label = el.str("label", "title", "name").ifBlank { k }
        if (k.isNotBlank()) add(LessonPlanTemplateSection(k, label))
      }
    }

    val plansArr = root.optJSONArray("plans") ?: extractArray(raw, "plans")
    val plans = buildList {
      for (i in 0 until plansArr.length()) {
        val o = plansArr.optJSONObject(i) ?: continue
        val id = o.str("id").ifBlank { "lp_$i" }
        val streamVal = o.str("stream").ifBlank { fallbackStream }
        val gradeVal = KicdCurriculumRepository.extractGradeFromStream(streamVal)
        val subjectId = o.str("subjectId", "learningArea")
        val areaName = KicdCurriculumRepository.areaNameForSubjectId(subjectId)
        val contentObj = o.optJSONObject("content") ?: JSONObject()
        val contentMap = buildMap {
          val keys = contentObj.keys()
          while (keys.hasNext()) {
            val k = keys.next()
            put(k, contentObj.optString(k, ""))
          }
        }

        fun sec(vararg keys: String): String {
          for (k in keys) {
            contentMap[k]?.takeIf { it.isNotBlank() }?.let { return it }
          }
          return ""
        }

        val subStrandVal = o.str("subStrand", "topic")
        add(
          LessonPlan(
            id = id,
            schoolName = schoolName,
            teacherName = teacherName,
            grade = gradeVal,
            stream = streamVal,
            learningArea = areaName,
            term = o.str("term").ifBlank { "Term 1" },
            week = o.intVal("week", default = 1),
            lessonNumber = o.intVal("lesson", default = 1),
            date = o.str("lessonDate", "day", "date"),
            timeSlot = "",
            rollCount = 0,
            strand = o.str("strand"),
            subStrand = subStrandVal,
            topic = o.str("topic").ifBlank { subStrandVal },
            specificOutcomes = sec("sec1", "outcomes", "specificOutcomes"),
            keyInquiryQuestions = sec("sec2", "inquiry", "keyInquiryQuestions"),
            coreCompetencies = sec("sec3", "competencies", "coreCompetencies"),
            values = sec("values", "sec4"),
            pcis = sec("pcis"),
            learningResources = sec("sec4", "resources", "learningResources"),
            organizationOfLearning = sec("sec5", "organization"),
            introduction = sec("sec6", "introduction", "intro"),
            lessonDevelopment = sec("sec7", "development", "lessonDevelopment"),
            conclusion = sec("sec8", "conclusion"),
            extendedActivity = sec("sec9", "extended"),
            reflection = sec("sec10", "reflection"),
            updatedAt = System.currentTimeMillis(),
            synced = true,
            extraContent = contentMap,
            remoteId = id,
            schemeId = o.str("schemeId"),
          )
        )
      }
    }
    return template to plans
  }

  fun parseRecordWork(
    raw: String,
    fallbackStream: String,
    schoolName: String,
    teacherName: String,
    fallbackTerm: String,
  ): List<RecordOfWork> {
    val arr = extractArray(raw, "rows", "records", "recordwork")
    val fallbackGrade = KicdCurriculumRepository.extractGradeFromStream(fallbackStream)
    return buildList {
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val id = o.str("id").ifBlank { "rw_$i" }
        val streamVal = o.str("stream").ifBlank { fallbackStream }
        val gradeVal = KicdCurriculumRepository.extractGradeFromStream(streamVal).ifBlank { fallbackGrade }
        val subjectId = o.str("subjectId", "learningArea")
        val areaName = KicdCurriculumRepository.areaNameForSubjectId(subjectId)
        val subStrandVal = o.str("subStrand", "topic")
        add(
          RecordOfWork(
            id = id,
            schoolName = schoolName,
            teacherName = o.str("author").ifBlank { teacherName },
            grade = gradeVal,
            stream = streamVal,
            learningArea = areaName,
            term = o.str("term").ifBlank { fallbackTerm },
            year = 2026,
            week = o.intVal("week", default = 1),
            date = o.str("day", "date"),
            strand = o.str("strand"),
            subStrand = subStrandVal,
            workCovered = o.str("workCovered"),
            absencesToday = o.str("absences"),
            homework = o.str("homework"),
            challengesRemarks = o.str("remarks"),
            teacherInitials = (o.str("author").ifBlank { teacherName })
              .split(" ").filter { it.isNotBlank() }.mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString(""),
            updatedAt = System.currentTimeMillis(),
            synced = true,
            topic = o.str("topic").ifBlank { subStrandVal },
            lessonNumber = o.intVal("lesson", default = 1),
            classSize = o.intVal("classSize", default = 0),
            schemeId = o.str("schemeId"),
            lessonPlanId = o.str("lessonPlanId"),
            remoteId = id,
          )
        )
      }
    }
  }

  fun parseProgress(raw: String, fallbackStream: String): List<LearnerProgressEntry> {
    val arr = extractArray(raw, "records", "progress")
    val fallbackGrade = KicdCurriculumRepository.extractGradeFromStream(fallbackStream)
    return buildList {
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val id = o.str("id").ifBlank { "prog_$i" }
        val learnerId = o.str("learnerId")
        val streamVal = o.str("stream").ifBlank { fallbackStream }
        val gradeVal = KicdCurriculumRepository.extractGradeFromStream(streamVal).ifBlank { fallbackGrade }
        val subjectId = o.str("subjectId")
        add(
          LearnerProgressEntry(
            id = id,
            learnerId = learnerId,
            learnerName = o.str("learnerName", "name"),
            grade = gradeVal,
            learningArea = if (subjectId.isBlank()) "General" else KicdCurriculumRepository.areaNameForSubjectId(subjectId),
            stage = o.str("stage").ifBlank { "Developing" },
            note = o.str("note"),
            date = o.str("day", "createdAt"),
            synced = true,
          )
        )
      }
    }
  }

  fun parseTimetable(raw: String): List<TimetableSlot> {
    val root = extractObject(raw)
    val periodsArr = root.optJSONArray("periods")
    val entriesArr = root.optJSONArray("entries")
    if (periodsArr != null && entriesArr != null) {
      data class PeriodMeta(val index: Int, val label: String, val timeRange: String)
      val periodById = mutableMapOf<String, PeriodMeta>()
      for (i in 0 until periodsArr.length()) {
        val p = periodsArr.optJSONObject(i) ?: continue
        val pid = p.str("id")
        val start = p.str("start")
        val end = p.str("end")
        val range = if (start.isNotBlank() && end.isNotBlank()) "$start - $end" else p.str("label")
        periodById[pid] = PeriodMeta(i, p.str("label"), range)
      }
      return buildList {
        for (i in 0 until entriesArr.length()) {
          val e = entriesArr.optJSONObject(i) ?: continue
          val pid = e.str("periodId")
          val meta = periodById[pid]
          val streamName = e.str("stream")
          val grade = KicdCurriculumRepository.extractGradeFromStream(streamName)
          val day = e.str("day").uppercase().take(3).ifBlank { "MON" }
          val subjectId = e.str("subjectId", "subject")
          val area = if (subjectId.isBlank()) meta?.label ?: "BREAK" else KicdCurriculumRepository.areaNameForSubjectId(subjectId)
          val isBreak = area.contains("BREAK", ignoreCase = true) || area.contains("LUNCH", ignoreCase = true)
          add(
            TimetableSlot(
              id = e.str("id").ifBlank { "${grade}_${day}_${meta?.index ?: i}" },
              grade = grade,
              dayOfWeek = day,
              slotIndex = meta?.index ?: i,
              timeRange = meta?.timeRange ?: pid,
              learningArea = area.uppercase(),
              isBreak = isBreak,
              room = streamName,
              teacherId = e.str("teacher"),
            )
          )
        }
      }
    }

    val arr = extractArray(raw, "timetable", "slots")
    return buildList {
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val streamOrGrade = o.str("grade", "stream")
        val grade = KicdCurriculumRepository.extractGradeFromStream(streamOrGrade)
        val day = o.str("dayOfWeek", "day").uppercase().take(3)
        val slotIdx = o.intVal("slotIndex", "period", "slot", default = i)
        val subjectRaw = o.str("learningArea", "subjectId", "subject", "title")
        val areaName = KicdCurriculumRepository.areaNameForSubjectId(subjectRaw)
        add(
          TimetableSlot(
            id = o.str("id").ifBlank { "${grade}_${day}_$slotIdx" },
            grade = grade,
            dayOfWeek = day.ifBlank { "MON" },
            slotIndex = slotIdx,
            timeRange = o.str("timeRange", "time"),
            learningArea = areaName,
            isBreak = o.boolVal("isBreak") || areaName.contains("BREAK", ignoreCase = true),
            room = o.str("room", "stream"),
            teacherId = o.str("teacher", "teacherId"),
          )
        )
      }
    }
  }

  fun parseKicdDesigns(raw: String, grade: String, subjectName: String): List<KicdDesignItem> {
    val arr = extractArray(raw, "designs")
    return buildList {
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val subId = o.str("subjectId", "subject")
        val resolvedArea = if (subId.isNotBlank()) KicdCurriculumRepository.areaNameForSubjectId(subId) else subjectName
        val resolvedGrade = o.str("grade", "level").ifBlank { grade }
        add(
          KicdDesignItem(
            id = o.str("id").ifBlank { "remote_kicd_$i" },
            level = o.str("level").ifBlank { KicdCurriculumRepository.levelForGrade(resolvedGrade) },
            grade = resolvedGrade,
            learningArea = resolvedArea,
            strand = o.str("strand"),
            subStrand = o.str("subStrand"),
            suggestedLessons = o.intVal("suggestedLessons", default = 4),
            specificLearningOutcomes = (o.optJSONArray("outcomes") ?: JSONArray()).toStringList(),
            keyInquiryQuestions = (o.optJSONArray("inquiry") ?: JSONArray()).toStringList(),
            learningExperiences = (o.optJSONArray("experiences") ?: JSONArray()).toStringList(),
            coreCompetencies = (o.optJSONArray("competencies") ?: JSONArray()).toStringList(),
            values = (o.optJSONArray("values") ?: JSONArray()).toStringList(),
            pcis = (o.optJSONArray("standards") ?: JSONArray()).toStringList(),
            suggestedResources = (o.optJSONArray("resources") ?: JSONArray()).toStringList(),
            assessmentRubric = emptyMap(),
            sourceType = o.str("origin").ifBlank { "KICD" }.uppercase(),
            subjectId = subId,
          )
        )
      }
    }
  }

  fun parseResources(raw: String, grade: String, subjectName: String): List<ApprovedResource> {
    val arr = extractArray(raw, "resources")
    return buildList {
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val subId = o.str("subjectId", "subject")
        add(
          ApprovedResource(
            id = o.str("id").ifBlank { "res_$i" },
            title = o.str("title", "name"),
            publisher = o.str("description").ifBlank { o.str("strand") },
            grade = o.str("grade").ifBlank { grade },
            learningArea = if (subId.isNotBlank()) KicdCurriculumRepository.areaNameForSubjectId(subId) else subjectName,
            resourceType = o.str("type", "resourceType").ifBlank { "Resource" },
            kicdApprovalCode = o.str("url", "subStrand"),
          )
        )
      }
    }
  }

  fun parsePayments(raw: String): List<FeePaymentRecord> {
    val arr = extractArray(raw, "payments")
    return buildList {
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        add(
          FeePaymentRecord(
            id = o.str("id").ifBlank { "pay_$i" },
            learnerId = o.str("learnerId"),
            learnerName = o.str("learnerName", "name"),
            amount = o.doubleVal("amount"),
            method = o.str("method").ifBlank { "mpesa" },
            reference = o.str("reference"),
            day = o.str("day", "date"),
            note = o.str("note"),
          )
        )
      }
    }
  }

  fun parseWhatsAppMessages(raw: String): List<GuardianMessage> {
    val arr = extractArray(raw, "messages", "whatsapp")
    return buildList {
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        add(
          GuardianMessage(
            id = o.str("id").ifBlank { "wa_$i" },
            learnerId = o.str("learnerId"),
            learnerName = o.str("learnerName", "name"),
            guardianPhone = o.str("phone"),
            grade = o.str("stream", "grade"),
            template = o.str("status").ifBlank { "SENT" },
            message = o.str("message"),
            date = o.str("createdAt", "day"),
            synced = true,
          )
        )
      }
    }
  }
}

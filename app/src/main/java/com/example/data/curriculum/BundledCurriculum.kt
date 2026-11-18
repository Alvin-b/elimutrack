package com.example.data.curriculum

import android.content.Context
import com.example.domain.model.KicdDesignItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * The complete CBC curriculum library that ships with ElimuTrack.
 *
 * The JSON is the same catalogue the school server imports, so a teacher can
 * browse and build schemes/lesson plans from every grade and learning area
 * even when the device has never managed to reach the server.
 */
object BundledCurriculum {
  private val _designs = MutableStateFlow<List<KicdDesignItem>>(emptyList())
  val designsFlow: StateFlow<List<KicdDesignItem>> = _designs.asStateFlow()

  val designs: List<KicdDesignItem>
    get() = _designs.value

  @Volatile
  private var loadingStarted = false

  fun start(context: Context) {
    if (loadingStarted || _designs.value.isNotEmpty()) return
    synchronized(this) {
      if (loadingStarted || _designs.value.isNotEmpty()) return
      loadingStarted = true
    }
    val appContext = context.applicationContext
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      val loaded = runCatching { readCatalogue(appContext) }.getOrDefault(emptyList())
      _designs.value = loaded
    }
  }

  fun forGrade(grade: String): List<KicdDesignItem> {
    if (grade.isBlank()) return designs
    return designs.filter { it.grade.equals(grade, ignoreCase = true) }
  }

  fun forGradeAndSubject(grade: String, subjectId: String): List<KicdDesignItem> {
    val all = forGrade(grade)
    if (subjectId.isBlank()) return all
    return all.filter { item ->
      KicdCurriculumRepository.subjectIdForDesign(grade, item.subjectId, item.learningArea) == subjectId
    }
  }

  private fun readCatalogue(context: Context): List<KicdDesignItem> {
    val raw = context.assets.open("cbc-curriculum.json").bufferedReader().use { it.readText() }
    val root = JSONArray(raw)
    val result = ArrayList<KicdDesignItem>(root.length())
    for (i in 0 until root.length()) {
      val o = root.optJSONObject(i) ?: continue
      if (o.optString("status").equals("disabled", ignoreCase = true)) continue
      val strand = o.optString("strand").trim()
      val subStrand = o.optString("sub_strand").trim()
      if (strand.isBlank() || subStrand.isBlank()) continue

      val subjectId = o.optString("subject_id").trim()
      val subjectName = o.optString("subject_name").trim()
      val canonicalArea = KicdCurriculumRepository.areaNameForSubjectId(subjectId)
      val learningArea = if (canonicalArea == subjectId && subjectName.isNotBlank()) subjectName else canonicalArea
      val rawId = o.optString("id").trim().ifBlank { "row_$i" }
      val origin = o.optString("origin").trim().ifBlank { "kicd" }

      result += KicdDesignItem(
        id = "bundled_$rawId",
        level = o.optString("level").trim().ifBlank {
          KicdCurriculumRepository.levelForGrade(o.optString("grade"))
        },
        grade = o.optString("grade").trim(),
        learningArea = learningArea,
        strand = strand,
        subStrand = subStrand,
        suggestedLessons = o.optInt("suggested_lessons", 1).coerceAtLeast(1),
        specificLearningOutcomes = stringList(o, "outcomes"),
        keyInquiryQuestions = stringList(o, "inquiry"),
        learningExperiences = stringList(o, "experiences"),
        coreCompetencies = stringList(o, "competencies"),
        values = stringList(o, "values"),
        pcis = stringList(o, "standards"),
        suggestedResources = stringList(o, "resources"),
        assessmentRubric = emptyMap(),
        sourceType = when (origin.lowercase()) {
          "kicd" -> "KICD"
          "school" -> "School"
          "sample" -> "Sample"
          else -> origin.uppercase()
        },
        subjectId = subjectId,
      )
    }
    return result
  }

  /**
   * The catalogue stores most lists as JSON-encoded strings, but a few rows
   * contain real JSON arrays. Accept both so no template is dropped.
   */
  private fun stringList(o: JSONObject, key: String): List<String> {
    val value = o.opt(key) ?: return emptyList()
    val array = when (value) {
      is JSONArray -> value
      is String -> runCatching { JSONArray(value) }.getOrNull()
      else -> null
    }
    if (array == null) {
      val text = value.toString().trim()
      return if (text.isBlank() || text == "[]" || text == "null") emptyList() else listOf(text)
    }
    return buildList {
      for (i in 0 until array.length()) {
        val item = array.opt(i)?.toString()?.trim().orEmpty()
        if (item.isNotBlank() && item != "null") add(item)
      }
    }
  }
}
